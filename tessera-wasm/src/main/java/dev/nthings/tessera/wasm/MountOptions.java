package dev.nthings.tessera.wasm;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.css.Theme;

/// Mounting configuration for [BrowserRoot#mount(String, LiveComponent, MountOptions)]. Start from
/// [#DEFAULT] and refine with [#withTheme(Theme)] or [#withScheduling(BrowserRoot.Scheduling)].
///
/// Options accept only a theme that passes [Theme#contrastDiagnostics()], the check the static
/// compiler applies: creating them with any other theme throws IllegalArgumentException listing the
/// theme's `theme-contrast` diagnostics. Created inside a [BrowserEntry] mount, the failure is
/// reported as a failed mount and the host keeps its fallback content.
public record MountOptions(Theme theme, BrowserRoot.Scheduling scheduling) {

  /// Default theme and default scheduling limits.
  public static final MountOptions DEFAULT =
      new MountOptions(Theme.defaults(), BrowserRoot.Scheduling.DEFAULT);

  /// Throws IllegalArgumentException if `theme` fails WCAG AA text contrast.
  public MountOptions {
    var failures = theme.contrastDiagnostics();
    if (!failures.isEmpty()) {
      var report = new StringBuilder("Theme fails WCAG AA text contrast:");
      failures.forEach(failure -> report.append('\n').append(failure.format()));
      throw new IllegalArgumentException(report.toString());
    }
  }

  /// Returns options with `theme` and these scheduling limits. Throws IllegalArgumentException if
  /// `theme` fails WCAG AA text contrast.
  @CheckReturnValue
  public MountOptions withTheme(Theme theme) {
    return new MountOptions(theme, scheduling);
  }

  /// Returns options with `scheduling` and this theme.
  @CheckReturnValue
  public MountOptions withScheduling(BrowserRoot.Scheduling scheduling) {
    return new MountOptions(theme, scheduling);
  }
}
