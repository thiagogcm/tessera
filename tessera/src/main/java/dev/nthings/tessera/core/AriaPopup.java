package dev.nthings.tessera.core;

/// The kind of popup an element opens (`aria-haspopup`), set with
/// [TagElement#ariaHasPopup(AriaPopup)].
public enum AriaPopup implements WireToken {
  MENU,
  LISTBOX,
  TREE,
  GRID,
  DIALOG
}
