package dev.nthings.tessera.render;

import dev.nthings.tessera.foundation.Fnv1a64;
import dev.nthings.tessera.render.ResolvedNode.ResolvedEmpty;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedSlot;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.owasp.encoder.Encode;

/// Encodes a linked document for the HTML output boundary.
final class HtmlWriter {
  private HtmlWriter() {}

  static @Nullable String paint(
      PageDocument doc,
      List<ResolvedNode> bodyNodes,
      String cssContent,
      OutputMode outputMode,
      List<ResolvedNode> headNodes,
      StringBuilder out) {
    out.append("<!doctype html>\n<html lang=\"");
    out.append(Encode.forHtmlAttribute(doc.lang()));
    out.append("\">\n<head>\n<meta charset=\"utf-8\">\n");
    out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
    // rem-based layouts follow the OS text size setting, not only the browser's.
    out.append("<meta name=\"text-scale\" content=\"scale\">\n");
    out.append("<title>");
    out.append(Encode.forHtmlContent(doc.title()));
    out.append("</title>\n");
    for (var head : headNodes) {
      paintNode(head, out);
      out.append('\n');
    }

    String externalCssPath = null;
    switch (outputMode) {
      case OutputMode.InlineStyle _ -> {
        out.append("<style>\n");
        out.append(cssContent);
        out.append("</style>\n");
      }
      case OutputMode.ExternalAsset(var basePath) -> {
        var cssHash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, cssContent);
        var normalizedBase =
            basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        externalCssPath = normalizedBase + "/styles-" + Fnv1a64.toBase36Hash(cssHash) + ".css";
        out.append("<link rel=\"stylesheet\" href=\"");
        out.append(Encode.forHtmlAttribute(externalCssPath));
        out.append("\">\n");
      }
    }

    out.append("</head>\n<body>\n");
    for (var node : bodyNodes) {
      paintNode(node, out);
    }
    out.append("\n</body>\n</html>\n");
    return externalCssPath;
  }

  private static void paintNode(ResolvedNode node, StringBuilder out) {
    switch (node) {
      case ResolvedEmpty _ -> {}
      case ResolvedSlot _ ->
          throw new IllegalStateException("Unlinked late slot reached HTML output");
      case ResolvedText(var content) -> out.append(Encode.forHtmlContent(content));
      case ResolvedRaw(var trustedHtml) -> out.append(trustedHtml);
      case ResolvedTag tag -> {
        out.append('<').append(tag.tag().tagName());
        for (var attr : tag.attributes().entrySet()) {
          var value = attr.getValue();
          out.append(' ').append(attr.getKey());
          if (!value.isEmpty()) {
            out.append("=\"").append(Encode.forHtmlAttribute(value)).append('"');
          }
        }
        out.append('>');

        if (!tag.tag().isVoid()) {
          for (var child : tag.children()) {
            paintNode(child, out);
          }
          out.append("</").append(tag.tag().tagName()).append('>');
        }

        var companion = tag.companionSibling();
        if (companion != null) {
          paintNode(companion, out);
        }
      }
    }
  }
}
