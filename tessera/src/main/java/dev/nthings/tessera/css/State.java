package dev.nthings.tessera.css;

/// An element state for [Styled#on(Condition, java.util.function.Consumer)], emitted as a nested
/// `&:pseudo-class` rule. Every state adds the specificity of one pseudo-class: the not-disabled
/// guard of `HOVER` and `ACTIVE` is wrapped in `:where()`, which adds none. Rules are emitted in
/// constant order, not call order, so when two states match at once the later constant wins:
/// `ACTIVE` overrides `HOVER`, and `DISABLED` overrides every other state. A state rule nested in
/// a [ScopeStyle] rule is as specific as the raw selector with the pseudo-class appended, so
/// `select(":scope a", s -> s.on(State.HOVER, ...))` matches `:scope a:hover`.
public enum State implements Condition {
  /// `:hover`, while the element is not disabled.
  HOVER("&:hover:not(:where(:disabled))"),
  /// `:focus-visible`: keyboard focus, or focus the browser decides to show.
  FOCUS_VISIBLE("&:focus-visible"),
  /// `:active` (pressed), while the element is not disabled.
  ACTIVE("&:active:not(:where(:disabled))"),
  /// `:checked`, for checkboxes, radios and options.
  CHECKED("&:checked"),
  /// `:open`, for an open `<details>`, `<dialog>` or `<select>` picker.
  OPEN("&:open"),
  /// `:user-invalid`: the user changed the control to an invalid value.
  USER_INVALID("&:user-invalid"),
  /// `:user-valid`: the user changed the control to a valid value.
  USER_VALID("&:user-valid"),
  /// `:disabled`.
  DISABLED("&:disabled");

  private final String prelude;

  State(String prelude) {
    this.prelude = prelude;
  }

  /// The prelude that opens the nested rule, such as `&:hover:not(:where(:disabled))`.
  String prelude() {
    return prelude;
  }
}
