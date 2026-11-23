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
import org.junit.jupiter.params.provider.ValueSource;

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
            "\"\" / \"Slide \" counter(item)"),
        argumentSet(
            "easing token",
            new DesignToken.Easing("ease-settle", Easing.ease()),
            "var(--ease-settle)"),
        argumentSet("opaque sRGB", CssColor.srgb(0x1d4ed8), "#1d4ed8"),
        argumentSet("sRGB with full alpha", CssColor.srgb(0x1d4ed8, 1), "#1d4ed8"),
        argumentSet("sRGB with alpha", CssColor.srgb(0x1d4ed8, 0.5), "rgb(29 78 216 / 0.5)"),
        argumentSet("sRGB with zero alpha", CssColor.srgb(0x000000, 0), "rgb(0 0 0 / 0)"),
        argumentSet(
            "sRGB white with quarter alpha",
            CssColor.srgb(0xffffff, 0.25),
            "rgb(255 255 255 / 0.25)"));
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
  @DisplayName("content-visibility: auto reserves an intrinsic block size the browser remembers")
  void contentVisibilityAutoReservesSize() {
    var page = renderMain(div().contentVisibilityAuto(CssLength.rem(20)).text("Section"));

    assertThat(page.ruleOf(page.element("main > div"), "u-"))
        .isEqualTo("content-visibility:auto;contain-intrinsic-block-size:auto 20rem");
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
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new DesignToken.Easing(name, Easing.ease()));
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
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.oklch(50, 0.1, 10, Double.NaN));
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.srgb(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.srgb(0x1000000));
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.srgb(0x1d4ed8, -0.01));
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.srgb(0x1d4ed8, 1.01));
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.srgb(0x1d4ed8, Double.NaN));
    assertThatIllegalArgumentException().isThrownBy(() -> CssDuration.ms(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> new TrackList(List.of()));
  }

  static Stream<Arguments> validHexColors() {
    return Stream.of(
        argumentSet("3-digit lowercase", "#f0a", "#ff00aa"),
        argumentSet("3-digit uppercase", "#FFF", "#ffffff"),
        argumentSet("3-digit black", "#000", "#000000"),
        argumentSet("4-digit opaque", "#f0af", "#ff00aa"),
        argumentSet("4-digit transparent", "#f0a0", "rgb(255 0 170 / 0)"),
        argumentSet("4-digit translucent", "#f0a8", "rgb(255 0 170 / 0.533)"),
        argumentSet("6-digit lowercase", "#1d4ed8", "#1d4ed8"),
        argumentSet("6-digit uppercase", "#1D4ED8", "#1d4ed8"),
        argumentSet("8-digit opaque", "#1d4ed8ff", "#1d4ed8"),
        argumentSet("8-digit transparent", "#1d4ed800", "rgb(29 78 216 / 0)"),
        argumentSet("8-digit translucent", "#1d4ed880", "rgb(29 78 216 / 0.502)"));
  }

  @ParameterizedTest
  @MethodSource("validHexColors")
  @DisplayName("parseHex accepts 3-, 4-, 6-, and 8-digit CSS hex colors")
  void parsesValidHexColors(String hex, String expectedCss) {
    assertThat(CssColor.parseHex(hex)).map(CssColor::toCss).contains(expectedCss);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "#",
        "1d4ed8",
        "#12",
        "#12345",
        "#1234567",
        "#123456789",
        "#gggggg",
        "#1d4ed8gg",
        "#f0az",
        " #fff",
        "#fff ",
        "+#fff",
        "#-ff",
        "rgb(0 0 0)"
      })
  @DisplayName("parseHex returns empty without throwing for invalid input")
  void rejectsInvalidHexColors(String invalid) {
    assertThat(CssColor.parseHex(invalid)).isEmpty();
  }
}
