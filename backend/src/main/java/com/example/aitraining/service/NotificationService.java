package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.repo.SupportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * <b>Service Layer Pattern</b> — sends email notifications when training jobs reach a
 * terminal state (NFR-REL-006).
 *
 * <p>Notifications are sent only for {@code SUCCESS} and {@code FAILED} outcomes; cancellation
 * is not notified per business rule BA §19.  Delivery is idempotent: a unique index on
 * {@code notification_dedupe.key} ({@code jobId:event}) prevents duplicate emails if the
 * method is called more than once for the same job (US4, NFR-REL-006).
 *
 * <p>Email send failures are logged at ERROR level but never propagate; the job's terminal
 * status is not affected by a notification failure.
 */
@Service
public class NotificationService {
  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final JavaMailSender mailSender;
  private final SupportRepository support;
  private final AppProperties props;

  // JavaMailSender is optional — null when spring.mail.host is not configured.
  public NotificationService(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
      JavaMailSender mailSender, SupportRepository support, AppProperties props) {
    this.mailSender = mailSender;
    this.support = support;
    this.props = props;
  }

  /**
   * Sends an email to the project owner when a job completes or fails.
   *
   * @param job        a job in a terminal state ({@code SUCCESS} or {@code FAILED})
   * @param project    the owning project (used in subject/body)
   * @param ownerEmail recipient address; typically fetched from the owner's {@code User} record
   */
  public void notifyJobTerminal(TrainingJob job, Project project, String ownerEmail) {
    if (job.status() != JobStatus.SUCCESS && job.status() != JobStatus.FAILED) {
      return;
    }
    if (!props.notification().enabled() || mailSender == null) {
      log.debug("Notifications disabled or mail unconfigured — skipping for job {}", job.jobId());
      return;
    }
    if (!support.claimNotification(job.jobId(), job.status().name())) {
      log.debug("Notification already sent for job {} ({})", job.jobId(), job.status());
      return;
    }
    try {
      boolean success = job.status() == JobStatus.SUCCESS;
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setFrom(props.notification().from());
      msg.setTo(ownerEmail);
      msg.setSubject(String.format("[ML Training] Job %s — %s",
          success ? "completed successfully" : "failed", project.projectName()));
      msg.setText(buildBody(job, project, success));
      mailSender.send(msg);
      log.info("Notification sent for job {} ({})", job.jobId(), job.status());
    } catch (Exception e) {
      log.error("Failed to send notification for job {} ({}): {}", job.jobId(), job.status(), e.getMessage());
    }
  }

  private String buildBody(TrainingJob job, Project project, boolean success) {
    String deepLink = props.notification().baseUrl() + "/projects/" + project.projectId() + "/jobs/" + job.jobId();
    return String.format("""
        Training job %s for project "%s".

        Job ID:   %s
        Project:  %s
        Started:  %s
        Ended:    %s
        %s
        View details: %s
        """,
        success ? "completed successfully" : "failed",
        project.projectName(),
        job.jobId(),
        project.projectName(),
        job.startedAt(),
        job.endedAt(),
        success ? "" : "Reason: " + job.failureReason(),
        deepLink);
  }
}
