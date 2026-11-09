package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.header;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
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
import dev.nthings.tessera.foundation.Fnv1a64;

/// Header that stays at the top of the viewport while the page scrolls. Once it is stuck, its
/// surface turns translucent, blurs what is behind it and casts a shadow. Browsers without
/// scroll-state support still keep the header at the top, with the plain surface. The content
/// is placed inside a `<header>` element.
public final class StickyHeader implements View {

  private final String id;
  private final Container container;
  private final Element content;

  /// Creates a sticky header with the given `id`, which is unique within its view scope.
  public StickyHeader(String id, IntoElement content) {
    this.id = Fnv1a64.sanitizeLocalId(id);
    this.container = Container.named("sticky-" + this.id, Container.Type.SCROLL_STATE);
    this.content = content.intoElement();
  }

  @Override
  public String viewId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.sticky().insetBlockStart(CssLength.px(0)).zIndex(30).container(container))
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
            container.stuck(Container.Edge.TOP),
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
  public Element render(RenderContext context) {
    return div().child(header().attr("class", "sticky-surface").child(content));
  }
}
