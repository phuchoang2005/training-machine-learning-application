package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.ArtifactType;
import com.example.aitraining.domain.Enums.StreamType;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.dto.CommonDtos.UserSummary;
import com.example.aitraining.dto.SupportDtos.*;
import org.bson.Document;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Repository Pattern</b> — aggregates all persistence operations for the supporting
 * collections that do not map to domain records:
 * {@code job_log_events}, {@code job_progress_events}, {@code artifacts},
 * {@code audit_logs}, and {@code notification_dedupe}.
 *
 * <p>All of these are written as raw {@link org.bson.Document}s because they are append-only
 * event streams that are never updated after insertion.  Spring Data's typed mapping layer
 * is not used here to avoid the overhead of creating record classes for purely structural data.
 */
@Repository
public class SupportRepository {
  static final String LOG_EVENTS = "job_log_events";
  static final String PROGRESS_EVENTS = "job_progress_events";
  static final String ARTIFACTS = "artifacts";
  static final String AUDIT_LOGS = "audit_logs";
  static final String NOTIFICATION_DEDUPE = "notification_dedupe";

  private final MongoTemplate mongo;

  public SupportRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  /**
   * Appends an audit entry to {@code audit_logs}.
   *
   * @param actorId      the user who performed the action; may be {@code null} for system events
   * @param projectId    the affected project; may be {@code null} for user-scoped actions
   * @param jobId        the affected job; may be {@code null} for project-level actions
   * @param action       a verb describing the action (e.g. {@code "JOB_STARTED"})
   * @param resourceType the type of resource changed (e.g. {@code "TRAINING_JOB"})
   * @param resourceId   the string ID of the resource
   */
  public void audit(UUID actorId, UUID projectId, UUID jobId, String action, String resourceType, String resourceId) {
    Document entry = new Document()
        .append("auditId", UUID.randomUUID().toString())
        .append("actorUserId", str(actorId))
        .append("projectId", str(projectId))
        .append("jobId", str(jobId))
        .append("action", action)
        .append("resourceType", resourceType)
        .append("resourceId", resourceId)
        .append("createdAt", Date.from(Instant.now()));
    mongo.insert(entry, AUDIT_LOGS);
  }

  /**
   * Appends a single log line to {@code job_log_events}.
   * Called by {@link com.example.aitraining.runner.AbstractTrainingRunner#logLine} in real time
   * as the container produces output.
   *
   * @param sequenceNo monotonically increasing number for ordered log replay
   */
  public void appendLog(UUID jobId, StreamType streamType, String message, int sequenceNo) {
    Document entry = new Document()
        .append("logEventId", UUID.randomUUID().toString())
        .append("jobId", jobId.toString())
        .append("sequenceNo", sequenceNo)
        .append("streamType", streamType.name())
        .append("message", message)
        .append("emittedAt", Date.from(Instant.now()));
    mongo.insert(entry, LOG_EVENTS);
  }

  /**
   * Appends a progress snapshot to {@code job_progress_events}.
   *
   * @param progressValue integer percentage [0, 100]
   * @param epoch         current epoch number, or {@code null} if unavailable
   * @param totalEpoch    total epochs, or {@code null} if unavailable
   */
  public void appendProgress(UUID jobId, int progressValue, Integer epoch, Integer totalEpoch) {
    Document entry = new Document()
        .append("progressId", UUID.randomUUID().toString())
        .append("jobId", jobId.toString())
        .append("progressValue", progressValue)
        .append("epoch", epoch)
        .append("totalEpoch", totalEpoch)
        .append("emittedAt", Date.from(Instant.now()));
    mongo.insert(entry, PROGRESS_EVENTS);
  }

  /**
   * Inserts an artifact record into {@code artifacts}.
   *
   * @param filePath  path relative to {@code app.storage-root} (e.g. {@code artifacts/{jobId}/model.pt})
   * @param sizeBytes byte count after copy to storage
   * @param checksum  hex-encoded SHA-256 of the stored file
   */
  public void registerArtifact(UUID jobId, String artifactName, ArtifactType type,
      String filePath, long sizeBytes, String checksum) {
    Document entry = new Document()
        .append("artifactId", UUID.randomUUID().toString())
        .append("jobId", jobId.toString())
        .append("artifactName", artifactName)
        .append("artifactType", type.name())
        .append("filePath", filePath)
        .append("fileSizeBytes", sizeBytes)
        .append("checksum", checksum)
        .append("createdAt", Date.from(Instant.now()));
    mongo.insert(entry, ARTIFACTS);
  }

  /**
   * Atomically claims the notification slot for this job+event pair (dedupe).
   * Returns true if this caller is the first to claim it (should send); false if already sent.
   */
  public boolean claimNotification(UUID jobId, String event) {
    try {
      Document entry = new Document()
          .append("key", jobId.toString() + ":" + event)
          .append("jobId", jobId.toString())
          .append("event", event)
          .append("sentAt", Date.from(Instant.now()));
      mongo.insert(entry, NOTIFICATION_DEDUPE);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public List<LogEventResponse> logs(UUID jobId, int limit) {
    Query query = Query.query(Criteria.where("jobId").is(jobId.toString()))
        .with(Sort.by(Sort.Direction.ASC, "sequenceNo"))
        .limit(limit);
    List<LogEventResponse> logs = new ArrayList<>();
    for (Document doc : mongo.find(query, Document.class, LOG_EVENTS)) {
      logs.add(new LogEventResponse(
          UUID.fromString(doc.getString("logEventId")),
          doc.getInteger("sequenceNo", 0),
          StreamType.valueOf(doc.getString("streamType")),
          doc.getString("message"),
          instant(doc.getDate("emittedAt"))));
    }
    return logs;
  }

  public List<ArtifactResponse> artifacts(UUID jobId) {
    Query query = Query.query(Criteria.where("jobId").is(jobId.toString()))
        .with(Sort.by(Sort.Direction.DESC, "createdAt"));
    List<ArtifactResponse> artifacts = new ArrayList<>();
    for (Document doc : mongo.find(query, Document.class, ARTIFACTS)) {
      artifacts.add(new ArtifactResponse(
          UUID.fromString(doc.getString("artifactId")),
          doc.getString("artifactName"),
          ArtifactType.valueOf(doc.getString("artifactType")),
          doc.get("fileSizeBytes", Number.class).longValue(),
          doc.getString("checksum"),
          instant(doc.getDate("createdAt"))));
    }
    return artifacts;
  }

  public String artifactPath(UUID artifactId) {
    return artifact(artifactId).getString("filePath");
  }

  public UUID artifactJobId(UUID artifactId) {
    return UUID.fromString(artifact(artifactId).getString("jobId"));
  }

  public List<AuditLogResponse> auditLogs(boolean admin, UUID actorId, int limit) {
    Query query = new Query().with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(limit);
    if (!admin) {
      query.addCriteria(Criteria.where("actorUserId").is(actorId.toString()));
    }
    List<AuditLogResponse> logs = new ArrayList<>();
    for (Document doc : mongo.find(query, Document.class, AUDIT_LOGS)) {
      logs.add(new AuditLogResponse(
          UUID.fromString(doc.getString("auditId")),
          actorSummary(doc.getString("actorUserId")),
          uuid(doc.getString("projectId")),
          uuid(doc.getString("jobId")),
          doc.getString("action"),
          doc.getString("resourceType"),
          doc.getString("resourceId"),
          Map.of(),
          instant(doc.getDate("createdAt"))));
    }
    return logs;
  }

  private UserSummary actorSummary(String actorUserId) {
    if (actorUserId == null) {
      return null;
    }
    UUID id = UUID.fromString(actorUserId);
    User user = mongo.findById(id, User.class);
    return new UserSummary(id, user == null ? null : user.email(), user == null ? null : user.fullName());
  }

  private Document artifact(UUID artifactId) {
    Document doc = mongo.findOne(
        Query.query(Criteria.where("artifactId").is(artifactId.toString())), Document.class, ARTIFACTS);
    if (doc == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return doc;
  }

  private static String str(UUID value) {
    return value == null ? null : value.toString();
  }

  private static UUID uuid(String value) {
    return value == null ? null : UUID.fromString(value);
  }

  private static Instant instant(Date date) {
    return date == null ? null : date.toInstant();
  }
}
