package dev.nthings.tessera.css;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

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
        argumentSet("oklch", CssColor.oklch(54, 0.21, 262), "oklch(54% 0.21 262)"),
        argumentSet("oklch with alpha", scrim, "oklch(0% 0 0 / 0.28)"),
        argumentSet("hex", CssColor.white(), "#ffffff"),
        argumentSet("keyword", CssColor.currentColor(), "currentColor"),
        argumentSet(
            "token reference",
            Tokens.ACCENT_PRIMARY.mixTransparent(24),
            "color-mix(in oklch, var(--accent-primary) 24%, transparent)"),
        argumentSet(
            "same hue",
            CssColor.sameHue(Tokens.STATUS_DANGER, 97, 0.03),
            "oklch(from var(--status-danger) 97% 0.03 h)"),
        argumentSet(
            "contrast", Tokens.ACCENT_PRIMARY.contrast(), "contrast-color(var(--accent-primary))"),
        argumentSet(
            "light and dark",
            CssColor.lightDark(CssColor.oklch(20, 0.02, 150), CssColor.oklch(96, 0.01, 150)),
            "light-dark(oklch(20% 0.02 150), oklch(96% 0.01 150))"),
        argumentSet("zero length", CssLength.zero(), "0"),
        argumentSet("token length", Tokens.SPACE_4, "var(--space-4)"),
        argumentSet("sum", CssLength.pct(100).minus(CssLength.rem(2)), "calc(100% - 2rem)"),
        argumentSet(
            "anchored inset",
            CssLength.anchor(CssLength.AnchorSide.LEFT).plus(CssLength.rem(0.5)),
            "calc(anchor(left) + 0.5rem)"),
        argumentSet(
            "nested calculation",
            CssLength.pct(20)
                .plus(
                    CssLength.pct(60)
                        .times(CssNumber.property(CssIdent.custom("step-index")))
                        .dividedBy(CssNumber.property(CssIdent.custom("step-total")))),
            "calc(20% + ((60% * var(--step-index)) / var(--step-total)))"),
        argumentSet("min", CssLength.min(CssLength.rem(22), CssLength.pct(82)), "min(22rem, 82%)"),
        argumentSet("line units", CssLength.lh(3).plus(CssLength.rem(1)), "calc(3lh + 1rem)"),
        argumentSet(
            "drop shadow",
            Shadow.drop(CssLength.px(24), CssLength.px(48), CssLength.px(-12), scrim),
            "0 24px 48px -12px oklch(0% 0 0 / 0.28)"),
        argumentSet(
            "hairline shadow",
            Shadow.drop(CssLength.px(1), CssLength.zero(), Tokens.BORDER_SUBTLE),
            "0 1px 0 var(--border-subtle)"),
        argumentSet(
            "hairline border",
            Border.hairline(Tokens.BORDER_SUBTLE),
            "1px solid var(--border-subtle)"),
        argumentSet(
            "dashed border",
            Border.dashed(CssLength.px(2), Tokens.BORDER_SUBTLE),
            "2px dashed var(--border-subtle)"),
        argumentSet("no border", Border.none(), "none"),
        argumentSet(
            "layered background",
            new Background(
                new Background.Dot(CssColor.white(), 80, 88),
                CssLength.zero(),
                CssLength.pct(50),
                CssLength.rem(1.5),
                CssLength.rem(1.5),
                Tokens.BORDER_SUBTLE),
            "radial-gradient(circle closest-side, #ffffff 80%, transparent 88%) 0 50% / 1.5rem 1.5rem"
                + " no-repeat var(--border-subtle)"),
        argumentSet(
            "grid tracks",
            TrackList.of(
                GridTrack.minmax(
                    GridTrack.length(CssLength.rem(10)), GridTrack.length(CssLength.pct(35))),
                GridTrack.fr(1)),
            "minmax(10rem, 35%) 1fr"),
        argumentSet(
            "auto-fit tracks",
            TrackList.autoFit(CssLength.rem(16)),
            "repeat(auto-fit, minmax(min(100%, 16rem), 1fr))"),
        argumentSet("equal tracks", TrackList.equal(2), "repeat(2, 1fr)"),
        argumentSet("subgrid", TrackList.subgrid(), "subgrid"),
        argumentSet("grid line", GridPlacement.at(2), "2"),
        argumentSet("grid range", GridPlacement.between(1, -1), "1 / -1"),
        argumentSet("grid span from line", GridPlacement.from(1, 3), "1 / span 3"),
        argumentSet("aspect ratio", AspectRatio.of(16, 9), "16 / 9"),
        argumentSet(
            "transform chain",
            Transform.translateY(CssLength.px(-12)).then(Transform.scale(0.98)),
            "translateY(-12px) scale(0.98)"),
        argumentSet("rotation", Transform.rotate(180), "rotate(180deg)"),
        argumentSet("translation", Translation.y(CssLength.rem(-0.25)), "0 -0.25rem"),
        argumentSet("blur", CssFilter.blur(CssLength.px(4)), "blur(4px)"),
        argumentSet("duration", CssDuration.ms(150), "150ms"),
        argumentSet("bezier", Easing.cubicBezier(0.16, 1, 0.3, 1), "cubic-bezier(0.16, 1, 0.3, 1)"),
        argumentSet(
            "font stack",
            FontStack.of(
                FontStack.Generic.UI_MONOSPACE,
                FontStack.named("SFMono-Regular"),
                FontStack.named("Fira Code"),
                FontStack.Generic.MONOSPACE),
            "ui-monospace,SFMono-Regular,\"Fira Code\",monospace"),
        argumentSet("content text", CssContent.text("a\"b"), "\"a\\22 b\""),
        argumentSet("decorative content", CssContent.decorative("/"), "\"\\2f\" / \"\""),
        argumentSet(
            "content with alternative",
            CssContent.withAlternative(
                CssContent.empty(),
                CssContent.text("Slide "),
                CssContent.counter(CssIdent.of("item"))),
            "\"\" / \"Slide \" counter(item)"),
        argumentSet(
            "position area",
            PositionArea.of(PositionArea.Part.BLOCK_END, PositionArea.Part.SPAN_INLINE_END),
            "block-end span-inline-end"),
        argumentSet(
            "position fallback",
            PositionTry.flip(PositionTry.Flip.BLOCK, PositionTry.Flip.INLINE),
            "flip-block flip-inline"),
        argumentSet("scroll snap", ScrollSnap.mandatory(ScrollSnap.Axis.X), "x mandatory"),
        argumentSet("keyword enum", WhiteSpace.PRE_WRAP, "pre-wrap"),
        argumentSet("sibling index", CssNumber.siblingIndex(), "sibling-index()"));
  }

  @ParameterizedTest
  @MethodSource("serialized")
  @DisplayName("a value serializes to the CSS the browser expects")
  void serializesToCss(CssValue value, String css) {
    assertThat(value.toCss()).isEqualTo(css);
  }

  @Test
  @DisplayName("a motion lists its properties and keeps discrete transitions explicit")
  void motionDeclaresTransitionLonghands() {
    var style = new StyleRefinement();
    style.motion(
        Motion.of(CssDuration.ms(200), Easing.easeOut(), Animatable.OPACITY, Animatable.TRANSLATE)
            .allowingDiscrete());

    assertThat(style.toCssBlockBody())
        .isEqualTo(
            "transition-property:opacity, translate;transition-duration:200ms;"
                + "transition-timing-function:ease-out;transition-behavior:allow-discrete");
  }

  @Test
  @DisplayName("token overrides keep their category and serialize their typed value")
  void tokenOverridesAreTyped() {
    assertThat(Tokens.DENSITY.withValue(0.8).cssDefinition()).isEqualTo("0.8");
    assertThat(Tokens.RADIUS_XL.withValue(CssLength.rem(1)).cssDefinition()).isEqualTo("1rem");
    assertThat(Tokens.SPACE_4.cssDefinition()).isEqualTo("calc(1rem * var(--density))");
    assertThat(Tokens.CONTROL_FAMILY.cssDefinition()).isEqualTo("var(--font-body)");
    assertThat(
            Tokens.CONTROL_FG
                .withLightDark(CssColor.oklch(20, 0.02, 150), CssColor.oklch(96, 0.01, 150))
                .cssDefinition())
        .isEqualTo("light-dark(oklch(20% 0.02 150), oklch(96% 0.01 150))");
  }

  @Test
  @DisplayName("identifiers reject text that is not a CSS identifier")
  void identifiersAreValidated() {
    assertThatIllegalArgumentException().isThrownBy(() -> CssIdent.of("1st"));
    assertThatIllegalArgumentException().isThrownBy(() -> CssIdent.of("has space"));
    assertThatIllegalArgumentException().isThrownBy(() -> CssIdent.of("--dashed"));
    assertThatIllegalArgumentException().isThrownBy(() -> CssIdent.of("None"));
    assertThatIllegalArgumentException().isThrownBy(() -> CssIdent.of("inherit"));
    assertThat(CssIdent.custom("anchor-x").toCss()).isEqualTo("--anchor-x");
  }

  @Test
  @DisplayName("malformed values fail at construction")
  void malformedValuesAreRejected() {
    assertThatIllegalArgumentException().isThrownBy(() -> GridPlacement.at(0));
    assertThatIllegalArgumentException().isThrownBy(() -> GridPlacement.span(0));
    assertThatIllegalArgumentException().isThrownBy(() -> CssColor.oklch(50, 0.1, 10, 1.5));
    assertThatIllegalArgumentException().isThrownBy(() -> CssDuration.ms(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> new TrackList(java.util.List.of()));
  }

  @Test
  @DisplayName("anchor names must be dashed identifiers")
  void anchorNamesAreDashed() {
    var style = new StyleRefinement();
    assertThatIllegalArgumentException().isThrownBy(() -> style.anchorName(CssIdent.of("plain")));
    style.anchorName(CssIdent.custom("plain"));
    assertThat(style.toCssBlockBody()).isEqualTo("anchor-name:--plain");
  }
}
