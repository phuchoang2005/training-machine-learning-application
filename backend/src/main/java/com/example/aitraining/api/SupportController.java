package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.dto.CommonDtos.*;
import com.example.aitraining.dto.SupportDtos.*;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.repo.SupportRepository;
import com.example.aitraining.service.AuthorizationService;
import com.example.aitraining.service.DownloadService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Support data endpoints: logs, artifacts, and audit logs.
 *
 * <p>All log and artifact endpoints require project ownership — admins are explicitly excluded
 * from sensitive data access (NFR-SEC-005).  Artifact downloads are additionally audited
 * (BA §21).
 *
 * <p>Routes (all under {@code /api/v1}):
 * <ul>
 *   <li>{@code GET /jobs/{jobId}/logs} — paginated log lines; owner only.</li>
 *   <li>{@code GET /jobs/{jobId}/logs/download} — full log as a text attachment; owner only.</li>
 *   <li>{@code GET /jobs/{jobId}/artifacts} — artifact metadata list; owner only.</li>
 *   <li>{@code GET /artifacts/{artifactId}/download} — download an artifact file; owner only.</li>
 *   <li>{@code GET /audit-logs} — audit trail; admin sees all, regular users see own actions.</li>
 * </ul>
 */
@RestController
public class SupportController {
  private final JobRepository jobs;
  private final SupportRepository support;
  private final AuthorizationService authorization;
  private final DownloadService downloads;

  public SupportController(JobRepository jobs, SupportRepository support, AuthorizationService authorization,
      DownloadService downloads) {
    this.jobs = jobs;
    this.support = support;
    this.authorization = authorization;
    this.downloads = downloads;
  }

  @GetMapping("/jobs/{jobId}/logs")
  LogEventPage logs(@PathVariable UUID jobId, @RequestParam(defaultValue = "200") int limit) {
    var job = jobs.get(jobId);
    authorization.requireSensitiveJobOwner(CurrentUserContext.require(), job);
    return new LogEventPage(support.logs(jobId, limit), new Page(limit, null, false));
  }

  @GetMapping("/jobs/{jobId}/logs/download")
  ResponseEntity<String> downloadLogs(@PathVariable UUID jobId) {
    var job = jobs.get(jobId);
    authorization.requireSensitiveJobOwner(CurrentUserContext.require(), job);
    String body = String.join("\n", support.logs(jobId, 10000).stream().map(LogEventResponse::message).toList());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"job-" + jobId + ".log\"")
        .body(body);
  }

  @GetMapping("/jobs/{jobId}/artifacts")
  ArtifactListResponse artifacts(@PathVariable UUID jobId) {
    var job = jobs.get(jobId);
    authorization.requireSensitiveJobOwner(CurrentUserContext.require(), job);
    return new ArtifactListResponse(support.artifacts(jobId));
  }

  @GetMapping("/artifacts/{artifactId}/download")
  ResponseEntity<Resource> downloadArtifact(@PathVariable UUID artifactId) {
    var user = CurrentUserContext.require();
    var job = jobs.get(support.artifactJobId(artifactId));
    authorization.requireSensitiveJobOwner(user, job);
    // Audit artifact download (BA §21)
    support.audit(user.userId(), job.projectId(), job.jobId(), "ARTIFACT_DOWNLOADED", "ARTIFACT",
        artifactId.toString());
    Resource resource = downloads.artifact(artifactId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
        .body(resource);
  }

  @GetMapping("/audit-logs")
  AuditLogPage audit(@RequestParam(defaultValue = "50") int limit) {
    var user = CurrentUserContext.require();
    return new AuditLogPage(support.auditLogs(user.isAdmin(), user.userId(), limit), new Page(limit, null, false));
  }
}
