package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.ProjectConfig;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.dto.CommonDtos.*;
import com.example.aitraining.dto.JobDtos.*;
import com.example.aitraining.repo.ConfigRepository;
import com.example.aitraining.repo.JobQueueRepository;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.repo.SupportRepository;
import com.example.aitraining.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * <b>Service Layer Pattern</b> — orchestrates the training-job lifecycle for user-facing
 * operations: starting, listing, viewing, cancelling, and retrying jobs.
 *
 * <p>The execution-phase transitions (QUEUED → RUNNING → SUCCESS/FAILED) are handled by
 * {@link JobDispatcherService} and {@link com.example.aitraining.runner.AbstractTrainingRunner}.
 * This service owns only the operations exposed through the REST API.
 *
 * <p>Every mutating operation writes an audit entry via {@link com.example.aitraining.repo.SupportRepository}.
 */
@Service
public class JobService {
  private final JobRepository jobs;
  private final JobQueueRepository queue;
  private final ConfigRepository configs;
  private final UserRepository users;
  private final SupportRepository support;
  private final AppProperties props;

  public JobService(JobRepository jobs, JobQueueRepository queue, ConfigRepository configs, UserRepository users,
      SupportRepository support, AppProperties props) {
    this.jobs = jobs;
    this.queue = queue;
    this.configs = configs;
    this.users = users;
    this.support = support;
    this.props = props;
  }

  /**
   * Snapshots the chosen config and places a new job on the queue.
   *
   * <p>If {@code request.yamlContent()} is non-blank it overrides the stored config YAML for
   * this run without persisting the change to {@code project_configs}.
   *
   * @return the job in {@code QUEUED} state with its initial queue position
   */
  public StartJobResponse start(User user, Project project, StartJobRequest request) {
    ProjectConfig config = configs.get(project.projectId(), request.configId());
    String yaml = request.yamlContent() == null || request.yamlContent().isBlank()
        ? config.yamlContent()
        : request.yamlContent();
    UUID snapshotId = configs.createSnapshot(project.projectId(), config.configId(), yaml);
    TrainingJob job = jobs.create(project.projectId(), user.userId(), snapshotId, null, 0);
    queue.enqueue(job.jobId());
    TrainingJob queued = jobs.get(job.jobId());
    support.audit(user.userId(), project.projectId(), job.jobId(), "JOB_STARTED", "TRAINING_JOB",
        job.jobId().toString());
    return new StartJobResponse(queued.jobId(), queued.projectId(), queued.status(), queued.queuePosition(),
        queued.configSnapshotId(), queued.createdAt());
  }

  /**
   * Returns the most recent jobs for the project, optionally filtered by status.
   *
   * @param status {@code null} to return all statuses
   * @param limit  maximum number of results (applied per-query, not paginated)
   */
  public JobPage list(Project project, JobStatus status, int limit) {
    List<JobDetail> data = jobs.listByProject(project.projectId(), status, limit).stream()
        .map(job -> detail(job, project))
        .toList();
    return new JobPage(data, new Page(limit, null, false));
  }

  public JobDetail detail(TrainingJob job, Project project) {
    User triggeredBy = users.get(job.triggeredByUserId());
    return new JobDetail(job.jobId(), job.projectId(), project.projectName(),
        new UserSummary(triggeredBy.userId(), triggeredBy.email(), triggeredBy.fullName()),
        job.status(), job.queuePosition(), jobs.latestProgress(job.jobId()), job.retryOfJobId(),
        job.retryAttempt(), job.createdAt(), job.queuedAt(), job.startedAt(), job.endedAt(), job.failureReason());
  }

  /**
   * Cancels the job.  Throws {@link IllegalStateException} if the job is already in a terminal
   * state; the caller is expected to propagate this as HTTP 409.
   */
  public CancelJobResponse cancel(User user, Project project, TrainingJob job, String reason) {
    if (job.terminal()) {
      throw new IllegalStateException("Terminal jobs cannot be cancelled");
    }
    TrainingJob cancelled = jobs.cancel(job.jobId(), reason);
    queue.refreshPositions();
    support.audit(user.userId(), project.projectId(), job.jobId(), "JOB_CANCELLED", "TRAINING_JOB",
        job.jobId().toString());
    return new CancelJobResponse(cancelled.jobId(), cancelled.status(), cancelled.endedAt());
  }

  /**
   * Creates a new job that retries a previously failed or cancelled one.
   * Only {@code FAILED} or {@code CANCELLED} jobs may be retried; throws
   * {@link IllegalStateException} otherwise.
   *
   * @param original the job to retry; must be in a retriable terminal state
   * @param request  optional override YAML; when blank the original config is reused
   */
  public RetryJobResponse retry(User user, Project project, TrainingJob original, RetryJobRequest request) {
    if (original.status() != JobStatus.FAILED && original.status() != JobStatus.CANCELLED) {
      throw new IllegalStateException("Only failed or cancelled jobs can be retried");
    }
    UUID snapshotId = configs.createSnapshot(project.projectId(), null,
        request.yamlContent() == null || request.yamlContent().isBlank() ? "retryOf: " + original.jobId()
            : request.yamlContent());
    TrainingJob retry = jobs.create(project.projectId(), user.userId(), snapshotId, original.jobId(),
        original.retryAttempt() + 1);
    queue.enqueue(retry.jobId());
    TrainingJob queued = jobs.get(retry.jobId());
    support.audit(user.userId(), project.projectId(), retry.jobId(), "JOB_RETRIED", "TRAINING_JOB",
        retry.jobId().toString());
    return new RetryJobResponse(original.jobId(), queued.jobId(), queued.status(), queued.queuePosition());
  }

  public QueueSnapshot queueSnapshot() {
    return new QueueSnapshot(queue.runningCount(), props.queue().runningLimit(), queue.queuedCount(), queue.items());
  }
}
