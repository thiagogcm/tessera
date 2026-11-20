package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("layout components")
class LayoutComponentsTest {

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  @DisplayName("a grid rejects a non-positive column count")
  void gridRejectsNonPositiveColumnCount(int columns) {
    assertThatIllegalArgumentException().isThrownBy(() -> Grid.columns(columns));
  }

  @Test
  @DisplayName("the scroll progress bar is hidden from assistive technology")
  void scrollProgressBarIsHidden() {
    assertThat(renderMain(ScrollProgressBar.of()).element("main > *"))
        .hasAttribute("aria-hidden", "true");
  }
}
