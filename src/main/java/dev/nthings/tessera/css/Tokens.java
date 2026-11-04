package dev.nthings.tessera.css;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Registry of design tokens emitted into `@layer theme`.
public final class Tokens {

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
