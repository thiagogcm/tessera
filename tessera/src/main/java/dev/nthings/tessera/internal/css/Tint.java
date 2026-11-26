package dev.nthings.tessera.internal.css;

import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssNumber;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/// The tinted surfaces built-in components paint behind text from a [Tone]'s color, each with the
/// text colors drawn on it. This is the only definition of each recipe: components paint with
/// [#fill(Tone)] (a ramp with [#ramp(Tone, CssNumber, CssNumber)]) and [#text(Tone)], and
/// `Theme.contrastDiagnostics()` measures every text of every tint for every tone it lists, over
/// every surface it can sit on. A tint is checked as soon as it is declared here.
public enum Tint {
  /// Tinted badges, labelled in the tone's text color.
  BADGE(
      "tinted badges",
      Blend.OVER,
      List.of(Tokens.SURFACE_CANVAS, Tokens.SURFACE_CARD),
      14,
      14,
      tonal(),
      List.of(Tone::textColor)),

  /// Outline and ghost buttons on hover, labelled in the tone's text color.
  BUTTON_HOVER(
      "outline and ghost buttons on hover",
      Blend.OVER,
      List.of(Tokens.SURFACE_CANVAS, Tokens.SURFACE_CARD),
      12,
      12,
      tonal(),
      List.of(Tone::textColor)),

  /// `highlight(Tone)` behind inherited body text.
  HIGHLIGHT(
      "text highlights",
      Blend.OVER,
      List.of(Tokens.SURFACE_CANVAS, Tokens.SURFACE_CARD),
      24,
      24,
      EnumSet.allOf(Tone.class),
      List.of(tone -> Tokens.TEXT_PRIMARY)),

  /// The table of contents entry for the section in view, in the accent text color.
  CURRENT_ENTRY(
      "the current table of contents entry",
      Blend.OVER,
      List.of(Tokens.SURFACE_CANVAS, Tokens.SURFACE_CARD),
      24,
      24,
      EnumSet.of(Tone.ACCENT),
      List.of(Tone::textColor)),

  /// Callout panels, in the control text color.
  CALLOUT(
      "callouts",
      Blend.INTO,
      List.of(Tokens.SURFACE_CARD),
      14,
      14,
      tonal(),
      List.of(tone -> Tokens.CONTROL_FG)),

  /// Inserted (success) and deleted (danger) code lines, in the tone's text color.
  CODE_CHANGE(
      "changed code lines",
      Blend.OVER,
      List.of(Tokens.SURFACE_CARD),
      16,
      16,
      EnumSet.of(Tone.SUCCESS, Tone.DANGER),
      List.of(Tone::textColor)),

  /// Highlighted code lines behind inherited body text and its syntax colors.
  CODE_HIGHLIGHT(
      "highlighted code lines",
      Blend.OVER,
      List.of(Tokens.SURFACE_CARD),
      15,
      15,
      EnumSet.of(Tone.ACCENT),
      List.of(
          tone -> Tokens.TEXT_PRIMARY,
          tone -> Tokens.SYNTAX_COMMENT,
          tone -> Tokens.SYNTAX_KEYWORD,
          tone -> Tokens.SYNTAX_STRING,
          tone -> Tokens.SYNTAX_CONSTANT,
          tone -> Tokens.SYNTAX_TYPE,
          tone -> Tokens.SYNTAX_FUNCTION,
          tone -> Tokens.SYNTAX_PROPERTY,
          tone -> Tokens.SYNTAX_TAG,
          tone -> Tokens.SYNTAX_META)),

  /// Form fields the user left invalid.
  INVALID_FIELD(
      "invalid form fields",
      Blend.INTO,
      List.of(Tokens.SURFACE_CARD),
      8,
      8,
      EnumSet.of(Tone.DANGER),
      List.of(tone -> Tokens.TEXT_PRIMARY)),

  /// Highlight sections: body text, the description and the eyebrow.
  SECTION(
      "highlight sections",
      Blend.INTO,
      List.of(Tokens.SURFACE_CARD),
      6,
      6,
      EnumSet.of(Tone.ACCENT),
      List.of(
          tone -> Tokens.TEXT_PRIMARY, tone -> Tokens.TEXT_MUTED, tone -> Tokens.ACCENT_PRIMARY)),

  /// Step numbers, shading toward the accent from the first step to the last.
  STEP(
      "step numbers",
      Blend.INTO,
      List.of(Tokens.SURFACE_SUBTLE),
      20,
      60,
      EnumSet.of(Tone.ACCENT),
      List.of(tone -> Tokens.TEXT_PRIMARY));

  /// How a tint combines the tone's color with its [#surfaces()].
  public enum Blend {
    /// The tone's color at the tint's opacity, showing whichever surface is beneath through it.
    OVER,
    /// The tone's color mixed in OKLab into the surface, an opaque surface of its own.
    INTO
  }

  /// One fill the contrast check measures: the tint at `percent`, painted with `fill`.
  public record Sample(int percent, CssColor fill) {}

  private final String use;
  private final Blend blend;
  private final List<DesignToken.Color> surfaces;
  private final int percent;
  private final int lastPercent;
  private final List<Tone> tones;
  private final List<Function<Tone, DesignToken.Color>> texts;

  Tint(
      String use,
      Blend blend,
      List<DesignToken.Color> surfaces,
      int percent,
      int lastPercent,
      EnumSet<Tone> tones,
      List<Function<Tone, DesignToken.Color>> texts) {
    this.use = use;
    this.blend = blend;
    this.surfaces = surfaces;
    this.percent = percent;
    this.lastPercent = lastPercent;
    this.tones = List.copyOf(tones);
    this.texts = texts;
  }

  /// Every tone but [Tone#NEUTRAL], whose components paint [Tokens#SURFACE_SUBTLE] instead.
  private static EnumSet<Tone> tonal() {
    return EnumSet.complementOf(EnumSet.of(Tone.NEUTRAL));
  }

  /// The background of this tint in `tone`. Throws IllegalArgumentException for a tone the tint
  /// does not declare, so a component cannot paint a pair the contrast check does not measure, and
  /// IllegalStateException for a ramp.
  public CssColor fill(Tone tone) {
    if (percent != lastPercent) {
      throw new IllegalStateException(name() + " tint is a ramp; paint it with ramp()");
    }
    return paint(tone, CssLength.pct(percent));
  }

  /// The background of this ramp in `tone` at step `index` of `count`, which shades from the first
  /// percentage toward the last and reaches it at the last step. Throws as [#fill(Tone)] does for
  /// an undeclared tone.
  public CssColor ramp(Tone tone, CssNumber index, CssNumber count) {
    var shading = CssLength.pct(lastPercent - percent).times(index).dividedBy(count);
    return paint(tone, CssLength.pct(percent).plus(shading));
  }

  /// The fills the contrast check measures in `tone`: the single fill, or both ends of a ramp.
  public List<Sample> samples(Tone tone) {
    var first = new Sample(percent, paint(tone, CssLength.pct(percent)));
    return percent == lastPercent
        ? List.of(first)
        : List.of(first, new Sample(lastPercent, paint(tone, CssLength.pct(lastPercent))));
  }

  private CssColor paint(Tone tone, CssLength weight) {
    requireTone(tone);
    var color = tone.color();
    return switch (blend) {
      case OVER -> color.mix(CssColor.Space.OKLCH, weight, CssColor.transparent());
      case INTO -> color.mix(CssColor.Space.OKLAB, weight, surfaces.getFirst());
    };
  }

  /// The text color components set on this tint in `tone`: the first of [#texts(Tone)].
  public DesignToken.Color text(Tone tone) {
    return texts(tone).getFirst();
  }

  /// Every text color drawn on this tint in `tone`, including text that inherits its color.
  public List<DesignToken.Color> texts(Tone tone) {
    requireTone(tone);
    return texts.stream().map(text -> text.apply(tone)).toList();
  }

  /// The tones this tint paints, in [Tone] order.
  public List<Tone> tones() {
    return tones;
  }

  public Blend blend() {
    return blend;
  }

  /// For [Blend#OVER], the opaque surfaces the tint can sit on; for [Blend#INTO], the one surface
  /// the tone is mixed into.
  public List<DesignToken.Color> surfaces() {
    return surfaces;
  }

  /// The components that paint this tint in `tone`, such as `info tinted badges`. A tint with one
  /// tone omits it.
  public String use(Tone tone) {
    return tones.size() == 1 ? use : tone.name().toLowerCase(Locale.ROOT) + " " + use;
  }

  private void requireTone(Tone tone) {
    if (!tones.contains(tone)) {
      throw new IllegalArgumentException(name() + " tint does not paint the " + tone + " tone");
    }
  }
}
