package dev.nthings.tessera.compiler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

/// Writes a [CompiledSite] to disk.
public final class SiteWriter {
  private SiteWriter() {}

  /// Writes every page and file of `compiledSite` under `outputDirectory`, creating directories as
  /// needed, then deletes every other file and empty directory left there by an earlier build, so a
  /// removed or renamed page is not deployed. Entries at the top of the directory whose names start
  /// with `.`, such as `.git`, are kept.
  ///
  /// Throws IllegalArgumentException, before writing anything, if `outputDirectory` is a file
  /// system root, the user's home directory, or the working directory or one of its parents, since
  /// cleaning it would delete unrelated files. Throws UncheckedIOException if a file cannot be
  /// written or deleted.
  public static void writeToDirectory(CompiledSite compiledSite, Path outputDirectory) {
    var root = outputDirectory.toAbsolutePath().normalize();
    checkCleanable(root);
    var written = new HashSet<Path>();
    try {
      Files.createDirectories(root);
      for (var entry : compiledSite.pages().entrySet()) {
        written.add(
            writeFile(
                root,
                SiteRoute.outputPathFor(entry.getKey()),
                entry.getValue().getBytes(StandardCharsets.UTF_8)));
      }
      var notFound = compiledSite.notFoundPage();
      if (notFound != null) {
        written.add(
            writeFile(
                root,
                Path.of(SiteArtifacts.NOT_FOUND_PATH.substring(1)),
                notFound.getBytes(StandardCharsets.UTF_8)));
      }
      for (var file : compiledSite.assets().values()) {
        written.add(writeFile(root, file.relativeOutputPath(), file.contentView()));
      }
      deleteStale(root, written);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void checkCleanable(Path root) {
    var home = Path.of(System.getProperty("user.home", "/")).toAbsolutePath().normalize();
    var working = Path.of("").toAbsolutePath().normalize();
    if (root.getParent() == null || root.equals(home) || working.startsWith(root)) {
      throw new IllegalArgumentException(
          "Refusing to clean "
              + root
              + ": choose a dedicated output directory such as target/site");
    }
  }

  private static void deleteStale(Path root, Set<Path> written) throws IOException {
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return kept(root, dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (!written.contains(file) && !kept(root, file)) {
              Files.delete(file);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException failure)
              throws IOException {
            if (failure != null) {
              throw failure;
            }
            if (!dir.equals(root)) {
              try (var entries = Files.list(dir)) {
                if (entries.findAny().isEmpty()) {
                  Files.delete(dir);
                }
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /// A dot entry directly under the root, such as `.git`, which a build never owns.
  private static boolean kept(Path root, Path path) {
    var name = path.getFileName();
    return root.equals(path.getParent()) && name != null && name.toString().startsWith(".");
  }

  private static Path writeFile(Path root, Path relative, byte[] content) throws IOException {
    var target = root.resolve(relative).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalArgumentException("Output escapes the site directory: " + relative);
    }
    var parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(target, content);
    return target;
  }
}
