package dev.nthings.tessera.core;

import java.io.Serializable;

/// Where authoring code built an element: its source file and line.
///
/// Captured only while JVM source tracking is active.
/// [#toString()] supplies the `File.java:42` form for diagnostics and `data-src`.
public record SourceLocation(String file, int line) implements Serializable {

  @Override
  public String toString() {
    return file + ":" + line;
  }
}
