package dev.nthings.tessera.render;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatCode;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.TagElement;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Accessibility rules enforced at compile time")
class AccessibilityRulesTest {

  static Stream<Arguments> violations() {
    return Stream.of(
        argumentSet(
            "button without text content",
            new PageDocument("Invalid Button", div().child(button())),
            "must have text content"),
        argumentSet(
            "decorative image without role=presentation",
            new PageDocument("Invalid Img", div().child(img("/deco.svg", "").dimensions(1, 1))),
            "role=\"presentation\""),
        argumentSet(
            "image with neither dimensions nor aspect-ratio",
            new PageDocument(
                "CLS Violation",
                main().child(img("/banner.jpg", "Banner without width/height or aspect-ratio"))),
            "aspect-ratio"),
        argumentSet(
            "duplicate DOM id",
            new PageDocument("Duplicate Id", div().children(p().id("dup"), p().id("dup"))),
            "Duplicate DOM id 'dup'"));
  }

  @ParameterizedTest
  @MethodSource("violations")
  @DisplayName("rejects invalid markup")
  void rejectsInvalidMarkup(PageDocument document, String message) {
    assertThatExceptionOfType(AccessibilityViolationException.class)
        .isThrownBy(() -> PageCompiler.compile(document))
        .withMessageContaining(message);
  }

  @Test
  @DisplayName("accepts decorative images and links labelled by aria-label")
  void acceptsDecorativeImagesAndAriaLabelledLinks() {
    var document =
        new PageDocument(
            "Valid A11y",
            div()
                .children(
                    img("/deco.svg", "").role("presentation").dimensions(100, 100),
                    a("/home")
                        .ariaLabel("Home page")
                        .child(new TagElement(HtmlTag.SPAN).ariaHidden(true))));

    assertThatCode(() -> PageCompiler.compile(document)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("percent encoded fragment links resolve without treating plus as space")
  void percentEncodedFragmentLinksResolveWithoutTreatingPlusAsSpace() {
    var document =
        new PageDocument(
            "Links",
            main().attr("id", "café+intro").child(a("#caf%C3%A9%2Bintro").text("Introduction")));

    assertThatCode(() -> PageCompiler.compile(document)).doesNotThrowAnyException();
  }
}
