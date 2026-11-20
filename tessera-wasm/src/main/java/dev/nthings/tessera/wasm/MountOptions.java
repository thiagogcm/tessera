package dev.nthings.tessera.wasm;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.css.Theme;

/// Mounting configuration for [BrowserRoot#mount(String, LiveComponent, MountOptions)]. Start from
/// [#DEFAULT] and refine with [#withTheme(Theme)] or [#withScheduling(BrowserRoot.Scheduling)].
public record MountOptions(Theme theme, BrowserRoot.Scheduling scheduling) {

  /// Default theme and default scheduling limits.
  public static final MountOptions DEFAULT =
      new MountOptions(Theme.defaults(), BrowserRoot.Scheduling.DEFAULT);

  /// Returns options with `theme` and these scheduling limits.
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
