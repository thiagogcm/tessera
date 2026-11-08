package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.main;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;

/// Minimal document for one case, appearance, and density. The factory runs during render.
record StoryPage(
    Story story, Story.Variant variant, Catalog.Appearance appearance, Catalog.Density density)
    implements RenderOnce {

  @Override
  public Element render(RenderContext context) {
    context.contributeHead(Elements.el(HtmlTag.STYLE).text(appearanceStyle()));
    return main()
        .px(CssLength.rem(1))
        .py(CssLength.rem(1.5))
        .flex()
        .flexCol()
        .gap(CssLength.rem(1))
        .minInlineSize(CssLength.px(0))
        .children(h1().text(story.heading(variant)), variant.create());
  }

  private String appearanceStyle() {
    return "html:root{color-scheme:"
        + appearance.wire()
        + ";"
        + Tokens.DENSITY.customPropertyName()
        + ":"
        + density.value()
        + "}";
  }
}
