package dev.nthings.tessera.compiler;

import dev.nthings.tessera.document.PageAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.owasp.encoder.Encode;

/// Serializes sitemap and search metadata.
final class SiteArtifacts {
  private SiteArtifacts() {}

  static final String SITEMAP_PATH = "/sitemap.xml";
  static final String SEARCH_INDEX_PATH = "/search-index.json";
  static final String HEADERS_PATH = "/_headers";
  static final String NOT_FOUND_PATH = "/404.html";
  static final String IMMUTABLE = "public, max-age=31536000, immutable";

  /// The `_headers` rules: `siteHeaders` for `/*`, a year-long immutable cache for each of
  /// `hashedFiles`, and each page's own `pageHeaders`, keyed by URL path. A directory holding only
  /// hashed files gets one `dir/*` rule, keeping the file under host rule limits; a hashed file
  /// beside other published files gets its own rule. `publishedFiles` lists every published URL
  /// path, pages included.
  static String generateHeadersFile(
      Map<String, String> siteHeaders,
      Set<String> hashedFiles,
      Set<String> publishedFiles,
      Map<String, Map<String, String>> pageHeaders) {
    var sb = new StringBuilder(512);
    if (!siteHeaders.isEmpty()) {
      rule(sb, "/*", siteHeaders);
    }
    var cache = Map.of("Cache-Control", IMMUTABLE);
    var directories = new LinkedHashSet<String>();
    for (var file : hashedFiles) {
      var directory = file.substring(0, file.lastIndexOf('/'));
      var prefix = directory + "/";
      var onlyHashed =
          !directory.isEmpty()
              && publishedFiles.stream()
                  .filter(path -> path.startsWith(prefix))
                  .allMatch(hashedFiles::contains);
      if (onlyHashed) {
        if (directories.add(directory)) {
          rule(sb, prefix + "*", cache);
        }
      } else {
        rule(sb, file, cache);
      }
    }
    pageHeaders.forEach((path, headers) -> rule(sb, path, headers));
    return sb.toString();
  }

  private static void rule(StringBuilder sb, String path, Map<String, String> headers) {
    sb.append(path).append('\n');
    headers.forEach(
        (name, value) -> sb.append("  ").append(name).append(": ").append(value).append('\n'));
  }

  /// Lists the absolute URL of each of `addresses`, which all carry one.
  static String generateSitemapXml(List<PageAddress> addresses) {
    var sb = new StringBuilder(1024);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    for (var address : addresses) {
      var loc = address.absoluteUrl().orElseThrow().toASCIIString();
      sb.append("  <url>\n");
      sb.append("    <loc>").append(Encode.forXmlContent(loc)).append("</loc>\n");
      sb.append("  </url>\n");
    }
    sb.append("</urlset>\n");
    return sb.toString();
  }

  static String generateSearchIndexJson(List<SiteRoute> routes, List<PreparedPage> preparedPages) {
    var sb = new StringBuilder(2048);
    sb.append("[\n");
    for (var i = 0; i < routes.size(); i++) {
      var route = routes.get(i);
      var title = route.displayTitle();
      sb.append("  {\n");
      sb.append("    \"path\": \"").append(escapeJson(route.path())).append("\",\n");
      sb.append("    \"title\": \"").append(escapeJson(title)).append("\"");
      var routeDesc = route.document().description();
      if (routeDesc != null) {
        sb.append(",\n    \"description\": \"").append(escapeJson(routeDesc)).append("\"");
      }
      sb.append(",\n    \"headings\": [");
      var headings = preparedPages.get(i).headings();
      for (var j = 0; j < headings.size(); j++) {
        var heading = headings.get(j);
        if (j > 0) {
          sb.append(", ");
        }
        sb.append("{\"id\": \"")
            .append(escapeJson(heading.domId()))
            .append("\", \"title\": \"")
            .append(escapeJson(heading.title()))
            .append("\", \"level\": ")
            .append(heading.level())
            .append("}");
      }
      sb.append("]\n");
      sb.append("  }");
      if (i < routes.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append("]\n");
    return sb.toString();
  }

  static String escapeJson(String s) {
    var out = new StringBuilder(s.length() + 8);
    for (var i = 0; i < s.length(); i++) {
      var c = s.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append("\\u%04x".formatted((int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }
}
