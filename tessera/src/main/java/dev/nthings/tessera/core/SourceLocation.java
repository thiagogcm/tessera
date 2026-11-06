package dev.nthings.tessera.core;

import java.io.Serializable;

/// Where authoring code built an element: its source file and line.
///
/// Captured only while [SourceTracking] is active. [#toString()] is the `File.java:42` form shown
/// in diagnostics and emitted as `data-src`.
public record SourceLocation(String file, int line) implements Serializable {

  @Override
  public String toString() {
    return file + ":" + line;
  }
}
