package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalStateException;
import static dev.nthings.tessera.support.HtmlAssertions.entry;

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

  @Test
  @DisplayName("compiler-facing attribute and child views are read-only")
  void viewsAreReadOnly() {
    var element = div().attr("data-x", "1").ariaLabel("Box").child(span());

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> element.attributes().put("aria-hidden", "true"));
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> element.idReferences().clear());
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> element.childrenList().clear());
    assertThat(element.attributes())
        .containsExactly(entry("data-x", "1"), entry("aria-label", "Box"));
  }

  @Test
  @DisplayName("removeAttr drops an attribute or a reference, and refuses id, role and aria-*")
  void removesAttributesAndReferences() {
    var control = ElementRef.scoped("name");
    var element = label().attr("data-x", "1").labelFor(control).removeAttr("data-x");
    element.removeAttr("for").removeAttr("missing");

    assertThat(element.attributes()).isEmpty();
    assertThat(element.idReferences()).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> element.removeAttr("aria-label"));
    assertThatIllegalArgumentException().isThrownBy(() -> element.removeAttr("role"));
    assertThatIllegalArgumentException().isThrownBy(() -> element.removeAttr("id"));
  }

  @Test
  @DisplayName("a plain value written after a reference replaces it")
  void laterPlainValueReplacesReference() {
    var element = a("/start").href(ElementRef.page("top")).attr("href", "/end");

    assertThat(element.idReferences()).isEmpty();
    assertThat(element.attributes()).containsEntry("href", "/end");
  }
}
