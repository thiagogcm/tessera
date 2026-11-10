package dev.nthings.tessera.view;

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

/// The identities, transitions and diagnostics of one expansion: a page or one live render.
/// Expansion and linking share it.
final class ViewState {
  private final Map<ElementRef, String> references = new IdentityHashMap<>();
  private final Map<IdCounter, Integer> counters = new HashMap<>();
  private final Set<String> ids = new LinkedHashSet<>();
  private final Set<String> ambiguousIds = new HashSet<>();
  private final Map<TransitionKey, Location> transitions = new LinkedHashMap<>();
  private final Set<String> transitionNames = new HashSet<>();
  private final List<Diagnostic> diagnostics = new ArrayList<>();

  /// Generated ids are numbered per dom id, keyed flag and prefix. `generated` is not part of
  /// the key, so an explicit scope id and a generated one in the same namespace share a counter.
  private record IdCounter(String domId, boolean keyed, String prefix) {}

  private static final String DUPLICATE_ID_FIX =
      "Give each element a distinct ID within its id scope.";

  private static final String DUPLICATE_KEYED_ID_FIX =
      "Give each element a distinct ID within its id scope. Keyed content is scoped by its id"
          + " scope, the nearest enclosing element bound to a named ElementRef, and its key, so"
          + " keyed lists in one id scope that reuse a key share ids: bind a named ElementRef"
          + " (ElementRef.scoped or ElementRef.page) on each list's parent.";

  void report(Location location, String rule, String message, String fix) {
    diagnostics.add(new Diagnostic(rule, message, fix, location));
  }

  void registerId(String id, Location location) {
    registerId(id, location, DUPLICATE_ID_FIX);
  }

  private void registerId(String id, Location location, String fix) {
    if (!ids.add(id)) {
      ambiguousIds.add(id);
      report(location, "duplicate-id", "Duplicate DOM id '" + id + "' on the same page", fix);
    }
  }

  /// Binds every ref of one element in `scope` to a single DOM id and registers it. The id comes
  /// from the first named ref, or is generated from the tag; a named ref asking for another id
  /// reports `conflicting-element-id`. A ref already bound in this expansion reports
  /// `duplicate-reference-binding`, and the element keeps that earlier id.
  String bind(List<ElementRef> refs, ViewScope scope, Location location) {
    String earlier = null;
    for (var ref : refs) {
      var existing = references.get(ref);
      if (existing != null) {
        ambiguousIds.add(existing);
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
    String id = null;
    ElementRef named = null;
    for (var ref : refs) {
      var name = ref.name();
      if (name == null) {
        continue;
      }
      var candidate = ref.pageLevel() ? name : scope.qualify(name);
      if (id == null) {
        named = ref;
        id = candidate;
      } else if (!candidate.equals(id)) {
        report(
            location.withDomId(id),
            "conflicting-element-id",
            "<"
                + location.tag()
                + "> is bound to "
                + named
                + " and "
                + ref
                + ", which ask for different ids ('"
                + id
                + "' and '"
                + candidate
                + "')",
            "Bind one named ElementRef per element and pass that ref around; extra handles can"
                + " use element.ref() or ElementRef.anonymous().");
      }
    }
    if (id == null) {
      id = scope.qualify(nextId(location.tag(), scope));
    }
    for (var ref : refs) {
      references.put(ref, id);
    }
    var scopedInKeyedContent = scope.keyed() && (named == null || !named.pageLevel());
    registerId(
        id,
        location.withDomId(id),
        scopedInKeyedContent ? DUPLICATE_KEYED_ID_FIX : DUPLICATE_ID_FIX);
    return id;
  }

  /// DOM id bound to `ref`, or `null` when ambiguous or unbound (unbound is reported).
  @Nullable String boundId(ElementRef ref, String usage, Location location) {
    var id = references.get(ref);
    if (ambiguousIds.contains(id)) {
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
  String nextId(String prefix, ViewScope scope) {
    return Fnv1a64.sanitizeLocalId(
        prefix
            + "-"
            + counters.merge(new IdCounter(scope.domId(), scope.keyed(), prefix), 1, Integer::sum));
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
    transitions.putIfAbsent(key, location);
  }

  List<Diagnostic> diagnostics() {
    return List.copyOf(diagnostics);
  }

  /// Every registered DOM id, in registration order; live, since expansion state is discarded
  /// after its turn.
  Set<String> ids() {
    return ids;
  }

  Set<String> ambiguousIds() {
    return ambiguousIds;
  }

  Map<TransitionKey, Location> transitions() {
    return transitions;
  }

  /// The bound refs and their DOM ids; live, since expansion state is discarded after its turn.
  Map<ElementRef, String> references() {
    return references;
  }
}
