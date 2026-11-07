package dev.nthings.tessera.render;

import dev.nthings.tessera.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Immutable snapshot of a compiled page's element tree for inspection: each element's tag, DOM
/// id, attributes, enclosing components, CSS scope, and authoring call site.
///
/// Source locations are present only while [dev.nthings.tessera.core.SourceTracking] is active.
/// Obtain a tree from [PreparedPage#tree()] or [PageCompiler#explain(PageDocument)].
public record PageTree(List<Node> head, List<Node> body) {

  public PageTree {
    head = List.copyOf(head);
    body = List.copyOf(body);
  }

  /// A node of the tree.
  public sealed interface Node permits Element, Text, Raw {}

  /// An element after expansion and linking.
  ///
  /// @param componentPath enclosing components, outermost first; view entries carry their instance
  ///     scope
  /// @param source where authoring code built the element, if tracked
  public record Element(
      String tag,
      List<String> componentPath,
      @Nullable SourceLocation source,
      Map<String, String> attributes,
      List<Node> children)
      implements Node {

    public Element {
      componentPath = List.copyOf(componentPath);
      attributes = Map.copyOf(attributes);
      children = List.copyOf(children);
    }

    /// The element's DOM id, if it has one.
    public @Nullable String id() {
      return attributes.get("id");
    }

    /// The `data-view` key of the CSS `@scope` block the element roots, if it roots one.
    public @Nullable String scope() {
      return attributes.get("data-view");
    }
  }

  /// Text content.
  public record Text(String content) implements Node {}

  /// Trusted raw HTML, which compilation does not inspect.
  public record Raw(int length) implements Node {}

  /// The tree as indented text: one line per node, with its component and source location.
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
        case ResolvedNode.ResolvedEmpty _ -> {}
        case ResolvedNode.ResolvedSlot _, ResolvedNode.ResolvedFragment _ ->
            throw PageValidator.unlinked();
        case ResolvedNode.ResolvedTag tag -> {
          var location = tag.location();
          converted.add(
              new Element(
                  tag.tag().tagName(),
                  location.componentPath(),
                  location.source(),
                  tag.attributes(),
                  convert(tag.children())));
          var companion = tag.companionSibling();
          if (companion != null) {
            converted.addAll(convert(List.of(companion)));
          }
        }
      }
    }
    return converted;
  }
}
