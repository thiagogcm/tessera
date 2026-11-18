package dev.nthings.tessera.document;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/// A closed set of resources for an already built browser entry. The classic launcher exports
/// `globalThis.GraalVM` with `Config` and `run(args, config)`. It consumes the verified primary
/// image in `config.tesseraWasmBytes` and any other payloads in `config.tesseraResources`, a map
/// from logical names to verified ArrayBuffers. Use the Web Image build adapter for GraalVM output.
public final class WasmBundle {
  private static final Pattern NAME =
      Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9_.-]*(/[A-Za-z0-9_-][A-Za-z0-9_.-]*)*");

  /// Resource kinds supported by the browser entry protocol, each published with its file
  /// extension; the extension determines the served media type.
  public enum Kind {
    LAUNCHER(".js"),
    WASM(".wasm"),
    DATA(".bin");

    private final String extension;

    Kind(String extension) {
      this.extension = extension;
    }

    public String extension() {
      return extension;
    }
  }

  /// One immutable resource with a logical name, SHA-384 SRI value, and its logical dependencies.
  /// Throws IllegalArgumentException if the name, integrity or Wasm header is invalid.
  public record Resource(
      String name, Kind kind, byte[] content, String integrity, List<String> references) {

    public Resource {
      if (!NAME.matcher(name).matches() || name.contains("..")) {
        throw new IllegalArgumentException("Invalid bundle resource name: " + name);
      }
      content = content.clone();
      references = List.copyOf(references);
      if (!sha384(content).equals(integrity)) {
        throw new IllegalArgumentException(
            "SHA-384 integrity mismatch for bundle resource " + name);
      }
      if (kind == Kind.WASM && !hasWasmHeader(content)) {
        throw new IllegalArgumentException(
            "Invalid WebAssembly header for bundle resource " + name);
      }
    }

    /// Creates a resource and computes its integrity from the supplied bytes.
    public static Resource of(String name, Kind kind, byte[] content, String... references) {
      return new Resource(name, kind, content, sha384(content), List.of(references));
    }

    @Override
    public byte[] content() {
      return content.clone();
    }
  }

  private final String launcher;
  private final String image;
  private final Map<String, Resource> resources;

  /// Validates the graph rooted at `launcher`, with `image` as its primary Wasm resource. Every
  /// dependency must exist and every resource must be reachable from the launcher.
  public WasmBundle(String launcher, String image, List<Resource> resources) {
    var indexed = new LinkedHashMap<String, Resource>();
    for (var resource : resources) {
      if (indexed.putIfAbsent(resource.name(), resource) != null) {
        throw new IllegalArgumentException("Duplicate bundle resource: " + resource.name());
      }
      if (resource.kind() == Kind.LAUNCHER && !resource.name().equals(launcher)) {
        throw new IllegalArgumentException("Unsupported additional launcher " + resource.name());
      }
    }
    var entry = indexed.get(launcher);
    var binary = indexed.get(image);
    if (entry == null
        || entry.kind() != Kind.LAUNCHER
        || binary == null
        || binary.kind() != Kind.WASM) {
      throw new IllegalArgumentException(
          "A bundle requires a launcher and a primary WebAssembly resource");
    }
    var reached = new HashSet<String>();
    var pending = new ArrayDeque<String>();
    pending.add(launcher);
    while (!pending.isEmpty()) {
      var name = pending.removeFirst();
      if (reached.add(name)) {
        var resource = indexed.get(name);
        if (resource == null) {
          throw new IllegalArgumentException("Missing bundle resource: " + name);
        }
        pending.addAll(resource.references());
      }
    }
    if (reached.size() != indexed.size()) {
      throw new IllegalArgumentException(
          "Unreachable bundle resource: "
              + indexed.keySet().stream()
                  .filter(name -> !reached.contains(name))
                  .findFirst()
                  .orElseThrow());
    }
    this.launcher = launcher;
    this.image = image;
    this.resources = Collections.unmodifiableMap(indexed);
  }

  public String launcher() {
    return launcher;
  }

  public String image() {
    return image;
  }

  public Map<String, Resource> resources() {
    return resources;
  }

  private static boolean hasWasmHeader(byte[] bytes) {
    return bytes.length >= 8
        && bytes[0] == 0
        && bytes[1] == 'a'
        && bytes[2] == 's'
        && bytes[3] == 'm'
        && bytes[4] == 1
        && bytes[5] == 0
        && bytes[6] == 0
        && bytes[7] == 0;
  }

  private static String sha384(byte[] bytes) {
    try {
      return "sha384-"
          + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-384").digest(bytes));
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-384 is unavailable", failure);
    }
  }
}
