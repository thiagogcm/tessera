package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.BundlePublication.File;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import dev.nthings.tessera.internal.view.LinkedNode;
import dev.nthings.tessera.internal.view.LinkedNode.Tag;
import dev.nthings.tessera.internal.view.LinkedNode.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.owasp.encoder.Encode;

/// Encodes a linked document for the HTML output boundary.
final class HtmlWriter {

  private static final Pattern SCRIPT_CLOSE = Pattern.compile("</script", Pattern.CASE_INSENSITIVE);
  private static final Pattern STYLE_CLOSE = Pattern.compile("</style", Pattern.CASE_INSENSITIVE);

  private HtmlWriter() {}

  /// The stylesheet and behavior modules shared by the pages of one compilation, hashed and encoded
  /// once: under external output each file is an [Asset] at a content-hashed path, which every page
  /// painted with these resources links.
  static final class Resources {
    private final String css;
    private final OutputMode outputMode;
    private final @Nullable Asset stylesheet;
    private final Map<PageScript, Asset> modules = new LinkedHashMap<>();

    /// Resources for `css` and every module in `behaviors`, in order; a repeated behavior is
    /// encoded once.
    Resources(String css, OutputMode outputMode, Iterable<PageScript> behaviors) {
      this.css = css;
      this.outputMode = outputMode;
      var base = outputMode.assetBase();
      if (base != null) {
        stylesheet = Asset.text(stylesheetPath(base, css), css);
        for (var behavior : behaviors) {
          modules.computeIfAbsent(behavior, b -> Asset.text(scriptPath(base, b), b.module()));
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
    private String moduleHref(PageScript behavior) {
      return Objects.requireNonNull(modules.get(behavior), behavior.id()).path();
    }
  }

  /// Paints the document envelope and linked nodes. Under external output the stylesheet and
  /// `behaviors` are linked at the paths of `resources`' assets.
  static String emit(
      PageDocument doc,
      List<LinkedNode<String>> bodyNodes,
      List<LinkedNode<String>> headNodes,
      Resources resources,
      List<PageScript> behaviors,
      @Nullable File bootstrap,
      @Nullable String speculationRules) {
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
    if (speculationRules != null) {
      out.append("<script type=\"speculationrules\">")
          .append(inlineModule(speculationRules))
          .append("</script>\n");
    }
    for (var head : headNodes) {
      writeNode(head, out);
      out.append('\n');
    }

    if (!resources.outputMode.isExternal()) {
      out.append("<style>\n");
      out.append(resources.css);
      out.append("</style>\n");
      for (var behavior : behaviors) {
        out.append("<script type=\"module\">\n");
        out.append(inlineModule(behavior.module()));
        out.append("\n</script>\n");
      }
    } else {
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

    if (bootstrap != null) {
      out.append("<script type=\"module\" src=\"")
          .append(Encode.forHtmlAttribute(bootstrap.asset().path()))
          .append("\" integrity=\"")
          .append(Encode.forHtmlAttribute(bootstrap.integrity()))
          .append("\" crossorigin=\"anonymous\"></script>\n");
    }
    out.append("</head>\n<body>\n");
    for (var node : bodyNodes) {
      writeNode(node, out);
    }
    out.append("\n</body>\n</html>\n");
    return out.toString();
  }

  private static String stylesheetPath(String base, String css) {
    return hashedPath(base, "styles", css, ".css");
  }

  private static String scriptPath(String base, PageScript behavior) {
    return hashedPath(
        base + "/scripts", Fnv1a64.sanitizeIdentifier(behavior.id()), behavior.module(), ".js");
  }

  /// `dir/name-<content hash>ext`, so changed files are never served stale from cache.
  static String hashedPath(String dir, String name, String content, String extension) {
    var hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, content);
    return dir + "/" + name + "-" + Fnv1a64.toBase36Hash(hash) + extension;
  }

  /// Keeps a module containing a literal end tag from closing the script element.
  private static String inlineModule(String module) {
    return SCRIPT_CLOSE.matcher(module).replaceAll(Matcher.quoteReplacement("<\\/script"));
  }

  private static void writeNode(LinkedNode<String> node, StringBuilder out) {
    switch (node) {
      case LinkedNode.Group<String> group ->
          group.children().forEach(child -> writeNode(child, out));
      case Text(var content) -> out.append(Encode.forHtmlContent(content));
      case LinkedNode.Special<String> raw -> out.append(raw.payload());
      case Tag<String> tag -> {
        out.append('<').append(tag.tag().tagName());
        for (var attr : tag.attributes().entrySet()) {
          var value = attr.getValue();
          out.append(' ').append(attr.getKey());
          if (!value.isEmpty()) {
            // Character references survive HTML's line-ending normalization.
            out.append("=\"")
                .append(Encode.forHtmlAttribute(value).replace("\r", "&#13;"))
                .append('"');
          }
        }
        var source = tag.location().source();
        if (source != null) {
          out.append(" data-src=\"").append(Encode.forHtmlAttribute(source.toString())).append('"');
        }
        out.append('>');

        if (!tag.tag().isVoid()) {
          // HTML consumes one leading line feed after a pre or textarea start tag.
          if ((tag.defaults().text() || tag.tag() == HtmlTag.PRE)
              && firstText(tag.children()) instanceof Text<String> text
              && text.content().startsWith("\n")) {
            out.append('\n');
          }
          if (tag.tag() == HtmlTag.STYLE) {
            writeRawText(tag.children(), STYLE_CLOSE, "<\\/style", out);
          } else {
            for (var child : tag.children()) {
              writeNode(child, out);
            }
          }
          out.append("</").append(tag.tag().tagName()).append('>');
        }
      }
    }
  }

  /// The first node holding content, looking through groups, or null if there is none.
  private static @Nullable LinkedNode<String> firstText(List<LinkedNode<String>> nodes) {
    for (var node : nodes) {
      if (node instanceof LinkedNode.Group<String> group) {
        var first = firstText(group.children());
        if (first != null) {
          return first;
        }
      } else {
        return node;
      }
    }
    return null;
  }

  /// Writes the text of a raw-text element such as `<style>` unencoded, escaping only a literal end
  /// tag that would close the element.
  private static void writeRawText(
      List<LinkedNode<String>> nodes, Pattern close, String escaped, StringBuilder out) {
    for (var node : nodes) {
      switch (node) {
        case Text(var content) ->
            out.append(close.matcher(content).replaceAll(Matcher.quoteReplacement(escaped)));
        case LinkedNode.Group<String> group -> writeRawText(group.children(), close, escaped, out);
        default -> writeNode(node, out);
      }
    }
  }
}
