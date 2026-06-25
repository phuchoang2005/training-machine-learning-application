package com.example.aitraining.runner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Safe ZIP extraction utility that enforces NFR-SEC-006: reject path traversal, absolute
 * paths, and oversized entries before any bytes reach the file system.
 *
 * <p>Use this class for <em>every</em> user-supplied ZIP archive (project uploads, dataset
 * imports, etc.).  Direct use of {@link java.util.zip.ZipFile} without these checks has been
 * a common source of path-traversal vulnerabilities (CVE class "Zip Slip").
 *
 * <h2>Rejection rules</h2>
 * <ul>
 *   <li>Entry name starts with {@code /} or {@code \} → absolute path → rejected.</li>
 *   <li>Entry name contains {@code ..} → potential traversal → rejected.</li>
 *   <li>Resolved entry path escapes the target directory after {@link Path#normalize()} →
 *       rejected.</li>
 *   <li>Entry declares a size greater than {@value #MAX_ENTRY_SIZE} bytes → rejected.</li>
 *   <li>More than {@value #MAX_ENTRIES} entries in a single archive → rejected (zip-bomb
 *       mitigation).</li>
 * </ul>
 */
public final class ZipExtractor {

  /** Maximum bytes that a single entry may occupy on disk (512 MB). */
  private static final long MAX_ENTRY_SIZE = 512L * 1024 * 1024;

  /** Maximum number of entries accepted from a single archive. */
  private static final int MAX_ENTRIES = 10_000;

  private ZipExtractor() {}

  /**
   * Extracts the ZIP file at {@code zipFile} into {@code targetDir}, creating the directory
   * if it does not yet exist.
   *
   * @param zipFile   path to the ZIP archive to extract
   * @param targetDir destination directory; created if absent
   * @throws IllegalArgumentException if any entry violates the security rules above
   * @throws IOException              on I/O errors reading the archive or writing to disk
   */
  public static void extract(Path zipFile, Path targetDir) throws IOException {
    Files.createDirectories(targetDir);
    Path canonicalTarget = targetDir.toRealPath();

    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
      ZipEntry entry;
      int entryCount = 0;
      while ((entry = zis.getNextEntry()) != null) {
        if (++entryCount > MAX_ENTRIES) {
          throw new IllegalArgumentException("ZIP contains too many entries (max " + MAX_ENTRIES + ")");
        }
        validateEntry(entry);

        Path resolved = canonicalTarget.resolve(entry.getName()).normalize();
        if (!resolved.startsWith(canonicalTarget)) {
          throw new IllegalArgumentException("Unsafe ZIP entry (path traversal): " + entry.getName());
        }

        if (entry.isDirectory()) {
          Files.createDirectories(resolved);
        } else {
          Files.createDirectories(resolved.getParent());
          copyEntry(zis, resolved);
        }
        zis.closeEntry();
      }
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────────────────────────

  /**
   * Validates a single ZIP entry's name and declared size before extraction.
   *
   * @throws IllegalArgumentException if the entry fails any security check
   */
  private static void validateEntry(ZipEntry entry) {
    String name = entry.getName();
    if (name.startsWith("/") || name.startsWith("\\")) {
      throw new IllegalArgumentException("ZIP entry has absolute path: " + name);
    }
    if (name.contains("..")) {
      throw new IllegalArgumentException("ZIP entry contains path traversal sequence: " + name);
    }
    if (entry.getSize() > MAX_ENTRY_SIZE) {
      throw new IllegalArgumentException("ZIP entry exceeds size limit: " + name);
    }
  }

  /**
   * Copies bytes from the current ZIP stream position to {@code dest}, enforcing
   * {@link #MAX_ENTRY_SIZE} even when the entry's declared size was not set (i.e., {@code -1}).
   */
  private static void copyEntry(InputStream src, Path dest) throws IOException {
    long written = 0;
    try (OutputStream out = Files.newOutputStream(dest)) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = src.read(buf)) != -1) {
        written += n;
        if (written > MAX_ENTRY_SIZE) {
          throw new IllegalArgumentException("ZIP entry exceeded size limit during extraction");
        }
        out.write(buf, 0, n);
      }
    }
  }
}
