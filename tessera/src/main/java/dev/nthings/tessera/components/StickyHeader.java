package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.header;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssFilter;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;

/// Header that stays at the top of the viewport while the page scrolls. Once it is stuck, its
/// surface turns translucent, blurs what is behind it and casts a shadow. Browsers without
/// scroll-state support still keep the header at the top, with the plain surface. The content
/// is placed inside a `<header>` element.
public final class StickyHeader implements RenderView {

  private final ElementId id;
  private final CssIdent containerName;
  private final Element content;

  /// Creates a sticky header with the given `id`, which is unique within its view scope.
  public StickyHeader(String id, IntoElement content) {
    this.id = ElementId.of(id);
    this.containerName = CssIdent.of("sticky-" + this.id.slug());
    this.content = content.intoElement();
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
                s.sticky()
                    .insetBlockStart(CssLength.px(0))
                    .zIndex(30)
                    .containerScrollState(containerName))
        .select(
            ":scope > .sticky-surface",
            s ->
                s.bg(Tokens.SURFACE_CARD)
                    .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
                    .motion(
                        Motion.of(
                            CssDuration.ms(200),
                            Easing.ease(),
                            Animatable.BACKGROUND_COLOR,
                            Animatable.BOX_SHADOW)))
        .containerScrollState(
            containerName,
            "stuck: top",
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
