package dev.nthings.tessera.css;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.internal.foundation.Decimals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// The text and surface color pairs built-in components draw from theme tokens, and the WCAG AA
/// check of each pair in both color schemes. Backs [Theme#contrastDiagnostics()].
final class ThemeContrast {

  static final String RULE = "theme-contrast";

  /// WCAG 2.2 success criterion 1.4.3 (AA) minimum for text below the large-text size. Every pair
  /// is text at component sizes; non-text indicators such as focus rings and switch tracks use
  /// [Tokens#ACCENT_PRIMARY], which its text pairs already hold to this stricter minimum.
  static final double TEXT_MINIMUM = 4.5;

  private static final List<Pair> PAIRS = catalog();

  private ThemeContrast() {}

  /// The color scheme a `light-dark()` value follows, and the opaque user-agent canvas a
  /// translucent page background shows through.
  enum Scheme {
    LIGHT(0xffffff),
    DARK(0x121212);

    private final ColorMath.Lab canvas;

    Scheme(int canvas) {
      this.canvas = ColorMath.srgb(canvas, 1);
    }

    String label() {
      return this == LIGHT ? "Light" : "Dark";
    }

    String adjective() {
      return this == LIGHT ? "light" : "dark";
    }
  }

  /// A text color. `token` is the one an author overrides to fix it, or null when the color is a
  /// component constant such as a solid fill's label color.
  private record Ink(CssColor color, DesignToken.@Nullable Color token, String label) {}

  /// The layers beneath the text, painted upward from the page canvas. `token` is the one an author
  /// overrides to fix it, or null for a tint whose fix is the text color.
  private record Surface(List<CssColor> layers, DesignToken.@Nullable Color token, String label) {}

  private record Pair(Ink ink, Surface surface, String use) {}

  /// One pair evaluated in one scheme, when every color in it resolves statically.
  record Measurement(ColorMath.Lab text, ColorMath.Lab surface, double ratio) {}

  /// Every failing pair of `theme`, light scheme first, in catalog order.
  static List<Diagnostic> diagnostics(Theme theme) {
    var diagnostics = new ArrayList<Diagnostic>();
    for (var scheme : Scheme.values()) {
      for (var pair : PAIRS) {
        var measured = measure(theme, scheme, pair.ink().color(), pair.surface().layers());
        if (measured != null && measured.ratio() < TEXT_MINIMUM) {
          diagnostics.add(diagnostic(theme, scheme, pair, measured));
        }
      }
    }
    return List.copyOf(diagnostics);
  }

  /// `text` over `layers` (painted upward over the scheme's canvas) in `scheme`, or null if any
  /// color depends on runtime state such as `currentColor`, an undefined token, or a token cycle.
  static @Nullable Measurement measure(
      Theme theme, Scheme scheme, CssColor text, List<CssColor> layers) {
    var surface = scheme.canvas;
    for (var layer : layers) {
      var painted = resolve(layer, theme, scheme, new HashSet<>());
      if (painted == null) {
        return null;
      }
      surface = ColorMath.over(painted, surface);
    }
    var ink = resolve(text, theme, scheme, new HashSet<>());
    if (ink == null) {
      return null;
    }
    var visible = ColorMath.over(ink, surface);
    return new Measurement(visible, surface, ColorMath.contrast(visible, surface));
  }

  /// `color` as `theme` computes it in `scheme`, or null if it cannot be known statically.
  /// `visiting` holds the tokens being resolved, so a cycle resolves to null as CSS makes it
  /// invalid.
  private static ColorMath.@Nullable Lab resolve(
      CssColor color, Theme theme, Scheme scheme, Set<String> visiting) {
    return switch (color) {
      case CssColors.Oklch oklch ->
          ColorMath.oklch(oklch.lightness(), oklch.chroma(), oklch.hue(), oklch.alpha());
      case CssColors.Srgb srgb -> ColorMath.srgb(srgb.rgb(), srgb.alpha());
      case CssColors.Named named ->
          named == CssColors.Named.TRANSPARENT ? ColorMath.TRANSPARENT : null;
      case CssColors.LightDark lightDark ->
          resolve(
              scheme == Scheme.LIGHT ? lightDark.light() : lightDark.dark(),
              theme,
              scheme,
              visiting);
      case CssColors.Relative relative -> {
        var origin = resolve(relative.base(), theme, scheme, visiting);
        yield origin == null
            ? null
            : ColorMath.oklch(
                relative.lightness(), relative.chroma(), origin.hue(), origin.alpha());
      }
      case CssColors.Mix mix -> {
        var share = share(mix.weight());
        var one = resolve(mix.first(), theme, scheme, visiting);
        var two = resolve(mix.second(), theme, scheme, visiting);
        yield share == null || one == null || two == null
            ? null
            : ColorMath.mix(mix.space(), one, share, two);
      }
      case CssColors.ContrastOn contrastOn -> {
        var behind = resolve(contrastOn.background(), theme, scheme, visiting);
        if (behind == null || !behind.opaque()) {
          yield null;
        }
        var white = ColorMath.srgb(0xffffff, 1);
        var black = ColorMath.srgb(0x000000, 1);
        yield ColorMath.contrast(white, behind) >= ColorMath.contrast(black, behind)
            ? white
            : black;
      }
      case DesignToken.Color token -> {
        if (!visiting.add(token.name())) {
          yield null;
        }
        var resolved =
            theme.value(token.name()) instanceof CssColor value
                ? resolve(value, theme, scheme, visiting)
                : null;
        visiting.remove(token.name());
        yield resolved;
      }
    };
  }

  /// A `color-mix()` weight as a share in `[0, 1]`, or null unless it is a plain percentage.
  private static @Nullable Double share(CssLength weight) {
    return weight instanceof CssLengths.Dimension dimension
            && dimension.unit() == CssLengths.Unit.PERCENT
            && dimension.value() >= 0
            && dimension.value() <= 100
        ? dimension.value() / 100
        : null;
  }

  private static Diagnostic diagnostic(
      Theme theme, Scheme scheme, Pair pair, Measurement measured) {
    var inkToken = pair.ink().token();
    var surfaceToken = pair.surface().token();
    var inkLabel = inkToken == null ? pair.ink().label() : describe(inkToken, theme);
    var surfaceLabel =
        surfaceToken == null ? pair.surface().label() : describe(surfaceToken, theme);
    var message =
        scheme.label()
            + " scheme: "
            + inkLabel
            + " ("
            + ColorMath.hex(measured.text())
            + ") on "
            + surfaceLabel
            + " ("
            + ColorMath.hex(measured.surface())
            + ") has a contrast ratio of "
            + ratio(measured.ratio())
            + ":1, below the WCAG AA minimum of "
            + Decimals.format(TEXT_MINIMUM)
            + ":1 for text in "
            + pair.use();
    return new Diagnostic(RULE, message, fix(theme, scheme, pair, measured), Location.ROOT);
  }

  private static String fix(Theme theme, Scheme scheme, Pair pair, Measurement measured) {
    var textLighter =
        ColorMath.luminance(measured.text()) >= ColorMath.luminance(measured.surface());
    var inkToken = pair.ink().token();
    var surfaceToken = pair.surface().token();
    var minimum = Decimals.format(TEXT_MINIMUM) + ":1";
    var where = " in the " + scheme.adjective() + " scheme";
    String advice;
    if (inkToken != null && surfaceToken != null) {
      advice =
          (textLighter ? "Lighten " : "Darken ")
              + name(inkToken)
              + " or "
              + (textLighter ? "darken " : "lighten ")
              + name(surfaceToken)
              + where
              + " until the pair reaches "
              + minimum
              + ".";
    } else if (inkToken != null) {
      advice =
          (textLighter ? "Lighten " : "Darken ")
              + name(inkToken)
              + where
              + " until it reaches "
              + minimum
              + " on "
              + pair.surface().label()
              + ".";
    } else {
      var token = surfaceToken == null ? "the surface" : name(surfaceToken);
      advice =
          (textLighter ? "Darken " : "Lighten ")
              + token
              + where
              + " until "
              + pair.ink().label()
              + " on it reaches "
              + minimum
              + ".";
    }
    var flat =
        (inkToken != null && !(theme.value(inkToken.name()) instanceof CssColors.LightDark))
            || (surfaceToken != null
                && !(theme.value(surfaceToken.name()) instanceof CssColors.LightDark));
    return flat
        ? advice
            + " Use CssColor.lightDark(light, dark) to give a token separate light and dark values."
        : advice;
  }

  /// `--name`, followed by the token it aliases in `theme`, if any.
  private static String describe(DesignToken.Color token, Theme theme) {
    var described = new StringBuilder(name(token));
    var seen = new HashSet<String>();
    seen.add(token.name());
    var value = theme.value(token.name());
    while (value instanceof DesignToken.Color alias && seen.add(alias.name())) {
      described.append(" = ").append(name(alias));
      value = theme.value(alias.name());
    }
    return described.toString();
  }

  private static String name(DesignToken.Color token) {
    return "--" + token.name();
  }

  /// `ratio` truncated to two decimals, so a failing ratio never prints as the minimum.
  private static String ratio(double ratio) {
    return BigDecimal.valueOf(ratio).setScale(2, RoundingMode.FLOOR).toPlainString();
  }

  private static List<Pair> catalog() {
    var canvas = on(Tokens.SURFACE_CANVAS);
    var card = on(Tokens.SURFACE_CARD);
    var subtle = on(Tokens.SURFACE_SUBTLE);
    var pairs = new LinkedHashMap<List<Object>, Pair>();

    add(pairs, ink(Tokens.TEXT_PRIMARY), canvas, "body text");
    add(pairs, ink(Tokens.TEXT_PRIMARY), card, "cards, dialogs, menus and form controls");
    add(pairs, ink(Tokens.TEXT_PRIMARY), subtle, "hovered menu items and search results");
    add(pairs, ink(Tokens.TEXT_MUTED), canvas, "secondary text, hints and breadcrumbs");
    add(pairs, ink(Tokens.TEXT_MUTED), card, "card and dialog descriptions");
    add(pairs, ink(Tokens.TEXT_MUTED), subtle, "table headers, code captions and neutral badges");
    add(pairs, ink(Tokens.CONTROL_FG), canvas, "field and switch labels");
    add(pairs, ink(Tokens.CONTROL_FG), card, "field labels inside cards");
    add(pairs, ink(Tokens.ACCENT_PRIMARY), canvas, "active navigation tabs and section eyebrows");
    add(pairs, ink(Tokens.ACCENT_PRIMARY), card, "active content tabs and pager links");
    add(pairs, ink(Tokens.ACCENT_PRIMARY), subtle, "hovered pager links");

    // Solid neutral buttons fill with primary text; solid neutral badges with muted text.
    add(pairs, ink(Tokens.SURFACE_CARD), on(Tokens.TEXT_PRIMARY), "solid neutral buttons");
    add(pairs, ink(Tokens.SURFACE_CARD), on(Tokens.TEXT_MUTED), "solid neutral badges");

    for (var tone : Tone.values()) {
      if (tone == Tone.NEUTRAL) {
        continue;
      }
      var name = tone.name().toLowerCase(Locale.ROOT);
      var text = ink(tone.textColor());
      add(pairs, text, canvas, name + " outline badges, outline and ghost buttons, and messages");
      add(pairs, text, card, name + " outline badges, buttons and messages inside cards");
      add(pairs, text, tint(tone.color(), 14, Tokens.SURFACE_CANVAS), name + " tinted badges");
      add(
          pairs,
          new Ink(tone.onSolid(), null, "the solid " + name + " label color"),
          on(tone.color()),
          name + " solid buttons and badges");
      add(pairs, ink(Tokens.CONTROL_FG), blend(tone.color(), 14), name + " callouts");
      add(
          pairs,
          ink(Tokens.TEXT_PRIMARY),
          tint(tone.color(), 24, Tokens.SURFACE_CANVAS),
          name + " text highlights");
    }

    var invalid = blend(Tokens.STATUS_DANGER, 8);
    add(pairs, ink(Tokens.TEXT_PRIMARY), invalid, "invalid form fields");
    var highlight = blend(Tokens.ACCENT_PRIMARY, 6);
    add(pairs, ink(Tokens.TEXT_PRIMARY), highlight, "highlight sections");
    add(pairs, ink(Tokens.TEXT_MUTED), highlight, "highlight section descriptions");
    add(pairs, ink(Tokens.ACCENT_PRIMARY), highlight, "highlight section eyebrows");
    add(
        pairs,
        ink(Tokens.STATUS_SUCCESS_TEXT),
        tint(Tokens.STATUS_SUCCESS, 16, Tokens.SURFACE_CARD),
        "inserted code lines");
    add(
        pairs,
        ink(Tokens.STATUS_DANGER_TEXT),
        tint(Tokens.STATUS_DANGER, 16, Tokens.SURFACE_CARD),
        "deleted code lines");
    return List.copyOf(pairs.values());
  }

  /// Records a pair, merging the uses of a pair already present.
  private static void add(Map<List<Object>, Pair> pairs, Ink ink, Surface surface, String use) {
    pairs.merge(
        List.of(ink.color(), surface.layers()),
        new Pair(ink, surface, use),
        (first, second) -> new Pair(first.ink(), first.surface(), first.use() + "; " + use));
  }

  private static Ink ink(DesignToken.Color token) {
    return new Ink(token, token, name(token));
  }

  /// An opaque surface token over the page canvas.
  private static Surface on(DesignToken.Color token) {
    var layers =
        token.equals(Tokens.SURFACE_CANVAS)
            ? List.<CssColor>of(token)
            : List.<CssColor>of(Tokens.SURFACE_CANVAS, token);
    return new Surface(layers, token, name(token));
  }

  /// `tone` at `percent` opacity over the opaque `base`, as `tone.mixTransparent(percent)` paints.
  private static Surface tint(DesignToken.Color tone, int percent, DesignToken.Color base) {
    var layers = new ArrayList<CssColor>(on(base).layers());
    layers.add(tone.mixTransparent(percent));
    return new Surface(
        List.copyOf(layers), null, name(tone) + " at " + percent + "% over " + name(base));
  }

  /// `tone` mixed `percent` into [Tokens#SURFACE_CARD] in OKLab, the tinted card of callouts,
  /// invalid fields and highlight sections.
  private static Surface blend(DesignToken.Color tone, int percent) {
    return new Surface(
        List.of(
            Tokens.SURFACE_CANVAS, tone.mix(CssColor.Space.OKLAB, percent, Tokens.SURFACE_CARD)),
        null,
        name(tone) + " mixed " + percent + "% into " + name(Tokens.SURFACE_CARD));
  }
}
