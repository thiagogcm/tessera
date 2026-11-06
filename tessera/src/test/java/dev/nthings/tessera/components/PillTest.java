package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Pill")
class PillTest {

  static Stream<Arguments> pillsByTone() {
    return Stream.of(
        argumentSet("of", Pill.of("Pill"), "badge--neutral"),
        argumentSet("neutral", Pill.neutral("Pill"), "badge--neutral"),
        argumentSet("accent", Pill.accent("Pill"), "badge--accent"),
        argumentSet("success", Pill.success("Pill"), "badge--success"),
        argumentSet("warning", Pill.warning("Pill"), "badge--warning"),
        argumentSet("danger", Pill.danger("Pill"), "badge--danger"),
        argumentSet("info", Pill.info("Pill"), "badge--info"));
  }

  @ParameterizedTest
  @MethodSource("pillsByTone")
  @DisplayName("factories produce fully rounded badges in their tone")
  void factoriesProduceRoundedBadgesInTheirTone(Badge pill, String toneClass) {
    var badge = renderMain(pill).element(".badge");

    assertThat(badge).hasClasses("is-pill", toneClass);
  }

  @Test
  @DisplayName("a pill can show a status dot")
  void pillCanShowStatusDot() {
    var page = renderMain(Pill.success("Active").dot());

    assertThat(page.elements(".badge.is-pill .badge-dot")).hasSize(1);
  }

  @Test
  @DisplayName("a pill can use the outline variant")
  void pillCanUseOutlineVariant() {
    var page = renderMain(Pill.warning("Warning").outline());

    assertThat(page.element(".badge.is-pill")).hasClasses("badge--outline");
  }
}
