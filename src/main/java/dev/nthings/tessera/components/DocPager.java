package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteRoute;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Editorial documentation pager providing prev/next navigation cards between site documentation
/// routes.
public final class DocPager implements RenderView, FluentBuilder<DocPager> {

  public record PageRef(String href, String title) {}

  private @Nullable PageRef prev;
  private @Nullable PageRef next;
  private final String instanceId;

  public DocPager() {
    this(null, null);
  }

  public DocPager(@Nullable PageRef prev, @Nullable PageRef next) {
    this.prev = prev;
    this.next = next;
    this.instanceId = "docpager-" + Fnv1a64.toBase36Hash(System.nanoTime());
  }

  public static DocPager of(@Nullable PageRef prev, @Nullable PageRef next) {
    return new DocPager(prev, next);
  }

  public static DocPager prev(String href, String title) {
    return new DocPager(new PageRef(href, title), null);
  }

  public static DocPager next(String href, String title) {
    return new DocPager(null, new PageRef(href, title));
  }

  public static DocPager fromSite(SiteDefinition site, String currentPath) {
    String normalizedCurrent = SiteRoute.normalizeRoutePath(currentPath);
    List<SiteRoute> routes = site.routes();
    int currentIdx = -1;
    for (int i = 0; i < routes.size(); i++) {
      if (routes.get(i).path().equals(normalizedCurrent)) {
        currentIdx = i;
        break;
      }
    }
    if (currentIdx < 0) {
      return new DocPager();
    }
    PageRef prev = null;
    if (currentIdx > 0) {
      SiteRoute p = routes.get(currentIdx - 1);
      String title = p.displayTitle();
      prev = new PageRef(p.path(), title);
    }
    PageRef next = null;
    if (currentIdx < routes.size() - 1) {
      SiteRoute n = routes.get(currentIdx + 1);
      String title = n.displayTitle();
      next = new PageRef(n.path(), title);
    }
    return new DocPager(prev, next);
  }

  @Override
  public DocPager self() {
    return this;
  }

  public DocPager prevPage(String href, String title) {
    this.prev = new PageRef(href, title);
    return this;
  }

  public DocPager nextPage(String href, String title) {
    this.next = new PageRef(href, title);
    return this;
  }

  public @Nullable PageRef prev() {
    return prev;
  }

  public @Nullable PageRef next() {
    return next;
  }

  @Override
  public String viewType() {
    return "tessera-doc-pager";
  }

  @Override
  public ElementId entityId() {
    return ElementId.of(instanceId);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.grid()
                    .gap(CssLength.rem(1))
                    .mt(CssLength.rem(3))
                    .pt(CssLength.rem(1.5))
                    .borderBlockStartSubtle(Tokens.BORDER_SUBTLE))
        .containerMinInlineSize(
            CssLength.rem(36), c -> c.root(s -> s.gridColumns("repeat(2, 1fr)")))
        .select(
            ":scope > .pager-card",
            s ->
                s.flex()
                    .flexCol()
                    .gap(CssLength.rem(0.25))
                    .px(CssLength.rem(1.25))
                    .py(CssLength.rem(1))
                    .rounded(CssLength.rem(0.5))
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .bg(Tokens.SURFACE_CARD)
                    .textDecorationNone()
                    .textColor(Tokens.TEXT_PRIMARY)
                    .transition("border-color 0.15s ease, background 0.15s ease"))
        .select(
            ":scope > .pager-card:hover",
            s -> s.borderSubtle(Tokens.ACCENT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
        .select(":scope > .pager-card.pager-prev", s -> s.gridColumn("1"))
        .select(":scope > .pager-card.pager-next", s -> s.gridColumn("2").textAlign("right"))
        .select(
            ":scope > .pager-card > .pager-hint",
            s -> s.fontSize(CssLength.rem(0.8)).fontWeight(500).textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope > .pager-card > .pager-title",
            s -> s.fontSize(CssLength.rem(1)).fontWeight(600).textColor(Tokens.ACCENT_PRIMARY));
  }

  @Override
  public Element render() {
    var navEl = nav().ariaLabel("Documentation navigation").attr("class", "doc-pager");

    if (prev != null) {
      var prevCard =
          a(prev.href())
              .attr("class", "pager-card pager-prev")
              .children(
                  span().attr("class", "pager-hint").text("← Previous"),
                  span().attr("class", "pager-title").text(prev.title()));
      navEl.child(prevCard);
    }

    if (next != null) {
      var nextCard =
          a(next.href())
              .attr("class", "pager-card pager-next")
              .children(
                  span().attr("class", "pager-hint").text("Next →"),
                  span().attr("class", "pager-title").text(next.title()));
      navEl.child(nextCard);
    }

    return navEl;
  }
}
