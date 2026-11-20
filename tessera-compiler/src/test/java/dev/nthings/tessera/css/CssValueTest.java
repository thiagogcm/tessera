package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Typed CSS values")
class CssValueTest {

  static Stream<Arguments> serialized() {
    var scrim = CssColor.oklch(0, 0, 0, 0.28);
    return Stream.of(
        argumentSet("rounded decimals", CssLength.rem(0.1 + 0.2), "0.3rem"),
        argumentSet(
            "nested calculation",
            CssLength.pct(20)
                .plus(
                    CssLength.pct(60)
                        .times(CssNumber.property(CustomProperty.named("step-index")))
                        .dividedBy(CssNumber.property(CustomProperty.named("step-total")))),
            "calc(20% + ((60% * var(--step-index)) / var(--step-total)))"),
        argumentSet(
            "drop shadow with spread",
            Shadow.drop(CssLength.px(24), CssLength.px(48), CssLength.px(-12), scrim),
            "0 24px 48px -12px oklch(0% 0 0 / 0.28)"),
        argumentSet(
            "inset shadow with spread and horizontal offset",
            Shadow.inset(CssLength.zero(), CssLength.px(6), CssLength.px(-2), scrim)
                .offsetX(CssLength.px(3)),
            "inset 3px 0 6px -2px oklch(0% 0 0 / 0.28)"),
        argumentSet(
            "auto-fit tracks",
            TrackList.autoFit(CssLength.rem(16)),
            "repeat(auto-fit, minmax(min(100%, 16rem), 1fr))"),
        argumentSet(
            "font stack",
            FontStack.of(
                FontStack.Generic.UI_MONOSPACE,
                FontStack.named("SFMono-Regular"),
                FontStack.named("Fira Code"),
                FontStack.Generic.MONOSPACE),
            "ui-monospace,SFMono-Regular,\"Fira Code\",monospace"),
        argumentSet("quoted content", CssContent.text("a\"b"), "\"a\\22 b\""),
        argumentSet(
            "content with backslash and newline", CssContent.text("a\\b\nc"), "\"a\\5c b\\a c\""),
        argumentSet(
            "font name with backslash",
            FontStack.of(FontStack.named("Odd\\Font")),
            "\"Odd\\5c Font\""),
        argumentSet("decorative content", CssContent.decorative("/"), "\"\\2f\" / \"\""),
        argumentSet(
            "content with alternative",
            CssContent.withAlternative(
                CssContent.empty(),
                CssContent.text("Slide "),
                CssContent.counter(Counter.named("item"))),
            "\"\" / \"Slide \" counter(item)"));
  }

  @ParameterizedTest
  @MethodSource("serialized")
  @DisplayName("a value serializes to the CSS the browser expects")
  void serializesToCss(CssValue value, String css) {
    assertThat(value.toCss()).isEqualTo(css);
  }

  @Test
  @DisplayName("a transition lists its properties and keeps discrete transitions explicit")
  void transitionDeclaresDiscreteBehavior() {
    var page =
        renderMain(
            div()
                .transition(
                    Transition.of(
                            CssDuration.ms(200),
                            Easing.easeOut(),
                            Animatable.OPACITY,
                            Animatable.TRANSLATE)
                        .allowingDiscrete()));

    assertThat(page.css())
        .contains("transition-property:opacity, translate", "transition-behavior:allow-discrete");
  }

  @Test
  @DisplayName("names are validated, not sanitized, and keywords are refused")
  void identifiersAreValidated() {
    for (var name : List.of("1st", "has space", "--dashed", "None", "")) {
      assertThatIllegalArgumentException().isThrownBy(() -> Counter.named(name));
    }
    for (var name : List.of("has space", "--dashed", "")) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new DesignToken.Length(name, CssLength.rem(1)));
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Container.named("inherit", Container.Type.INLINE_SIZE));
    assertThatIllegalArgumentException().isThrownBy(() -> Keyframes.named("default"));
    assertThatIllegalArgumentException().isThrownBy(() -> Anchor.named("--x"));
  }

  @Test
  @DisplayName("malformed values fail at construction")
  void malformedValuesAreRejected() {
    assertThatIllegalArgumentException().isThrownBy(() -> GridPlacement.at(0));
    assertThatIllegalArgumentException().isThrownBy(() -> GridPlacement.span(0));
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.oklch(50, 0.1, 10, 1.5));
    assertThatIllegalArgumentException().isThrownBy(() -> CssDuration.ms(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> new TrackList(List.of()));
  }
}
