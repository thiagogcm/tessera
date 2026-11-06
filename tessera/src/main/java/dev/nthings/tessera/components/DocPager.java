package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.GridPlacement;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import dev.nthings.tessera.foundation.FluentBuilder;
import org.jspecify.annotations.Nullable;

/// Editorial documentation pager providing prev/next navigation cards between site documentation
/// routes.
public final class DocPager implements RenderView, FluentBuilder<DocPager> {

  public record PageRef(String href, String title) {}

  private @Nullable PageRef prev;
  private @Nullable PageRef next;
  private String label = "Documentation navigation";

  private DocPager(@Nullable PageRef prev, @Nullable PageRef next) {
    this.prev = prev;
    this.next = next;
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

  @Override
  public DocPager self() {
    return this;
  }

  /// The landmark's accessible name. Pages with several pagers need a distinct name for each.
  public DocPager label(String label) {
    this.label = label;
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
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.grid()
                    .gap(Tokens.SPACE_4)
                    .mt(Tokens.SPACE_12)
                    .pt(Tokens.SPACE_6)
                    .borderBlockStartSubtle(Tokens.BORDER_SUBTLE))
        .containerMinInlineSize(
            CssLength.rem(36), c -> c.root(s -> s.gridColumns(TrackList.equal(2))))
        .select(
            ":scope > .pager-card",
            s ->
                s.flex()
                    .flexCol()
                    .gap(Tokens.SPACE_1)
                    .px(Tokens.SPACE_5)
                    .py(Tokens.SPACE_4)
                    .rounded(Tokens.RADIUS_LG)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .bg(Tokens.SURFACE_CARD)
                    .textDecorationNone()
                    .textColor(Tokens.TEXT_PRIMARY)
                    .motion(
                        Motion.of(
                            CssDuration.ms(150),
                            Easing.ease(),
                            Animatable.BORDER_COLOR,
                            Animatable.BACKGROUND)))
        .select(
            ":scope > .pager-card:hover",
            s -> s.borderSubtle(Tokens.ACCENT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
        .select(":scope > .pager-card.pager-prev", s -> s.gridColumn(GridPlacement.at(1)))
        .select(
            ":scope > .pager-card.pager-next",
            s -> s.gridColumn(GridPlacement.at(2)).textAlign(TextAlign.RIGHT))
        .select(
            ":scope > .pager-card > .pager-hint",
            s -> s.fontSize(Tokens.FONT_SIZE_XS).fontWeight(500).textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope > .pager-card > .pager-title",
            s -> s.fontSize(Tokens.FONT_SIZE_MD).fontWeight(600).textColor(Tokens.ACCENT_PRIMARY));
  }

  @Override
  public Element render() {
    var navEl = nav().ariaLabel(label).attr("class", "doc-pager");

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
