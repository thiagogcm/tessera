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

@DisplayName("Attribute authoring")
class AttributeAuthoringTest {

  @Test
  @DisplayName("a void element rejects children")
  void voidElementRejectsChildren() {
    assertThatIllegalStateException()
        .isThrownBy(() -> img("/hero.webp", "Hero").child(span().text("Invalid")));
  }

  @Test
  @DisplayName(
      "attributes the compiler owns are refused, and removeAttr drops plain values and refs")
  void compilerOwnedAttributesAreRefused() {
    var control = ElementRef.scoped("name");
    var element = label().attr("data-x", "1").labelFor(control).removeAttr("data-x");
    element.removeAttr("for").removeAttr("missing");

    assertThat(renderMain(element.text("Name")).element("label"))
        .doesNotHaveAttribute("data-x")
        .doesNotHaveAttribute("for");
    for (var name : List.of("id", "role", "aria-label")) {
      assertThatIllegalArgumentException().isThrownBy(() -> element.removeAttr(name));
      assertThatIllegalArgumentException().isThrownBy(() -> div().attr(name, "x"));
    }
  }

  @Test
  @DisplayName("a plain value written after a reference replaces it")
  void laterPlainValueReplacesReference() {
    var element = a("/start").href(ElementRef.page("top")).attr("href", "/end");

    assertThat(renderMain(element.text("End")).element("a")).hasAttribute("href", "/end");
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
        .isThrownBy(() -> Elements.el(SvgTag.SVG).idRef("xlink:href", target));
  }

  @Test
  @DisplayName("HTML attribute names are stored in lowercase and SVG names keep their case")
  void attributeNamesFollowTheirNamespaceCaseRules() {
    var page =
        renderMain(
            div().attr("Data-Mode", "on"), Elements.el(SvgTag.SVG).attr("viewBox", "0 0 1 1"));

    assertThat(page.element("main > div")).hasAttribute("data-mode", "on");
    assertThat(page.html()).contains("viewBox=\"0 0 1 1\"");
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
            Elements.custom("my-widget.v2_x").text("Hello"),
            image.dimensions(1, 1).attr("data-x.y:z", "v"));

    assertThat(page.element("main > img")).hasAttribute("data-x.y:z", "v");
    assertThat(page.html()).contains("<my-widget.v2_x>Hello</my-widget.v2_x>");
  }
}
