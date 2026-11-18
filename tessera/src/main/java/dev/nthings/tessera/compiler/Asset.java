package dev.nthings.tessera.compiler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/// A file published at `path`: a static file registered on a site, or a stylesheet or client page
/// script produced under [OutputMode#external(String)].
///
/// The path must start with `/` and must not end with `/`, contain `..`, or contain `//`; the
/// content type must not be blank. Violations throw IllegalArgumentException. Site compilation
/// validates internal `href`, `src` and `poster` URLs against registered assets, as it does routes.
/// The record holds its own copy of the bytes.
public record Asset(String path, String contentType, byte[] content) {

  public Asset {
    if (!path.startsWith("/") || path.endsWith("/") || path.contains("..") || path.contains("//")) {
      throw new IllegalArgumentException("Asset path must be an absolute file path: " + path);
    }
    if (contentType.isBlank()) {
      throw new IllegalArgumentException("Asset content type cannot be blank: " + path);
    }
    content = content.clone();
  }

  /// Creates an asset with `content`, inferring the content type from the extension of `path`
  /// (`application/octet-stream` when unknown).
  public static Asset of(String path, byte[] content) {
    return new Asset(path, contentTypeFor(path), content);
  }

  /// Creates a UTF-8 text asset, such as SVG, JSON or CSS, with the content type inferred from the
  /// extension of `path`.
  public static Asset text(String path, String content) {
    return of(path, content.getBytes(StandardCharsets.UTF_8));
  }

  /// Reads `source` into memory now and creates an asset at `path` from its bytes. Throws
  /// UncheckedIOException if the file cannot be read.
  public static Asset file(String path, Path source) {
    try {
      return of(path, Files.readAllBytes(source));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read asset " + source, e);
    }
  }

  /// A copy of the file's bytes.
  @Override
  public byte[] content() {
    return content.clone();
  }

  /// The content decoded as UTF-8, for text assets such as stylesheets and modules.
  public String textContent() {
    return new String(content, StandardCharsets.UTF_8);
  }

  /// Owned bytes, not a defensive copy.
  byte[] contentView() {
    return content;
  }

  Path relativeOutputPath() {
    return Path.of(path.substring(1));
  }

  static String contentTypeFor(String path) {
    var name = path.toLowerCase(Locale.ROOT);
    var dot = name.lastIndexOf('.');
    var ext = dot >= 0 ? name.substring(dot + 1) : "";
    return switch (ext) {
      case "svg" -> "image/svg+xml";
      case "png" -> "image/png";
      case "jpg", "jpeg" -> "image/jpeg";
      case "webp" -> "image/webp";
      case "avif" -> "image/avif";
      case "gif" -> "image/gif";
      case "ico" -> "image/x-icon";
      case "woff2" -> "font/woff2";
      case "woff" -> "font/woff";
      case "css" -> "text/css; charset=utf-8";
      case "js", "mjs" -> "text/javascript; charset=utf-8";
      case "json", "webmanifest" -> "application/json; charset=utf-8";
      case "txt" -> "text/plain; charset=utf-8";
      case "xml" -> "application/xml; charset=utf-8";
      case "pdf" -> "application/pdf";
      case "wasm" -> "application/wasm";
      default -> "application/octet-stream";
    };
  }
}
