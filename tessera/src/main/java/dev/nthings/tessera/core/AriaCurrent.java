package dev.nthings.tessera.core;

/// Value of `aria-current`, set with [TagElement#ariaCurrent(AriaCurrent)]: marks the item that is
/// current within a set, such as `PAGE` for the active link of a navigation.
public enum AriaCurrent {
  PAGE,
  STEP,
  LOCATION,
  DATE,
  TIME,
  TRUE
}
