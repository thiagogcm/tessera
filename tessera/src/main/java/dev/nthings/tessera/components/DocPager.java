package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridPlacement;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.foundation.FluentBuilder;
import org.jspecify.annotations.Nullable;

/// Previous and next page cards in a `<nav>`. Each card shows "← Previous" or "Next →" above the
/// page title; those labels are fixed English. The cards sit side by side when the nearest ancestor
/// container, such as the one `Toc.docLayout` sets, is 36rem or wider. Otherwise they stack.
public final class DocPager implements View, FluentBuilder<DocPager> {

  /// A page link: its `href` and the `title` shown in the card.
  public record PageRef(String href, String title) {}

  private @Nullable PageRef prev;
  private @Nullable PageRef next;
  private String label = "Documentation navigation";

  private DocPager(@Nullable PageRef prev, @Nullable PageRef next) {
    this.prev = prev;
    this.next = next;
  }

  /// Creates a pager with an optional previous and next page. A null side is omitted.
  public static DocPager of(@Nullable PageRef prev, @Nullable PageRef next) {
    return new DocPager(prev, next);
  }

  /// Creates a pager with only a previous page link.
  public static DocPager prev(String href, String title) {
    return new DocPager(new PageRef(href, title), null);
  }

  /// Creates a pager with only a next page link.
  public static DocPager next(String href, String title) {
    return new DocPager(null, new PageRef(href, title));
  }

  @Override
  public DocPager self() {
    return this;
  }

  /// Accessible name of the landmark; make it distinct when a page has several pagers. The default
  /// is `Documentation navigation`.
  public DocPager label(String label) {
    this.label = label;
    return this;
  }

  /// Sets the previous page link, replacing any earlier one.
  public DocPager prevPage(String href, String title) {
    this.prev = new PageRef(href, title);
    return this;
  }

  /// Sets the next page link, replacing any earlier one.
  public DocPager nextPage(String href, String title) {
    this.next = new PageRef(href, title);
    return this;
  }

  /// Returns the previous page link set on this pager, or `null` if there is none. This reads the
  /// link; it is not the [#prev(String, String)] factory.
  public @Nullable PageRef prev() {
    return prev;
  }

  /// Returns the next page link set on this pager, or `null` if there is none. This reads the
  /// link; it is not the [#next(String, String)] factory.
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
                    .borderBlockStart(Border.hairline(Tokens.BORDER_SUBTLE)))
        .on(
            Container.unnamed(Container.Type.INLINE_SIZE).atLeast(CssLength.rem(36)),
            c -> c.root(s -> s.gridColumns(TrackList.equal(2))))
        .select(
            ":scope > .pager-card",
            s ->
                s.flex()
                    .flexCol()
                    .gap(Tokens.SPACE_1)
                    .px(Tokens.SPACE_5)
                    .py(Tokens.SPACE_4)
                    .rounded(Tokens.RADIUS_LG)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .bg(Tokens.SURFACE_CARD)
                    .textDecorationNone()
                    .textColor(Tokens.TEXT_PRIMARY)
                    .transition(
                        Transition.controlFeedback()
                            .properties(Animatable.BORDER_COLOR, Animatable.BACKGROUND)))
        .select(
            ":scope > .pager-card:hover",
            s -> s.border(Border.hairline(Tokens.ACCENT_PRIMARY)).bg(Tokens.SURFACE_SUBTLE))
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
  public Element render(RenderContext context) {
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
