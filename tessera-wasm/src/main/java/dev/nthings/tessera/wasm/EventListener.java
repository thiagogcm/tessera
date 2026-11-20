package dev.nthings.tessera.wasm;

import dev.nthings.tessera.wasm.ControlProperties.Kind;
import dev.nthings.tessera.wasm.EventType.Source;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// Backend-facing: a Java event handler bound with [LiveElement#on], with its native options.
record EventListener(String type, Flags flags, Consumer<Source> handler, @Nullable Kind observed) {

  record Flags(boolean capture, boolean passive, boolean once) {
    static final Flags DEFAULT = new Flags(false, false, false);

    static Flags of(EventOption... options) {
      var capture = false;
      var passive = false;
      var once = false;
      for (var option : options) {
        switch (option) {
          case CAPTURE -> capture = true;
          case PASSIVE -> passive = true;
          case ONCE -> once = true;
        }
      }
      return new Flags(capture, passive, once);
    }
  }

  public EventListener {
    if (type.isBlank()) {
      throw new IllegalArgumentException("An event type must not be blank");
    }
  }

  static <E extends BrowserEvent> EventListener of(
      EventType<E> event, EventOption[] options, Consumer<E> handler, @Nullable Kind observed) {
    return new EventListener(
        event.name(), Flags.of(options), source -> handler.accept(event.adapt(source)), observed);
  }

  /// The listener's identity on its element: one listener per type and phase, as with
  /// `addEventListener`.
  public Key key() {
    return new Key(type, flags.capture());
  }

  /// Native listener identity: the exact event type and capture phase.
  public record Key(String type, boolean capture) {}
}
