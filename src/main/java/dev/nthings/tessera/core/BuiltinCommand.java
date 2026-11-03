package dev.nthings.tessera.core;

/** Native HTML Invoker Commands supported by Chrome 156+ ({@code command} attribute). */
public enum BuiltinCommand {
  SHOW_MODAL("show-modal"),
  CLOSE("close"),
  REQUEST_CLOSE("request-close"),
  TOGGLE_POPOVER("toggle-popover"),
  SHOW_POPOVER("show-popover"),
  HIDE_POPOVER("hide-popover");

  private final String wireValue;

  BuiltinCommand(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
