package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.options.AriaRole;
import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// The live tooltip on the search page, on a trigger whose page id contains a dot.
@DisplayName("Tooltip")
class TooltipIT {

  @BothEngines
  @DisplayName(
      "hover and focus show the tooltip beside its trigger, Escape hides it, removal ends it")
  void tooltipFollowsInterest(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/search/");
    var trigger = session.button("Search tip");
    var tooltip = page.getByRole(AriaRole.TOOLTIP);

    trigger.hover();
    assertThat(tooltip).isVisible();
    assertThat(tooltip).hasText("More search details");
    assertThat(trigger).hasAccessibleDescription("More search details");
    var anchor = Objects.requireNonNull(trigger.boundingBox());
    var box = Objects.requireNonNull(tooltip.boundingBox());
    var gap = Math.max(box.y - (anchor.y + anchor.height), anchor.y - (box.y + box.height));
    Assertions.assertThat(gap).as("vertical gap between trigger and tooltip").isBetween(-1.0, 24.0);
    Assertions.assertThat(box.x).isLessThan(anchor.x + anchor.width);
    Assertions.assertThat(box.x + box.width).isGreaterThan(anchor.x);

    tooltip.hover();
    assertThat(tooltip).isVisible();
    page.keyboard().press("Escape");
    assertThat(tooltip).isHidden();

    page.mouse().move(0, 0);
    trigger.focus();
    assertThat(tooltip).isVisible();
    page.clock().runFor(60_000);
    assertThat(trigger).hasCount(0);
    assertThat(tooltip).hasCount(0);
  }
}
