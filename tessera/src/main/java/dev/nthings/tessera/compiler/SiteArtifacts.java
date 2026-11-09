package dev.nthings.tessera.compiler;

import java.util.List;
import org.owasp.encoder.Encode;

/// Serializes sitemap and search metadata.
final class SiteArtifacts {
  private SiteArtifacts() {}

  static final String SITEMAP_PATH = "/sitemap.xml";
  static final String SEARCH_INDEX_PATH = "/search-index.json";

  static String generateSitemapXml(String baseUrl, List<SiteRoute> routes) {
    var sb = new StringBuilder(1024);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    for (var route : routes) {
      var loc = baseUrl + route.path();
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
      var routeDesc = route.description();
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
            .append(escapeJson(heading.targetDomId()))
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

  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }
}
