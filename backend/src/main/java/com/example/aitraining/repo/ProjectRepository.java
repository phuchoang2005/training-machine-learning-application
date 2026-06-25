package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.SourceType;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.ProjectConfig;
import com.example.aitraining.domain.Models.TrainingJob;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * <b>Repository Pattern</b> — all persistence operations for {@link Project} documents in
 * the {@code projects} collection.
 *
 * <p>{@link #delete} performs a cascading delete: it removes all training jobs, queue entries,
 * log events, progress events, config snapshots, and artifacts associated with the project
 * before removing the project itself.  This is intentionally not transactional (MongoDB
 * single-document atomicity only); partial deletes are possible if the server crashes mid-way,
 * but they are benign orphans that do not affect correctness.
 */
@Repository
public class ProjectRepository {
  private final MongoTemplate mongo;

  public ProjectRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  /**
   * Creates a new project with status {@code ACTIVE} and inserts it into MongoDB.
   *
   * @param repositoryUrl remote Git URL; {@code null} for ZIP-source projects
   * @param localPath     relative path under {@code app.storage-root}; {@code null} for GitHub projects
   */
  public Project create(UUID ownerId, String name, String description, SourceType sourceType,
      String repositoryUrl, String localPath, String entrypoint) {
    Instant now = Instant.now();
    Project project = new Project(UUID.randomUUID(), ownerId, name, description, sourceType, repositoryUrl,
        localPath, entrypoint, "ACTIVE", now, now);
    return mongo.insert(project);
  }

  /**
   * Loads a project by ID.
   *
   * @throws org.springframework.dao.EmptyResultDataAccessException if not found
   */
  public Project get(UUID projectId) {
    Project project = mongo.findById(projectId, Project.class);
    if (project == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return project;
  }

  /**
   * Lists projects visible to a user, sorted newest-first with optional name filter.
   *
   * @param userId the caller's user ID (used for ownership filter when {@code admin=false})
   * @param admin  when {@code true} all projects are returned; when {@code false} only owned ones
   * @param query  optional case-insensitive substring filter on {@code projectName}; {@code null} skips filtering
   * @param limit  maximum number of results
   */
  public List<Project> listVisible(UUID userId, boolean admin, String query, int limit) {
    Criteria criteria = new Criteria();
    if (!admin) {
      criteria.and("ownerUserId").is(userId);
    }
    if (query != null && !query.isBlank()) {
      criteria.and("projectName").regex(Pattern.quote(query), "i");
    }
    Query mongoQuery = Query.query(criteria)
        .with(Sort.by(Sort.Direction.DESC, "createdAt"))
        .limit(limit);
    return mongo.find(mongoQuery, Project.class);
  }

  public void delete(UUID projectId) {
    List<TrainingJob> jobs = mongo.find(Query.query(Criteria.where("projectId").is(projectId)), TrainingJob.class);
    List<String> jobIds = jobs.stream().map(job -> job.jobId().toString()).toList();
    if (!jobIds.isEmpty()) {
      Query byJob = Query.query(Criteria.where("jobId").in(jobIds));
      mongo.remove(byJob, "job_queue_entries");
      mongo.remove(byJob, "job_log_events");
      mongo.remove(byJob, "job_progress_events");
    }
    mongo.remove(Query.query(Criteria.where("projectId").is(projectId)), TrainingJob.class);
    mongo.remove(Query.query(Criteria.where("projectId").is(projectId)), ProjectConfig.class);
    mongo.remove(Query.query(Criteria.where("projectId").is(projectId.toString())), "config_snapshots");
    mongo.remove(Query.query(Criteria.where("projectId").is(projectId.toString())), "artifacts");
    mongo.remove(Query.query(Criteria.where("_id").is(projectId)), Project.class);
  }
}
