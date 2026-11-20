package dev.nthings.tessera.core;

/// Standard HTML `<input>` types for [Elements#input(InputType)] and `Forms.input(String,
/// InputType)`.
public enum InputType {
  BUTTON,
  CHECKBOX,
  COLOR,
  DATE,
  DATETIME_LOCAL,
  EMAIL,
  FILE,
  HIDDEN,
  IMAGE,
  MONTH,
  NUMBER,
  PASSWORD,
  RADIO,
  RANGE,
  RESET,
  SEARCH,
  SUBMIT,
  TEL,
  TEXT,
  TIME,
  URL,
  WEEK;

  /// The HTML `type` attribute value, such as `datetime-local` or `checkbox`.
  public String wireValue() {
    return WireToken.of(this);
  }
}
