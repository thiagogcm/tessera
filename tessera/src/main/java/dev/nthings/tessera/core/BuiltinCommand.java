package dev.nthings.tessera.core;

import java.util.Locale;

/// Native HTML Invoker Commands (`command` attribute) supported by the target browsers.
public enum BuiltinCommand {
  SHOW_MODAL,
  CLOSE,
  REQUEST_CLOSE,
  TOGGLE_POPOVER,
  SHOW_POPOVER,
  HIDE_POPOVER;

  private final String wireValue = name().toLowerCase(Locale.ROOT).replace('_', '-');

  public String wireValue() {
    return wireValue;
  }
}
