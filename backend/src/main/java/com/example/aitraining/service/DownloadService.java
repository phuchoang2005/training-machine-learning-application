package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.repo.SupportRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

/**
 * <b>Service Layer Pattern</b> — resolves managed storage paths for file downloads, enforcing
 * a path-confinement check (NFR-SEC-007) before returning a {@link Resource}.
 *
 * <p>Every resolved path is checked to ensure it remains under {@code app.storage-root};
 * any path that escapes the root (via {@code ..} or absolute segments) throws
 * {@link IllegalArgumentException}, which maps to HTTP 400.
 */
@Service
public class DownloadService {
  private final SupportRepository support;
  private final Path root;

  public DownloadService(SupportRepository support, AppProperties props) {
    this.support = support;
    this.root = Path.of(props.storageRoot()).toAbsolutePath().normalize();
  }

  /**
   * Resolves the file path registered for the given artifact and returns it as a
   * {@link FileSystemResource}.
   *
   * @param artifactId the artifact whose path is looked up in the database
   * @return a resource backed by the artifact file on disk
   * @throws org.springframework.dao.EmptyResultDataAccessException if the artifact is not found
   * @throws IllegalArgumentException if the resolved path escapes the storage root
   */
  public Resource artifact(UUID artifactId) {
    return safeResource(support.artifactPath(artifactId));
  }

  /**
   * Resolves a relative storage path safely.
   *
   * @param path relative path under {@code app.storage-root}
   * @throws IllegalArgumentException if {@code path} would escape the storage root
   */
  public Resource safeResource(String path) {
    Path resolved = root.resolve(path).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Unsafe storage path");
    }
    return new FileSystemResource(resolved);
  }
}
