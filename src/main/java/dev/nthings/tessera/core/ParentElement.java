package dev.nthings.tessera.core;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Composable child-accumulation contract supporting varargs, collections, and JDK 25 {@link
 * Gatherers} stream pipelines.
 *
 * @param <SELF> concrete element type
 */
public interface ParentElement<SELF extends ParentElement<SELF>> {

  SELF self();

  void appendChild(Element child);

  default SELF child(IntoElement child) {
    appendChild(child.intoElement());
    return self();
  }

  default SELF text(String text) {
    appendChild(new TextElement(text));
    return self();
  }

  default SELF children(IntoElement... children) {
    for (var child : children) {
      appendChild(child.intoElement());
    }
    return self();
  }

  default SELF children(Collection<? extends IntoElement> children) {
    for (var child : children) {
      appendChild(child.intoElement());
    }
    return self();
  }

  default SELF children(Stream<? extends IntoElement> stream) {
    stream.forEach(c -> appendChild(c.intoElement()));
    return self();
  }

  default <T> SELF childrenWindowed(
      List<T> items, int windowSize, Function<List<T>, ? extends IntoElement> rowRenderer) {
    items.stream()
        .gather(Gatherers.windowFixed(windowSize))
        .map(rowRenderer)
        .forEach(row -> appendChild(row.intoElement()));
    return self();
  }
}
