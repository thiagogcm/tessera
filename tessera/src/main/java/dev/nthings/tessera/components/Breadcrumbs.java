package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ol;

import dev.nthings.tessera.core.AriaCurrent;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Breadcrumb trail, first link to last, rendered as a `<nav>` around an ordered list. Create one
/// with [#of(Link...)]. The last link gets `aria-current="page"`, and every crumb stays a link.
/// Separators are decorative, so screen readers skip them.
///
/// Without an [#id(String)], the compiler numbers instances `breadcrumbs-<n>`.
public final class Breadcrumbs extends BuiltinView<Breadcrumbs> {

  private final List<Link> links;
  private String label = "Breadcrumb";

  private Breadcrumbs(List<Link> links) {
    if (links.isEmpty()) {
      throw new IllegalArgumentException("Breadcrumbs requires at least 1 link");
    }
    this.links = links;
  }

  /// Creates a trail of `links`, first to last. Throws IllegalArgumentException if none are given.
  public static Breadcrumbs of(Link... links) {
    return new Breadcrumbs(List.of(links));
  }

  /// Sets the accessible name of the landmark; make it distinct when a page has several trails.
  /// The default is `Breadcrumb`.
  public Breadcrumbs label(String label) {
    this.label = label;
    return this;
  }

  @Override
  public Breadcrumbs self() {
    return this;
  }

  @Override
  String viewType() {
    return "breadcrumbs";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.block())
        .select(
            ":scope ol",
            s ->
                s.listStyleNone()
                    .flex()
                    .flexWrap()
                    .itemsCenter()
                    .gap(Tokens.SPACE_2)
                    .p(CssLength.px(0))
                    .m(CssLength.px(0))
                    .fontSize(Tokens.FONT_SIZE_SM))
        .select(":scope li", s -> s.inlineFlex().itemsCenter().gap(Tokens.SPACE_2))
        .select(
            ":scope li + li::before",
            s -> s.content(CssContent.decorative("/")).textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope a",
            s ->
                s.textColor(Tokens.TEXT_MUTED)
                    .textDecorationNone()
                    .on(State.HOVER, h -> h.textColor(Tokens.TEXT_PRIMARY)))
        .select(
            ":scope a[aria-current=\"page\"]",
            s -> s.textColor(Tokens.TEXT_PRIMARY).fontWeight(600));
  }

  @Override
  IntoElement render(RenderContext context) {
    var list = ol();
    for (var i = 0; i < links.size(); i++) {
      var isCurrent = i == links.size() - 1;
      var link = links.get(i);
      list.child(
          li().child(
                  a(link.href())
                      .onlyIf(isCurrent, el -> el.ariaCurrent(AriaCurrent.PAGE))
                      .text(link.label())));
    }
    return nav().ariaLabel(label).child(list);
  }
}
