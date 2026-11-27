package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.ArrayList;
import java.util.List;

/// A long article that the page publishes as fallback content and the live root renders with extra
/// reading tools above it. The fields and the chapter headings use scoped ids, which the fallback
/// and the live root both prefix with the host id, so a reader who typed in, focused or scrolled
/// the fallback keeps their place when the live view replaces it. The note field is left to the
/// browser and keeps what the reader typed; the bookmark field is bound to application state, which
/// it shows instead.
public final class HandoffDemo {
  private static final int CHAPTERS = 8;
  private static final String FIRST_BOOKMARK = "Chapter 1";

  private String bookmark = FIRST_BOOKMARK;

  private HandoffDemo() {}

  static BrowserRoot mount(String host) {
    var demo = new HandoffDemo();
    return BrowserRoot.mount(host, _ -> demo.view());
  }

  private LiveContent view() {
    var chapters = chapters().stream().map(LiveContent::of).toList();
    return LiveElement.of(Elements.article())
        .children(
            LiveContent.of(note()),
            LiveElement.of(bookmark()).bindValue(bookmark).onValueChange(value -> bookmark = value),
            LiveContent.of(
                Elements.aside()
                    .ariaLabel("Reading tools")
                    .blockSize(CssLength.px(320))
                    .p(Tokens.SPACE_4)
                    .text("Reading tools")))
        .children(chapters);
  }

  /// The article as the page publishes it before the live view mounts.
  public static TagElement fallback() {
    return Elements.article()
        .child(note())
        .child(bookmark().initialValue(FIRST_BOOKMARK))
        .child(Elements.p().text("Loading the reading tools."))
        .children(chapters());
  }

  private static TagElement note() {
    return Forms.input("note", InputType.TEXT)
        .id(ElementRef.scoped("reader-note"))
        .ariaLabel("Reader note");
  }

  private static TagElement bookmark() {
    return Forms.input("bookmark", InputType.TEXT)
        .id(ElementRef.scoped("reader-bookmark"))
        .ariaLabel("Bookmark");
  }

  private static List<TagElement> chapters() {
    var chapters = new ArrayList<TagElement>();
    for (var chapter = 1; chapter <= CHAPTERS; chapter++) {
      chapters.add(
          Elements.section()
              .child(
                  Elements.h2()
                      .id(ElementRef.scoped("chapter-" + chapter))
                      .text("Chapter " + chapter))
              .child(Elements.p().blockSize(CssLength.px(400)).text("Text of chapter " + chapter)));
    }
    return chapters;
  }
}
