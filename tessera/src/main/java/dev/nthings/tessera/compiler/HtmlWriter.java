package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.owasp.encoder.Encode;

/// Encodes a linked document for the HTML output boundary.
final class HtmlWriter {

  private static final Pattern SCRIPT_CLOSE = Pattern.compile("</script", Pattern.CASE_INSENSITIVE);

  private HtmlWriter() {}

  /// Paints the document envelope and linked nodes. Under external output the stylesheet and
  /// modules are linked at the paths [#assets] returns for the same inputs.
  static String paint(
      PageDocument doc,
      List<ResolvedNode> bodyNodes,
      List<ResolvedNode> headNodes,
      String css,
      OutputMode outputMode,
      List<ClientBehavior> behaviors) {
    var out = new StringBuilder(4096);
    out.append("<!doctype html>\n<html lang=\"");
    out.append(Encode.forHtmlAttribute(doc.lang()));
    out.append("\">\n<head>\n<meta charset=\"utf-8\">\n");
    out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
    // rem layouts follow the OS text size, not only the browser's.
    out.append("<meta name=\"text-scale\" content=\"scale\">\n");
    out.append("<title>");
    out.append(Encode.forHtmlContent(doc.title()));
    out.append("</title>\n");
    for (var head : headNodes) {
      paintNode(head, out);
      out.append('\n');
    }

    switch (outputMode) {
      case InlineOutput _ -> {
        out.append("<style>\n");
        out.append(css);
        out.append("</style>\n");
        for (var behavior : behaviors) {
          out.append("<script type=\"module\">\n");
          out.append(inlineModule(behavior.module()));
          out.append("\n</script>\n");
        }
      }
      case ExternalOutput(var base) -> {
        out.append("<link rel=\"stylesheet\" href=\"");
        out.append(Encode.forHtmlAttribute(stylesheetPath(base, css)));
        out.append("\">\n");
        for (var behavior : behaviors) {
          out.append("<script type=\"module\" src=\"");
          out.append(Encode.forHtmlAttribute(behaviorPath(base, behavior)));
          out.append("\"></script>\n");
        }
      }
    }

    out.append("</head>\n<body>\n");
    for (var node : bodyNodes) {
      paintNode(node, out);
    }
    out.append("\n</body>\n</html>\n");
    return out.toString();
  }

  /// The files a page painted with these inputs links: none inline; otherwise the stylesheet,
  /// then each behavior module.
  static List<Asset> assets(String css, OutputMode outputMode, List<ClientBehavior> behaviors) {
    return switch (outputMode) {
      case InlineOutput _ -> List.of();
      case ExternalOutput(var base) -> {
        var assets = new ArrayList<Asset>(behaviors.size() + 1);
        assets.add(Asset.text(stylesheetPath(base, css), css));
        for (var behavior : behaviors) {
          assets.add(Asset.text(behaviorPath(base, behavior), behavior.module()));
        }
        yield assets;
      }
    };
  }

  private static String stylesheetPath(String base, String css) {
    return hashedPath(base, "styles", css, ".css");
  }

  private static String behaviorPath(String base, ClientBehavior behavior) {
    return hashedPath(
        base + "/behaviors", Fnv1a64.sanitizeIdentifier(behavior.id()), behavior.module(), ".js");
  }

  /// `dir/name-<content hash>ext`, so changed files are never served stale from cache.
  private static String hashedPath(String dir, String name, String content, String extension) {
    var hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, content);
    return dir + "/" + name + "-" + Fnv1a64.toBase36Hash(hash) + extension;
  }

  /// Keeps a module containing a literal end tag from closing the script element.
  private static String inlineModule(String module) {
    return SCRIPT_CLOSE.matcher(module).replaceAll(ignored -> "<\\/script");
  }

  private static void paintNode(ResolvedNode node, StringBuilder out) {
    switch (node) {
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
      }
    }
  }
}
