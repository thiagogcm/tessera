package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.view.ViewExpansion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// What one page validates and publishes, once its expansion is linked: its DOM ids and
/// transitions, outgoing links and diagnostics.
final class CompilationState {
  final Set<String> registeredDomIds;
  final Set<String> ambiguousDomIds;
  final Map<TransitionKey, Location> transitionLocations;
  final Map<String, Location> outgoingLinks = new LinkedHashMap<>();
  final List<Diagnostic> diagnostics = new ArrayList<>();
  private final String pageTitle;

  /// Starts from a linked expansion: its identities, transitions and diagnostics.
  CompilationState(String pageTitle, ViewExpansion<?> expansion) {
    this.pageTitle = pageTitle;
    this.registeredDomIds = Collections.unmodifiableSet(expansion.ids());
    this.ambiguousDomIds = Collections.unmodifiableSet(expansion.ambiguousIds());
    var transitions = new LinkedHashMap<TransitionKey, Location>();
    expansion.transitions().forEach((key, location) -> transitions.put(key, location(location)));
    this.transitionLocations = Collections.unmodifiableMap(transitions);
    for (var diagnostic : expansion.diagnostics()) {
      diagnostics.add(diagnostic.atPage(pageTitle));
    }
  }

  Location location(Location view) {
    return view.atPage(pageTitle);
  }

  void report(Location location, String rule, String message, String suggestion) {
    diagnostics.add(new Diagnostic(rule, message, suggestion, location(location)));
  }
}
