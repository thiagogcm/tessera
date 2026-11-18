package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Unordered `<ul>` list; each item is wrapped in an `<li>`. Create one with [#of(String...)] for
/// plain text items or [#of(IntoElement...)] for items with markup.
public final class BulletList extends BuiltinComponent {

  private final List<Element> items;

  private BulletList(List<Element> items) {
    this.items = items;
  }

  /// Creates a list of plain text `items`.
  public static BulletList of(String... items) {
    return new BulletList(ListItems.texts(items));
  }

  /// Creates a list of `items`, each of which may contain markup.
  public static BulletList of(IntoElement... items) {
    return new BulletList(ListItems.elements(items));
  }

  @Override
  IntoElement expand(ViewContext context) {
    return Elements.ul()
        .flex()
        .flexCol()
        .gap(Tokens.SPACE_2)
        .px(Tokens.SPACE_5)
        .children(ListItems.wrap(items));
  }
}
