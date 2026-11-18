package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;

import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Diagnostics.Found;
import dev.nthings.tessera.support.RenderedPage;
import dev.nthings.tessera.support.Snapshots;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TransitionKey")
class TransitionKeyTest {

  @Test
  @DisplayName("one name on two elements is reported in both backends, whatever their classes")
  void rejectsDuplicateNames() {
    var content =
        main()
            .children(
                h1().text("First").transitionKey(TransitionKey.of("hero", "first")),
                h1().text("Second").transitionKey(TransitionKey.of("hero", "second")));

    for (var render :
        List.<ThrowingCallable>of(
            () -> RenderedPage.render(content), () -> Snapshots.render(content))) {
      assertThat(Diagnostics.of(render))
          .extracting(Found::rule, Found::tag)
          .containsExactly(tuple("duplicate-transition-key", "h1"));
    }
  }

  @Test
  @DisplayName("keywords in any case and non-identifiers are refused as names and classes")
  void rejectsKeywordsAndInvalidIdentifiers() {
    for (var name : List.of("None", "auto", "AUTO", "", "1hero", "--hero", "-")) {
      assertThatIllegalArgumentException().isThrownBy(() -> TransitionKey.of(name));
      assertThatIllegalArgumentException().isThrownBy(() -> TransitionKey.of("hero", name));
    }
  }
}
