package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.header;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssFilter;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transition;

/// Header that stays at the top of the viewport while the page scrolls. Once it is stuck, its
/// surface turns translucent, blurs what is behind it and casts a shadow. Browsers without
/// scroll-state support still keep the header at the top, with the plain surface. Create one with
/// [#of(IntoElement)]; the content is placed inside a `<header>` element.
///
/// Without an [#id(String)], the compiler numbers instances `sticky-header-<n>`.
public final class StickyHeader extends BuiltinView<StickyHeader> {

  /// Scroll-state container queried by the header's own surface.
  private static final Container STUCK =
      Container.named("tessera-sticky-header", Container.Type.SCROLL_STATE);

  private final Element content;

  private StickyHeader(Element content) {
    this.content = content;
  }

  /// Creates a sticky header around `content`.
  public static StickyHeader of(IntoElement content) {
    return new StickyHeader(content.intoElement());
  }

  @Override
  public StickyHeader self() {
    return this;
  }

  @Override
  String viewType() {
    return "sticky-header";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.sticky().insetBlockStart(CssLength.px(0)).zIndex(30).container(STUCK))
        .select(
            ":scope > .sticky-surface",
            s ->
                s.bg(Tokens.SURFACE_CARD)
                    .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE))
                    .transition(
                        Transition.of(
                            CssDuration.ms(200),
                            Easing.ease(),
                            Animatable.BACKGROUND_COLOR,
                            Animatable.BOX_SHADOW)))
        .on(
            STUCK.stuck(Container.Edge.TOP),
            c ->
                c.select(
                    ":scope > .sticky-surface",
                    s ->
                        s.bg(Tokens.SURFACE_CARD.mixTransparent(88))
                            .backdropFilter(CssFilter.blur(CssLength.px(12)))
                            .shadow(
                                Shadow.drop(
                                    CssLength.px(6),
                                    CssLength.px(20),
                                    CssLength.px(-4),
                                    CssColor.oklch(0, 0, 0, 0.14)))));
  }

  @Override
  IntoElement render(RenderContext context) {
    return div().child(header().attr("class", "sticky-surface").child(content));
  }
}
