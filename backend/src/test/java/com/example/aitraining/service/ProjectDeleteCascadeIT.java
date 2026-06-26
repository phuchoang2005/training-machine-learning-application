package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.config.AppProperties.Docker;
import com.example.aitraining.config.AppProperties.Notification;
import com.example.aitraining.config.AppProperties.Queue;
import com.example.aitraining.domain.Enums.ArtifactType;
import com.example.aitraining.domain.Enums.SourceType;
import com.example.aitraining.domain.Enums.StreamType;
import com.example.aitraining.domain.Enums.UserRole;
import com.example.aitraining.domain.Enums.UserStatus;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.ProjectConfig;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.repo.ConfigRepository;
import com.example.aitraining.repo.JobQueueRepository;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.repo.ProjectRepository;
import com.example.aitraining.repo.SupportRepository;
import com.example.aitraining.repo.UserRepository;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for the <b>delete project</b> feature against a real MongoDB.
 *
 * <p>Asserts {@link ProjectService#delete} cascades to every dependency a project owns — config,
 * snapshots, jobs, queue entries, logs, progress, and <b>artifacts</b> (both the DB records and the
 * on-disk artifact files) — while leaving a second, unrelated project completely untouched.
 *
 * <p>The artifact assertions guard the fix where {@code artifacts} were keyed by {@code projectId}
 * (a field they never carry) and so were never removed; they are now removed by the project's
 * {@code jobId}s, and the on-disk {@code storageRoot/artifacts/&#123;jobId&#125;} trees are deleted too.
 *
 * <p>Requires Docker for Testcontainers, so it is tagged {@code integration}.
 */
@Tag("integration")
class ProjectDeleteCascadeIT {

  static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:8"));
  static MongoTemplate mongoTemplate;

  @BeforeAll
  static void startMongo() {
    mongo.start();
    // Match production: force the STANDARD UUID BSON representation so UUID _id/projectId values
    // round-trip (the driver default rejects encoding UUIDs). See MongoConfig.
    MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(mongo.getReplicaSetUrl()))
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .build();
    MongoClient client = MongoClients.create(settings);
    mongoTemplate = new MongoTemplate(client, "test_ai_training");
  }

  @AfterAll
  static void stopMongo() {
    mongo.stop();
  }

  @Test
  void deletingProjectCascadesAllDependenciesAndArtifactsAndSpareProjectSurvives() throws Exception {
    Path storageRoot = Files.createTempDirectory("delete-it-storage-");
    Path sourcesRoot = Files.createTempDirectory("delete-it-sources-");
    AppProperties props = new AppProperties(storageRoot.toString(), new Queue(2),
        new Docker("python:3.11-slim", "./workspaces", sourcesRoot.toString(), 1024L, 900L),
        new Notification(false, "noreply@localhost", "http://localhost"));

    ProjectRepository projects = new ProjectRepository(mongoTemplate);
    ConfigRepository configs = new ConfigRepository(mongoTemplate);
    JobRepository jobs = new JobRepository(mongoTemplate);
    JobQueueRepository queue = new JobQueueRepository(mongoTemplate);
    UserRepository users = new UserRepository(mongoTemplate);
    SupportRepository support = new SupportRepository(mongoTemplate);
    ImageBuildService imageBuilder = new ImageBuildService(props); // docker cleanup is best-effort
    ProjectService service = new ProjectService(projects, configs, users, jobs, support, imageBuilder, props);

    UUID ownerId = UUID.randomUUID();
    User owner = new User(ownerId, "owner@example.com", "Owner", UserRole.USER, UserStatus.ACTIVE,
        Instant.now(), null);

    // ---- Project under test, with one of every dependency ----
    String localSourcePath = "projects/" + UUID.randomUUID();
    Project project = projects.create(ownerId, "to-delete", "doomed", SourceType.ZIP, null,
        localSourcePath, "python main.py");
    UUID projectId = project.projectId();

    ProjectConfig config = configs.createDefault(projectId);
    UUID snapshotId = configs.createSnapshot(projectId, config.configId(), "epochs: 10");
    TrainingJob job = jobs.create(projectId, ownerId, snapshotId, null, 0);
    UUID jobId = job.jobId();
    queue.enqueue(jobId);
    support.appendLog(jobId, StreamType.STDOUT, "Epoch 1/10", 1);
    support.appendProgress(jobId, 50, 5, 10);
    support.registerArtifact(jobId, "model.pt", ArtifactType.MODEL,
        "artifacts/" + jobId + "/model.pt", 42L, "deadbeef");

    // On-disk trees the delete must remove: the extracted source and the job's artifact files.
    Path sourceTree = storageRoot.resolve(localSourcePath);
    Files.createDirectories(sourceTree);
    Files.writeString(sourceTree.resolve("main.py"), "print('hi')");
    Path artifactDir = storageRoot.resolve("artifacts").resolve(jobId.toString());
    Files.createDirectories(artifactDir);
    Files.writeString(artifactDir.resolve("model.pt"), "weights");
    Path dockerSourceDir = sourcesRoot.resolve(projectId.toString());
    Files.createDirectories(dockerSourceDir);
    Files.writeString(dockerSourceDir.resolve("Dockerfile"), "FROM scratch");

    // ---- A second project that must be left completely intact ----
    Project survivor = projects.create(ownerId, "keep-me", "safe", SourceType.ZIP, null,
        "projects/" + UUID.randomUUID(), "python main.py");
    ProjectConfig survivorConfig = configs.createDefault(survivor.projectId());
    TrainingJob survivorJob = jobs.create(survivor.projectId(), ownerId, snapshotId, null, 0);
    support.registerArtifact(survivorJob.jobId(), "model.pt", ArtifactType.MODEL,
        "artifacts/" + survivorJob.jobId() + "/model.pt", 7L, "feedface");

    // ---- Act ----
    service.delete(owner, project);

    // ---- The project and every dependency are gone (DB) ----
    assertThatThrownBy(() -> projects.get(projectId)).isInstanceOf(EmptyResultDataAccessException.class);
    assertThat(count(projectId, ProjectConfig.class)).as("project_configs").isZero();
    assertThat(count(projectId, TrainingJob.class)).as("training_jobs").isZero();
    assertThat(countStr("projectId", projectId, "config_snapshots")).as("config_snapshots").isZero();
    assertThat(countStr("jobId", jobId, "job_queue_entries")).as("job_queue_entries").isZero();
    assertThat(countStr("jobId", jobId, "job_log_events")).as("job_log_events").isZero();
    assertThat(countStr("jobId", jobId, "job_progress_events")).as("job_progress_events").isZero();
    assertThat(countStr("jobId", jobId, "artifacts")).as("artifacts (the bug)").isZero();

    // ---- On-disk trees are gone ----
    assertThat(Files.exists(artifactDir)).as("on-disk artifact files").isFalse();
    assertThat(Files.exists(sourceTree)).as("on-disk source tree").isFalse();
    assertThat(Files.exists(dockerSourceDir)).as("docker source dir").isFalse();

    // ---- The unrelated project is untouched ----
    assertThat(projects.get(survivor.projectId())).isNotNull();
    assertThat(count(survivor.projectId(), ProjectConfig.class)).as("survivor config").isEqualTo(1);
    assertThat(count(survivor.projectId(), TrainingJob.class)).as("survivor job").isEqualTo(1);
    assertThat(countStr("jobId", survivorJob.jobId(), "artifacts")).as("survivor artifact").isEqualTo(1);

    // ---- Audit trail is retained (append-only) ----
    assertThat(countStr("projectId", projectId, "audit_logs")).as("audit retained").isGreaterThanOrEqualTo(1);

    // sanity: silence unused warning and keep a reference to the seeded config snapshot
    assertThat(survivorConfig).isNotNull();
  }

  private static long count(UUID projectId, Class<?> type) {
    return mongoTemplate.count(Query.query(Criteria.where("projectId").is(projectId)), type);
  }

  private static long countStr(String field, UUID id, String collection) {
    return mongoTemplate.count(Query.query(Criteria.where(field).is(id.toString())), collection);
  }
}
