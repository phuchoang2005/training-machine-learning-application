package com.example.aitraining.domain;

public final class Enums {
    private Enums() {
    }

    public enum UserRole { USER, ADMIN }
    public enum UserStatus { ACTIVE, DISABLED }
    public enum SourceType { GITHUB, ZIP }
    public enum JobStatus { CREATED, QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED, RETRYING }
    public enum QueueStatus { WAITING, DISPATCHED, CANCELLED }
    public enum StreamType { STDOUT, STDERR }
    public enum ArtifactType { MODEL, CHECKPOINT, METRIC, OTHER }
    public enum NotificationChannel { IN_APP, EMAIL }
    public enum NotificationStatus { PENDING, SENT, FAILED, READ }
}
