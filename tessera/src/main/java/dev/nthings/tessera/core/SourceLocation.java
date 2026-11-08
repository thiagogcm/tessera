package dev.nthings.tessera.core;

import java.io.Serializable;

/// Authoring source file and line of an element, captured only while source tracking is active.
/// `toString()` gives the `File.java:42` form used in `data-src`.
public record SourceLocation(String file, int line) implements Serializable {

  @Override
  public String toString() {
    return file + ":" + line;
  }
}
