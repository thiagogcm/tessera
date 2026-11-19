package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.PortableNode;
import dev.nthings.tessera.internal.authoring.SourceProvenance;

/// Portable reusable component with its own id namespace and scoped CSS, placed in static pages and
/// browser roots alike. See [ScopeDefinition] for its id, name and styles. Use [Component] for
/// markup that needs no scope of its own, and `DocumentScope` for a scope whose host holds document
/// content.
public interface Scope extends ScopeDefinition, IntoElement {

  /// Builds the scope's host element, which carries `data-scope` and `data-scope-id`. Place nested
  /// scopes and other content inside it: a list scope returns `ul` or `ol`, and a table-row scope
  /// returns `tr`. Expansion never inserts a wrapper or replaces a nested scope's host.
  TagElement expand(ViewContext context);

  @Override
  default Element intoElement() {
    return new PortablePlacement(new PortableNode.ScopePlacement(this, SourceProvenance.capture()));
  }
}
