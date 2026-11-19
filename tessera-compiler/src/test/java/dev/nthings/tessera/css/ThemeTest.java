package dev.nthings.tessera.css;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Theme")
class ThemeTest {

  @Test
  @DisplayName("overrides replace token values and derived tokens keep following them")
  void overridesKeepDerivedTokens() {
    var theme =
        Theme.defaults()
            .with(Tokens.DENSITY, CssNumber.of(0.8))
            .with(
                Tokens.CONTROL_FG,
                CssColor.lightDark(CssColor.oklch(20, 0.02, 150), CssColor.oklch(96, 0.01, 150)));

    assertThat(theme.declarations())
        .contains(
            "--density:0.8",
            "--space-4:calc(1rem * var(--density))",
            "--control-fg:light-dark(oklch(20% 0.02 150), oklch(96% 0.01 150))")
        .doesNotContain("--density:1");
    assertThat(Theme.defaults().declarations()).contains("--density:1");
  }

  @Test
  @DisplayName("a new token is added, and a name of another category is refused")
  void addsTokensAndRefusesClashes() {
    var brand = new DesignToken.Color("brand", CssColor.oklch(60, 0.2, 300));
    var theme = Theme.defaults().with(brand, brand.defaultValue());

    assertThat(theme.declarations()).contains("--brand:oklch(60% 0.2 300)");
    assertThat(brand.toCss()).isEqualTo("var(--brand)");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> theme.with(new DesignToken.Length("brand", CssLength.rem(1)), CssLength.rem(2)));
  }
}
