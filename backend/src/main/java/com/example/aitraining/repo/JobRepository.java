package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.dto.ApiDtos.ProgressResponse;
import com.example.aitraining.dto.ApiDtos.QueueItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JobRepository {
    private final JdbcTemplate jdbc;

    public JobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TrainingJob create(UUID projectId, UUID userId, UUID snapshotId, UUID retryOfJobId, int retryAttempt) {
        return jdbc.queryForObject("""
                INSERT INTO training_jobs (project_id, triggered_by_user_id, config_snapshot_id, retry_of_job_id,
                  status, retry_attempt, queued_at)
                VALUES (?, ?, ?, ?, 'QUEUED', ?, now()) RETURNING *
                """, this::map, projectId, userId, snapshotId, retryOfJobId, retryAttempt);
    }

    public TrainingJob get(UUID jobId) {
        return jdbc.queryForObject("SELECT * FROM training_jobs WHERE job_id = ?", this::map, jobId);
    }

    public List<TrainingJob> listByProject(UUID projectId, JobStatus status, int limit) {
        if (status == null) {
            return jdbc.query("SELECT * FROM training_jobs WHERE project_id = ? ORDER BY created_at DESC LIMIT ?",
                    this::map, projectId, limit);
        }
        return jdbc.query("""
                SELECT * FROM training_jobs WHERE project_id = ? AND status = ?::job_status
                ORDER BY created_at DESC LIMIT ?
                """, this::map, projectId, status.name(), limit);
    }

    public void enqueue(UUID jobId) {
        jdbc.update("""
                INSERT INTO job_queue_entries (job_id, queue_status) VALUES (?, 'WAITING')
                """, jobId);
        refreshQueuePositions();
    }

    public TrainingJob cancel(UUID jobId, String reason) {
        jdbc.update("UPDATE job_queue_entries SET queue_status = 'CANCELLED' WHERE job_id = ? AND queue_status = 'WAITING'",
                jobId);
        TrainingJob job = jdbc.queryForObject("""
                UPDATE training_jobs
                SET status = 'CANCELLED', ended_at = now(), failure_reason = COALESCE(?, failure_reason)
                WHERE job_id = ? AND status IN ('QUEUED', 'RUNNING', 'CREATED')
                RETURNING *
                """, this::map, reason, jobId);
        refreshQueuePositions();
        return job;
    }

    public int runningCount() {
        return jdbc.queryForObject("SELECT count(*) FROM training_jobs WHERE status = 'RUNNING'", Integer.class);
    }

    public int queuedCount() {
        return jdbc.queryForObject("SELECT count(*) FROM job_queue_entries WHERE queue_status = 'WAITING'", Integer.class);
    }

    public List<QueueItem> queueItems() {
        return jdbc.query("""
                SELECT j.job_id, p.project_name, j.status, j.queue_position, q.enqueued_at
                FROM job_queue_entries q
                JOIN training_jobs j ON j.job_id = q.job_id
                JOIN projects p ON p.project_id = j.project_id
                WHERE q.queue_status = 'WAITING'
                ORDER BY q.enqueued_at
                """, (rs, rowNum) -> new QueueItem(
                rs.getObject("job_id", UUID.class),
                rs.getString("project_name"),
                JobStatus.valueOf(rs.getString("status")),
                (Integer) rs.getObject("queue_position"),
                rs.getTimestamp("enqueued_at").toInstant()));
    }

    public ProgressResponse latestProgress(UUID jobId) {
        List<ProgressResponse> rows = jdbc.query("""
                SELECT progress_value, epoch, total_epoch, emitted_at
                FROM job_progress_events WHERE job_id = ? ORDER BY emitted_at DESC LIMIT 1
                """, (rs, rowNum) -> new ProgressResponse(true, rs.getInt("progress_value"),
                (Integer) rs.getObject("epoch"), (Integer) rs.getObject("total_epoch"),
                rs.getTimestamp("emitted_at").toInstant()), jobId);
        return rows.isEmpty() ? new ProgressResponse(false, null, null, null, null) : rows.getFirst();
    }

    private void refreshQueuePositions() {
        jdbc.update("""
                WITH ranked AS (
                  SELECT job_id, row_number() OVER (ORDER BY enqueued_at)::integer AS pos
                  FROM job_queue_entries WHERE queue_status = 'WAITING'
                )
                UPDATE training_jobs j SET queue_position = ranked.pos
                FROM ranked WHERE ranked.job_id = j.job_id
                """);
    }

    private TrainingJob map(ResultSet rs, int rowNum) throws SQLException {
        return new TrainingJob(
                rs.getObject("job_id", UUID.class),
                rs.getObject("project_id", UUID.class),
                rs.getObject("triggered_by_user_id", UUID.class),
                rs.getObject("config_snapshot_id", UUID.class),
                rs.getObject("retry_of_job_id", UUID.class),
                JobStatus.valueOf(rs.getString("status")),
                rs.getInt("retry_attempt"),
                (Integer) rs.getObject("queue_position"),
                rs.getTimestamp("queued_at") == null ? null : rs.getTimestamp("queued_at").toInstant(),
                rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("ended_at") == null ? null : rs.getTimestamp("ended_at").toInstant(),
                rs.getString("failure_reason"),
                rs.getTimestamp("created_at").toInstant());
    }
}
