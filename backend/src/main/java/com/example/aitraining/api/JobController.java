package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.dto.JobDtos.*;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.service.AuthorizationService;
import com.example.aitraining.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Training job lifecycle endpoints.
 *
 * <p>Routes (all under {@code /api/v1}):
 * <ul>
 *   <li>{@code GET  /projects/{projectId}/jobs} — list jobs; visible to owner and admin.</li>
 *   <li>{@code POST /projects/{projectId}/jobs} — start a new job; owner only.</li>
 *   <li>{@code GET  /jobs/{jobId}} — get job detail; visible to owner and admin.</li>
 *   <li>{@code POST /jobs/{jobId}/cancel} — cancel a job; admin may cancel any, owner only for own.</li>
 *   <li>{@code POST /jobs/{jobId}/retry} — retry a failed/cancelled job; owner only.</li>
 *   <li>{@code GET  /jobs/queue} — queue snapshot; admin only.</li>
 * </ul>
 */
@RestController
public class JobController {
  private final AuthorizationService authorization;
  private final JobService service;
  private final JobRepository jobs;

  public JobController(AuthorizationService authorization, JobService service, JobRepository jobs) {
    this.authorization = authorization;
    this.service = service;
    this.jobs = jobs;
  }

  @GetMapping("/projects/{projectId}/jobs")
  JobPage list(@PathVariable UUID projectId,
      @RequestParam(required = false) JobStatus status,
      @RequestParam(defaultValue = "50") int limit) {
    var project = authorization.requireProjectVisible(CurrentUserContext.require(), projectId);
    return service.list(project, status, limit);
  }

  @PostMapping("/projects/{projectId}/jobs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  StartJobResponse start(@PathVariable UUID projectId, @Valid @RequestBody StartJobRequest request) {
    var user = CurrentUserContext.require();
    var project = authorization.requireProjectOwner(user, projectId);
    return service.start(user, project, request);
  }

  @GetMapping("/jobs/{jobId}")
  JobDetail get(@PathVariable UUID jobId) {
    var user = CurrentUserContext.require();
    var job = jobs.get(jobId);
    var project = authorization.requireJobVisible(user, job);
    return service.detail(job, project);
  }

  @PostMapping("/jobs/{jobId}/cancel")
  @ResponseStatus(HttpStatus.ACCEPTED)
  CancelJobResponse cancel(@PathVariable UUID jobId, @RequestBody(required = false) CancelJobRequest request) {
    var user = CurrentUserContext.require();
    var job = jobs.get(jobId);
    var project = user.isAdmin() ? authorization.requireJobVisible(user, job)
        : authorization.requireSensitiveJobOwner(user, job);
    return service.cancel(user, project, job, request == null ? null : request.reason());
  }

  @PostMapping("/jobs/{jobId}/retry")
  @ResponseStatus(HttpStatus.ACCEPTED)
  RetryJobResponse retry(@PathVariable UUID jobId, @Valid @RequestBody RetryJobRequest request) {
    var user = CurrentUserContext.require();
    var job = jobs.get(jobId);
    var project = authorization.requireSensitiveJobOwner(user, job);
    return service.retry(user, project, job, request);
  }

  @GetMapping("/jobs/queue")
  QueueSnapshot queue() {
    authorization.requireAdmin(CurrentUserContext.require());
    return service.queueSnapshot();
  }
}
