package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.owasp.encoder.Encode;

/// Encodes a linked document for the HTML output boundary.
final class HtmlWriter {

  private static final Pattern SCRIPT_CLOSE = Pattern.compile("</script", Pattern.CASE_INSENSITIVE);

  private HtmlWriter() {}

  /// The stylesheet and behavior modules shared by the pages of one compilation, hashed and
  /// encoded once: under external output each file is an [Asset] at a content-hashed path, which
  /// every page painted with these resources links.
  static final class Resources {
    private final String css;
    private final OutputMode outputMode;
    private final @Nullable Asset stylesheet;
    private final Map<ClientBehavior, Asset> modules = new LinkedHashMap<>();

    /// Resources for `css` and every module in `behaviors`, in order; a repeated behavior is
    /// encoded once.
    Resources(String css, OutputMode outputMode, Iterable<ClientBehavior> behaviors) {
      this.css = css;
      this.outputMode = outputMode;
      if (outputMode instanceof ExternalOutput(var base)) {
        stylesheet = Asset.text(stylesheetPath(base, css), css);
        for (var behavior : behaviors) {
          modules.computeIfAbsent(behavior, b -> Asset.text(behaviorPath(base, b), b.module()));
        }
      } else {
        stylesheet = null;
      }
    }

    /// The files the pages link: none inline; otherwise the stylesheet, then each module in
    /// first-use order.
    List<Asset> assets() {
      var linked = stylesheet;
      if (linked == null) {
        return List.of();
      }
      var assets = new ArrayList<Asset>(modules.size() + 1);
      assets.add(linked);
      assets.addAll(modules.values());
      return assets;
    }

    /// The path of `behavior`'s module, which must be one these resources were built with.
    private String moduleHref(ClientBehavior behavior) {
      return Objects.requireNonNull(modules.get(behavior), behavior.id()).path();
    }
  }

  /// Paints the document envelope and linked nodes. Under external output the stylesheet and
  /// `behaviors` are linked at the paths of `resources`' assets.
  static String paint(
      PageDocument doc,
      List<ResolvedNode> bodyNodes,
      List<ResolvedNode> headNodes,
      Resources resources,
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

    switch (resources.outputMode) {
      case InlineOutput _ -> {
        out.append("<style>\n");
        out.append(resources.css);
        out.append("</style>\n");
        for (var behavior : behaviors) {
          out.append("<script type=\"module\">\n");
          out.append(inlineModule(behavior.module()));
          out.append("\n</script>\n");
        }
      }
      case ExternalOutput _ -> {
        out.append("<link rel=\"stylesheet\" href=\"");
        var stylesheet = Objects.requireNonNull(resources.stylesheet);
        out.append(Encode.forHtmlAttribute(stylesheet.path()));
        out.append("\">\n");
        for (var behavior : behaviors) {
          out.append("<script type=\"module\" src=\"");
          out.append(Encode.forHtmlAttribute(resources.moduleHref(behavior)));
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
