package dev.nthings.tessera.support;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.view.EmbeddedRule;
import dev.nthings.tessera.view.LiveNode;
import dev.nthings.tessera.view.ViewRenderer;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.List;
import java.util.stream.Stream;

/// Live-render helpers for tests that inspect a linked view tree.
public final class Snapshots {
  private Snapshots() {}

  /// Renders portable `content` as a mounted view with page-level ids.
  public static ViewSnapshot<Void> render(IntoElement content) {
    return render(content, "");
  }

  public static ViewSnapshot<Void> render(IntoElement content, String scope) {
    var node = new LiveNode.Portable<Void>(content.intoElement());
    return ViewRenderer.render(_ -> node, scope, base -> base, (_, _, _, _, _) -> {});
  }

  /// The text of each embedded style rule of `snapshot`, in order.
  public static List<String> styleRules(ViewSnapshot<?> snapshot) {
    return snapshot.styleRules().stream().map(EmbeddedRule::css).toList();
  }

  /// Every tag in document order, looking through groups.
  public static Stream<ViewSnapshot.Tag<?>> tags(List<? extends ViewSnapshot.Node<?>> nodes) {
    return nodes.stream()
        .flatMap(node ->
            switch (node) {
              case ViewSnapshot.Tag<?> tag -> Stream.concat(Stream.of(tag), tags(tag.children()));
              case ViewSnapshot.Group<?> group -> tags(group.children());
              case ViewSnapshot.Text<?> _, ViewSnapshot.Special<?> _ -> Stream.empty();
            });
  }
}
