package com.example.aitraining.service;

import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.repo.JobQueueRepository;
import com.example.aitraining.repo.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * <b>Service Layer Pattern</b> — startup reconciler that recovers orphaned jobs left
 * in {@code RUNNING} state when the server last stopped (NFR-REL-003, NFR-REL-004).
 *
 * <p>Implements {@link ApplicationRunner} so it executes <em>once</em> immediately after the
 * application context is ready, before the scheduled dispatcher starts dispatching.
 *
 * <p>Each RUNNING job is checked via {@code docker inspect}: if no container is alive the
 * job is marked {@code RETRYING} and placed back on the queue; if a container is still
 * alive the job is left as {@code RUNNING} so an in-progress training is not interrupted.
 */
@Service
public class JobReconcilerService implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(JobReconcilerService.class);

  private final JobRepository jobs;
  private final JobQueueRepository queue;

  public JobReconcilerService(JobRepository jobs, JobQueueRepository queue) {
    this.jobs = jobs;
    this.queue = queue;
  }

  /**
   * Invoked once at startup.  Scans for orphaned RUNNING jobs and re-queues those whose
   * Docker container is no longer alive.
   */
  @Override
  public void run(ApplicationArguments args) {
    List<TrainingJob> running = jobs.findByStatus(JobStatus.RUNNING);
    if (running.isEmpty()) {
      return;
    }
    log.warn("Found {} orphaned RUNNING job(s) from previous run — checking liveness", running.size());
    for (TrainingJob job : running) {
      if (!isContainerAlive(job.jobId())) {
        log.warn("Job {} has no live container — marking RETRYING and re-enqueueing", job.jobId());
        jobs.transitionToRetrying(job.jobId());
        queue.enqueue(job.jobId());
      } else {
        log.info("Job {} container still alive — leaving as RUNNING", job.jobId());
      }
    }
    queue.refreshPositions();
  }

  private boolean isContainerAlive(UUID jobId) {
    try {
      Process p = new ProcessBuilder("docker", "inspect", "--format={{.State.Running}}", "job-" + jobId)
          .redirectErrorStream(true)
          .start();
      String output = new String(p.getInputStream().readAllBytes()).trim();
      return "true".equals(output) && p.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }
}
