package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.header;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.IntoElement;
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

/// State-aware sticky header backed by `container-type: scroll-state` and `@container <name>
/// scroll-state(stuck: top)` where supported; sticky positioning and the base surface remain
/// available otherwise.
///
/// The outer `:scope` acts as the `position: sticky` scroll-state container, while the inner
/// `<header class="sticky-surface">` queries `scroll-state(stuck: top)` to apply backdrop blur
/// and elevation shadow only when actively stuck.
public final class StickyHeader implements RenderView {

  private final ElementId id;
  private final CssIdent containerName;
  private final Element content;

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
  public Element render() {
    return div().child(header().attr("class", "sticky-surface").child(content));
  }
}
