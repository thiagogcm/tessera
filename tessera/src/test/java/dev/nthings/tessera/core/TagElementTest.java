package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalStateException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TagElement")
class TagElementTest {

  @Test
  @DisplayName("a void element rejects children")
  void voidElementRejectsChildren() {
    assertThatIllegalStateException()
        .isThrownBy(() -> img("/hero.webp", "Hero").child(span().text("Invalid")))
        .withMessageContaining("void element");
  }
}
