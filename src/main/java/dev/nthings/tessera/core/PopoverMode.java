package dev.nthings.tessera.core;

/** Native HTML Popover API modes ({@code popover} attribute). */
public enum PopoverMode {
  AUTO("auto"),
  MANUAL("manual"),
  HINT("hint");

  private final String wireValue;

  PopoverMode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
