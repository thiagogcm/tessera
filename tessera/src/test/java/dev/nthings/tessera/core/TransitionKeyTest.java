package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.compiler.CompilationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TransitionKey")
class TransitionKeyTest {

  @Test
  @DisplayName("a duplicate key on one page fails compilation")
  void rejectsDuplicateOnPage() {
    var key = TransitionKey.of("hero-title");

    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(
            () ->
                renderMain(
                    h1().text("First Heading").transitionKey(key),
                    h1().text("Second Heading").transitionKey(key)))
        .withMessageContaining("Duplicate TransitionKey name 'hero-title'");
  }

  @Test
  @DisplayName("rejects reserved keywords case-insensitively")
  void rejectsKeywordsInAnyCase() {
    assertThatIllegalArgumentException().isThrownBy(() -> TransitionKey.of("None"));
    assertThatIllegalArgumentException().isThrownBy(() -> TransitionKey.of("Auto"));
    assertThatIllegalArgumentException().isThrownBy(() -> TransitionKey.of("hero", "NONE"));
  }
}
