package dev.nthings.tessera.wasm;

import java.util.function.Function;

/// Typed key identifying a native browser event channel and the [BrowserEvent] subtype its handlers
/// receive. Use constants from [Events] or [Events#custom(String)].
///
/// @param <E> event subtype delivered to handlers
public final class EventType<E extends BrowserEvent> {

  interface Source {
    BasicEvent basic();

    PointerEvent pointer();

    KeyboardEvent keyboard();

    ControlEvent control();
  }

  private final String name;
  private final Function<Source, E> adapter;

  EventType(String name, Function<Source, E> adapter) {
    if (name.isBlank()) {
      throw new IllegalArgumentException("An event type must not be blank");
    }
    this.name = name;
    this.adapter = adapter;
  }

  /// The native DOM event name, such as `click`, `keydown`, or `input`.
  public String name() {
    return name;
  }

  E adapt(Source source) {
    return adapter.apply(source);
  }

  @Override
  public String toString() {
    return name;
  }
}
