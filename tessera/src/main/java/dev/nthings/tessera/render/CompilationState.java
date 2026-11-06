package dev.nthings.tessera.render;

import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.core.ViewScope;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.jspecify.annotations.Nullable;

final class CompilationState {
  final PriorityQueue<ResolvedDeferred> deferredQueue = new PriorityQueue<>();
  final Set<String> registeredDomIds = new LinkedHashSet<>();
  final Map<String, CompilationLocation> outgoingLinks = new LinkedHashMap<>();
  final Map<TransitionKey, CompilationLocation> transitionLocations = new LinkedHashMap<>();
  final Set<String> ambiguousDomIds = new HashSet<>();
  final Map<String, ResolvedTag> elementsById = new HashMap<>();
  final List<CompilationDiagnostic> diagnostics = new ArrayList<>();
  final Set<String> viewTransitionNames = new LinkedHashSet<>();
  final Map<ElementRef, String> boundDomIds = new IdentityHashMap<>();
  final Map<String, Integer> autoIdCounters = new HashMap<>();
  final List<@Nullable ResolvedNode> lateSlots = new ArrayList<>();
  final ArrayDeque<Runnable> pendingLate = new ArrayDeque<>();
  private final String pageTitle;
  private long deferredSequence = 0L;

  CompilationState(String pageTitle) {
    this.pageTitle = pageTitle;
  }

  CompilationLocation location(
      List<String> components,
      HtmlTag tag,
      Map<String, String> attrs,
      @Nullable SourceLocation source) {
    var id = attrs.get("id");
    return new CompilationLocation(pageTitle, null, components, tag.tagName(), id, source);
  }

  void report(CompilationLocation location, String rule, String message, String suggestion) {
    diagnostics.add(new CompilationDiagnostic(rule, location, message, suggestion));
  }

  void enqueueDeferred(int priority, ResolvedNode node) {
    deferredQueue.add(new ResolvedDeferred(priority, deferredSequence++, node));
  }

  /// Deterministic per-page `<prefix>-<n>` local id.
  ElementId nextAutoId(String prefix) {
    return ElementId.of(prefix + "-" + autoIdCounters.merge(prefix, 1, Integer::sum));
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

  String bind(ElementRef ref, ViewScope scope, HtmlTag tag, CompilationLocation location) {
    var existing = boundDomIds.get(ref);
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
    var domId = scope.extend(local != null ? local : nextAutoId(tag.tagName())).domId();
    boundDomIds.put(ref, domId);
    registerDomId(
        domId,
        new CompilationLocation(
            pageTitle, null, location.componentPath(), tag.tagName(), domId, location.source()));
    return domId;
  }

  @Nullable String boundDomId(ElementRef ref, String usage, CompilationLocation location) {
    var domId = boundDomIds.get(ref);
    if (ambiguousDomIds.contains(domId)) {
      return null;
    }
    if (domId == null) {
      report(
          location,
          "unbound-reference",
          usage + " references " + ref + ", which is not bound to any element on this page",
          "Bind this reference with .id(ref) on the intended target.");
    }
    return domId;
  }

  record ResolvedDeferred(int priority, long sequence, ResolvedNode node)
      implements Comparable<ResolvedDeferred> {
    @Override
    public int compareTo(ResolvedDeferred other) {
      var cmp = Integer.compare(this.priority, other.priority);
      return cmp != 0 ? cmp : Long.compare(this.sequence, other.sequence);
    }
  }
}
