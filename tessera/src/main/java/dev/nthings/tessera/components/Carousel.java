package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Pure CSS scroll-snap carousel backed by Chrome 156+ `scroll-snap-type: x mandatory`,
/// `container-type: scroll-state` with `@container scroll-state(snapped: x)`, and native
/// `scroll-marker-group: after` / `::scroll-marker:target-current` pagination dots.
public final class Carousel implements RenderView {

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
                    .scrollSnapType("x mandatory")
                    .scrollMarkerGroup("after")
                    .overscrollBehaviorX("contain")
                    .scrollbarWidthNone()
                    .counterReset("carousel-item")
                    .py(Tokens.SPACE_2))
        .select(
            ":scope > .carousel-item",
            s ->
                s.flexShrink(0)
                    .inlineSize(CssLength.expr("min(22rem, 82%)"))
                    .scrollSnapAlign("center")
                    .counterIncrement("carousel-item")
                    .containerScrollState())
        .select(
            ":scope > .carousel-item > .carousel-card",
            s ->
                s.p(Tokens.SPACE_5)
                    .blockSize(CssLength.pct(100))
                    .rounded(Tokens.RADIUS_XL)
                    .bg(Tokens.SURFACE_CARD)
                    .border("2px solid " + Tokens.BORDER_SUBTLE.varReference())
                    .transition(
                        "border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease"))
        .select(
            ":scope::scroll-marker-group",
            s -> s.flex().itemsCenter().justifyCenter().gap(Tokens.SPACE_2).pt(Tokens.SPACE_3))
        .select(
            ":scope > .carousel-item::scroll-marker",
            s ->
                s.content("\"\" / \"Slide \" counter(carousel-item)")
                    .inlineSize(CssLength.rem(0.625))
                    .blockSize(CssLength.rem(0.625))
                    .rounded(Tokens.RADIUS_FULL)
                    .bg(Tokens.BORDER_SUBTLE)
                    .borderNone()
                    .cursorPointer()
                    .transition("inline-size 0.2s ease, background-color 0.2s ease"))
        .select(
            ":scope > .carousel-item::scroll-marker:target-current",
            s -> s.bg(Tokens.ACCENT_PRIMARY).inlineSize(CssLength.rem(1.5)))
        // Native prev/next controls (::scroll-button), overlaid on the track edges.
        .select(
            ":scope::scroll-button(*)",
            s ->
                s.absolute()
                    .positionAnchor(anchorIdent())
                    .insetBlockStart(CssLength.expr("anchor(center)"))
                    .translate("0 -50%")
                    .inlineSize(CssLength.rem(2.25))
                    .blockSize(CssLength.rem(2.25))
                    .rounded(Tokens.RADIUS_FULL)
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .shadow("0 4px 12px -4px oklch(0% 0 0 / 0.2)")
                    .fontSize(Tokens.FONT_SIZE_LG)
                    .cursorPointer()
                    .zIndex(1))
        .select(
            ":scope::scroll-button(inline-start)",
            s ->
                s.content("\"‹\" / \"Previous slide\"")
                    .insetInlineStart(CssLength.expr("calc(anchor(left) + 0.5rem)")))
        .select(
            ":scope::scroll-button(inline-end)",
            s ->
                s.content("\"›\" / \"Next slide\"")
                    .insetInlineEnd(CssLength.expr("calc(anchor(right) + 0.5rem)")))
        .select(":scope::scroll-button(*):disabled", s -> s.opacity(0.35).cursor("default"))
        .motionSafe(
            m ->
                m.select(":scope", s -> s.scrollBehaviorSmooth())
                    .containerScrollState(
                        "snapped: x",
                        c ->
                            c.select(
                                ":scope > .carousel-item > .carousel-card",
                                s ->
                                    s.border("2px solid " + Tokens.ACCENT_PRIMARY.varReference())
                                        .shadow("0 12px 24px -6px oklch(0% 0 0 / 0.14)"))));
  }

  private String anchorIdent() {
    return "--carousel-" + id.slug();
  }

  @Override
  public Element render() {
    var track = div().role("region").ariaLabel(ariaLabel);
    for (var slide : slides) {
      track.child(
          div()
              .attr("class", "carousel-item")
              .child(div().attr("class", "carousel-card").child(slide.content())));
    }
    return track;
  }
}
