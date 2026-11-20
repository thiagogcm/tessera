package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.wasm.LiveElement;

/// Live adapters for the portable components the fixtures use.
final class Ui {

  private Ui() {}

  /// A [Button] labelled `label` that runs `onClick` when clicked.
  static LiveElement button(String label, Runnable onClick) {
    return button(Button.of(label), onClick);
  }

  /// `button`'s element, running `onClick` when clicked.
  static LiveElement button(Button button, Runnable onClick) {
    return LiveElement.of(button).onClick(onClick);
  }
}
