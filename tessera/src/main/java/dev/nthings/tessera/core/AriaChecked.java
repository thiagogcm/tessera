package dev.nthings.tessera.core;

/// Value of a tri-state `aria-checked`, set with [TagElement#ariaChecked(AriaChecked)].
public enum AriaChecked implements WireToken {
  TRUE,
  FALSE,
  MIXED
}
