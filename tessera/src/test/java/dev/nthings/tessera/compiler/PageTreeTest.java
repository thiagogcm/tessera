package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.document.Documents.rawHtml;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;

import dev.nthings.tessera.document.DocumentElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PageTree")
class PageTreeTest {

  @Test
  @DisplayName("trusted raw HTML is summarized by length instead of parsed")
  void rawHtmlIsSummarizedByLength() {
    var svg = "<svg viewBox=\"0 0 10 10\"><circle cx=\"5\" cy=\"5\" r=\"4\"/></svg>";
    var tree =
        Compiler.explain(new PageDocument("Icon", DocumentElement.of(main()).child(rawHtml(svg))));

    var body = (PageTree.Tag) tree.body().getFirst();
    assertThat(body.children())
        .singleElement()
        .isInstanceOfSatisfying(
            PageTree.Raw.class, raw -> assertThat(raw.length()).isEqualTo(svg.length()));
    assertThat(tree.toText()).contains("<raw html, " + svg.length() + " chars>");
  }
}
