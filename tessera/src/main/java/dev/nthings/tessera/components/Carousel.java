package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.Counter;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.Overscroll;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.ScrollMarkerGroup;
import dev.nthings.tessera.css.ScrollSnap;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.SnapAlign;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.Translation;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.List;

/// Horizontal carousel of slides that snap into place as the user scrolls. It needs no script.
/// Create one with [#of(String)]; the track is a focusable region named by that label. Chromium
/// also shows native previous and next buttons and slide markers; other browsers keep the
/// scrollable track.
///
/// Without an [#id(String)], the compiler numbers instances `carousel-<n>`.
public final class Carousel extends BuiltinView<Carousel> {

  private static final Counter ITEM_COUNTER = Counter.named("carousel-item");

  /// Each slide is a scroll-state container, so its card can react while it is snapped.
  private static final Container ITEM = Container.unnamed(Container.Type.SCROLL_STATE);

  private final String label;
  private final List<Element> slides = new ArrayList<>();

  private Carousel(String label) {
    this.label = label;
  }

  /// Creates an empty carousel whose track region is named `label`. Give carousels on one page
  /// distinct labels or ids: the native scroll buttons are anchored by a name derived from them.
  public static Carousel of(String label) {
    return new Carousel(label);
  }

  /// Adds a slide with an `h3` title and a muted summary. The heading level does not follow the
  /// page outline.
  public Carousel slide(String title, String summary) {
    return slide(
        div()
            .flex()
            .flexCol()
            .gap(Tokens.SPACE_2)
            .children(
                h3().fontSize(Tokens.FONT_SIZE_LG).textWrap(TextWrap.BALANCE).text(title),
                p().textColor(Tokens.TEXT_MUTED).textWrap(TextWrap.PRETTY).text(summary)));
  }

  /// Adds a slide whose body is `content`.
  public Carousel slide(IntoElement content) {
    slides.add(content.intoElement());
    return this;
  }

  @Override
  public Carousel self() {
    return this;
  }

  @Override
  String viewType() {
    return "carousel";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.anchorName(anchorIdent())
                    .flex()
                    .gap(Tokens.SPACE_4)
                    .overflowX(Overflow.AUTO)
                    .scrollSnapType(ScrollSnap.mandatory(ScrollSnap.Axis.X))
                    .scrollMarkerGroup(ScrollMarkerGroup.AFTER)
                    .overscrollBehaviorX(Overscroll.CONTAIN)
                    .scrollbarWidthNone()
                    .counterReset(ITEM_COUNTER)
                    .py(Tokens.SPACE_2))
        .select(
            ":scope > .carousel-item",
            s ->
                s.flexShrink(0)
                    .inlineSize(CssLength.min(CssLength.rem(22), CssLength.pct(82)))
                    .scrollSnapAlign(SnapAlign.CENTER)
                    .counterIncrement(ITEM_COUNTER)
                    .container(ITEM))
        .select(
            ":scope > .carousel-item > .carousel-card",
            s ->
                s.p(Tokens.SPACE_5)
                    .blockSize(CssLength.pct(100))
                    .rounded(Tokens.RADIUS_XL)
                    .bg(Tokens.SURFACE_CARD)
                    .border(Border.solid(CssLength.px(2), Tokens.BORDER_SUBTLE))
                    .transition(
                        Transition.of(
                            CssDuration.ms(250),
                            Easing.ease(),
                            Animatable.BORDER_COLOR,
                            Animatable.BOX_SHADOW,
                            Animatable.TRANSFORM)))
        .select(
            ":scope::scroll-marker-group",
            s -> s.flex().itemsCenter().justifyCenter().gap(Tokens.SPACE_2).pt(Tokens.SPACE_3))
        .select(
            ":scope > .carousel-item::scroll-marker",
            s ->
                s.content(
                        CssContent.withAlternative(
                            CssContent.empty(),
                            CssContent.text("Slide "),
                            CssContent.counter(ITEM_COUNTER)))
                    .inlineSize(CssLength.rem(0.625))
                    .blockSize(CssLength.rem(0.625))
                    .rounded(Tokens.RADIUS_FULL)
                    .bg(Tokens.BORDER_SUBTLE)
                    .border(Border.none())
                    .cursor(Cursor.POINTER)
                    .transition(
                        Transition.of(
                            CssDuration.ms(200),
                            Easing.ease(),
                            Animatable.INLINE_SIZE,
                            Animatable.BACKGROUND_COLOR)))
        .select(
            ":scope > .carousel-item::scroll-marker:target-current",
            s -> s.bg(Tokens.ACCENT_PRIMARY).inlineSize(CssLength.rem(1.5)))
        // Native prev/next (::scroll-button) overlaid on the track edges.
        .select(
            ":scope::scroll-button(*)",
            s ->
                s.absolute()
                    .positionAnchor(anchorIdent())
                    .insetBlockStart(CssLength.anchor(CssLength.AnchorSide.CENTER))
                    .translate(Translation.y(CssLength.pct(-50)))
                    .inlineSize(CssLength.rem(2.25))
                    .blockSize(CssLength.rem(2.25))
                    .rounded(Tokens.RADIUS_FULL)
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .shadow(
                        Shadow.drop(
                            CssLength.px(4),
                            CssLength.px(12),
                            CssLength.px(-4),
                            CssColor.oklch(0, 0, 0, 0.2)))
                    .fontSize(Tokens.FONT_SIZE_LG)
                    .cursor(Cursor.POINTER)
                    .zIndex(1))
        .select(
            ":scope::scroll-button(inline-start)",
            s ->
                s.content(CssContent.labelled("‹", "Previous slide"))
                    .insetInlineStart(
                        CssLength.anchor(CssLength.AnchorSide.LEFT).plus(CssLength.rem(0.5))))
        .select(
            ":scope::scroll-button(inline-end)",
            s ->
                s.content(CssContent.labelled("›", "Next slide"))
                    .insetInlineEnd(
                        CssLength.anchor(CssLength.AnchorSide.RIGHT).plus(CssLength.rem(0.5))))
        .select(":scope::scroll-button(*):disabled", s -> s.opacity(0.35).cursor(Cursor.DEFAULT))
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(":scope", s -> s.scrollBehaviorSmooth())
                    .on(
                        ITEM.snapped(Container.Axis.X),
                        c ->
                            c.select(
                                ":scope > .carousel-item > .carousel-card",
                                s ->
                                    s.border(Border.solid(CssLength.px(2), Tokens.ACCENT_PRIMARY))
                                        .shadow(
                                            Shadow.drop(
                                                CssLength.px(12),
                                                CssLength.px(24),
                                                CssLength.px(-6),
                                                CssColor.oklch(0, 0, 0, 0.14))))));
  }

  /// Anchor for the native scroll buttons, unique per carousel on the page.
  private Anchor anchorIdent() {
    var instance = id;
    return Anchor.named("carousel-" + Fnv1a64.sanitizeLocalId(instance != null ? instance : label));
  }

  @Override
  IntoElement render(RenderContext context) {
    var track = div().role(Role.REGION).ariaLabel(label).tabIndex(0);
    for (var slide : slides) {
      track.child(
          div()
              .attr("class", "carousel-item")
              .child(div().attr("class", "carousel-card").child(slide)));
    }
    return track;
  }
}
