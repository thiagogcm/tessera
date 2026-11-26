package dev.nthings.tessera.internal.components;

import static dev.nthings.tessera.components.Icon.circle;
import static dev.nthings.tessera.components.Icon.line;
import static dev.nthings.tessera.components.Icon.path;
import static dev.nthings.tessera.components.Icon.polyline;
import static dev.nthings.tessera.components.Icon.stroked;

import dev.nthings.tessera.core.TagElement;

/// The icons bundled components use, built with [dev.nthings.tessera.components.Icon].
public final class Icons {

  private Icons() {}

  /// The magnifier of search fields and triggers.
  public static TagElement search() {
    return stroked(16, 24, 2, circle(11, 11, 8), line(21, 21, 16.65, 16.65));
  }

  /// The right-pointing chevron of disclosure rows.
  public static TagElement chevronRight(int size) {
    return stroked(size, 24, 2.2, polyline("9 18 15 12 9 6"));
  }

  /// The chain link of heading permalinks.
  public static TagElement link(int size) {
    return stroked(
        size,
        24,
        2,
        path("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"),
        path("M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"));
  }

  /// The sun of the light color scheme.
  public static TagElement sun() {
    return stroked(
        18,
        24,
        2,
        circle(12, 12, 4),
        path(
            "M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2"
                + "M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"));
  }

  /// The crescent moon of the dark color scheme.
  public static TagElement moon() {
    return stroked(18, 24, 2, path("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"));
  }

  /// The monitor of the system color scheme.
  public static TagElement monitor() {
    return stroked(
        18,
        24,
        2,
        path("M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"),
        path("M8 21h8M12 17v4"));
  }
}
