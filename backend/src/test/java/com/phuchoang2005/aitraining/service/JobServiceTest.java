package com.phuchoang2005.aitraining.service;

import com.phuchoang2005.aitraining.config.AppProperties;
import com.phuchoang2005.aitraining.domain.Enums.JobStatus;
import com.phuchoang2005.aitraining.domain.Enums.SourceType;
import com.phuchoang2005.aitraining.domain.Models.Project;
import com.phuchoang2005.aitraining.domain.Models.TrainingJob;
import com.phuchoang2005.aitraining.domain.Models.User;
import com.phuchoang2005.aitraining.domain.Enums.UserRole;
import com.phuchoang2005.aitraining.domain.Enums.UserStatus;
import com.phuchoang2005.aitraining.dto.JobDtos.CancelJobResponse;
import com.phuchoang2005.aitraining.dto.JobDtos.RetryJobRequest;
import com.phuchoang2005.aitraining.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobServiceTest {

  @Mock JobRepository jobRepository;
  @Mock JobQueueRepository queueRepository;
  @Mock ConfigRepository configRepository;
  @Mock UserRepository userRepository;
  @Mock SupportRepository supportRepository;
  @Mock AppProperties appProperties;
  @Mock AppProperties.Queue queueConfig;

  JobService service;

  UUID projectId = UUID.randomUUID();
  UUID ownerId = UUID.randomUUID();
  UUID jobId = UUID.randomUUID();

  User owner;
  Project project;

  @BeforeEach
  void setUp() {
    when(appProperties.queue()).thenReturn(queueConfig);
    when(queueConfig.runningLimit()).thenReturn(2);
    service = new JobService(jobRepository, queueRepository, configRepository, userRepository, supportRepository, appProperties);
    owner = new User(ownerId, "owner@example.com", "Owner", UserRole.USER, UserStatus.ACTIVE, Instant.now(), null);
    project = new Project(projectId, ownerId, "Test Project", null, SourceType.GITHUB,
        "https://github.com.phuchoang2005/repo", null, "train.py", "ACTIVE", "READY", null,
        Instant.now(), Instant.now());
  }

  @Test
  void cancelTerminalJobThrows() {
    TrainingJob terminal = buildJob(JobStatus.SUCCESS);
    assertThatThrownBy(() -> service.cancel(owner, project, terminal, "reason"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Terminal");
  }

  @Test
  void cancelRunningJobSucceeds() {
    TrainingJob running = buildJob(JobStatus.RUNNING);
    TrainingJob cancelled = buildJob(JobStatus.CANCELLED);
    when(jobRepository.cancel(jobId, "stop it")).thenReturn(cancelled);

    CancelJobResponse response = service.cancel(owner, project, running, "stop it");
    assertThat(response.status()).isEqualTo(JobStatus.CANCELLED);
  }

  @Test
  void retryNonTerminalJobThrows() {
    TrainingJob running = buildJob(JobStatus.RUNNING);
    assertThatThrownBy(() -> service.retry(owner, project, running, new RetryJobRequest("SAME_CONFIG", null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("failed or cancelled");
  }

  @Test
  void retryFailedJobSucceeds() {
    TrainingJob failed = buildJob(JobStatus.FAILED);
    UUID newJobId = UUID.randomUUID();
    UUID snapshotId = UUID.randomUUID();

    // Retry reuses the original job's config snapshot YAML rather than a placeholder.
    when(configRepository.getSnapshotYaml(failed.configSnapshotId())).thenReturn("training:\n  epochs: 10\n");
    when(configRepository.createSnapshot(eq(projectId), isNull(), anyString())).thenReturn(snapshotId);
    TrainingJob retryJob = new TrainingJob(newJobId, projectId, ownerId, snapshotId, jobId,
        JobStatus.QUEUED, 1, 1, Instant.now(), null, null, null, Instant.now());
    when(jobRepository.create(projectId, ownerId, snapshotId, jobId, 1)).thenReturn(retryJob);
    when(jobRepository.get(newJobId)).thenReturn(retryJob);

    var response = service.retry(owner, project, failed, new RetryJobRequest("SAME_CONFIG", null));
    assertThat(response.retryOfJobId()).isEqualTo(jobId);
    assertThat(response.jobId()).isEqualTo(newJobId);
    assertThat(response.status()).isEqualTo(JobStatus.QUEUED);
    assertThat(response.retryAttempt()).isEqualTo(1);
  }

  private TrainingJob buildJob(JobStatus status) {
    Instant now = Instant.now();
    return new TrainingJob(jobId, projectId, ownerId, UUID.randomUUID(), null,
        status, 0, null, now, null, null, null, now);
  }
}
