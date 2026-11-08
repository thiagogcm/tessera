package dev.nthings.tessera.css;

/// Wraps a [StyleRefinement] in a nested rule. Declaration order is emission order, so later
/// states win at equal specificity (e.g. [#ACTIVE] overrides [#HOVER]).
public enum InteractiveState {
  HOVER,
  FOCUS_VISIBLE,
  ACTIVE,
  CHECKED,
  USER_INVALID,
  USER_VALID,
  /// Argument: sanitized `data-group` name of the hovered ancestor.
  GROUP_HOVER,
  /// Argument: relative selector inside `:has(...)`.
  HAS,
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
