package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.repo.JobQueueRepository;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.repo.ProjectRepository;
import com.example.aitraining.runner.TrainingRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * <b>Service Layer Pattern</b> — polls the job queue every 2 seconds and dispatches ready
 * jobs to the {@link TrainingRunner} (NFR-PERF-006).
 *
 * <p>The atomic MongoDB {@code findAndModify} claim ({@code WAITING→DISPATCHED}) prevents
 * duplicate dispatch even when concurrent invocations of {@link #dispatch} overlap.
 * Database state is persisted before any WebSocket event is sent (NFR-REL-005).
 *
 * <p>{@code @EnableScheduling} is declared on
 * {@link com.example.aitraining.AiTrainingBackendApplication}.
 */
@Service
public class JobDispatcherService {
  private static final Logger log = LoggerFactory.getLogger(JobDispatcherService.class);

  private final JobRepository jobs;
  private final JobQueueRepository queue;
  private final ProjectRepository projects;
  private final TrainingRunner runner;
  private final AppProperties props;
  private final TaskExecutor trainingExecutor;

  public JobDispatcherService(JobRepository jobs, JobQueueRepository queue, ProjectRepository projects,
      TrainingRunner runner, AppProperties props,
      @Qualifier("trainingExecutor") TaskExecutor trainingExecutor) {
    this.jobs = jobs;
    this.queue = queue;
    this.projects = projects;
    this.runner = runner;
    this.props = props;
    this.trainingExecutor = trainingExecutor;
  }

  /**
   * Scheduled dispatcher — claims and launches as many waiting jobs as the running-limit allows.
   *
   * <p>Called every 2 seconds (fixed delay, not fixed rate) via Spring's task scheduler.
   * Each iteration loops until either the queue is empty or the running-limit is reached.
   * All unhandled exceptions are caught and logged so that a transient error (e.g. a network
   * hiccup to MongoDB) does not silently kill the scheduler thread.
   */
  @Scheduled(fixedDelay = 2000)
  public void dispatch() {
    try {
      while (queue.runningCount() < props.queue().runningLimit()) {
        UUID jobId = queue.claimNext();
        if (jobId == null) {
          break;
        }

        TrainingJob job = jobs.transitionToRunning(jobId);
        if (job == null) {
          log.warn("Job {} could not be transitioned to RUNNING (cancelled before dispatch)", jobId);
          queue.refreshPositions();
          continue;
        }

        Project project = projects.get(job.projectId());
        queue.refreshPositions();

        log.info("Dispatching job {} (project={})", jobId, project.projectName());
        final TrainingJob dispatchedJob = job;
        final Project dispatchedProject = project;
        trainingExecutor.execute(() -> {
          try {
            runner.run(dispatchedJob, dispatchedProject);
          } catch (Exception e) {
            log.error("Unhandled error in training runner for job {}: {}", dispatchedJob.jobId(), e.getMessage(), e);
          }
        });
      }
    } catch (Exception e) {
      log.error("Error in job dispatcher: {}", e.getMessage(), e);
    }
  }
}
