package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.header;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;

/**
 * State-aware sticky header backed by Chrome 156+ {@code container-type: scroll-state} and {@code
 * @container <name> scroll-state(stuck: top)}.
 *
 * <p>The outer {@code :scope} acts as the {@code position: sticky} scroll-state container, while
 * the inner {@code <header class="sticky-surface">} queries {@code scroll-state(stuck: top)} to
 * apply backdrop blur and elevation shadow only when actively stuck.
 */
public final class StickyHeader implements RenderView {

  private final ElementId id;
  private final String containerName;
  private final Element content;

  public StickyHeader(String id, IntoElement content) {
    this.id = ElementId.of(id);
    this.containerName = "sticky-" + Fnv1a64.sanitizeIdentifier(id);
    this.content = content.intoElement();
  }

  @Override
  public ElementId entityId() {
    return id;
  }

  @Override
  public String viewType() {
    return "sticky-header";
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
                    .transition("background-color 0.2s ease, box-shadow 0.2s ease"))
        .containerScrollState(
            containerName,
            "stuck: top",
            c ->
                c.select(
                    ":scope > .sticky-surface",
                    s ->
                        s.bgRaw("color-mix(in oklch, var(--surface-card) 88%, transparent)")
                            .backdropFilter("blur(12px)")
                            .shadow("0 6px 20px -4px oklch(0% 0 0 / 0.14)")));
  }

  @Override
  public Element render() {
    return div().child(header().attr("class", "sticky-surface").child(content));
  }
}
