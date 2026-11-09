package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import java.util.Arrays;
import java.util.List;

/// Item conversions shared by the list components.
final class ListItems {

  private ListItems() {}

  static List<Element> texts(String... items) {
    return Arrays.stream(items).<Element>map(Elements::text).toList();
  }

  static List<Element> elements(IntoElement... items) {
    return Arrays.stream(items).map(IntoElement::intoElement).toList();
  }

  static List<TagElement> wrap(List<Element> items) {
    return items.stream().map(item -> Elements.li().child(item)).toList();
  }
}
