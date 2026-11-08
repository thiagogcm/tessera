package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.Overscroll;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.ScrollMarkerGroup;
import dev.nthings.tessera.css.ScrollSnap;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.SnapAlign;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Translation;
import java.util.ArrayList;
import java.util.List;

/// Pure CSS scroll-snap carousel backed by `scroll-snap-type: x mandatory`. Chromium adds
/// `container-type: scroll-state` with `@container scroll-state(snapped: x)` and native
/// `scroll-marker-group` / `::scroll-marker` pagination; other browsers keep the scrollable track
/// and keyboard access.
public final class Carousel implements RenderView {

  private static final CssIdent ITEM_COUNTER = CssIdent.of("carousel-item");

  public record Slide(String title, String summary, Element content) {
    public static Slide ofText(String title, String summary) {
      return new Slide(
          title,
          summary,
          div()
              .flex()
              .flexCol()
              .gap(Tokens.SPACE_2)
              .children(
                  h3().fontSize(Tokens.FONT_SIZE_LG).textBalance().text(title),
                  p().textColor(Tokens.TEXT_MUTED).textPretty().text(summary)));
    }

    public static Slide of(String title, String summary, IntoElement content) {
      return new Slide(title, summary, content.intoElement());
    }
  }

  private final ElementId id;
  private final String ariaLabel;
  private final List<Slide> slides = new ArrayList<>();

  private Carousel(String id, String ariaLabel) {
    this.id = ElementId.of(id);
    this.ariaLabel = ariaLabel;
  }

  public static Carousel create(String id, String ariaLabel) {
    return new Carousel(id, ariaLabel);
  }

  public Carousel slide(String title, String summary) {
    slides.add(Slide.ofText(title, summary));
    return this;
  }

  public Carousel slide(String title, String summary, IntoElement content) {
    slides.add(Slide.of(title, summary, content));
    return this;
  }

  @Override
  public ElementId entityId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.anchorName(anchorIdent())
                    .flex()
                    .gap(Tokens.SPACE_4)
                    .overflowXAuto()
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
                    .containerScrollState())
        .select(
            ":scope > .carousel-item > .carousel-card",
            s ->
                s.p(Tokens.SPACE_5)
                    .blockSize(CssLength.pct(100))
                    .rounded(Tokens.RADIUS_XL)
                    .bg(Tokens.SURFACE_CARD)
                    .border(Border.solid(CssLength.px(2), Tokens.BORDER_SUBTLE))
                    .motion(
                        Motion.of(
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
                    .borderNone()
                    .cursorPointer()
                    .motion(
                        Motion.of(
                            CssDuration.ms(200),
                            Easing.ease(),
                            Animatable.INLINE_SIZE,
                            Animatable.BACKGROUND_COLOR)))
        .select(
            ":scope > .carousel-item::scroll-marker:target-current",
            s -> s.bg(Tokens.ACCENT_PRIMARY).inlineSize(CssLength.rem(1.5)))
        // Native prev/next controls (::scroll-button), overlaid on the track edges.
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
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .shadow(
                        Shadow.drop(
                            CssLength.px(4),
                            CssLength.px(12),
                            CssLength.px(-4),
                            CssColor.oklch(0, 0, 0, 0.2)))
                    .fontSize(Tokens.FONT_SIZE_LG)
                    .cursorPointer()
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
        .motionSafe(
            m ->
                m.select(":scope", s -> s.scrollBehaviorSmooth())
                    .containerScrollState(
                        "snapped: x",
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

  private CssIdent anchorIdent() {
    return CssIdent.custom("carousel-" + id.slug());
  }

  @Override
  public Element render(RenderContext context) {
    var track = div().role(Role.REGION).ariaLabel(ariaLabel).tabIndex(0);
    for (var slide : slides) {
      track.child(
          div()
              .attr("class", "carousel-item")
              .child(div().attr("class", "carousel-card").child(slide.content())));
    }
    return track;
  }
}
