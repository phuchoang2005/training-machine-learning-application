package com.example.aitraining.repo;

import com.example.aitraining.config.MongoConfig;
import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Models.TrainingJob;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for queue position consistency and atomic claim.
 * Requires Docker for Testcontainers.
 */
@Tag("integration")
class JobQueueRepositoryTest {

  static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:8"));
  static MongoTemplate mongoTemplate;

  @BeforeAll
  static void startMongo() {
    mongo.start();
    MongoClient client = MongoClients.create(mongo.getReplicaSetUrl());
    mongoTemplate = new MongoTemplate(client, "test_ai_training");
    // UUID representation is set per-connection; skip the customizer here since Testcontainers
    // uses a plain MongoClient with default settings and the test only uses string jobIds.
  }

  @AfterAll
  static void stopMongo() {
    mongo.stop();
  }

  @Test
  void enqueuedJobsGetPositionsInFifoOrder() {
    JobQueueRepository queue = new JobQueueRepository(mongoTemplate);
    JobRepository jobs = new JobRepository(mongoTemplate);

    UUID projectId = UUID.randomUUID();
    TrainingJob job1 = insertJob(projectId);
    TrainingJob job2 = insertJob(projectId);
    TrainingJob job3 = insertJob(projectId);

    queue.enqueue(job1.jobId());
    queue.enqueue(job2.jobId());
    queue.enqueue(job3.jobId());

    assertThat(jobs.get(job1.jobId()).queuePosition()).isEqualTo(1);
    assertThat(jobs.get(job2.jobId()).queuePosition()).isEqualTo(2);
    assertThat(jobs.get(job3.jobId()).queuePosition()).isEqualTo(3);
    assertThat(queue.queuedCount()).isEqualTo(3);
  }

  @Test
  void claimNextIsAtomicAndFifo() {
    JobQueueRepository queue = new JobQueueRepository(mongoTemplate);

    UUID projectId = UUID.randomUUID();
    TrainingJob first = insertJob(projectId);
    TrainingJob second = insertJob(projectId);

    queue.enqueue(first.jobId());
    queue.enqueue(second.jobId());

    UUID claimed = queue.claimNext();
    assertThat(claimed).isEqualTo(first.jobId());
    assertThat(queue.queuedCount()).isEqualTo(1);

    UUID claimed2 = queue.claimNext();
    assertThat(claimed2).isEqualTo(second.jobId());
    assertThat(queue.claimNext()).isNull();
  }

  @Test
  void refreshPositionsAfterCancelReindexes() {
    JobQueueRepository queue = new JobQueueRepository(mongoTemplate);
    JobRepository jobs = new JobRepository(mongoTemplate);

    UUID projectId = UUID.randomUUID();
    TrainingJob job1 = insertJob(projectId);
    TrainingJob job2 = insertJob(projectId);
    TrainingJob job3 = insertJob(projectId);

    queue.enqueue(job1.jobId());
    queue.enqueue(job2.jobId());
    queue.enqueue(job3.jobId());

    jobs.cancel(job2.jobId(), "cancelled");
    queue.refreshPositions();

    assertThat(jobs.get(job1.jobId()).queuePosition()).isEqualTo(1);
    assertThat(jobs.get(job3.jobId()).queuePosition()).isEqualTo(2);
  }

  private TrainingJob insertJob(UUID projectId) {
    TrainingJob job = new TrainingJob(UUID.randomUUID(), projectId, UUID.randomUUID(),
        UUID.randomUUID(), null, JobStatus.QUEUED, 0, null, Instant.now(), null, null, null, Instant.now());
    return mongoTemplate.insert(job);
  }
}
