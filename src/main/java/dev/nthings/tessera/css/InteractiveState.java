package dev.nthings.tessera.css;

/// Element-relative interactive state that wraps a [StyleRefinement] in a nested CSS rule.
///
/// Declaration order is emission order, so later states win the cascade at equal specificity
/// (e.g. [#ACTIVE] overrides [#HOVER]).
public enum InteractiveState {
  HOVER,
  FOCUS_VISIBLE,
  ACTIVE,
  USER_INVALID,
  USER_VALID,
  /// Argument: sanitized `data-group` name of the hovered ancestor.
  GROUP_HOVER,
  /// Argument: relative selector inside `:has(...)`.
  HAS,
  MOTION_SAFE;

  /// Opening of the nested rule for this state; the caller appends the block body and `}`.
  String openRule(String argument) {
    return switch (this) {
      case HOVER -> "&:hover:not(:disabled){";
      case FOCUS_VISIBLE -> "&:focus-visible{";
      case ACTIVE -> "&:active:not(:disabled){";
      case USER_INVALID -> "&:user-invalid{";
      case USER_VALID -> "&:user-valid{";
      case GROUP_HOVER -> "[data-group=\"" + argument + "\"]:hover &{";
      case HAS -> "&:has(" + argument + "){";
      case MOTION_SAFE -> "@media (prefers-reduced-motion: no-preference){";
    };
  }
}
