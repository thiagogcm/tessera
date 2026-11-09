package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridPlacement;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import dev.nthings.tessera.css.Transition;
import org.jspecify.annotations.Nullable;

/// Previous and next page cards in a `<nav>`. Create one with [#of(Link, Link)]. Each card shows
/// "← Previous" or "Next →" above the link label; those hints are fixed English. The cards sit
/// side by side when the nearest ancestor container, such as the one [DocLayout] sets, is 36rem
/// or wider. Otherwise they stack.
public final class DocPager extends BuiltinView {

  private final @Nullable Link previous;
  private final @Nullable Link next;
  private String label = "Documentation navigation";

  private DocPager(@Nullable Link previous, @Nullable Link next) {
    this.previous = previous;
    this.next = next;
  }

  /// Creates a pager linking to the `previous` and `next` pages; a `null` side is omitted.
  public static DocPager of(@Nullable Link previous, @Nullable Link next) {
    return new DocPager(previous, next);
  }

  /// Names this instance in the enclosing view scope. Without it, the compiler assigns
  /// `tessera-doc-pager-<n>`.
  public DocPager id(String id) {
    this.id = id;
    return this;
  }

  /// Sets the accessible name of the landmark; make it distinct when a page has several pagers.
  /// The default is `Documentation navigation`.
  public DocPager label(String label) {
    this.label = label;
    return this;
  }

  @Override
  String viewType() {
    return "tessera-doc-pager";
  }

  @Override
  void configureScope(ScopeStyle scope) {
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
                            .properties(Animatable.BORDER_COLOR, Animatable.BACKGROUND))
                    .on(
                        State.HOVER,
                        h ->
                            h.border(Border.hairline(Tokens.ACCENT_PRIMARY))
                                .bg(Tokens.SURFACE_SUBTLE)))
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
  IntoElement render(RenderContext context) {
    var navEl = nav().ariaLabel(label).attr("class", "doc-pager");

    if (previous != null) {
      var prevCard =
          a(previous.href())
              .attr("class", "pager-card pager-prev")
              .children(
                  span().attr("class", "pager-hint").text("← Previous"),
                  span().attr("class", "pager-title").text(previous.label()));
      navEl.child(prevCard);
    }

    if (next != null) {
      var nextCard =
          a(next.href())
              .attr("class", "pager-card pager-next")
              .children(
                  span().attr("class", "pager-hint").text("Next →"),
                  span().attr("class", "pager-title").text(next.label()));
      navEl.child(nextCard);
    }

    return navEl;
  }
}
