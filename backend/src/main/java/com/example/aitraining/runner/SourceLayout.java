package com.example.aitraining.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the effective <em>project root</em> inside an extracted ZIP or cloned repository.
 *
 * <p>ZIP archives — especially those produced by macOS — commonly wrap the project in a single
 * top-level directory (plus an {@code __MACOSX} sidecar), so the files that matter
 * ({@code main.py}, {@code requirements.txt}, {@code configs/}) live one level down. The image
 * build (at registration), the read-only {@code /source} mount, and any on-demand rebuild (at job
 * time) must all agree on this directory: if the build context root and the runtime root differ,
 * {@code pip install} runs against a context without {@code requirements.txt} and produces an image
 * missing the project's dependencies (surfacing as {@code ModuleNotFoundError} at run time).
 *
 * <p>Centralizing the resolution here is what keeps {@link com.example.aitraining.service.ProjectService}
 * (build side) and {@link AbstractTrainingRunner} (run side) in lockstep.
 */
public final class SourceLayout {
  private SourceLayout() {}

  /**
   * Returns the effective project root inside {@code dir}: if {@code dir} contains a single
   * non-hidden wrapper directory (ignoring dot-files and {@code __MACOSX}), returns that wrapper
   * directory; otherwise returns {@code dir} itself. Handles the common macOS ZIP wrapper layout.
   */
  public static Path resolveProjectRoot(Path dir) {
    try (var entries = Files.list(dir)) {
      List<Path> visible = entries
          .filter(p -> {
            String name = p.getFileName().toString();
            return !name.startsWith(".") && !name.equals("__MACOSX");
          })
          .toList();
      if (visible.size() == 1 && Files.isDirectory(visible.get(0))) {
        return visible.get(0);
      }
    } catch (IOException ignored) {
      // unreadable directory — fall through to returning dir itself
    }
    return dir;
  }
}
