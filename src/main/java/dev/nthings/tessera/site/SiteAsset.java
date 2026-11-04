package dev.nthings.tessera.site;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/// Static file (image, font, icon, ...) published at `path` alongside the compiled pages.
///
/// Internal links and `<img src>` paths are validated against registered assets, just like routes.
public record SiteAsset(String path, String contentType, byte[] content) {

  public SiteAsset {
    if (!path.startsWith("/") || path.endsWith("/") || path.contains("..") || path.contains("//")) {
      throw new IllegalArgumentException("Asset path must be an absolute file path: " + path);
    }
    if (contentType.isBlank()) {
      throw new IllegalArgumentException("Asset content type cannot be blank: " + path);
    }
    content = content.clone();
  }

  /// Asset with its content type inferred from the file extension.
  public static SiteAsset of(String path, byte[] content) {
    return new SiteAsset(path, contentTypeFor(path), content);
  }

  /// UTF-8 text asset (SVG, JSON, CSS, ...).
  public static SiteAsset text(String path, String content) {
    return of(path, content.getBytes(StandardCharsets.UTF_8));
  }

  /// Asset read from `source` on disk.
  public static SiteAsset file(String path, Path source) {
    try {
      return of(path, Files.readAllBytes(source));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read asset " + source, e);
    }
  }

  @Override
  public byte[] content() {
    return content.clone();
  }

  /// Output location relative to the site root.
  Path relativeOutputPath() {
    return Path.of(path.substring(1));
  }

  static String contentTypeFor(String path) {
    String name = path.toLowerCase(Locale.ROOT);
    int dot = name.lastIndexOf('.');
    String ext = dot >= 0 ? name.substring(dot + 1) : "";
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
      case "json", "webmanifest" -> "application/json; charset=utf-8";
      case "txt" -> "text/plain; charset=utf-8";
      case "xml" -> "application/xml; charset=utf-8";
      case "pdf" -> "application/pdf";
      default -> "application/octet-stream";
    };
  }
}
