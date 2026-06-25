package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.dto.JobDtos.ProgressResponse;
import org.bson.Document;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * <b>Repository Pattern</b> — all persistence operations for {@link TrainingJob} documents
 * in the {@code training_jobs} collection, plus progress event reads from
 * {@code job_progress_events}.
 *
 * <p>Status transitions use MongoDB {@code findAndModify} with a status precondition so that
 * concurrent updates are atomic and idempotent.  A {@code null} return from any
 * {@code transition*()} method means the precondition was not met (the job was cancelled
 * or already in the target state) — callers should treat this as a no-op rather than an error.
 *
 * <p>A missing document always throws {@link org.springframework.dao.EmptyResultDataAccessException}
 * which the {@link com.example.aitraining.config.ApiExceptionHandler} maps to HTTP 404.
 */
@Repository
public class JobRepository {
  private final MongoTemplate mongo;

  public JobRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  /**
   * Creates a new job in {@code QUEUED} state and inserts it into MongoDB.
   *
   * @param projectId     the owning project
   * @param userId        the user who triggered this job
   * @param snapshotId    the config snapshot created just before this job
   * @param retryOfJobId  {@code null} for first runs; the original job ID for retries
   * @param retryAttempt  0 for first runs; incremented on each retry
   */
  public TrainingJob create(UUID projectId, UUID userId, UUID snapshotId, UUID retryOfJobId, int retryAttempt) {
    TrainingJob job = new TrainingJob(UUID.randomUUID(), projectId, userId, snapshotId, retryOfJobId,
        JobStatus.QUEUED, retryAttempt, null, Instant.now(), null, null, null, Instant.now());
    return mongo.insert(job);
  }

  /**
   * Loads a job by ID.
   *
   * @throws org.springframework.dao.EmptyResultDataAccessException if not found
   */
  public TrainingJob get(UUID jobId) {
    TrainingJob job = mongo.findById(jobId, TrainingJob.class);
    if (job == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return job;
  }

  /**
   * Lists jobs for a project, sorted newest-first.
   *
   * @param status {@code null} to include all statuses
   * @param limit  maximum number of results; use {@code Integer.MAX_VALUE} for unbounded
   */
  public List<TrainingJob> listByProject(UUID projectId, JobStatus status, int limit) {
    Criteria criteria = Criteria.where("projectId").is(projectId);
    if (status != null) {
      criteria.and("status").is(status);
    }
    Query query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(limit);
    return mongo.find(query, TrainingJob.class);
  }

  /**
   * Returns all jobs globally that have the given status.
   * Used by {@link com.example.aitraining.service.JobReconcilerService} to locate orphaned RUNNING jobs.
   */
  public List<TrainingJob> findByStatus(JobStatus status) {
    return mongo.find(Query.query(Criteria.where("status").is(status)), TrainingJob.class);
  }

  /** Atomically transitions QUEUED or RETRYING → RUNNING. Returns null if the job was cancelled. */
  public TrainingJob transitionToRunning(UUID jobId) {
    return mongo.findAndModify(
        Query.query(Criteria.where("_id").is(jobId)
            .and("status").in(JobStatus.QUEUED, JobStatus.RETRYING)),
        new Update().set("status", JobStatus.RUNNING).set("startedAt", Instant.now()),
        FindAndModifyOptions.options().returnNew(true),
        TrainingJob.class);
  }

  /** Atomically transitions RUNNING → SUCCESS or FAILED. Returns null if cancelled externally. */
  public TrainingJob transitionToTerminal(UUID jobId, JobStatus status, String failureReason) {
    Update update = new Update().set("status", status).set("endedAt", Instant.now());
    if (failureReason != null) {
      update.set("failureReason", failureReason);
    }
    return mongo.findAndModify(
        Query.query(Criteria.where("_id").is(jobId).and("status").is(JobStatus.RUNNING)),
        update,
        FindAndModifyOptions.options().returnNew(true),
        TrainingJob.class);
  }

  /** Marks a RUNNING job RETRYING so the reconciler can re-enqueue it after a restart. */
  public void transitionToRetrying(UUID jobId) {
    mongo.updateFirst(
        Query.query(Criteria.where("_id").is(jobId).and("status").is(JobStatus.RUNNING)),
        new Update().set("status", JobStatus.RETRYING),
        TrainingJob.class);
  }

  public TrainingJob cancel(UUID jobId, String reason) {
    mongo.updateMulti(
        Query.query(Criteria.where("jobId").is(jobId.toString()).and("queueStatus").is("WAITING")),
        new Update().set("queueStatus", "CANCELLED"),
        "job_queue_entries");

    Update update = new Update().set("status", JobStatus.CANCELLED).set("endedAt", Instant.now());
    if (reason != null) {
      update.set("failureReason", reason);
    }
    TrainingJob job = mongo.findAndModify(
        Query.query(Criteria.where("_id").is(jobId)
            .and("status").in(JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.CREATED, JobStatus.RETRYING)),
        update,
        FindAndModifyOptions.options().returnNew(true),
        TrainingJob.class);
    if (job == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return job;
  }

  /**
   * Returns the most recent progress event for a job, or a "no data" sentinel if none exists.
   */
  public ProgressResponse latestProgress(UUID jobId) {
    Query query = Query.query(Criteria.where("jobId").is(jobId.toString()))
        .with(Sort.by(Sort.Direction.DESC, "emittedAt"))
        .limit(1);
    Document row = mongo.findOne(query, Document.class, "job_progress_events");
    if (row == null) {
      return new ProgressResponse(false, null, null, null, null);
    }
    Date emittedAt = row.getDate("emittedAt");
    return new ProgressResponse(true, row.getInteger("progressValue"), row.getInteger("epoch"),
        row.getInteger("totalEpoch"), emittedAt == null ? null : emittedAt.toInstant());
  }
}
