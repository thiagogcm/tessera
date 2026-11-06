package dev.nthings.tessera.site;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Filesystem adapter for compiled site output.
public final class SiteWriter {
  private SiteWriter() {}

  public static void writeToDirectory(CompiledSite compiledSite, Path outputDirectory) {
    try {
      Files.createDirectories(outputDirectory);
      for (var entry : compiledSite.pagesByRoute().entrySet()) {
        writeFile(
            outputDirectory,
            SiteRoute.outputPathFor(entry.getKey()),
            entry.getValue().html().getBytes(StandardCharsets.UTF_8));
      }
      for (var file : compiledSite.files().values()) {
        writeFile(outputDirectory, file.relativeOutputPath(), file.contentView());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeFile(Path root, Path relative, byte[] content) throws IOException {
    var target = root.resolve(relative).normalize();
    if (!target.startsWith(root.normalize())) {
      throw new IllegalArgumentException("Output escapes the site directory: " + relative);
    }
    var parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(target, content);
  }
}
