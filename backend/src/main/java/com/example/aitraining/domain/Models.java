package com.example.aitraining.domain;

import com.example.aitraining.domain.Enums.*;

import java.time.Instant;
import java.util.UUID;

public final class Models {
    private Models() {
    }

    public record User(UUID userId, String email, String fullName, UserRole role, UserStatus status,
                       Instant createdAt, Instant lastLoginAt) {
        public boolean isAdmin() {
            return role == UserRole.ADMIN;
        }
    }

    public record Project(UUID projectId, UUID ownerUserId, String projectName, String description,
                          SourceType sourceType, String repositoryUrl, String localSourcePath,
                          String trainingEntrypoint, String status, Instant createdAt, Instant updatedAt) {
    }

    public record ProjectConfig(UUID configId, UUID projectId, String configName, String configPath,
                                String yamlContent, boolean defaultConfig, Instant updatedAt) {
    }

    public record TrainingJob(UUID jobId, UUID projectId, UUID triggeredByUserId, UUID configSnapshotId,
                              UUID retryOfJobId, JobStatus status, int retryAttempt, Integer queuePosition,
                              Instant queuedAt, Instant startedAt, Instant endedAt, String failureReason,
                              Instant createdAt) {
        public boolean terminal() {
            return status == JobStatus.SUCCESS || status == JobStatus.FAILED || status == JobStatus.CANCELLED;
        }
    }
}
