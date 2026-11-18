package dev.nthings.tessera.core;

import java.util.Collection;
import java.util.stream.Stream;

/// Child-accumulation contract: single children, text, varargs, collections and streams. Adding a
/// child to a void element (`input`, `img`, `br`, `hr`, `meta`, `link`, `source`, `track`) throws
/// IllegalStateException.
///
/// @param <SELF> concrete element type
public interface ParentElement<SELF extends ParentElement<SELF>> {

  SELF self();

  /// Adds the next child, lowered at placement.
  SELF child(IntoElement child);

  /// Adds a text child. Its content is escaped on output.
  default SELF text(String text) {
    return child(Elements.text(text));
  }

  /// Adds each child in order.
  default SELF children(IntoElement... children) {
    for (var child : children) {
      child(child);
    }
    return self();
  }

  /// Adds each child in iteration order.
  default SELF children(Collection<? extends IntoElement> children) {
    for (var child : children) {
      child(child);
    }
    return self();
  }

  /// Adds each child from `stream` in order. The stream is consumed.
  default SELF children(Stream<? extends IntoElement> stream) {
    stream.forEach(this::child);
    return self();
  }
}
