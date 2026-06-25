package com.example.aitraining.api;

import com.example.aitraining.config.AppProperties;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System health check endpoint (NFR-OPS-001).
 *
 * <p>{@code GET /api/v1/health} is the only unauthenticated endpoint.  It returns:
 * <ul>
 *   <li>{@code status: "UP"} when MongoDB, the storage directory, and Docker are all healthy.</li>
 *   <li>{@code status: "DEGRADED"} if any component check fails; details are included per check.</li>
 * </ul>
 *
 * <p>Individual checks:
 * <ul>
 *   <li>{@code mongo} — issues a {@code ping} command against the MongoDB server.</li>
 *   <li>{@code storage} — writes and deletes a probe file under {@code app.storage-root}.</li>
 *   <li>{@code docker} — runs {@code docker info --format {{.ServerVersion}}}.</li>
 * </ul>
 */
@RestController
public class HealthController {
  private final MongoTemplate mongo;
  private final AppProperties props;

  public HealthController(MongoTemplate mongo, AppProperties props) {
    this.mongo = mongo;
    this.props = props;
  }

  /**
   * Returns the aggregated health status of all subsystems.
   */
  @GetMapping("/health")
  Map<String, Object> health() {
    Map<String, Object> checks = new LinkedHashMap<>();
    checks.put("mongo", checkMongo());
    checks.put("storage", checkStorage());
    checks.put("docker", checkDocker());

    boolean allUp = checks.values().stream()
        .allMatch(v -> v instanceof Map<?, ?> m && "UP".equals(m.get("status")));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", allUp ? "UP" : "DEGRADED");
    result.put("service", "ai-training-backend");
    result.put("instance", System.getenv().getOrDefault("HOSTNAME", "unknown"));
    result.put("timestamp", Instant.now().toString());
    result.put("checks", checks);
    return result;
  }

  private Map<String, Object> checkMongo() {
    try {
      mongo.getDb().runCommand(new Document("ping", 1));
      return Map.of("status", "UP");
    } catch (Exception e) {
      return Map.of("status", "DOWN", "error", e.getMessage());
    }
  }

  private Map<String, Object> checkStorage() {
    Path root = Path.of(props.storageRoot());
    try {
      Files.createDirectories(root);
      Path probe = root.resolve(".health-probe");
      Files.writeString(probe, "ok");
      Files.delete(probe);
      return Map.of("status", "UP", "path", root.toAbsolutePath().toString());
    } catch (IOException e) {
      return Map.of("status", "DOWN", "error", e.getMessage());
    }
  }

  private Map<String, Object> checkDocker() {
    try {
      Process p = new ProcessBuilder("docker", "info", "--format", "{{.ServerVersion}}")
          .redirectErrorStream(true)
          .start();
      String version = new String(p.getInputStream().readAllBytes()).trim();
      if (p.waitFor() == 0 && !version.isBlank()) {
        return Map.of("status", "UP", "version", version);
      }
      return Map.of("status", "DOWN", "error", "docker info returned non-zero");
    } catch (Exception e) {
      return Map.of("status", "DOWN", "error", e.getMessage());
    }
  }
}
