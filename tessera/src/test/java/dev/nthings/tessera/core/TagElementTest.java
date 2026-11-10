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
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("TagElement")
class TagElementTest {

  @Test
  @DisplayName("a void element rejects children")
  void voidElementRejectsChildren() {
    assertThatIllegalStateException()
        .isThrownBy(() -> img("/hero.webp", "Hero").child(span().text("Invalid")))
        .withMessage("HTML5 void element <img> cannot have children");
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
    assertThat(element.idReferences()).isEmpty();
    assertThat(element.childrenList()).singleElement().isInstanceOf(TagElement.class);
  }

  @Test
  @DisplayName("removeAttr drops an attribute or a reference, and refuses id, role and aria-*")
  void removesAttributesAndReferences() {
    var control = ElementRef.scoped("name");
    var element = label().attr("data-x", "1").labelFor(control).removeAttr("data-x");
    element.removeAttr("for").removeAttr("missing");

    assertThat(element.attributes()).isEmpty();
    assertThat(element.idReferences()).isEmpty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> element.removeAttr("aria-label"))
        .withMessageContaining("attribute 'aria-label'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> element.removeAttr("role"))
        .withMessageContaining("attribute 'role'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> element.removeAttr("id"))
        .withMessage(
            "Use id(ElementRef) instead of the attribute 'id', so the compiler can scope and link it");
  }

  @Test
  @DisplayName("a plain value written after a reference replaces it")
  void laterPlainValueReplacesReference() {
    var element = a("/start").href(ElementRef.page("top")).attr("href", "/end");

    assertThat(element.idReferences()).isEmpty();
    assertThat(element.attributes()).containsExactly(entry("href", "/end"));
  }

  @ParameterizedTest
  @CsvSource({"MENU,menu", "LISTBOX,listbox", "TREE,tree", "GRID,grid", "DIALOG,dialog"})
  @DisplayName("aria-haspopup serializes its wire token")
  void ariaHasPopupSerializesWireToken(AriaPopup popup, String wire) {
    assertThat(renderMain(div().ariaHasPopup(popup)).element("main > div"))
        .hasAttribute("aria-haspopup", wire);
  }

  @ParameterizedTest
  @CsvSource({"OFF,off", "POLITE,polite", "ASSERTIVE,assertive"})
  @DisplayName("aria-live serializes its wire token")
  void ariaLiveSerializesWireToken(AriaLive live, String wire) {
    assertThat(renderMain(div().ariaLive(live)).element("main > div"))
        .hasAttribute("aria-live", wire);
  }
}
