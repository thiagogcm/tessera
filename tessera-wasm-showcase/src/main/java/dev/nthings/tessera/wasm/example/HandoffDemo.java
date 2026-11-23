package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;

/// A long article that the page publishes as fallback content and the live root renders with extra
/// reading tools above it. Both name the note field and the chapter headings with the same
/// page-level ids, so a reader who focused or scrolled the fallback keeps their place when the live
/// view replaces it.
public final class HandoffDemo {
  private static final int CHAPTERS = 8;

  private HandoffDemo() {}

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(
        host,
        _ ->
            LiveContent.of(
                article(
                    Elements.aside()
                        .ariaLabel("Reading tools")
                        .blockSize(CssLength.px(320))
                        .p(Tokens.SPACE_4)
                        .text("Reading tools"))));
  }

  /// The article as the page publishes it before the live view mounts.
  public static TagElement fallback() {
    return article(Elements.p().text("Loading the reading tools."));
  }

  private static TagElement article(IntoElement tools) {
    var article =
        Elements.article()
            .child(Forms.text("note").id(ElementRef.page("reader-note")).ariaLabel("Reader note"))
            .child(tools);
    for (var chapter = 1; chapter <= CHAPTERS; chapter++) {
      article.child(
          Elements.section()
              .child(
                  Elements.h2()
                      .id(ElementRef.page("chapter-" + chapter))
                      .text("Chapter " + chapter))
              .child(Elements.p().blockSize(CssLength.px(400)).text("Text of chapter " + chapter)));
    }
    return article;
  }
}
