package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.foundation.Fnv1a64;
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

/// One page's identities, generated-id counters, transitions and expansion diagnostics.
/// [Expansion], [PageContext] and [ReferenceLinker] share it until document linking is complete.
final class ExpansionState {
  final Set<String> registeredDomIds = new LinkedHashSet<>();
  final Set<String> ambiguousDomIds = new HashSet<>();
  final Map<TransitionKey, Location> transitionLocations = new LinkedHashMap<>();
  final List<Diagnostic> diagnostics = new ArrayList<>();
  private final Map<ElementRef, String> references = new IdentityHashMap<>();
  private final Map<IdCounter, Integer> counters = new HashMap<>();
  private final Set<String> transitionNames = new HashSet<>();

  /// Numbered per DOM id, keyed bit and prefix. [IdScope#generated] is not part of the key, so an
  /// encounter-numbered scope and an explicit `scopeId` with the same DOM id share one counter.
  private record IdCounter(String domId, boolean keyed, String prefix) {}

  void report(Location location, String rule, String message, String suggestion) {
    diagnostics.add(new Diagnostic(rule, message, suggestion, location));
  }

  void registerDomId(String domId, Location location) {
    registerDomId(domId, location, DUPLICATE_ID_FIX);
  }

  private static final String DUPLICATE_ID_FIX =
      "Give each element a distinct ID within its id scope.";

  private static final String DUPLICATE_KEYED_ID_FIX =
      "Give each element a distinct ID within its id scope. Keyed content is scoped by its id"
          + " scope, the nearest enclosing element bound to a named ElementRef, and its key, so"
          + " keyed lists in one id scope that reuse a key share ids: bind a named ElementRef"
          + " (ElementRef.scoped or ElementRef.page) on each list's parent.";

  private void registerDomId(String domId, Location location, String fix) {
    if (!registeredDomIds.add(domId)) {
      ambiguousDomIds.add(domId);
      report(location, "duplicate-id", "Duplicate DOM id '" + domId + "' on the same page", fix);
    }
  }

  /// Binds every ref of one element in `scope` to a single DOM id and registers it. The id comes
  /// from the first named ref, or is generated from the tag; a named ref asking for another id
  /// reports `conflicting-element-id`. A ref already bound on this page reports
  /// `duplicate-reference-binding`, and the element keeps that earlier id.
  String bind(List<ElementRef> refs, IdScope scope, Location location) {
    String earlier = null;
    for (var ref : refs) {
      var existing = references.get(ref);
      if (existing != null) {
        ambiguousDomIds.add(existing);
        report(
            location,
            "duplicate-reference-binding",
            ref + " is bound to more than one element on the same page",
            "Bind this ElementRef once; use a separate reference for the other element.");
        if (earlier == null) {
          earlier = existing;
        }
      }
    }
    if (earlier != null) {
      for (var ref : refs) {
        references.putIfAbsent(ref, earlier);
      }
      return earlier;
    }
    String domId = null;
    ElementRef named = null;
    for (var ref : refs) {
      var name = ref.name();
      if (name == null) {
        continue;
      }
      var candidate = ref.pageLevel() ? name : scope.qualify(name);
      if (domId == null) {
        named = ref;
        domId = candidate;
      } else if (!candidate.equals(domId)) {
        report(
            location.withDomId(domId),
            "conflicting-element-id",
            "<"
                + location.tag()
                + "> is bound to "
                + named
                + " and "
                + ref
                + ", which ask for different ids ('"
                + domId
                + "' and '"
                + candidate
                + "')",
            "Bind one named ElementRef per element and pass that ref around; extra handles can"
                + " use element.ref() or ElementRef.anonymous().");
      }
    }
    if (domId == null) {
      domId = scope.qualify(nextId(location.tag(), scope));
    }
    for (var ref : refs) {
      references.put(ref, domId);
    }
    var scopedInKeyedContent = scope.keyed() && (named == null || !named.pageLevel());
    registerDomId(
        domId,
        location.withDomId(domId),
        scopedInKeyedContent ? DUPLICATE_KEYED_ID_FIX : DUPLICATE_ID_FIX);
    return domId;
  }

  /// DOM id bound to `ref`, or `null` when ambiguous or unbound (unbound is reported).
  @Nullable String boundId(ElementRef ref, String usage, Location location) {
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

  /// The local id `<prefix>-<n>`, numbered within `scope` so other scopes never shift it.
  /// [IdScope#generated] does not split the counter.
  String nextId(String prefix, IdScope scope) {
    var counter = new IdCounter(scope.domId(), scope.keyed(), prefix);
    return Fnv1a64.sanitizeLocalId(prefix + "-" + counters.merge(counter, 1, Integer::sum));
  }

  /// Records `key` at `location`, reporting a name another key already uses.
  void registerTransition(TransitionKey key, Location location) {
    if (!transitionNames.add(key.name())) {
      report(
          location,
          "duplicate-transition-key",
          "Duplicate TransitionKey name '"
              + key.name()
              + "' found on the same page. TransitionKey names must be unique per document.",
          "Give each transitioning element a distinct TransitionKey name.");
    }
    transitionLocations.putIfAbsent(key, location);
  }
}
