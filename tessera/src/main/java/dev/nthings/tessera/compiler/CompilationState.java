package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.core.ViewScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// One page's identities, transitions, outgoing links and diagnostics.
/// Expansion and linking share it; [ViewExpansion] keeps only placement.
final class CompilationState {
  final Set<String> registeredDomIds = new LinkedHashSet<>();
  final Set<String> ambiguousDomIds = new HashSet<>();
  final Map<TransitionKey, Location> transitionLocations = new LinkedHashMap<>();
  final Map<String, Location> outgoingLinks = new LinkedHashMap<>();
  final Map<String, ResolvedTag> elementsById = new HashMap<>();
  final Set<String> labelledControlIds = new HashSet<>();
  final List<Diagnostic> diagnostics = new ArrayList<>();
  private final Map<ElementRef, String> references = new IdentityHashMap<>();
  private final Map<IdCounter, Integer> counters = new HashMap<>();
  private final Set<String> transitionNames = new HashSet<>();
  private final String pageTitle;

  /// Generated ids are numbered per scope and prefix.
  private record IdCounter(ViewScope scope, String prefix) {}

  CompilationState(String pageTitle) {
    this.pageTitle = pageTitle;
  }

  Location location(ViewLocation view) {
    return new Location(
        null, pageTitle, view.componentPath(), view.tagName(), view.domId(), view.source());
  }

  void report(ViewLocation location, String rule, String message, String suggestion) {
    diagnostics.add(new Diagnostic(rule, message, suggestion, location(location)));
  }

  void registerDomId(String domId, ViewLocation location) {
    if (!registeredDomIds.add(domId)) {
      ambiguousDomIds.add(domId);
      report(
          location,
          "duplicate-id",
          "Duplicate DOM id '" + domId + "' on the same page",
          "Give each element a distinct ID within its view scope.");
    }
  }

  /// Binds `ref` in `scope` and registers its DOM id. A second binding reports and keeps the
  /// first.
  String bind(ElementRef ref, ViewScope scope, ViewLocation location) {
    var existing = references.get(ref);
    if (existing != null) {
      ambiguousDomIds.add(existing);
      report(
          location,
          "duplicate-reference-binding",
          ref + " is bound to more than one element on the same page",
          "Bind this ElementRef once; use a separate reference for the other element.");
      return existing;
    }
    var local = ref.localId();
    var domId = scope.extend(local != null ? local : nextId(location.tagName(), scope)).domId();
    references.put(ref, domId);
    registerDomId(domId, location.withDomId(domId));
    return domId;
  }

  /// DOM id bound to `ref`, or `null` when ambiguous or unbound (unbound is reported).
  @Nullable String boundId(ElementRef ref, String usage, ViewLocation location) {
    var id = references.get(ref);
    if (ambiguousDomIds.contains(id)) {
      return null;
    }
    if (id == null) {
      report(
          location,
          "unbound-reference",
          usage + " references " + ref + ", which is not bound to any element on this page",
          "Bind this reference with .id(ref) on the intended target.");
    }
    return id;
  }

  /// `<prefix>-<n>`, numbered within `scope` so other scopes never shift it.
  ElementId nextId(String prefix, ViewScope scope) {
    return ElementId.of(
        prefix + "-" + counters.merge(new IdCounter(scope, prefix), 1, Integer::sum));
  }

  /// Records `key` at `location`, reporting a name another key already uses.
  void registerTransition(TransitionKey key, ViewLocation location) {
    if (!transitionNames.add(key.name())) {
      report(
          location,
          "duplicate-transition-key",
          "Duplicate TransitionKey name '"
              + key.name()
              + "' found on the same page. TransitionKey names must be unique per document.",
          "Give each transitioning element a distinct TransitionKey name.");
    }
    transitionLocations.putIfAbsent(key, location(location));
  }
}
