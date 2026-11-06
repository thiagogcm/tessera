package dev.nthings.tessera.css;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Registry of design tokens emitted into `@layer theme`.
public final class Tokens {

  public static final DesignToken FONT_BODY =
      DesignToken.value("font-body", "system-ui,sans-serif");
  public static final DesignToken FONT_MONO =
      DesignToken.value("font-mono", "ui-monospace,SFMono-Regular,Consolas,monospace");
  public static final DesignToken FONT_SIZE_XS = DesignToken.value("font-size-xs", "0.75rem");
  public static final DesignToken FONT_SIZE_SM = DesignToken.value("font-size-sm", "0.875rem");
  public static final DesignToken FONT_SIZE_MD = DesignToken.value("font-size-md", "1rem");
  public static final DesignToken FONT_SIZE_LG = DesignToken.value("font-size-lg", "1.125rem");
  public static final DesignToken FONT_SIZE_XL = DesignToken.value("font-size-xl", "1.25rem");
  public static final DesignToken FONT_SIZE_2XL = DesignToken.value("font-size-2xl", "1.5rem");
  public static final DesignToken LINE_HEIGHT_BODY = DesignToken.value("line-height-body", "1.5");
  public static final DesignToken DENSITY = DesignToken.value("density", "1");
  public static final DesignToken CONTROL_PADDING_INLINE =
      space("control-padding-inline", "0.875rem");
  public static final DesignToken SPACE_0_5 = space("space-0-5", "0.125rem");
  public static final DesignToken SPACE_1_5 = space("space-1-5", "0.375rem");
  public static final DesignToken SPACE_2_5 = space("space-2-5", "0.625rem");
  public static final DesignToken SPACE_3_5 = space("space-3-5", "0.875rem");
  public static final DesignToken SPACE_1 = space("space-1", "0.25rem");
  public static final DesignToken SPACE_2 = space("space-2", "0.5rem");
  public static final DesignToken SPACE_3 = space("space-3", "0.75rem");
  public static final DesignToken SPACE_4 = space("space-4", "1rem");
  public static final DesignToken SPACE_5 = space("space-5", "1.25rem");
  public static final DesignToken SPACE_6 = space("space-6", "1.5rem");
  public static final DesignToken SPACE_8 = space("space-8", "2rem");
  public static final DesignToken SPACE_10 = space("space-10", "2.5rem");
  public static final DesignToken SPACE_12 = space("space-12", "3rem");
  public static final DesignToken RADIUS_SM = DesignToken.value("radius-sm", "0.25rem");
  public static final DesignToken RADIUS_MD = DesignToken.value("radius-md", "0.375rem");
  public static final DesignToken RADIUS_LG = DesignToken.value("radius-lg", "0.5rem");
  public static final DesignToken RADIUS_XL = DesignToken.value("radius-xl", "0.75rem");
  public static final DesignToken RADIUS_FULL = DesignToken.value("radius-full", "9999px");

  private static DesignToken space(String name, String size) {
    return DesignToken.value(name, "calc(" + size + " * var(--density))");
  }

  public static final DesignToken SURFACE_CANVAS =
      DesignToken.lightDark("surface-canvas", "oklch(99% 0.005 260)", "oklch(14% 0.015 260)");
  public static final DesignToken SURFACE_CARD =
      DesignToken.lightDark("surface-card", "oklch(100% 0 0)", "oklch(19% 0.02 260)");
  public static final DesignToken SURFACE_SUBTLE =
      DesignToken.lightDark("surface-subtle", "oklch(95% 0.01 260)", "oklch(24% 0.02 260)");
  public static final DesignToken TEXT_PRIMARY =
      DesignToken.lightDark("text-primary", "oklch(18% 0.02 260)", "oklch(96% 0.01 260)");
  public static final DesignToken TEXT_MUTED =
      DesignToken.lightDark("text-muted", "oklch(45% 0.02 260)", "oklch(72% 0.02 260)");
  public static final DesignToken ACCENT_PRIMARY =
      DesignToken.lightDark("accent-primary", "oklch(54% 0.21 262)", "oklch(68% 0.19 262)");
  public static final DesignToken BORDER_SUBTLE =
      DesignToken.lightDark("border-subtle", "oklch(88% 0.01 260)", "oklch(30% 0.02 260)");
  public static final DesignToken STATUS_SUCCESS =
      DesignToken.lightDark("status-success", "oklch(55% 0.17 152)", "oklch(72% 0.16 152)");
  public static final DesignToken STATUS_WARNING =
      DesignToken.lightDark("status-warning", "oklch(68% 0.17 75)", "oklch(80% 0.16 75)");
  public static final DesignToken STATUS_DANGER =
      DesignToken.lightDark("status-danger", "oklch(56% 0.21 25)", "oklch(70% 0.19 25)");
  public static final DesignToken STATUS_INFO =
      DesignToken.lightDark("status-info", "oklch(58% 0.16 235)", "oklch(74% 0.14 235)");

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
          RADIUS_FULL
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
  }

  public Tokens register(DesignToken token) {
    entries.put(token.name(), token);
    return this;
  }

  public Collection<DesignToken> all() {
    return Collections.unmodifiableCollection(entries.values());
  }
}
