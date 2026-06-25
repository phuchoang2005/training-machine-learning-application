package com.example.aitraining.repo;

import com.example.aitraining.domain.Models.ProjectConfig;
import org.bson.Document;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * <b>Repository Pattern</b> — persistence operations for {@link com.example.aitraining.domain.Models.ProjectConfig}
 * documents in {@code project_configs} and raw {@code config_snapshots} documents.
 *
 * <p>Snapshots are written as raw {@link org.bson.Document}s because they are intentionally
 * immutable records that should never be mapped back to a domain object — they exist only to
 * give the job runner a stable view of the YAML that was chosen at job-start time.
 */
@Repository
public class ConfigRepository {
  static final String SNAPSHOTS = "config_snapshots";

  private final MongoTemplate mongo;

  public ConfigRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  /**
   * Creates the default config for a new project.
   * The YAML is seeded with the project's training entrypoint so at least one runnable config
   * always exists before the user edits anything.
   */
  public ProjectConfig createDefault(UUID projectId, String entrypoint) {
    String yaml = "trainingEntrypoint: " + entrypoint + "\n";
    ProjectConfig config = new ProjectConfig(UUID.randomUUID(), projectId, "default", "configs/default.yaml",
        yaml, true, Instant.now());
    return mongo.insert(config);
  }

  public List<ProjectConfig> list(UUID projectId) {
    Query query = Query.query(Criteria.where("projectId").is(projectId))
        .with(Sort.by(Sort.Order.desc("defaultConfig"), Sort.Order.asc("configPath")));
    return mongo.find(query, ProjectConfig.class);
  }

  public ProjectConfig get(UUID projectId, UUID configId) {
    ProjectConfig config = mongo.findOne(
        Query.query(Criteria.where("_id").is(configId).and("projectId").is(projectId)), ProjectConfig.class);
    if (config == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return config;
  }

  /**
   * Persists an immutable copy of the YAML content used for a specific training run.
   * Returns the generated snapshot ID, which is stored on the job so the runner can
   * retrieve the YAML at execution time.
   *
   * @param configId the source config; may be {@code null} for retry-generated snapshots
   */
  public UUID createSnapshot(UUID projectId, UUID configId, String yamlContent) {
    UUID snapshotId = UUID.randomUUID();
    Document snapshot = new Document()
        .append("snapshotId", snapshotId.toString())
        .append("projectId", projectId.toString())
        .append("configId", configId == null ? null : configId.toString())
        .append("yamlContent", yamlContent)
        .append("contentHash", sha256(yamlContent))
        .append("createdAt", Date.from(Instant.now()));
    mongo.insert(snapshot, SNAPSHOTS);
    return snapshotId;
  }

  /** Returns the YAML content from a previously-stored config snapshot. */
  public String getSnapshotYaml(UUID snapshotId) {
    Document snapshot = mongo.findOne(
        Query.query(Criteria.where("snapshotId").is(snapshotId.toString())), Document.class, SNAPSHOTS);
    if (snapshot == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return snapshot.getString("yamlContent");
  }

  public String contentHash(String yamlContent) {
    return sha256(yamlContent);
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to hash YAML content", ex);
    }
  }
}
