package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.document.PageScript;

/// Backend-facing: the static behaviors bundled with the library's portable widgets, declared by
/// the widget. Document output contributes the behavior's page script once per page; a browser root
/// renders the structure without it, so the markup stays readable before and without the script.
/// The set is closed: an application's own scripts belong to document content, through
/// `DocumentContext.contributeScript`.
public enum BundledBehavior {
  /// Panel selection and keyboard focus for `components.ContentTabs`.
  TABS("tessera-tabs", "/dev/nthings/tessera/components/tessera-tabs.js"),
  /// Result filtering and selection for `components.SearchPalette`.
  SEARCH("tessera-search", "/dev/nthings/tessera/components/search-palette.js");

  private final PageScript script;

  BundledBehavior(String id, String resource) {
    this.script = PageScript.fromResource(id, BundledBehavior.class, resource);
  }

  /// The page script a document contributes for this behavior.
  public PageScript script() {
    return script;
  }
}
