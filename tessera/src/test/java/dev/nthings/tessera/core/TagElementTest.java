package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalStateException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import java.util.List;
import java.util.Map;
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
  @DisplayName("removeAttr drops an attribute or a reference, and refuses id, role and aria-*")
  void removesAttributesAndReferences() {
    var control = ElementRef.scoped("name");
    var element = label().attr("data-x", "1").labelFor(control).removeAttr("data-x");
    element.removeAttr("for").removeAttr("missing");

    assertThat(renderMain(element.text("Name")).element("label"))
        .doesNotHaveAttribute("data-x")
        .doesNotHaveAttribute("for");
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

    assertThat(renderMain(element.text("End")).element("a")).hasAttribute("href", "/end");
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

  @Test
  @DisplayName("attributes that embed script or markup are refused")
  void attributesThatEmbedScriptOrMarkupAreRefused() {
    var button = Elements.button();
    for (var name : List.of("onclick", "OnLoad", "srcdoc")) {
      assertThatIllegalArgumentException().isThrownBy(() -> button.attr(name, "alert(1)"));
      assertThatIllegalArgumentException().isThrownBy(() -> button.boolAttr(name));
    }
    assertThat(renderMain(button.text("Go")).element("button"))
        .doesNotHaveAttribute("onclick")
        .doesNotHaveAttribute("onload")
        .doesNotHaveAttribute("srcdoc");
  }

  @Test
  @DisplayName("id references cannot fill URL attributes, whose values would skip URL checks")
  void idReferencesCannotFillUrlAttributes() {
    var target = ElementRef.page("data:text/html,%3Cscript%3Ealert(1)%3C/script%3E");
    for (var name : List.of("src", "SRC", "href", "action", "formaction", "poster", "data")) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Elements.el(HtmlTag.IFRAME).idRef(name, target));
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Elements.svg(SvgTag.SVG).idRef("xlink:href", target));
    var section = ElementRef.page("target");
    assertThat(renderMain(div().id(section), a(section).text("Target")).element("a"))
        .hasAttribute("href", "#target");
  }

  @Test
  @DisplayName("HTML attribute names are stored in lowercase and SVG names keep their case")
  void attributeNamesFollowTheirNamespaceCaseRules() {
    var page =
        renderMain(
            div().attr("Data-Mode", "on"), Elements.svg(SvgTag.SVG).attr("viewBox", "0 0 1 1"));
    assertThat(page.html()).contains("data-mode=\"on\"", "viewBox=\"0 0 1 1\"");
  }

  @Test
  @DisplayName("names that are not plain attribute or custom element names are refused")
  void namesThatAreNotPlainNamesAreRefused() {
    var image = Elements.img("/a.png", "A");
    for (var name : List.of(" onload", "x onload", "x>y", "a\"b", "", "1x")) {
      assertThatIllegalArgumentException().isThrownBy(() -> image.attr(name, "v"));
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Command.custom("cart", "add", Map.of(name, "v")));
    }
    assertThatIllegalArgumentException().isThrownBy(() -> image.attr("ID", "x"));
    assertThatIllegalArgumentException().isThrownBy(() -> image.attr("Aria-Label", "x"));
    for (var name : List.of("x-a><script>", "x-a b", "-x", "X-a", "x-é")) {
      assertThatIllegalArgumentException().isThrownBy(() -> Elements.custom(name));
    }
    var page =
        renderMain(
            Elements.custom("my-widget.v2_x"), image.dimensions(1, 1).attr("data-x.y:z", "v"));
    assertThat(page.html()).contains("<my-widget.v2_x>", "data-x.y:z=\"v\"");
  }
}
