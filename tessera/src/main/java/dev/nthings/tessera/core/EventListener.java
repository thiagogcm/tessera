package dev.nthings.tessera.core;

import java.util.function.Consumer;

/// Backend-facing: a Java event handler bound with [TagElement#onEvent], with its native options.
/// Throws IllegalArgumentException if `type` is blank.
public record EventListener(String type, EventOptions options, Consumer<BrowserEvent> handler) {

  public EventListener {
    if (type.isBlank()) {
      throw new IllegalArgumentException("An event type must not be blank");
    }
  }

  /// The listener's identity on its element: one listener per type and phase, as with
  /// `addEventListener`.
  public Key key() {
    return new Key(type, options.capture());
  }

  /// Native listener identity: the exact event type and capture phase.
  public record Key(String type, boolean capture) {}
}
