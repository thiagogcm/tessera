package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.core.TransitionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The linked page's validation indexes, outgoing links and diagnostics. Expansion hands over
/// its registered identities and transitions before validation starts.
final class CompilationState {
  final Set<String> registeredDomIds;
  final Set<String> ambiguousDomIds;
  final Map<TransitionKey, Location> transitionLocations;
  final Map<String, Location> outgoingLinks = new LinkedHashMap<>();
  final Map<String, ResolvedTag> elementsById = new HashMap<>();
  final Set<String> labelledControlIds = new HashSet<>();
  final List<Diagnostic> diagnostics;

  CompilationState(ExpansionState expansion) {
    registeredDomIds = Collections.unmodifiableSet(expansion.registeredDomIds);
    ambiguousDomIds = Collections.unmodifiableSet(expansion.ambiguousDomIds);
    transitionLocations = Collections.unmodifiableMap(expansion.transitionLocations);
    diagnostics = new ArrayList<>(expansion.diagnostics);
  }

  void report(Location location, String rule, String message, String suggestion) {
    diagnostics.add(new Diagnostic(rule, message, suggestion, location));
  }
}
