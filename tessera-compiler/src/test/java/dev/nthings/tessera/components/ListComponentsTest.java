package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.IntoElement;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("list components")
class ListComponentsTest {

  static Stream<Arguments> textLists() {
    return Stream.of(
        argumentSet("bullet list", BulletList.of("Describe", "Compile", "Deploy"), "ul"),
        argumentSet("numbered list", NumberedList.of("Describe", "Compile", "Deploy"), "ol"),
        argumentSet("step list", StepList.of("Describe", "Compile", "Deploy"), "ol"));
  }

  @ParameterizedTest
  @MethodSource("textLists")
  @DisplayName("renders each text item as one list item, in order")
  void wrapsTextItems(IntoElement list, String listTag) {
    var rendered = renderMain(list).element("main > *");

    assertThat(rendered).hasTagName(listTag);
    assertThat(rendered.children())
        .extracting(item -> item.tagName() + ":" + item.text())
        .containsExactly("li:Describe", "li:Compile", "li:Deploy");
  }

  static Stream<Arguments> markupLists() {
    return Stream.of(
        argumentSet(
            "bullet list", BulletList.of(a("/docs").text("Docs"), code().text("just verify"))),
        argumentSet(
            "numbered list", NumberedList.of(a("/docs").text("Docs"), code().text("just verify"))),
        argumentSet("step list", StepList.of(a("/docs").text("Docs"), code().text("just verify"))));
  }

  @ParameterizedTest
  @MethodSource("markupLists")
  @DisplayName("keeps markup items intact, one per list item")
  void keepsMarkupItems(IntoElement list) {
    var items = renderMain(list).element("main > *").children();

    assertThat(items).hasSize(2).allSatisfy(item -> assertThat(item).hasTagName("li"));
    assertThat(items.get(0).selectFirst("a")).hasAttribute("href", "/docs").hasText("Docs");
    assertThat(items.get(1).selectFirst("code")).hasText("just verify");
  }

  @Test
  @DisplayName("a description list pairs each term with its description, keeping markup")
  void rendersTermDescriptionPairs() {
    var page =
        renderMain(
            DescriptionList.of()
                .term("Runtime", "Latest Chromium + Firefox")
                .term("Compile", p().text("Run ").child(code().text("just verify"))));

    var list = page.element("main > dl");
    assertThat(list.children())
        .extracting(item -> item.tagName() + ":" + item.text())
        .containsExactly(
            "dt:Runtime", "dd:Latest Chromium + Firefox", "dt:Compile", "dd:Run just verify");
    assertThat(list.children().last().selectFirst("code")).hasText("just verify");
  }
}
