package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ol;

import dev.nthings.tessera.core.Accessible;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Semantic breadcrumb navigation (`<nav aria-label="Breadcrumb"><ol>`) using CSS `content: "/" /
/// ""` decorative separators (silent to screen readers) and `aria-current="page"` on the terminal
/// item.
public final class Breadcrumbs implements RenderView {

  public record Crumb(String href, String label) {
    public static Crumb of(String href, String label) {
      return new Crumb(href, label);
    }
  }

  private final ElementId id;
  private final List<Crumb> crumbs;
  private String label = "Breadcrumb";

  private Breadcrumbs(String id, List<Crumb> crumbs) {
    if (crumbs.isEmpty()) {
      throw new IllegalArgumentException("Breadcrumbs requires at least 1 Crumb");
    }
    this.id = ElementId.of(id);
    this.crumbs = crumbs;
  }

  public static Breadcrumbs of(String id, Crumb... crumbs) {
    return new Breadcrumbs(id, List.of(crumbs));
  }

  /// The landmark's accessible name. Pages with several trails need a distinct name for each.
  public Breadcrumbs label(String label) {
    this.label = label;
    return this;
  }

  @Override
  public ElementId entityId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
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
        .select(":scope a", s -> s.textColor(Tokens.TEXT_MUTED).textDecorationNone())
        .select(":scope a:hover", s -> s.textColor(Tokens.TEXT_PRIMARY))
        .select(
            ":scope a[aria-current=\"page\"]",
            s -> s.textColor(Tokens.TEXT_PRIMARY).fontWeight(600));
  }

  @Override
  public Element render() {
    var list = ol();
    for (var i = 0; i < crumbs.size(); i++) {
      var isCurrent = i == crumbs.size() - 1;
      var crumb = crumbs.get(i);
      var link =
          a(crumb.href())
              .when(isCurrent, el -> el.ariaCurrent(Accessible.Current.PAGE))
              .text(crumb.label());
      list.child(li().child(link));
    }
    return nav().ariaLabel(label).child(list);
  }
}
