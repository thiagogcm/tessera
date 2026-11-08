package dev.nthings.tessera.render;

import dev.nthings.tessera.core.SourceLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Immutable snapshot of a compiled page's element tree, for inspection. Source locations are
/// present only while [SourceTracking] is active. Obtain a tree from [PreparedPage#tree()] or
/// [PageCompiler#explain(PageDocument)].
public record PageTree(List<Node> head, List<Node> body) {

  public PageTree {
    head = List.copyOf(head);
    body = List.copyOf(body);
  }

  /// A node in the snapshot: an element, text, or trusted raw HTML.
  public sealed interface Node permits Element, Text, Raw {}

  /// An element after expansion and linking. The component path lists the enclosing components,
  /// outermost first; view entries include their instance scope. The source is where authoring
  /// code built the element, when tracked.
  public record Element(
      String tag,
      List<String> componentPath,
      @Nullable SourceLocation source,
      Map<String, String> attributes,
      List<Node> children)
      implements Node {

    public Element {
      componentPath = List.copyOf(componentPath);
      attributes = Collections.unmodifiableMap(attributes);
      children = List.copyOf(children);
    }

    public @Nullable String id() {
      return attributes.get("id");
    }

    /// `data-view` key of the `@scope` block the element roots, if any.
    public @Nullable String scope() {
      return attributes.get("data-view");
    }
  }

  public record Text(String content) implements Node {}

  /// Trusted raw HTML, not inspected by compilation.
  public record Raw(int length) implements Node {}

  /// Renders the tree as indented text, with `<head>` and `<body>` sections and one line per node.
  public String toText() {
    var out = new StringBuilder();
    appendSection(out, "head", head);
    appendSection(out, "body", body);
    return out.toString();
  }

  private static void appendSection(StringBuilder out, String name, List<Node> nodes) {
    out.append('<').append(name).append(">\n");
    for (var node : nodes) {
      append(out, node, 1, List.of());
    }
  }

  private static void append(StringBuilder out, Node node, int depth, List<String> parentPath) {
    out.append("  ".repeat(depth));
    switch (node) {
      case Text text -> out.append('"').append(abbreviate(text.content())).append("\"\n");
      case Raw raw -> out.append("<raw html, ").append(raw.length()).append(" chars>\n");
      case Element element -> {
        out.append('<').append(element.tag());
        var id = element.id();
        if (id != null) {
          out.append(" id=\"").append(id).append('"');
        }
        for (var name : List.of("role", "aria-label", "href", "type", "name")) {
          var value = element.attributes().get(name);
          if (value != null) {
            out.append(' ').append(name).append("=\"").append(value).append('"');
          }
        }
        out.append('>');
        var scope = element.scope();
        if (scope != null) {
          out.append("  scope ").append(scope);
        }
        var path = element.componentPath();
        if (!path.equals(parentPath) && !path.isEmpty()) {
          out.append("  in ").append(path.getLast());
        }
        var source = element.source();
        if (source != null) {
          out.append("  at ").append(source);
        }
        out.append('\n');
        for (var child : element.children()) {
          append(out, child, depth + 1, path);
        }
      }
    }
  }

  private static String abbreviate(String content) {
    var oneLine = content.strip().replaceAll("\\s+", " ");
    return oneLine.length() <= 60 ? oneLine : oneLine.substring(0, 57) + "...";
  }

  static PageTree of(List<ResolvedNode> head, List<ResolvedNode> body) {
    return new PageTree(convert(head), convert(body));
  }

  private static List<Node> convert(List<ResolvedNode> nodes) {
    var converted = new ArrayList<Node>(nodes.size());
    for (var node : nodes) {
      switch (node) {
        case ResolvedNode.ResolvedText(var content) -> converted.add(new Text(content));
        case ResolvedNode.ResolvedRaw(var html) -> converted.add(new Raw(html.length()));
        case ResolvedNode.ResolvedTag tag -> {
          var location = tag.location();
          converted.add(
              new Element(
                  tag.tag().tagName(),
                  location.componentPath(),
                  location.source(),
                  tag.attributes(),
                  convert(tag.children())));
        }
      }
    }
    return converted;
  }
}
