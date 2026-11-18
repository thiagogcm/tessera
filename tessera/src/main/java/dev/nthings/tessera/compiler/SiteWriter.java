package dev.nthings.tessera.compiler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes a [CompiledSite] to disk.
public final class SiteWriter {
  private SiteWriter() {}

  /// Writes every page and file of `compiledSite` under `outputDirectory`, creating directories as
  /// needed. Existing files are overwritten, and files left from an earlier build are kept. Throws
  /// UncheckedIOException if a file cannot be written.
  public static void writeToDirectory(CompiledSite compiledSite, Path outputDirectory) {
    var root = outputDirectory.toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
      for (var entry : compiledSite.pages().entrySet()) {
        writeFile(
            root,
            SiteRoute.outputPathFor(entry.getKey()),
            entry.getValue().getBytes(StandardCharsets.UTF_8));
      }
      for (var file : compiledSite.files().values()) {
        writeFile(root, file.relativeOutputPath(), file.contentView());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeFile(Path root, Path relative, byte[] content) throws IOException {
    var target = root.resolve(relative).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalArgumentException("Output escapes the site directory: " + relative);
    }
    var parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(target, content);
  }
}
