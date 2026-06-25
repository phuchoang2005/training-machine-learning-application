package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.domain.Enums.ArtifactType;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.repo.SupportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * <b>Service Layer Pattern</b> — copies training output files from the ephemeral job workspace
 * to permanent managed storage and registers them in {@code artifacts} (BA §17/§18, NFR-STO-003).
 *
 * <p>This service is called by {@link com.example.aitraining.runner.AbstractTrainingRunner}
 * after the training process succeeds.  Any failure during collection or registration is
 * logged but <em>never</em> changes the job's terminal status (NFR-REL-007).
 *
 * <p>Artifact types are inferred heuristically from file extensions; see {@code detectType}.
 * Integrity is assured by computing SHA-256 during the file copy.
 */
@Service
public class ArtifactService {
  private static final Logger log = LoggerFactory.getLogger(ArtifactService.class);

  private final SupportRepository support;
  private final AppProperties props;

  public ArtifactService(SupportRepository support, AppProperties props) {
    this.support = support;
    this.props = props;
  }

  /**
   * Walks {@code workspace/output/} and copies every regular file to
   * {@code storage/artifacts/{jobId}/}. Registers each file in the database.
   *
   * @param job       the job that produced the workspace; its {@code jobId} is used for storage paths
   * @param workspace per-job scratch directory; expected to contain an {@code output/} subdirectory
   */
  public void collectFromWorkspace(TrainingJob job, Path workspace) {
    Path outputDir = workspace.resolve("output");
    if (!Files.exists(outputDir)) {
      log.info("No output/ directory in workspace for job {} — no artifacts to register", job.jobId());
      return;
    }
    Path artifactStore = Path.of(props.storageRoot()).resolve("artifacts").resolve(job.jobId().toString());
    try {
      Files.createDirectories(artifactStore);
      Files.walk(outputDir)
          .filter(Files::isRegularFile)
          .forEach(file -> registerFile(job.jobId(), outputDir, file, artifactStore));
    } catch (Exception e) {
      log.error("Artifact collection failed for job {} (job status unchanged): {}", job.jobId(), e.getMessage());
    }
  }

  private void registerFile(UUID jobId, Path outputRoot, Path file, Path artifactStore) {
    try {
      String relativeName = outputRoot.relativize(file).toString();
      Path dest = artifactStore.resolve(relativeName);
      Files.createDirectories(dest.getParent());
      Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);

      long sizeBytes = Files.size(dest);
      String checksum = sha256(dest);
      ArtifactType type = detectType(relativeName);
      String storagePath = "artifacts/" + jobId + "/" + relativeName;

      support.registerArtifact(jobId, relativeName, type, storagePath, sizeBytes, checksum);
      log.debug("Registered artifact {} for job {}", relativeName, jobId);
    } catch (Exception e) {
      log.warn("Failed to register artifact {} for job {}: {}", file, jobId, e.getMessage());
    }
  }

  private ArtifactType detectType(String name) {
    String lower = name.toLowerCase();
    if (lower.endsWith(".pt") || lower.endsWith(".pth") || lower.endsWith(".h5")
        || lower.endsWith(".onnx") || lower.endsWith(".pb") || lower.endsWith(".pkl")) {
      return ArtifactType.MODEL;
    }
    if (lower.contains("checkpoint")) {
      return ArtifactType.CHECKPOINT;
    }
    if (lower.endsWith(".json") || lower.endsWith(".csv") || lower.contains("metric")) {
      return ArtifactType.METRIC;
    }
    return ArtifactType.OTHER;
  }

  private String sha256(Path file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream is = new DigestInputStream(Files.newInputStream(file), digest)) {
        is.transferTo(java.io.OutputStream.nullOutputStream());
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("SHA-256 failed for " + file, e);
    }
  }
}
