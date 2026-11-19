package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.CustomElement;

/// Backend-facing: the static behaviors bundled with the library's portable widgets, declared by
/// the widget on its host. Every backend emits the host as the custom element the behavior's script
/// defines, and document output also contributes the script once per page. A browser root loads no
/// scripts, so the element stays undefined and the widget keeps its readable fallback; a live
/// adapter that supplies the behavior itself clears it from the host. The set is closed: an
/// application's own scripts belong to document content, through
/// `DocumentContext.contributeScript`.
public enum BundledBehavior {
  /// Panel selection and keyboard focus for `components.ContentTabs`, whose host becomes the
  /// autonomous `tessera-tabs` element.
  TABS("tessera-tabs", "/dev/nthings/tessera/components/tessera-tabs.js", false),
  /// Focus return for the command items of `components.Dropdown`, whose host becomes the autonomous
  /// `tessera-menu` element: a dialog opened from the hidden menu gives focus back to the menu's
  /// button.
  MENU("tessera-menu", "/dev/nthings/tessera/components/tessera-menu.js", false),
  /// Result filtering and selection for `components.SearchPalette`, whose host `<dialog>` becomes
  /// the customized built-in `is="tessera-search"`.
  SEARCH("tessera-search", "/dev/nthings/tessera/components/search-palette.js", true);

  private final CustomElement element;
  private final String resource;
  private final boolean builtIn;

  BundledBehavior(String element, String resource, boolean builtIn) {
    this.element = new CustomElement(element);
    this.resource = resource;
    this.builtIn = builtIn;
  }

  /// Classpath resource path of the client module for this behavior.
  public String resource() {
    return resource;
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
