package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.ScopeDefinition;
import dev.nthings.tessera.internal.authoring.SourceProvenance;
import dev.nthings.tessera.view.LiveNode;

/// Styled scope whose host carries live behavior, such as event handlers on the scope's own
/// element. See [ScopeDefinition] for its id, name and styles. Its host comes from the live builder
/// family, so it is always one authored element.
public interface LiveScope extends ScopeDefinition, LiveContent {

  /// Builds the scope's host, which carries `data-scope` and `data-scope-id`.
  LiveElement expand(LiveContext context);

  @Override
  default LiveTree intoLiveTree() {
    return new LivePlacement(
        new LiveNode.ScopePlacement<>(
            this, SourceProvenance.capture(), context -> expand((LiveContext) context).capture()));
  }
}
