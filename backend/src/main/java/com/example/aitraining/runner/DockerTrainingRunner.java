package com.example.aitraining.runner;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.domain.Enums.StreamType;
import com.example.aitraining.domain.Models.TrainingJob;
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
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <b>Concrete Strategy</b> in the Strategy pattern — executes training jobs inside Docker
 * containers (NFR-COMP-004).
 *
 * <p>This class extends {@link AbstractTrainingRunner} and is responsible solely for the
 * engine-specific step: building the {@code docker run} command, launching the container,
 * and forwarding its stdout/stderr to {@link #logLine} line by line.  All lifecycle
 * orchestration (workspace setup, artifact collection, notifications, cleanup) is inherited
 * from the base class.
 *
 * <h2>Container naming convention</h2>
 * <p>Every container is named {@code job-{jobId}} so the reconciler ({@link
 * com.example.aitraining.service.JobReconcilerService}) can check liveness via
 * {@code docker inspect} after a server restart.
 *
 * <h2>Future extensibility</h2>
 * <p>Swapping to Kubernetes requires only a new subclass of {@link AbstractTrainingRunner}
 * that overrides {@link #execute}; no dispatcher or lifecycle code needs to change.
 *
 * @see AbstractTrainingRunner  Template Method base class
 * @see TrainingRunner          Strategy interface
 */
@Component
public class DockerTrainingRunner extends AbstractTrainingRunner {
  private static final Logger log = LoggerFactory.getLogger(DockerTrainingRunner.class);

  /**
   * Wires all shared infrastructure into the abstract base class.
   */
  public DockerTrainingRunner(JobRepository jobs, JobQueueRepository queue, ConfigRepository configs,
      SupportRepository support, UserRepository users, ArtifactService artifacts,
      NotificationService notifications, JobStreamWebSocketHandler ws, AppProperties props) {
    super(jobs, queue, configs, support, users, artifacts, notifications, ws, props);
  }

  /**
   * Engine-specific hook — launches a Docker container for the training job, streams its
   * output line by line, and returns the container's exit status.
   *
   * <p>The container receives two volume mounts:
   * <ul>
   *   <li>{@code workspace} → {@code /workspace} (read/write; training output goes here)</li>
   *   <li>{@code sourcePath} → {@code /source:ro} (read-only; the project's source tree)</li>
   * </ul>
   *
   * <p>stdout and stderr are streamed concurrently on Java virtual threads so neither blocks
   * the other.  Each line is persisted to {@code job_log_events} and pushed to WebSocket
   * clients via {@link #logLine}.
   *
   * @param job        the training job descriptor
   * @param workspace  per-job scratch directory already created by the base class
   * @param sourcePath project source tree, or {@code null} if unavailable
   * @param entrypoint the shell command to execute inside the container
   * @return {@code true} if the container exited with code 0, {@code false} otherwise
   */
  @Override
  protected boolean execute(TrainingJob job, Path workspace, Path sourcePath, String entrypoint)
      throws IOException, InterruptedException {
    List<String> cmd = buildDockerCommand(job.jobId(), workspace, sourcePath, entrypoint);
    log.info("Starting Docker container for job {}: {}", job.jobId(), String.join(" ", cmd));

    ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(false);
    Process process = pb.start();

    AtomicInteger seqNo = new AtomicInteger(0);

    Thread stdoutThread = Thread.ofVirtual().start(() ->
        streamLines(process.inputReader(), job.jobId(), StreamType.STDOUT, seqNo));
    Thread stderrThread = Thread.ofVirtual().start(() ->
        streamLines(new BufferedReader(new InputStreamReader(process.getErrorStream())),
            job.jobId(), StreamType.STDERR, seqNo));

    stdoutThread.join();
    stderrThread.join();
    return process.waitFor() == 0;
  }

  // ── Private helpers ───────────────────────────────────────────────────────────────────────────

  /**
   * Reads lines from the given reader until EOF, forwarding each to {@link #logLine}.
   * Runs on a virtual thread so stdout and stderr can be consumed concurrently.
   */
  private void streamLines(BufferedReader reader, UUID jobId, StreamType streamType, AtomicInteger seqNo) {
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        logLine(jobId, streamType, line, seqNo.incrementAndGet());
      }
    } catch (IOException e) {
      log.warn("Stream read error for job {} ({}): {}", jobId, streamType, e.getMessage());
    }
  }

  /**
   * Assembles the {@code docker run} argument list.
   *
   * <p>If {@code sourcePath} is present and exists on disk it is mounted read-only as
   * {@code /source} and the entrypoint is prefixed with {@code cd /source &&}.
   */
  private List<String> buildDockerCommand(UUID jobId, Path workspace, Path sourcePath, String entrypoint) {
    List<String> cmd = new ArrayList<>(List.of(
        "docker", "run", "--rm",
        "--name", "job-" + jobId,
        "-v", workspace.toAbsolutePath() + ":/workspace"));

    if (sourcePath != null && Files.exists(sourcePath)) {
      cmd.addAll(List.of("-v", sourcePath.toAbsolutePath() + ":/source:ro"));
      cmd.addAll(List.of(props.docker().image(), "sh", "-c", "cd /source && " + entrypoint));
    } else {
      cmd.addAll(List.of(props.docker().image(), "sh", "-c", entrypoint));
    }
    return cmd;
  }
}
