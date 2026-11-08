package dev.nthings.tessera.css;

/// The condition a nested style applies under, set through the `InteractiveElement` methods
/// (`hover`, `focusVisible`, `motionSafe`, ...). Rules are emitted in constant order, not in call
/// order, so when two rules match at equal specificity the later constant wins: ACTIVE overrides
/// HOVER.
public enum InteractiveState {
  /// `:hover`, excluding disabled elements.
  HOVER,
  FOCUS_VISIBLE,
  /// `:active`, excluding disabled elements.
  ACTIVE,
  CHECKED,
  USER_INVALID,
  USER_VALID,
  /// Argument: sanitized `data-group` name of the hovered ancestor.
  GROUP_HOVER,
  /// Argument: relative selector inside `:has(...)`.
  HAS,
  /// `@media (prefers-reduced-motion: no-preference)` around the nested rule, not a pseudo-class.
  MOTION_SAFE;

  /// Opens the nested rule; the caller appends the body and `}`.
  String openRule(String argument) {
    return switch (this) {
      case HOVER -> "&:hover:not(:disabled){";
      case FOCUS_VISIBLE -> "&:focus-visible{";
      case ACTIVE -> "&:active:not(:disabled){";
      case CHECKED -> "&:checked{";
      case USER_INVALID -> "&:user-invalid{";
      case USER_VALID -> "&:user-valid{";
      case GROUP_HOVER -> "[data-group=\"" + argument + "\"]:hover &{";
      case HAS -> "&:has(" + argument + "){";
      case MOTION_SAFE -> "@media (prefers-reduced-motion: no-preference){";
    };
  }
}
