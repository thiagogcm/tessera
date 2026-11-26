package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.CustomElement;

/// Backend-facing: the static behaviors bundled with the library's portable widgets, declared by
/// the widget on its host during portable expansion. Expansion emits the host as the custom element
/// the behavior defines; the static compiler contributes the matching client script once per page.
/// A browser root loads no scripts, so raw portable placement keeps its readable fallback, while
/// live widget adapters request plain hosts without a bundled behavior.
public enum BundledBehavior {
  /// Panel selection and keyboard focus for `components.ContentTabs`, whose host becomes the
  /// autonomous `tessera-tabs` element.
  TABS("tessera-tabs", false),
  /// Focus return for the command items of `components.Dropdown`, whose host becomes the autonomous
  /// `tessera-menu` element: a dialog opened from the hidden menu gives focus back to the menu's
  /// button.
  MENU("tessera-menu", false),
  /// Result filtering and selection for `components.SearchPalette`, whose host `<dialog>` becomes
  /// the customized built-in `is="tessera-search"`.
  SEARCH("tessera-search", true),
  /// The saved light or dark choice of `components.ColorSchemeToggle`, whose host becomes the
  /// autonomous `tessera-color-scheme` element. A page with one also restores the saved choice in
  /// `<head>`, before first paint.
  COLOR_SCHEME("tessera-color-scheme", false);

  private final CustomElement element;
  private final boolean builtIn;

  BundledBehavior(String element, boolean builtIn) {
    this.element = new CustomElement(element);
    this.builtIn = builtIn;
  }

  /// The custom element the script defines.
  public CustomElement element() {
    return element;
  }

  /// Whether the script customizes the host's built-in element through its `is` attribute, rather
  /// than replacing the host's tag with [#element()].
  public boolean customizesBuiltIn() {
    return builtIn;
  }
}
