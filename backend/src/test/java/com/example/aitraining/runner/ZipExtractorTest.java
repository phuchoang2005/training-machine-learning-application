package com.example.aitraining.runner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipExtractorTest {

  @TempDir
  Path tempDir;

  @Test
  void extractsNormalZip() throws IOException {
    Path zip = createZip("data/train.py", "hello world");
    Path target = tempDir.resolve("out");
    ZipExtractor.extract(zip, target);
    assertThat(target.resolve("data/train.py")).exists();
    assertThat(Files.readString(target.resolve("data/train.py"))).isEqualTo("hello world");
  }

  @Test
  void rejectsAbsolutePath() throws IOException {
    Path zip = createRawZip("/etc/passwd", "bad");
    assertThatThrownBy(() -> ZipExtractor.extract(zip, tempDir.resolve("out")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("absolute path");
  }

  @Test
  void rejectsPathTraversal() throws IOException {
    Path zip = createRawZip("../../../etc/passwd", "bad");
    assertThatThrownBy(() -> ZipExtractor.extract(zip, tempDir.resolve("out")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsDoubleDotInName() throws IOException {
    Path zip = createRawZip("safe/../../../etc/passwd", "bad");
    assertThatThrownBy(() -> ZipExtractor.extract(zip, tempDir.resolve("out")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void extractsNestedDirectories() throws IOException {
    Path zip = tempDir.resolve("multi.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
      addEntry(zos, "a/b/c/file.txt", "deep");
      addEntry(zos, "root.txt", "root");
    }
    Path target = tempDir.resolve("out2");
    ZipExtractor.extract(zip, target);
    assertThat(target.resolve("a/b/c/file.txt")).exists();
    assertThat(target.resolve("root.txt")).exists();
  }

  private Path createZip(String entryName, String content) throws IOException {
    Path zip = tempDir.resolve("test.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
      addEntry(zos, entryName, content);
    }
    return zip;
  }

  private Path createRawZip(String rawEntryName, String content) throws IOException {
    Path zip = tempDir.resolve("raw.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
      ZipEntry entry = new ZipEntry(rawEntryName);
      zos.putNextEntry(entry);
      zos.write(content.getBytes());
      zos.closeEntry();
    }
    return zip;
  }

  private void addEntry(ZipOutputStream zos, String name, String content) throws IOException {
    zos.putNextEntry(new ZipEntry(name));
    zos.write(content.getBytes());
    zos.closeEntry();
  }
}
