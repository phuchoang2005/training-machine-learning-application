package com.phuchoang2005.aitraining.service;

import com.phuchoang2005.aitraining.domain.Enums.JobStatus;
import com.phuchoang2005.aitraining.domain.Models.Project;
import com.phuchoang2005.aitraining.domain.Models.TrainingJob;
import com.phuchoang2005.aitraining.repo.JobQueueRepository;
import com.phuchoang2005.aitraining.repo.JobRepository;
import com.phuchoang2005.aitraining.repo.ProjectRepository;
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
  private final ProjectRepository projects;

  public JobReconcilerService(JobRepository jobs, JobQueueRepository queue, ProjectRepository projects) {
    this.jobs = jobs;
    this.queue = queue;
    this.projects = projects;
  }

  /**
   * Invoked once at startup.  Scans for orphaned RUNNING jobs and re-queues those whose
   * Docker container is no longer alive.
   */
  @Override
  public void run(ApplicationArguments args) {
    reconcileStuckBuilds();
    reconcileOrphanedJobs();
  }

  /**
   * Recovers projects left {@code BUILDING} when the server last stopped. The image build runs as a
   * JVM child process, so it died with the previous run and will never finalize — mark such projects
   * {@code FAILED} so the dashboard stops showing a perpetual spinner and the user can delete/re-register.
   */
  private void reconcileStuckBuilds() {
    List<Project> building = projects.findByBuildStatus("BUILDING");
    if (building.isEmpty()) {
      return;
    }
    log.warn("Found {} project(s) stuck in BUILDING from previous run — marking FAILED", building.size());
    for (Project project : building) {
      projects.markBuildFailed(project.projectId(),
          "Image build did not complete: the server restarted before the build finished.");
    }
  }

  /** Scans for orphaned RUNNING jobs and re-queues those whose Docker container is no longer alive. */
  private void reconcileOrphanedJobs() {
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
