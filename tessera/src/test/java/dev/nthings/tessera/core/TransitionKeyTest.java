package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.compiler.CompilationException;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("TransitionKey")
class TransitionKeyTest {

  @Test
  @DisplayName("a duplicate key on one page fails compilation")
  void rejectsDuplicateOnPage() {
    var key = TransitionKey.of("hero-title");

    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                renderMain(
                    h1().text("First Heading").transitionKey(key),
                    h1().text("Second Heading").transitionKey(key)));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(d -> {
          assertThat(d.rule()).isEqualTo("duplicate-transition-key");
          assertThat(d.message())
              .isEqualTo(
                  "Duplicate TransitionKey name 'hero-title' found on the same page. TransitionKey names must be unique per document.");
          assertThat(d.fix())
              .isEqualTo("Give each transitioning element a distinct TransitionKey name.");
          assertThat(d.location().page()).isEqualTo("Test");
          assertThat(d.location().components()).isEmpty();
          assertThat(d.location().tag()).isEqualTo("h1");
          assertThat(d.location().domId()).isNull();
        });
  }

  @Test
  @DisplayName("rejects reserved keywords case-insensitively")
  void rejectsKeywordsInAnyCase() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TransitionKey.of("None"))
        .withMessage("TransitionKey name is reserved: None");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TransitionKey.of("Auto"))
        .withMessage("TransitionKey name is reserved: Auto");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TransitionKey.of("hero", "NONE"))
        .withMessage("TransitionKey name is reserved: NONE");
  }

  static Stream<Arguments> invalidIdentifiers() {
    return Stream.of(
        argumentSet(
            "empty name",
            (ThrowingCallable) () -> TransitionKey.of(""),
            "Identifier cannot be empty"),
        argumentSet(
            "leading digit",
            (ThrowingCallable) () -> TransitionKey.of("1hero"),
            "Not a CSS identifier: '1hero'"),
        argumentSet(
            "double hyphen prefix",
            (ThrowingCallable) () -> TransitionKey.of("--hero"),
            "Not a CSS identifier: '--hero'"),
        argumentSet(
            "lone hyphen",
            (ThrowingCallable) () -> TransitionKey.of("-"),
            "Not a CSS identifier: '-'"),
        argumentSet(
            "leading digit in class",
            (ThrowingCallable) () -> TransitionKey.of("hero", "1class"),
            "Not a CSS identifier: '1class'"));
  }

  @ParameterizedTest
  @MethodSource("invalidIdentifiers")
  @DisplayName("rejects names and classes that are not CSS identifiers")
  void rejectsInvalidIdentifiers(ThrowingCallable call, String message) {
    assertThatIllegalArgumentException().isThrownBy(call).withMessage(message);
  }
}
