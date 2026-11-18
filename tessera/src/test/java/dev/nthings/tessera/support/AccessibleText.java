package dev.nthings.tessera.support;

import org.jsoup.nodes.Element;

/// What assistive technology reads from an element's content.
public final class AccessibleText {

  private AccessibleText() {}

  /// `element`'s text without its `aria-hidden="true"` subtrees.
  public static String of(Element element) {
    var copy = element.clone();
    copy.select("[aria-hidden=true]").remove();
    return copy.text();
  }

  /// `element`'s `aria-label` when it has one, otherwise [#of(Element)].
  public static String name(Element element) {
    return element.hasAttr("aria-label") ? element.attr("aria-label") : of(element);
  }
}
