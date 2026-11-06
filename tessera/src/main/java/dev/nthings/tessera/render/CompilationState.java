package dev.nthings.tessera.render;

import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.core.ViewScope;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.jspecify.annotations.Nullable;

final class CompilationState {
  final PriorityQueue<ResolvedDeferred> deferredQueue = new PriorityQueue<>();
  final Set<String> registeredDomIds = new LinkedHashSet<>();
  final Set<String> outgoingHrefs = new LinkedHashSet<>();
  final Set<TransitionKey> transitionKeys = new LinkedHashSet<>();
  final Set<String> viewTransitionNames = new LinkedHashSet<>();
  final Map<ElementRef, String> boundDomIds = new IdentityHashMap<>();
  final Map<String, Integer> autoIdCounters = new HashMap<>();
  final List<@Nullable ResolvedNode> lateSlots = new ArrayList<>();
  final ArrayDeque<Runnable> pendingLate = new ArrayDeque<>();
  private long deferredSequence = 0L;

  void enqueueDeferred(int priority, ResolvedNode node) {
    deferredQueue.add(new ResolvedDeferred(priority, deferredSequence++, node));
  }

  /// Deterministic per-page `<prefix>-<n>` local id.
  ElementId nextAutoId(String prefix) {
    return ElementId.of(prefix + "-" + autoIdCounters.merge(prefix, 1, Integer::sum));
  }

  void registerDomId(String domId) {
    if (!registeredDomIds.add(domId)) {
      throw new AccessibilityViolationException(
          "Duplicate DOM id '" + domId + "' on the same page");
    }
  }

  String bind(ElementRef ref, ViewScope scope, HtmlTag tag) {
    var local = ref.localId();
    var domId = scope.extend(local != null ? local : nextAutoId(tag.tagName())).domId();
    if (boundDomIds.putIfAbsent(ref, domId) != null) {
      throw new AccessibilityViolationException(
          ref + " is bound to more than one element on the same page");
    }
    registerDomId(domId);
    return domId;
  }

  String boundDomId(ElementRef ref, String usage) {
    var domId = boundDomIds.get(ref);
    if (domId == null) {
      throw new AccessibilityViolationException(
          usage + " references " + ref + ", which is not bound to any element on this page");
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
