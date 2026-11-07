package dev.nthings.tessera.render;

import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.foundation.Fnv1a64;
import dev.nthings.tessera.render.ResolvedNode.ResolvedEmpty;
import dev.nthings.tessera.render.ResolvedNode.ResolvedFragment;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedSlot;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.owasp.encoder.Encode;

/// Encodes a linked document for the HTML output boundary.
final class HtmlWriter {

  private static final Pattern SCRIPT_CLOSE = Pattern.compile("</script", Pattern.CASE_INSENSITIVE);

  private HtmlWriter() {}

  record PaintResult(@Nullable String externalCssPath, List<CompiledAsset> assets) {
    PaintResult {
      assets = List.copyOf(assets);
    }
  }

  static PaintResult paint(
      PageDocument doc,
      List<ResolvedNode> bodyNodes,
      String cssContent,
      OutputMode outputMode,
      List<ResolvedNode> headNodes,
      List<ClientBehavior> behaviors,
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
    String assetBasePath = null;
    switch (outputMode) {
      case OutputMode.InlineStyle _ -> {
        out.append("<style>\n");
        out.append(cssContent);
        out.append("</style>\n");
      }
      case OutputMode.ExternalAsset(var basePath) -> {
        var cssHash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, cssContent);
        assetBasePath =
            basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        externalCssPath = assetBasePath + "/styles-" + Fnv1a64.toBase36Hash(cssHash) + ".css";
        out.append("<link rel=\"stylesheet\" href=\"");
        out.append(Encode.forHtmlAttribute(externalCssPath));
        out.append("\">\n");
      }
    }

    var assets = new ArrayList<CompiledAsset>();
    for (var behavior : behaviors) {
      if (assetBasePath == null) {
        out.append("<script type=\"module\">\n");
        out.append(inlineModule(behavior.module()));
        out.append("\n</script>\n");
      } else {
        var behaviorHash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, behavior.module());
        var path =
            assetBasePath
                + "/behaviors/"
                + Fnv1a64.sanitizeIdentifier(behavior.id())
                + "-"
                + Fnv1a64.toBase36Hash(behaviorHash)
                + ".js";
        out.append("<script type=\"module\" src=\"");
        out.append(Encode.forHtmlAttribute(path));
        out.append("\"></script>\n");
        assets.add(
            new CompiledAsset(
                path,
                "text/javascript; charset=utf-8",
                behavior.module().getBytes(StandardCharsets.UTF_8)));
      }
    }

    out.append("</head>\n<body>\n");
    for (var node : bodyNodes) {
      paintNode(node, out);
    }
    out.append("\n</body>\n</html>\n");
    return new PaintResult(externalCssPath, assets);
  }

  /// Keeps a module containing a literal end tag from closing the script element.
  private static String inlineModule(String module) {
    return SCRIPT_CLOSE.matcher(module).replaceAll(ignored -> "<\\/script");
  }

  private static void paintNode(ResolvedNode node, StringBuilder out) {
    switch (node) {
      case ResolvedEmpty _ -> {}
      case ResolvedSlot _ ->
          throw new IllegalStateException("Unlinked late slot reached HTML output");
      case ResolvedFragment(var children) -> {
        for (var child : children) {
          paintNode(child, out);
        }
      }
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
        var source = tag.location().source();
        if (source != null) {
          out.append(" data-src=\"").append(Encode.forHtmlAttribute(source.toString())).append('"');
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
