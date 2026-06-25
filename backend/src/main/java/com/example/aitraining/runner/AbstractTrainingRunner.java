package com.example.aitraining.runner;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Enums.SourceType;
import com.example.aitraining.domain.Enums.StreamType;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.realtime.JobStreamWebSocketHandler;
import com.example.aitraining.repo.ConfigRepository;
import com.example.aitraining.repo.JobQueueRepository;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.repo.SupportRepository;
import com.example.aitraining.repo.UserRepository;
import com.example.aitraining.service.ArtifactService;
import com.example.aitraining.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Template Method Pattern</b> — defines the fixed lifecycle skeleton for every training job
 * execution, delegating the engine-specific step to subclasses via {@link #execute}.
 *
 * <h2>Lifecycle (in order)</h2>
 * <ol>
 *   <li>Create and validate the job workspace on disk.</li>
 *   <li>Assert that at least {@code app.docker.min-disk-bytes} of free space is available.</li>
 *   <li>Load the immutable config snapshot and write it to the workspace.</li>
 *   <li>Prepare the project source tree (git clone or ZIP mount).</li>
 *   <li>Publish a {@code STATUS_CHANGE / RUNNING} WebSocket event.</li>
 *   <li>Call {@link #execute} — the hook subclasses must implement.</li>
 *   <li>Transition the job to {@code SUCCESS} or {@code FAILED}, collect artifacts, refresh the
 *       queue, publish a terminal WebSocket event, and send an email notification.</li>
 *   <li>Delete the workspace directory (always, even on failure).</li>
 * </ol>
 *
 * <p>Subclasses are <em>not</em> expected to interact with the database, the queue, or
 * notifications — those concerns are owned by this base class.  The only responsibility of a
 * subclass is to run the training process and return {@code true} on success.
 *
 * @see DockerTrainingRunner
 * @see TrainingRunner
 */
public abstract class AbstractTrainingRunner implements TrainingRunner {
  private static final Logger log = LoggerFactory.getLogger(AbstractTrainingRunner.class);

  /** Repositories and services injected once and shared with subclasses. */
  protected final JobRepository jobs;
  protected final JobQueueRepository queue;
  protected final ConfigRepository configs;
  protected final SupportRepository support;
  protected final UserRepository users;
  protected final ArtifactService artifacts;
  protected final NotificationService notifications;
  protected final JobStreamWebSocketHandler ws;
  protected final AppProperties props;

  /**
   * Constructs the runner with all shared infrastructure dependencies.
   * Subclass constructors must call {@code super(...)} with the full set.
   */
  protected AbstractTrainingRunner(JobRepository jobs, JobQueueRepository queue, ConfigRepository configs,
      SupportRepository support, UserRepository users, ArtifactService artifacts,
      NotificationService notifications, JobStreamWebSocketHandler ws, AppProperties props) {
    this.jobs = jobs;
    this.queue = queue;
    this.configs = configs;
    this.support = support;
    this.users = users;
    this.artifacts = artifacts;
    this.notifications = notifications;
    this.ws = ws;
    this.props = props;
  }

  /**
   * <b>Template method</b> — sealed lifecycle that all runner implementations share.
   * Subclasses may not override this; they must implement {@link #execute} instead.
   *
   * @param job     the training job to run; must be in {@code RUNNING} state when called
   * @param project the owning project, used for source resolution and notification delivery
   */
  @Override
  public final void run(TrainingJob job, Project project) {
    UUID jobId = job.jobId();
    Path workspace = Path.of(props.docker().workspaceRoot()).resolve(jobId.toString()).toAbsolutePath();

    try {
      Files.createDirectories(workspace);

      long freeBytes = Files.getFileStore(workspace).getUsableSpace();
      if (freeBytes < props.docker().minDiskBytes()) {
        String reason = String.format("Insufficient disk space: %d MB free, need %d MB",
            freeBytes / (1024 * 1024), props.docker().minDiskBytes() / (1024 * 1024));
        failPreLaunch(job, project, reason);
        return;
      }

      String configYaml = configs.getSnapshotYaml(job.configSnapshotId());
      Files.writeString(workspace.resolve("config.yaml"), configYaml);

      Path sourcePath = prepareSource(project);
      String entrypoint = parseEntrypoint(configYaml, project.trainingEntrypoint());

      ws.publish(jobId, "STATUS_CHANGE", Map.of("status", "RUNNING"));

      boolean success = execute(job, workspace, sourcePath, entrypoint);

      JobStatus terminalStatus = success ? JobStatus.SUCCESS : JobStatus.FAILED;
      String failureReason = success ? null : "Training process exited with non-zero status";
      TrainingJob terminal = jobs.transitionToTerminal(jobId, terminalStatus, failureReason);

      if (terminal == null) {
        log.info("Job {} was cancelled externally during execution — skipping finalization", jobId);
        return;
      }

      if (success) {
        artifacts.collectFromWorkspace(terminal, workspace);
      }

      queue.refreshPositions();
      ws.publish(jobId, "STATUS_CHANGE", Map.of("status", terminalStatus.name()));

      User owner = users.get(project.ownerUserId());
      notifications.notifyJobTerminal(terminal, project, owner.email());

    } catch (Exception e) {
      log.error("Unexpected error in runner for job {}: {}", jobId, e.getMessage(), e);
      TrainingJob failed = jobs.transitionToTerminal(jobId, JobStatus.FAILED, e.getMessage());
      if (failed != null) {
        queue.refreshPositions();
        ws.publish(jobId, "STATUS_CHANGE", Map.of("status", "FAILED"));
        try {
          User owner = users.get(project.ownerUserId());
          notifications.notifyJobTerminal(failed, project, owner.email());
        } catch (Exception ignored) {}
      }
    } finally {
      deleteQuietly(workspace);
    }
  }

  /**
   * <b>Hook method</b> — subclasses implement the engine-specific execution step.
   *
   * <p>Called after the workspace is set up, the config snapshot has been written, and the
   * project source is ready.  The implementation is responsible for running the training
   * process and streaming its output via {@link #logLine}.
   *
   * @param job        the training job descriptor (read-only during execution)
   * @param workspace  the per-job scratch directory (writable); the runner writes any
   *                   output artifacts here under an {@code output/} subdirectory
   * @param sourcePath path to the project source tree, or {@code null} if unavailable
   * @param entrypoint the training command parsed from the config snapshot
   * @return {@code true} if the training process completed successfully, {@code false} on failure
   * @throws Exception on unrecoverable errors (I/O failures, process errors, etc.)
   */
  protected abstract boolean execute(TrainingJob job, Path workspace, Path sourcePath, String entrypoint)
      throws Exception;

  /**
   * Persists a log line to {@code job_log_events} and pushes it to all connected WebSocket
   * clients in real time.  Also parses the line for progress markers and, when one is found,
   * persists a {@code job_progress_events} entry and publishes a {@code PROGRESS} WebSocket event.
   *
   * <p>Subclasses call this method for every line they read from the training process.
   *
   * @param jobId      the job whose log this line belongs to
   * @param streamType {@code STDOUT} or {@code STDERR}
   * @param line       a single output line (without a trailing newline)
   * @param seqNo      monotonically increasing sequence number for ordered replay
   */
  protected void logLine(UUID jobId, StreamType streamType, String line, int seqNo) {
    support.appendLog(jobId, streamType, line, seqNo);
    ws.publish(jobId, "LOG", Map.of("message", line, "streamType", streamType.name(), "sequenceNo", seqNo));
    ProgressParser.parse(line).ifPresent(p -> {
      support.appendProgress(jobId, p.value(), p.epoch(), p.totalEpoch());
      ws.publish(jobId, "PROGRESS", Map.of("value", p.value(), "epoch", p.epoch(), "totalEpoch", p.totalEpoch()));
    });
  }

  // ── Shared helpers ────────────────────────────────────────────────────────────────────────────

  /**
   * Prepares the project source tree.
   * <ul>
   *   <li>{@code GITHUB} — shallow-clones the repository to
   *       {@code app.docker.sources-root/{projectId}/} (skipped if already present).</li>
   *   <li>{@code ZIP} — resolves {@code localSourcePath} under {@code app.storage-root}.</li>
   * </ul>
   *
   * @return path to the source directory, or {@code null} if no source is available
   */
  protected Path prepareSource(Project project) throws IOException, InterruptedException {
    if (project.sourceType() == SourceType.GITHUB && project.repositoryUrl() != null) {
      Path sourceDir = Path.of(props.docker().sourcesRoot())
          .resolve(project.projectId().toString()).toAbsolutePath();
      if (!Files.exists(sourceDir)) {
        Files.createDirectories(sourceDir);
        log.info("Cloning {} for project {}", project.repositoryUrl(), project.projectId());
        Process p = new ProcessBuilder("git", "clone", "--depth=1",
            project.repositoryUrl(), sourceDir.toString())
            .redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor() != 0) {
          throw new IOException("git clone failed for " + project.repositoryUrl() + ": " + out);
        }
      }
      return sourceDir;
    }
    if (project.sourceType() == SourceType.ZIP && project.localSourcePath() != null) {
      Path sourceDir = Path.of(props.storageRoot()).resolve(project.localSourcePath()).toAbsolutePath();
      return Files.exists(sourceDir) ? sourceDir : null;
    }
    return null;
  }

  /**
   * Parses {@code trainingEntrypoint} from the YAML config snapshot.
   * Falls back to the project-level entrypoint, or a safe no-op command if neither is set.
   */
  @SuppressWarnings("unchecked")
  protected String parseEntrypoint(String configYaml, String fallback) {
    try {
      Object parsed = new Yaml().load(configYaml);
      if (parsed instanceof Map<?, ?> map) {
        Object ep = ((Map<String, Object>) map).get("trainingEntrypoint");
        if (ep instanceof String s && !s.isBlank()) {
          return s;
        }
      }
    } catch (Exception ignored) {}
    return fallback != null ? fallback : "echo 'No entrypoint configured'";
  }

  /**
   * Transitions the job to FAILED before it even launched and publishes appropriate events.
   * Used for pre-flight failures (disk space, workspace creation).
   */
  private void failPreLaunch(TrainingJob job, Project project, String reason) {
    log.warn("Job {} failed before launch: {}", job.jobId(), reason);
    TrainingJob failed = jobs.transitionToTerminal(job.jobId(), JobStatus.FAILED, reason);
    if (failed != null) {
      queue.refreshPositions();
      ws.publish(job.jobId(), "STATUS_CHANGE", Map.of("status", "FAILED", "reason", reason));
      try {
        User owner = users.get(project.ownerUserId());
        notifications.notifyJobTerminal(failed, project, owner.email());
      } catch (Exception ignored) {}
    }
  }

  /**
   * Recursively deletes the workspace directory, suppressing all errors.
   * Called in the {@code finally} block so the job never leaks workspace files.
   */
  private void deleteQuietly(Path dir) {
    try {
      if (!Files.exists(dir)) return;
      Files.walk(dir)
          .sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try { Files.delete(p); } catch (IOException ignored) {}
          });
    } catch (Exception e) {
      log.warn("Failed to clean up workspace {}: {}", dir, e.getMessage());
    }
  }
}
