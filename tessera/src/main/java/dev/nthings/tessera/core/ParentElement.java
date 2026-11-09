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

  /// Appends an already lowered child. Every child method below goes through it.
  void appendChild(Element child);

  /// Adds `child`, lowered now through [IntoElement#intoElement()], as the next child.
  default SELF child(IntoElement child) {
    appendChild(child.intoElement());
    return self();
  }

  /// Adds a text child. Its content is escaped on output.
  default SELF text(String text) {
    appendChild(new TextElement(text));
    return self();
  }

  /// Adds each child in order.
  default SELF children(IntoElement... children) {
    for (var child : children) {
      appendChild(child.intoElement());
    }
    return self();
  }

  /// Adds each child in iteration order.
  default SELF children(Collection<? extends IntoElement> children) {
    for (var child : children) {
      appendChild(child.intoElement());
    }
    return self();
  }

  /// Adds each child from `stream` in order. The stream is consumed.
  default SELF children(Stream<? extends IntoElement> stream) {
    stream.forEach(c -> appendChild(c.intoElement()));
    return self();
  }
}
