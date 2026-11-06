package dev.nthings.tessera.css;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Registry of design tokens emitted into `@layer theme`.
public final class Tokens {

  public static final DesignToken.FontFamily FONT_BODY =
      new DesignToken.FontFamily("font-body", "system-ui,sans-serif");
  public static final DesignToken.FontFamily FONT_MONO =
      new DesignToken.FontFamily("font-mono", "ui-monospace,SFMono-Regular,Consolas,monospace");
  public static final DesignToken.Length FONT_SIZE_XS =
      new DesignToken.Length("font-size-xs", "0.75rem");
  public static final DesignToken.Length FONT_SIZE_SM =
      new DesignToken.Length("font-size-sm", "0.875rem");
  public static final DesignToken.Length FONT_SIZE_MD =
      new DesignToken.Length("font-size-md", "1rem");
  public static final DesignToken.Length FONT_SIZE_LG =
      new DesignToken.Length("font-size-lg", "1.125rem");
  public static final DesignToken.Length FONT_SIZE_XL =
      new DesignToken.Length("font-size-xl", "1.25rem");
  public static final DesignToken.Length FONT_SIZE_2XL =
      new DesignToken.Length("font-size-2xl", "1.5rem");
  public static final DesignToken.Number LINE_HEIGHT_BODY =
      new DesignToken.Number("line-height-body", "1.5");
  public static final DesignToken.Length TRACKING_NORMAL =
      new DesignToken.Length("tracking-normal", "0");
  public static final DesignToken.Number DENSITY = new DesignToken.Number("density", "1");
  public static final DesignToken.Length CONTROL_PADDING_INLINE =
      DesignToken.Length.space("control-padding-inline", "0.875rem");
  public static final DesignToken.Length SPACE_0_5 =
      DesignToken.Length.space("space-0-5", "0.125rem");
  public static final DesignToken.Length SPACE_1_5 =
      DesignToken.Length.space("space-1-5", "0.375rem");
  public static final DesignToken.Length SPACE_2_5 =
      DesignToken.Length.space("space-2-5", "0.625rem");
  public static final DesignToken.Length SPACE_3_5 =
      DesignToken.Length.space("space-3-5", "0.875rem");
  public static final DesignToken.Length SPACE_1 = DesignToken.Length.space("space-1", "0.25rem");
  public static final DesignToken.Length SPACE_2 = DesignToken.Length.space("space-2", "0.5rem");
  public static final DesignToken.Length SPACE_3 = DesignToken.Length.space("space-3", "0.75rem");
  public static final DesignToken.Length SPACE_4 = DesignToken.Length.space("space-4", "1rem");
  public static final DesignToken.Length SPACE_5 = DesignToken.Length.space("space-5", "1.25rem");
  public static final DesignToken.Length SPACE_6 = DesignToken.Length.space("space-6", "1.5rem");
  public static final DesignToken.Length SPACE_8 = DesignToken.Length.space("space-8", "2rem");
  public static final DesignToken.Length SPACE_10 = DesignToken.Length.space("space-10", "2.5rem");
  public static final DesignToken.Length SPACE_12 = DesignToken.Length.space("space-12", "3rem");
  public static final DesignToken.Length RADIUS_SM = new DesignToken.Length("radius-sm", "0.25rem");
  public static final DesignToken.Length RADIUS_MD =
      new DesignToken.Length("radius-md", "0.375rem");
  public static final DesignToken.Length RADIUS_LG = new DesignToken.Length("radius-lg", "0.5rem");
  public static final DesignToken.Length RADIUS_XL = new DesignToken.Length("radius-xl", "0.75rem");
  public static final DesignToken.Length RADIUS_FULL =
      new DesignToken.Length("radius-full", "9999px");

  public static final DesignToken.FontFamily CONTROL_FAMILY =
      DesignToken.FontFamily.alias("control-family", FONT_BODY);
  public static final DesignToken.Length CONTROL_FONT_SIZE =
      DesignToken.Length.alias("control-font-size", FONT_SIZE_SM);
  public static final DesignToken.Number CONTROL_LINE_HEIGHT =
      DesignToken.Number.alias("control-line-height", LINE_HEIGHT_BODY);
  public static final DesignToken.Length CONTROL_TRACKING =
      DesignToken.Length.alias("control-tracking", TRACKING_NORMAL);
  public static final DesignToken.Duration CONTROL_FEEDBACK_DURATION =
      new DesignToken.Duration("control-feedback-duration", "150ms");
  public static final DesignToken.Duration OVERLAY_DURATION =
      new DesignToken.Duration("overlay-duration", "200ms");
  public static final DesignToken.Duration DISCLOSURE_DURATION =
      new DesignToken.Duration("disclosure-duration", "250ms");

  public static final DesignToken.Color SURFACE_CANVAS =
      DesignToken.Color.lightDark("surface-canvas", "oklch(99% 0.005 260)", "oklch(14% 0.015 260)");
  public static final DesignToken.Color SURFACE_CARD =
      DesignToken.Color.lightDark("surface-card", "oklch(100% 0 0)", "oklch(19% 0.02 260)");
  public static final DesignToken.Color SURFACE_SUBTLE =
      DesignToken.Color.lightDark("surface-subtle", "oklch(95% 0.01 260)", "oklch(24% 0.02 260)");
  public static final DesignToken.Color TEXT_PRIMARY =
      DesignToken.Color.lightDark("text-primary", "oklch(18% 0.02 260)", "oklch(96% 0.01 260)");
  public static final DesignToken.Color TEXT_MUTED =
      DesignToken.Color.lightDark("text-muted", "oklch(45% 0.02 260)", "oklch(72% 0.02 260)");
  public static final DesignToken.Color ACCENT_PRIMARY =
      DesignToken.Color.lightDark("accent-primary", "oklch(54% 0.21 262)", "oklch(68% 0.19 262)");
  public static final DesignToken.Color BORDER_SUBTLE =
      DesignToken.Color.lightDark("border-subtle", "oklch(88% 0.01 260)", "oklch(30% 0.02 260)");
  public static final DesignToken.Color STATUS_SUCCESS =
      DesignToken.Color.lightDark("status-success", "oklch(55% 0.17 152)", "oklch(72% 0.16 152)");
  public static final DesignToken.Color STATUS_WARNING =
      DesignToken.Color.lightDark("status-warning", "oklch(68% 0.17 75)", "oklch(80% 0.16 75)");
  public static final DesignToken.Color STATUS_DANGER =
      DesignToken.Color.lightDark("status-danger", "oklch(56% 0.21 25)", "oklch(70% 0.19 25)");
  public static final DesignToken.Color STATUS_INFO =
      DesignToken.Color.lightDark("status-info", "oklch(58% 0.16 235)", "oklch(74% 0.14 235)");
  public static final DesignToken.Color CONTROL_FG =
      DesignToken.Color.alias("control-fg", TEXT_PRIMARY);

  private final Map<String, DesignToken> entries = new LinkedHashMap<>();

  public Tokens() {
    for (var token :
        new DesignToken[] {
          FONT_BODY,
          FONT_MONO,
          FONT_SIZE_XS,
          FONT_SIZE_SM,
          FONT_SIZE_MD,
          FONT_SIZE_LG,
          FONT_SIZE_XL,
          FONT_SIZE_2XL,
          LINE_HEIGHT_BODY,
          TRACKING_NORMAL,
          DENSITY,
          CONTROL_PADDING_INLINE,
          SPACE_0_5,
          SPACE_1_5,
          SPACE_2_5,
          SPACE_3_5,
          SPACE_1,
          SPACE_2,
          SPACE_3,
          SPACE_4,
          SPACE_5,
          SPACE_6,
          SPACE_8,
          SPACE_10,
          SPACE_12,
          RADIUS_SM,
          RADIUS_MD,
          RADIUS_LG,
          RADIUS_XL,
          RADIUS_FULL,
          CONTROL_FAMILY,
          CONTROL_FONT_SIZE,
          CONTROL_LINE_HEIGHT,
          CONTROL_TRACKING,
          CONTROL_FEEDBACK_DURATION,
          OVERLAY_DURATION,
          DISCLOSURE_DURATION
        }) {
      register(token);
    }
    register(SURFACE_CANVAS);
    register(SURFACE_CARD);
    register(SURFACE_SUBTLE);
    register(TEXT_PRIMARY);
    register(TEXT_MUTED);
    register(ACCENT_PRIMARY);
    register(BORDER_SUBTLE);
    register(STATUS_SUCCESS);
    register(STATUS_WARNING);
    register(STATUS_DANGER);
    register(STATUS_INFO);
    register(CONTROL_FG);
  }

  public Tokens register(DesignToken token) {
    entries.put(token.name(), token);
    return this;
  }

  public Collection<DesignToken> all() {
    return Collections.unmodifiableCollection(entries.values());
  }
}
