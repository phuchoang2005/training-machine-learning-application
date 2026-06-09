package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.*;
import com.example.aitraining.dto.ApiDtos.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class SupportRepository {
    private final JdbcTemplate jdbc;

    public SupportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void audit(UUID actorId, UUID projectId, UUID jobId, String action, String resourceType, String resourceId) {
        jdbc.update("""
                INSERT INTO audit_logs (actor_user_id, project_id, job_id, action, resource_type, resource_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, actorId, projectId, jobId, action, resourceType, resourceId);
    }

    public List<LogEventResponse> logs(UUID jobId, int limit) {
        return jdbc.query("""
                SELECT * FROM job_log_events WHERE job_id = ? ORDER BY sequence_no LIMIT ?
                """, (rs, rowNum) -> new LogEventResponse(
                rs.getObject("log_event_id", UUID.class),
                rs.getInt("sequence_no"),
                StreamType.valueOf(rs.getString("stream_type")),
                rs.getString("message"),
                rs.getTimestamp("emitted_at").toInstant()), jobId, limit);
    }

    public List<ArtifactResponse> artifacts(UUID jobId) {
        return jdbc.query("""
                SELECT * FROM artifacts WHERE job_id = ? ORDER BY created_at DESC
                """, (rs, rowNum) -> new ArtifactResponse(
                rs.getObject("artifact_id", UUID.class),
                rs.getString("artifact_name"),
                ArtifactType.valueOf(rs.getString("artifact_type")),
                rs.getLong("file_size_bytes"),
                rs.getString("checksum"),
                rs.getTimestamp("created_at").toInstant()), jobId);
    }

    public String artifactPath(UUID artifactId) {
        return jdbc.queryForObject("SELECT file_path FROM artifacts WHERE artifact_id = ?", String.class, artifactId);
    }

    public UUID artifactJobId(UUID artifactId) {
        return jdbc.queryForObject("SELECT job_id FROM artifacts WHERE artifact_id = ?", UUID.class, artifactId);
    }

    public List<NotificationResponse> notifications(UUID userId, NotificationStatus status, int limit) {
        if (status == null) {
            return jdbc.query("""
                    SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?
                    """, this::notification, userId, limit);
        }
        return jdbc.query("""
                SELECT * FROM notifications WHERE user_id = ? AND status = ?::notification_status
                ORDER BY created_at DESC LIMIT ?
                """, this::notification, userId, status.name(), limit);
    }

    public NotificationStatusResponse markRead(UUID notificationId, UUID userId) {
        return jdbc.queryForObject("""
                UPDATE notifications SET status = 'READ'
                WHERE notification_id = ? AND user_id = ? RETURNING notification_id, status
                """, (rs, rowNum) -> new NotificationStatusResponse(
                rs.getObject("notification_id", UUID.class),
                NotificationStatus.valueOf(rs.getString("status"))), notificationId, userId);
    }

    public List<AuditLogResponse> auditLogs(boolean admin, UUID actorId, int limit) {
        String sql = admin
                ? "SELECT a.*, u.email, u.full_name FROM audit_logs a LEFT JOIN users u ON u.user_id = a.actor_user_id ORDER BY a.created_at DESC LIMIT ?"
                : "SELECT a.*, u.email, u.full_name FROM audit_logs a LEFT JOIN users u ON u.user_id = a.actor_user_id WHERE a.actor_user_id = ? ORDER BY a.created_at DESC LIMIT ?";
        Object[] args = admin ? new Object[]{limit} : new Object[]{actorId, limit};
        return jdbc.query(sql, (rs, rowNum) -> new AuditLogResponse(
                rs.getObject("audit_id", UUID.class),
                new UserSummary(rs.getObject("actor_user_id", UUID.class), rs.getString("email"), rs.getString("full_name")),
                rs.getObject("project_id", UUID.class),
                rs.getObject("job_id", UUID.class),
                rs.getString("action"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                Map.of(),
                rs.getTimestamp("created_at").toInstant()), args);
    }

    private NotificationResponse notification(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new NotificationResponse(
                rs.getObject("notification_id", UUID.class),
                rs.getObject("job_id", UUID.class),
                rs.getString("type"),
                NotificationChannel.valueOf(rs.getString("channel")),
                NotificationStatus.valueOf(rs.getString("status")),
                rs.getString("message"),
                rs.getTimestamp("created_at").toInstant());
    }
}
