package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ul;

import dev.nthings.tessera.core.Accessible;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.WhiteSpace;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Navigation bar of links for site navigation, in a `<nav>`. Create one with
/// [#of(String, List)]; the link whose `href` equals [#active(String)] is marked with
/// `aria-current="page"` and an underline that slides between links.
///
/// Without an [#id(String)], the compiler numbers instances `nav-tabs-<n>`.
public final class NavTabs extends BuiltinView<NavTabs> {

  /// The active link's anchor; each bar's underline is positioned inside its own list.
  private static final Anchor ACTIVE = Anchor.named("tessera-active-nav");

  private final String label;
  private final List<Link> links;
  private @Nullable String active;

  private NavTabs(String label, List<Link> links) {
    this.label = label;
    this.links = List.copyOf(links);
  }

  /// Creates a bar of `links` whose `<nav>` is named `label`.
  public static NavTabs of(String label, List<Link> links) {
    return new NavTabs(label, links);
  }

  /// Marks the link whose `href` equals `href` as the current page. The comparison is exact, so
  /// `/guide` and `/guide/` differ.
  public NavTabs active(String href) {
    this.active = href;
    return this;
  }

  @Override
  public NavTabs self() {
    return this;
  }

  @Override
  String viewType() {
    return "nav-tabs";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.inlineFlex().maxInlineSize(CssLength.pct(100)))
        .select(
            ":scope ul",
            s ->
                s.listStyleNone()
                    .flex()
                    .flexWrap()
                    .itemsCenter()
                    .gap(Tokens.SPACE_1)
                    .relative()
                    .p(CssLength.px(0))
                    .m(CssLength.px(0)))
        .select(":scope li.active", s -> s.anchorName(ACTIVE));
    TabUnderline.apply(scope, ":scope ul::before", ACTIVE)
        .select(
            ":scope a",
            s ->
                s.block()
                    .px(Tokens.SPACE_2_5)
                    .py(Tokens.SPACE_1_5)
                    .rounded(Tokens.RADIUS_MD)
                    .textColor(Tokens.TEXT_MUTED)
                    .fontSize(Tokens.FONT_SIZE_SM)
                    .fontWeight(500)
                    .whiteSpace(WhiteSpace.NOWRAP)
                    .textDecorationNone()
                    .on(
                        State.HOVER,
                        h -> h.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE)))
        .select(":scope li.active > a", s -> s.textColor(Tokens.ACCENT_PRIMARY).fontWeight(600));
  }

  @Override
  IntoElement render(RenderContext context) {
    var list = ul();
    for (var item : links) {
      var isActive = item.href().equals(active);
      var link =
          a(item.href())
              .onlyIf(isActive, el -> el.ariaCurrent(Accessible.Current.PAGE))
              .text(item.label());
      var liEl = li().onlyIf(isActive, el -> el.attr("class", "active")).child(link);
      list.child(liEl);
    }
    return nav().ariaLabel(label).child(list);
  }
}
