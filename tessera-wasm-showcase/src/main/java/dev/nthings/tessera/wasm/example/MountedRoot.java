package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.wasm.BrowserRoot;
import org.jspecify.annotations.Nullable;

/// A demo's browser root that other roots reach, mounted (and remounted) later.
final class MountedRoot {
  private final String name;
  private @Nullable BrowserRoot root;

  /// `name` describes the view in the failure of [#get()].
  MountedRoot(String name) {
    this.name = name;
  }

  void set(BrowserRoot root) {
    this.root = root;
  }

  /// The latest mounted root. Throws IllegalStateException if none was mounted yet.
  BrowserRoot get() {
    var mounted = root;
    if (mounted == null) {
      throw new IllegalStateException("The " + name + " is not mounted");
    }
    return mounted;
  }
}
