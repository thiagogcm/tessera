package dev.nthings.tessera.wasm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.css.Tokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mount options")
class MountOptionsTest {

  @Test
  @DisplayName("a theme that fails WCAG AA text contrast is rejected with its diagnostics")
  void rejectsInaccessibleTheme() {
    var failing = Theme.defaults().with(Tokens.ACCENT_PRIMARY, CssColor.oklch(55, 0.18, 150));

    assertThatThrownBy(() -> MountOptions.DEFAULT.withTheme(failing))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Theme fails WCAG AA text contrast:\n[theme-contrast] ")
        .hasMessageContaining("--accent-primary");
  }

  @Test
  @DisplayName("a passing theme is kept")
  void keepsAccessibleTheme() {
    var theme =
        Theme.defaults()
            .with(
                Tokens.ACCENT_PRIMARY,
                CssColor.lightDark(CssColor.oklch(50, 0.16, 150), CssColor.oklch(72, 0.16, 150)));

    assertThat(MountOptions.DEFAULT.withTheme(theme).theme()).isSameAs(theme);
  }
}
