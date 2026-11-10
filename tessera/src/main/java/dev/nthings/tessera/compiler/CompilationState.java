package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.TransitionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The linked page's identities, transitions and diagnostics, annotated with its title.
/// Expansion hands over its results before validation starts.
final class CompilationState {
  final Set<String> registeredDomIds;
  final Set<String> ambiguousDomIds;
  final Map<TransitionKey, Location> transitionLocations;
  final List<Diagnostic> diagnostics = new ArrayList<>();
  private final String pageTitle;

  CompilationState(String pageTitle, ExpansionState expansion) {
    this.pageTitle = pageTitle;
    registeredDomIds = expansion.ids();
    ambiguousDomIds = expansion.ambiguousIds();
    var transitions = new LinkedHashMap<TransitionKey, Location>();
    expansion.transitions().forEach((key, location) -> transitions.put(key, location(location)));
    transitionLocations = Collections.unmodifiableMap(transitions);
    for (var diagnostic : expansion.diagnostics()) {
      diagnostics.add(diagnostic.atPage(pageTitle));
    }
  }

  Location location(Location location) {
    return location.atPage(pageTitle);
  }

  void report(Location location, String rule, String message, String suggestion) {
    diagnostics.add(new Diagnostic(rule, message, suggestion, location(location)));
  }
}
