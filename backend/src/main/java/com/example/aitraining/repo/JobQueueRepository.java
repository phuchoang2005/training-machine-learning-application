package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.dto.JobDtos.QueueItem;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * <b>Repository Pattern</b> — manages the {@code job_queue_entries} collection that drives
 * the FIFO job dispatch loop.
 *
 * <p>The queue uses a separate collection from {@code training_jobs} so that queue state
 * transitions (WAITING → DISPATCHED → CANCELLED) can be controlled atomically via
 * {@code findAndModify} without touching the job document itself.  This decoupling allows
 * the dispatcher to claim a queue entry atomically before updating the job status, preventing
 * double-dispatch under concurrent scheduler invocations (NFR-DATA-003).
 *
 * <p>Queue positions ({@code queuePosition} on each {@link TrainingJob}) are denormalized into
 * the job document for convenient API responses.  They are recomputed by
 * {@link #refreshPositions()} after every enqueue or cancel.
 */
@Repository
public class JobQueueRepository {
  static final String QUEUE = "job_queue_entries";

  private final MongoTemplate mongo;

  public JobQueueRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  /**
   * Adds a job to the end of the queue (status {@code WAITING}) and refreshes positions.
   */
  public void enqueue(UUID jobId) {
    Document entry = new Document()
        .append("queueEntryId", UUID.randomUUID().toString())
        .append("jobId", jobId.toString())
        .append("queueStatus", "WAITING")
        .append("enqueuedAt", Date.from(Instant.now()))
        .append("dispatchedAt", null);
    mongo.insert(entry, QUEUE);
    refreshPositions();
  }

  /**
   * Atomically claims the oldest WAITING entry and marks it DISPATCHED.
   * Returns the claimed jobId, or null if the queue is empty.
   */
  public UUID claimNext() {
    Document claimed = mongo.findAndModify(
        Query.query(Criteria.where("queueStatus").is("WAITING"))
            .with(Sort.by(Sort.Direction.ASC, "enqueuedAt"))
            .limit(1),
        new Update().set("queueStatus", "DISPATCHED").set("dispatchedAt", Date.from(Instant.now())),
        FindAndModifyOptions.options().returnNew(false),
        Document.class,
        QUEUE);
    return claimed == null ? null : UUID.fromString(claimed.getString("jobId"));
  }

  /**
   * Returns the number of jobs currently in {@code RUNNING} state.
   * Used by the dispatcher to check whether the concurrency cap has been reached.
   */
  public int runningCount() {
    return (int) mongo.count(Query.query(Criteria.where("status").is(JobStatus.RUNNING)), TrainingJob.class);
  }

  public int queuedCount() {
    return (int) mongo.count(Query.query(Criteria.where("queueStatus").is("WAITING")), QUEUE);
  }

  public List<QueueItem> items() {
    List<QueueItem> items = new ArrayList<>();
    for (Document entry : waitingEntries()) {
      UUID jobId = UUID.fromString(entry.getString("jobId"));
      TrainingJob job = mongo.findById(jobId, TrainingJob.class);
      if (job == null) {
        continue;
      }
      Project project = mongo.findById(job.projectId(), Project.class);
      Date enqueuedAt = entry.getDate("enqueuedAt");
      items.add(new QueueItem(jobId, project == null ? null : project.projectName(), job.status(),
          job.queuePosition(), enqueuedAt == null ? null : enqueuedAt.toInstant()));
    }
    return items;
  }

  /**
   * Recomputes and writes the {@code queuePosition} field for all WAITING jobs,
   * ordered FIFO by {@code enqueuedAt}.
   *
   * <p>Must be called after any operation that changes the WAITING set
   * (enqueue, cancel, dispatch) to keep positions accurate for the queue-status API.
   */
  public void refreshPositions() {
    int position = 1;
    for (Document entry : waitingEntries()) {
      UUID jobId = UUID.fromString(entry.getString("jobId"));
      mongo.updateFirst(Query.query(Criteria.where("_id").is(jobId)),
          new Update().set("queuePosition", position), TrainingJob.class);
      position++;
    }
  }

  private List<Document> waitingEntries() {
    return mongo.find(
        Query.query(Criteria.where("queueStatus").is("WAITING")).with(Sort.by(Sort.Direction.ASC, "enqueuedAt")),
        Document.class, QUEUE);
  }
}
