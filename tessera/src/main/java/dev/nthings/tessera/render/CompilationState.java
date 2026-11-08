package dev.nthings.tessera.render;

import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// What one page publishes and validates: its DOM ids, transitions, outgoing links and
/// diagnostics. Component expansion keeps its own bindings and placement in [ViewExpansion].
final class CompilationState {
  final Set<String> registeredDomIds = new LinkedHashSet<>();
  final Set<String> ambiguousDomIds = new HashSet<>();
  final Map<TransitionKey, CompilationLocation> transitionLocations = new LinkedHashMap<>();
  final Map<String, CompilationLocation> outgoingLinks = new LinkedHashMap<>();
  final Map<String, ResolvedTag> elementsById = new HashMap<>();
  final Set<String> labelledControlIds = new HashSet<>();
  final List<CompilationDiagnostic> diagnostics = new ArrayList<>();
  private final String pageTitle;

  CompilationState(String pageTitle) {
    this.pageTitle = pageTitle;
  }

  CompilationLocation location(ViewLocation view) {
    return new CompilationLocation(pageTitle, null, view);
  }

  void report(CompilationLocation location, String rule, String message, String suggestion) {
    diagnostics.add(new CompilationDiagnostic(rule, location, message, suggestion));
  }

  void registerDomId(String domId, CompilationLocation location) {
    if (!registeredDomIds.add(domId)) {
      ambiguousDomIds.add(domId);
      report(
          location,
          "duplicate-id",
          "Duplicate DOM id '" + domId + "' on the same page",
          "Give each element a distinct ID within its view scope.");
    }
  }
}
