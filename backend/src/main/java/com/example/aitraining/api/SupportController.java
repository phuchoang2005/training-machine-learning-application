package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.domain.Enums.NotificationStatus;
import com.example.aitraining.dto.ApiDtos.*;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.repo.SupportRepository;
import com.example.aitraining.service.AuthorizationService;
import com.example.aitraining.service.DownloadService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
        var job = jobs.get(support.artifactJobId(artifactId));
        authorization.requireSensitiveJobOwner(CurrentUserContext.require(), job);
        Resource resource = downloads.artifact(artifactId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/notifications")
    NotificationPage notifications(@RequestParam(required = false) NotificationStatus status,
                                   @RequestParam(defaultValue = "50") int limit) {
        var user = CurrentUserContext.require();
        return new NotificationPage(support.notifications(user.userId(), status, limit), new Page(limit, null, false));
    }

    @PostMapping("/notifications/{notificationId}/read")
    NotificationStatusResponse markRead(@PathVariable UUID notificationId) {
        var user = CurrentUserContext.require();
        return support.markRead(notificationId, user.userId());
    }

    @GetMapping("/audit-logs")
    AuditLogPage audit(@RequestParam(defaultValue = "50") int limit) {
        var user = CurrentUserContext.require();
        return new AuditLogPage(support.auditLogs(user.isAdmin(), user.userId(), limit), new Page(limit, null, false));
    }
}
