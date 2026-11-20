package dev.nthings.tessera.core;

/// A portable value backed by a single host [TagElement], such as a [TagElement] itself, a
/// `Button`, an `IconButton`, or an `Image`. Document and live wrappers accept `TagHost` directly
/// so callers do not unpack the underlying tag by hand.
public interface TagHost {

  /// Returns the host element representing this value.
  TagElement toTagElement();
}
