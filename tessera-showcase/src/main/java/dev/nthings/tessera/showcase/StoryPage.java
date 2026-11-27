package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.main;

import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;

/// Document for one case, appearance and density; the factory runs during render.
record StoryPage(
    Story story, Story.Variant variant, Catalog.Appearance appearance, Catalog.Density density)
    implements DocumentComponent {

  @Override
  public DocumentContent expand(DocumentContext context) {
    context.contributeHead(Elements.el(HtmlTag.STYLE).text(appearanceStyle()));
    return DocumentElement.of(
            main()
                .px(CssLength.rem(1))
                .py(CssLength.rem(1.5))
                .flex()
                .flexCol()
                .gap(CssLength.rem(1))
                .minInlineSize(CssLength.px(0))
                .child(h1().text(story.heading(variant))))
        .child(variant.create());
  }

  private String appearanceStyle() {
    return "html:root{color-scheme:"
        + appearance.wire()
        + ";"
        + "--"
        + Tokens.DENSITY.name()
        + ":"
        + density.value()
        + "}";
  }
}
