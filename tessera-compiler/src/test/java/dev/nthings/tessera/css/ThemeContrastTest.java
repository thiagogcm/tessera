package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.within;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.css.ThemeContrast.Scheme;
import dev.nthings.tessera.support.Diagnostics;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Theme contrast")
class ThemeContrastTest {

  private static final CssColor WHITE = CssColor.white();
  private static final CssColor BLACK = CssColor.srgb(0x000000);

  private static double ratio(CssColor text, CssColor surface) {
    return measured(Theme.defaults(), Scheme.LIGHT, text, List.of(surface)).ratio();
  }

  private static ThemeContrast.Measurement measured(
      Theme theme, Scheme scheme, CssColor text, List<CssColor> layers) {
    return Objects.requireNonNull(ThemeContrast.measure(theme, scheme, text, layers));
  }

  private static ThemeContrast.@Nullable Measurement measure(Theme theme, CssColor text) {
    return ThemeContrast.measure(theme, Scheme.LIGHT, text, List.of(WHITE));
  }

  private static String hex(CssColor color) {
    return ColorMath.hex(measured(Theme.defaults(), Scheme.LIGHT, color, List.of(WHITE)).text());
  }

  private static List<String> messages(Theme theme) {
    return theme.contrastDiagnostics().stream().map(Diagnostic::message).toList();
  }

  /// A scheme-aware color keeping the default dark value of `token`.
  private static CssColor withLight(DesignToken.Color token, CssColor light) {
    return CssColor.lightDark(light, ((CssColors.LightDark) token.defaultValue()).dark());
  }

  @Nested
  @DisplayName("color math")
  class ColorMathTest {

    @Test
    @DisplayName("black on white is 21:1 and a color on itself is 1:1")
    void extremes() {
      assertThat(ratio(BLACK, WHITE)).isCloseTo(21, within(1e-4));
      assertThat(ratio(WHITE, WHITE)).isCloseTo(1, within(1e-6));
      assertThat(ratio(CssColor.oklch(100, 0, 0), BLACK)).isCloseTo(21, within(1e-3));
    }

    @Test
    @DisplayName("sRGB greys straddle 4.5:1 on white, as WCAG tables list them")
    void wcagBoundary() {
      assertThat(ratio(CssColor.srgb(0x767676), WHITE)).isCloseTo(4.54, within(0.005));
      assertThat(ratio(CssColor.srgb(0x767676), WHITE)).isGreaterThanOrEqualTo(4.5);
      assertThat(ratio(CssColor.srgb(0x777777), WHITE)).isCloseTo(4.48, within(0.005));
      assertThat(ratio(CssColor.srgb(0x777777), WHITE)).isLessThan(4.5);
    }

    @Test
    @DisplayName("OKLCH converts to the sRGB colors CSS Color 4 specifies")
    void oklchToSrgb() {
      assertThat(hex(CssColor.oklch(62.796, 0.25768, 29.234))).isEqualTo("#ff0000");
      assertThat(hex(CssColor.oklch(45.201, 0.31321, 264.052))).isEqualTo("#0000ff");
      assertThat(hex(CssColor.oklch(59.987, 0, 0))).isEqualTo("#808080");
      assertThat(hex(CssColor.oklch(100, 0, 0))).isEqualTo("#ffffff");
    }

    @Test
    @DisplayName("colors outside the sRGB gamut clip per channel")
    void clipsOutOfGamut() {
      assertThat(hex(CssColor.oklch(90, 0.4, 145))).isEqualTo("#00ff00");
    }

    @Test
    @DisplayName("color-mix interpolates in OKLab and OKLCH, premultiplied by alpha")
    void mixes() {
      var halfLab = CssColor.srgb(0xffffff).mix(CssColor.Space.OKLAB, 50, BLACK);
      assertThat(hex(halfLab)).isEqualTo(hex(CssColor.oklch(50, 0, 0)));

      // Red and blue meet on the shorter hue arc, through magenta rather than green.
      var red = CssColor.oklch(60, 0.2, 20);
      var blue = CssColor.oklch(60, 0.2, 300);
      assertThat(hex(red.mix(CssColor.Space.OKLCH, 50, blue)))
          .isEqualTo(hex(CssColor.oklch(60, 0.2, 340)));

      // A transparent partner keeps the color and scales its opacity; an achromatic partner
      // keeps its hue.
      var tint = red.mixTransparent(40);
      assertThat(measured(Theme.defaults(), Scheme.LIGHT, tint, List.of(WHITE)).text())
          .isEqualTo(
              measured(
                      Theme.defaults(),
                      Scheme.LIGHT,
                      CssColor.oklch(60, 0.2, 20, 0.4),
                      List.of(WHITE))
                  .text());
      assertThat(hex(red.mix(CssColor.Space.OKLCH, 50, CssColor.oklch(60, 0, 0))))
          .isEqualTo(hex(CssColor.oklch(60, 0.1, 20)));
    }

    @Test
    @DisplayName("translucent colors composite over the layers beneath them")
    void composites() {
      var shade = CssColor.srgb(0x000000, 0.4);
      var measurement = measured(Theme.defaults(), Scheme.LIGHT, WHITE, List.of(WHITE, shade));
      assertThat(ColorMath.hex(measurement.surface())).isEqualTo("#999999");
      assertThat(
              ColorMath.hex(measured(Theme.defaults(), Scheme.LIGHT, shade, List.of(WHITE)).text()))
          .isEqualTo("#999999");
    }
  }

  @Nested
  @DisplayName("resolution")
  class Resolution {

    @Test
    @DisplayName("light-dark() follows the scheme")
    void lightDark() {
      var text = CssColor.lightDark(BLACK, WHITE);
      assertThat(ColorMath.hex(measured(Theme.defaults(), Scheme.LIGHT, text, List.of()).text()))
          .isEqualTo("#000000");
      assertThat(ColorMath.hex(measured(Theme.defaults(), Scheme.DARK, text, List.of()).text()))
          .isEqualTo("#ffffff");
    }

    @Test
    @DisplayName("tokens resolve through the theme, including aliases and sameHue derivations")
    void tokens() {
      var theme = Theme.defaults().with(Tokens.ACCENT_PRIMARY, CssColor.oklch(50, 0.2, 30));
      var derived = measured(theme, Scheme.LIGHT, Tokens.ACCENT_TEXT, List.of(WHITE)).text();
      var expected =
          measured(theme, Scheme.LIGHT, CssColor.oklch(38, 0.16, 30), List.of(WHITE)).text();
      assertThat(ColorMath.hex(derived)).isEqualTo(ColorMath.hex(expected));

      var aliased = theme.with(Tokens.CONTROL_FG, Tokens.ACCENT_TEXT);
      assertThat(
              ColorMath.hex(
                  measured(aliased, Scheme.LIGHT, Tokens.CONTROL_FG, List.of(WHITE)).text()))
          .isEqualTo(ColorMath.hex(expected));
    }

    @Test
    @DisplayName("contrast-color() picks white or black, whichever contrasts more")
    void contrastColor() {
      var onCard = Tokens.SURFACE_CARD.contrast();
      assertThat(ColorMath.hex(measured(Theme.defaults(), Scheme.LIGHT, onCard, List.of()).text()))
          .isEqualTo("#000000");
      assertThat(ColorMath.hex(measured(Theme.defaults(), Scheme.DARK, onCard, List.of()).text()))
          .isEqualTo("#ffffff");
    }

    @Test
    @DisplayName("currentColor, undefined tokens, alias cycles and computed weights are skipped")
    void unresolvable() {
      var theme = Theme.defaults();
      var undefined = new DesignToken.Color("undefined-ink", BLACK);
      assertThat(measure(theme, CssColor.currentColor())).isNull();
      assertThat(measure(theme, undefined)).isNull();
      assertThat(measure(theme.with(Tokens.CONTROL_FG, Tokens.CONTROL_FG), Tokens.CONTROL_FG))
          .isNull();
      assertThat(
              measure(
                  theme,
                  BLACK.mix(CssColor.Space.OKLAB, CssLength.pct(50).plus(CssLength.px(1)), WHITE)))
          .isNull();
      assertThat(measure(theme, CssColor.srgb(0x000000, 0.5).contrast())).isNull();
    }
  }

  @Nested
  @DisplayName("diagnostics")
  class Reported {

    @Test
    @DisplayName("the default theme passes in both schemes")
    void defaultsPass() {
      assertThat(Theme.defaults().contrastDiagnostics()).isEmpty();
    }

    @Test
    @DisplayName("a scheme-aware accent override passes")
    void accentOverridePasses() {
      var theme =
          Theme.defaults()
              .with(
                  Tokens.ACCENT_PRIMARY,
                  CssColor.lightDark(CssColor.oklch(50, 0.16, 150), CssColor.oklch(72, 0.16, 150)))
              .with(Tokens.CONTROL_FG, Tokens.ACCENT_TEXT);
      assertThat(theme.contrastDiagnostics()).isEmpty();
    }

    @Test
    @DisplayName("a light-only failure reports only the light scheme")
    void lightOnly() {
      var theme =
          Theme.defaults()
              .with(Tokens.TEXT_MUTED, withLight(Tokens.TEXT_MUTED, CssColor.oklch(70, 0.02, 260)));
      var messages = messages(theme);

      assertThat(messages)
          .isNotEmpty()
          .allSatisfy(message ->
              assertThat(message).startsWith("Light scheme: ").contains("--text-muted ("))
          .contains(
              "Light scheme: --text-muted (#979fab) on --surface-subtle (#ebeff5) has a contrast"
                  + " ratio of 2.30:1, below the WCAG AA minimum of 4.5:1 for text in table"
                  + " headers, code captions and neutral badges");
      assertThat(theme.contrastDiagnostics())
          .allSatisfy(diagnostic -> {
            assertThat(diagnostic.rule()).isEqualTo("theme-contrast");
            assertThat(diagnostic.location()).isEqualTo(Location.ROOT);
            assertThat(diagnostic.fix()).contains("--text-muted").contains("light scheme");
          });
    }

    @Test
    @DisplayName("a dark-only failure reports only the dark scheme")
    void darkOnly() {
      var theme =
          Theme.defaults()
              .with(
                  Tokens.SURFACE_SUBTLE,
                  CssColor.lightDark(CssColor.oklch(95, 0.01, 260), CssColor.oklch(60, 0.02, 260)));

      assertThat(messages(theme))
          .isNotEmpty()
          .allSatisfy(message -> assertThat(message).startsWith("Dark scheme: "))
          .anySatisfy(message -> assertThat(message).startsWith("Dark scheme: --text-primary ("));
    }

    @Test
    @DisplayName("an alias names the token it follows")
    void aliased() {
      var brand = new DesignToken.Color("brand-ink", CssColor.oklch(80, 0.1, 150));
      var theme = Theme.defaults().with(brand, brand.defaultValue()).with(Tokens.CONTROL_FG, brand);

      assertThat(messages(theme))
          .anySatisfy(message ->
              assertThat(message)
                  .startsWith("Light scheme: --control-fg = --brand-ink (")
                  .contains(" on --surface-canvas ("));
      assertThat(theme.contrastDiagnostics())
          .allSatisfy(diagnostic ->
              assertThat(diagnostic.fix()).contains("CssColor.lightDark(light, dark)"));
    }

    @Test
    @DisplayName("a sameHue text token derived from a pale source still uses its own lightness")
    void sameHue() {
      var theme =
          Theme.defaults()
              .with(
                  Tokens.STATUS_INFO_TEXT,
                  CssColor.lightDark(
                      CssColor.sameHue(Tokens.STATUS_INFO, 62, 0.14),
                      CssColor.sameHue(Tokens.STATUS_INFO, 84, 0.12)));

      assertThat(messages(theme))
          .anySatisfy(message ->
              assertThat(message)
                  .startsWith("Light scheme: --status-info-text (")
                  .contains(" on --status-info at 14% over --surface-canvas (")
                  .endsWith("for text in info tinted badges"))
          .noneSatisfy(message -> assertThat(message).startsWith("Dark scheme"));
    }

    @Test
    @DisplayName("a mixed text color is measured after mixing")
    void mixed() {
      var faded = Tokens.TEXT_PRIMARY.mix(CssColor.Space.OKLAB, 40, Tokens.SURFACE_CANVAS);
      var theme = Theme.defaults().with(Tokens.CONTROL_FG, faded);

      assertThat(messages(theme))
          .isNotEmpty()
          .allSatisfy(message -> assertThat(message).contains("--control-fg ("))
          .anySatisfy(message -> assertThat(message).startsWith("Light scheme"))
          .anySatisfy(message -> assertThat(message).startsWith("Dark scheme"));
    }

    @Test
    @DisplayName("a solid fill too light for its label reports the fill token to darken")
    void solidFill() {
      var theme =
          Theme.defaults()
              .with(
                  Tokens.STATUS_SUCCESS,
                  CssColor.lightDark(CssColor.oklch(60, 0.17, 152), CssColor.oklch(72, 0.16, 152)));
      var solid =
          theme.contrastDiagnostics().stream()
              .filter(diagnostic -> diagnostic.message().contains("solid buttons"))
              .toList();

      assertThat(solid)
          .singleElement()
          .satisfies(diagnostic -> {
            assertThat(diagnostic.message())
                .startsWith(
                    "Light scheme: the solid success label color (#ffffff) on --status-success (")
                .contains("below the WCAG AA minimum of 4.5:1");
            assertThat(diagnostic.fix())
                .isEqualTo(
                    "Darken --status-success in the light scheme until the solid success label"
                        + " color on it reaches 4.5:1.");
          });
    }

    @Test
    @DisplayName("the reported ratio truncates, so a near miss never prints as 4.50:1")
    void boundary() {
      var passing = Theme.defaults().with(Tokens.CONTROL_FG, withLight(Tokens.TEXT_PRIMARY, BLACK));
      assertThat(passing.contrastDiagnostics()).isEmpty();

      // #777777 reaches 4.48:1 on white, the light card; the canvas is darker still.
      var failing =
          Theme.defaults()
              .with(
                  Tokens.CONTROL_FG,
                  CssColor.lightDark(CssColor.srgb(0x777777), CssColor.oklch(96, 0.01, 260)));
      assertThat(messages(failing))
          .anySatisfy(message ->
              assertThat(message)
                  .isEqualTo(
                      "Light scheme: --control-fg (#777777) on --surface-card (#ffffff) has a"
                          + " contrast ratio of 4.47:1, below the WCAG AA minimum of 4.5:1 for"
                          + " text in field labels inside cards"));
    }

    @Test
    @DisplayName("unresolvable pairs are skipped without false positives")
    void skipsUnresolvable() {
      var theme =
          Theme.defaults()
              .with(Tokens.TEXT_MUTED, CssColor.currentColor())
              .with(Tokens.CONTROL_FG, new DesignToken.Color("missing", BLACK));
      assertThat(theme.contrastDiagnostics()).isEmpty();
    }
  }

  @Nested
  @DisplayName("compilation")
  class Compilation {

    private final Theme failing =
        Theme.defaults().with(Tokens.ACCENT_PRIMARY, CssColor.oklch(55, 0.18, 150));

    @Test
    @DisplayName("a page compile rejects a failing theme before rendering")
    void page() {
      assertThat(
              Diagnostics.rules(() ->
                  Compiler.forPage()
                      .withTheme(failing)
                      .compile(PageDocument.of("Home", main(p("x"))))))
          .isNotEmpty()
          .containsOnly("theme-contrast");
    }

    @Test
    @DisplayName("a site compile reports theme failures with its path checks")
    void site() {
      var site =
          SiteDefinition.builder()
              .route(SiteRoute.of("/", PageDocument.of("Home", main(p("x")))))
              .route(SiteRoute.of("/", PageDocument.of("Again", main(p("y")))))
              .build();

      assertThat(Diagnostics.rules(() -> Compiler.forSite().withTheme(failing).compile(site)))
          .contains("theme-contrast")
          .endsWith("duplicate-route");
    }
  }
}
