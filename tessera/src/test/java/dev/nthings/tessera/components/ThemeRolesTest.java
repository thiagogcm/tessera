package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.OutputMode;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.support.CssBlocks;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("theme roles")
class ThemeRolesTest {

  @Test
  @DisplayName("a role override restyles a field and a callout in both appearances")
  void roleOverrideRestylesFieldAndCallout() {
    var tokens =
        new Tokens()
            .register(
                DesignToken.Color.lightDark(
                    Tokens.CONTROL_FG.name(), "oklch(20% 0.03 250)", "oklch(95% 0.02 250)"))
            .register(DesignToken.Length.of(Tokens.CONTROL_FONT_SIZE.name(), "1.25rem"));
    var page =
        RenderedPage.of(
            PageCompiler.compile(
                new PageDocument(
                    "Theme",
                    main()
                        .children(
                            Forms.field("Display name", Forms.text("name")),
                            Callout.note("Read this note"))),
                tokens,
                OutputMode.inlineStyle()));

    var fieldScope = scopeBody(page, page.element(".field-label"));
    var calloutScope = scopeBody(page, page.element(".callout-title"));
    assertThat(fieldScope)
        .contains(":scope .field-label{")
        .contains("font-size:var(--control-font-size)")
        .contains("color:var(--control-fg)");
    assertThat(calloutScope)
        .contains(":scope > .callout-header{")
        .contains("font-size:var(--control-font-size)")
        .contains("color:var(--control-fg)");
    assertThat(page.css())
        .contains("--control-fg:light-dark(oklch(20% 0.03 250), oklch(95% 0.02 250))")
        .contains("--control-font-size:1.25rem")
        .contains("--font-size-md:1rem");
    assertThat(page.element(".field-label")).hasText("Display name");
    assertThat(page.element(".callout-title")).hasText("Note");
    assertThat(page.element(".callout-content")).hasText("Read this note");
  }

  @Test
  @DisplayName("compact density scales spacing and leaves the type scale alone")
  void compactDensityLeavesTypographyAlone() {
    var tokens = new Tokens().register(DesignToken.Number.of(Tokens.DENSITY.name(), "0.5"));
    var page =
        RenderedPage.of(
            PageCompiler.compile(
                new PageDocument("Density", main().child(Forms.field("Name", Forms.text("name")))),
                tokens,
                OutputMode.inlineStyle()));

    assertThat(page.css())
        .contains("--density:0.5")
        .contains("--space-4:calc(1rem * var(--density))")
        .contains("--control-padding-inline:calc(0.875rem * var(--density))")
        .contains("--font-size-sm:0.875rem")
        .contains("--font-size-md:1rem")
        .contains("--control-font-size:var(--font-size-sm)");
  }

  @Test
  @DisplayName("overlay travel and control feedback stay inside the reduced-motion query")
  void motionTravelStaysInsideTheReducedMotionQuery() {
    var page =
        renderMain(
            Forms.switchField("Lights", "lights", false),
            Overlays.modal("confirm", "Confirm").bodyText("Deploy"));

    var dialog = page.element("dialog");
    var scope = scopeBody(page, dialog);
    var media = scope.indexOf("@media (prefers-reduced-motion: no-preference)");
    assertThat(media).isPositive();
    assertThat(scope.substring(0, media)).doesNotContain("transform:").doesNotContain("opacity:0");
    assertThat(scope.substring(media))
        .contains("transform:scale(0.95)")
        .contains("opacity:1")
        .contains("transition-duration:var(--overlay-duration)");

    var interactive = page.ruleOf(page.element("input[role=switch]"), "i-");
    var focus = interactive.indexOf("&:focus-visible");
    var motion = interactive.indexOf("@media (prefers-reduced-motion: no-preference)");
    assertThat(focus).isNotNegative();
    assertThat(motion).isGreaterThan(focus);
    assertThat(interactive.substring(0, motion)).contains("outline:");
    assertThat(interactive.substring(motion))
        .contains("transition-duration:var(--control-feedback-duration)");
  }

  private static String scopeBody(RenderedPage page, org.jsoup.nodes.Element element) {
    var current = element;
    while (current != null && !current.hasAttr("data-view")) {
      current = current.parent();
    }
    if (current == null) {
      throw new AssertionError("No data-view ancestor");
    }
    return CssBlocks.block(
        page.css(),
        "@scope ([data-view=\"" + current.attr("data-view") + "\"]) to (:scope [data-view] > *){");
  }
}
