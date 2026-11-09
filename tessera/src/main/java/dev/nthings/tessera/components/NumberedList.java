package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Ordered `<ol>` list with tabular numbers; each item is wrapped in an `<li>`. Create one with
/// [#of(String...)] for plain text items or [#of(IntoElement...)] for items with markup.
public final class NumberedList extends BuiltinComponent {

  private final List<Element> items;

  private NumberedList(List<Element> items) {
    this.items = items;
  }

  /// Creates a list of plain text `items`.
  public static NumberedList of(String... items) {
    return new NumberedList(ListItems.texts(items));
  }

  /// Creates a list of `items`, each of which may contain markup.
  public static NumberedList of(IntoElement... items) {
    return new NumberedList(ListItems.elements(items));
  }

  @Override
  IntoElement render(RenderContext context) {
    return Elements.ol()
        .flex()
        .flexCol()
        .gap(Tokens.SPACE_2)
        .px(Tokens.SPACE_5)
        .tabularNums()
        .children(ListItems.wrap(items));
  }
}
