package dev.nthings.tessera.wasm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// How one parent's children change between two renders, decided without touching the DOM.
///
/// Each next child retains the earliest unmatched previous sibling with an equal identity, so the
/// k-th occurrence of an identity among the next children retains its k-th occurrence among the
/// previous ones. Retained children on one longest run whose previous order is already increasing
/// stay in place; only the others move. A previous child no next child retains is removed.
///
/// Every next child is placed before its [anchor][#anchor], the previous child the nearest later
/// retained sibling retains, or the end of the parent's range. Applying the plan moves the children
/// that [move][#moves] walking backwards, so each anchor is already in its final place when a child
/// moves before it, then creates the new children before their anchors in any order.
final class SiblingPlan {
  /// A next child that retains nothing and is created.
  static final int NEW = -1;

  /// The anchor of a next child with no retained sibling after it: the end of the range.
  static final int END = -2;

  /// For each next child: the index of the previous child it retains, or [#NEW].
  private final int[] matches;

  /// For each next child: the index of the previous child it is placed before, or [#END].
  private final int[] anchors;

  /// For each previous child: whether a next child retains it.
  private final boolean[] retained;

  /// For each previous child: whether it stays in place, or `null` when every retained child does.
  private final boolean @Nullable [] stable;

  private SiblingPlan(
      int[] matches, int[] anchors, boolean[] retained, boolean @Nullable [] stable) {
    this.matches = matches;
    this.anchors = anchors;
    this.retained = retained;
    this.stable = stable;
  }

  /// The previous children from some index on that no next child retains yet, by identity. Each
  /// identity's children form a chain through `nextSame`, earliest first, so polling allocates
  /// nothing.
  private static final class Unmatched {
    private final Map<Object, Chain> chains = new HashMap<>();
    private final int[] nextSame;

    private static final class Chain {
      private int head = NEW;
    }

    <P> Unmatched(List<P> previous, Function<? super P, ?> identity, int from) {
      nextSame = new int[previous.size()];
      // Prepending from the end leaves each chain in previous order.
      for (var slot = previous.size() - 1; slot >= from; slot--) {
        var chain = chains.computeIfAbsent(identity.apply(previous.get(slot)), _ -> new Chain());
        nextSame[slot] = chain.head;
        chain.head = slot;
      }
    }

    /// Removes and returns the earliest unmatched previous child with `identity`, or
    /// [SiblingPlan#NEW].
    int poll(Object identity) {
      var chain = chains.get(identity);
      if (chain == null || chain.head == NEW) {
        return NEW;
      }
      var slot = chain.head;
      chain.head = nextSame[slot];
      return slot;
    }
  }

  /// Plans the change from `previous` to `next`, comparing the identity each function gives. Equal
  /// identities must be interchangeable: whichever previous child a next child retains is patched
  /// to match it.
  static <P, N> SiblingPlan of(
      List<P> previous,
      Function<? super P, ?> previousIdentity,
      List<N> next,
      Function<? super N, ?> nextIdentity) {
    var matches = new int[next.size()];
    var retained = new boolean[previous.size()];
    // Built at the first child that does not match its position; until then the sibling structure
    // is unchanged and each child retains the previous child at its index.
    @Nullable Unmatched unmatched = null;
    var lastSlot = -1;
    var reordered = false;
    for (var index = 0; index < next.size(); index++) {
      var identity = nextIdentity.apply(next.get(index));
      int slot;
      if (unmatched == null
          && index < previous.size()
          && identity.equals(previousIdentity.apply(previous.get(index)))) {
        slot = index;
      } else {
        if (unmatched == null) {
          unmatched = new Unmatched(previous, previousIdentity, index);
        }
        slot = unmatched.poll(identity);
      }
      matches[index] = slot;
      if (slot != NEW) {
        retained[slot] = true;
        reordered |= slot < lastSlot;
        lastSlot = slot;
      }
    }
    var anchors = new int[next.size()];
    var anchor = END;
    for (var index = next.size() - 1; index >= 0; index--) {
      anchors[index] = anchor;
      if (matches[index] != NEW) {
        anchor = matches[index];
      }
    }
    return new SiblingPlan(
        matches, anchors, retained, reordered ? stableSlots(matches, previous.size()) : null);
  }

  /// The index of the previous child next child `index` retains, or [#NEW].
  int match(int index) {
    return matches[index];
  }

  /// The index of the previous child that next child `index` is placed before: the one its nearest
  /// later retained sibling retains, or [#END].
  int anchor(int index) {
    return anchors[index];
  }

  /// Whether next child `index` retains a previous child that must move.
  boolean moves(int index) {
    var slot = matches[index];
    return slot != NEW && stable != null && !stable[slot];
  }

  /// Whether a next child retains previous child `slot`; otherwise it is removed.
  boolean retains(int slot) {
    return retained[slot];
  }

  @Override
  public String toString() {
    return "SiblingPlan" + Arrays.toString(matches);
  }

  /// Marks the previous slots on one longest increasing run of retained slots in next order; those
  /// children are already in relative order and stay where they are.
  private static boolean[] stableSlots(int[] matches, int previousSize) {
    var slots = new int[matches.length];
    var count = 0;
    for (var slot : matches) {
      if (slot != NEW) {
        slots[count++] = slot;
      }
    }
    // tails[length - 1] ends the longest run found so far; each entry links to its predecessor.
    var tails = new int[count];
    var predecessors = new int[count];
    var length = 0;
    for (var index = 0; index < count; index++) {
      var low = 0;
      var high = length;
      while (low < high) {
        var middle = (low + high) >>> 1;
        if (slots[tails[middle]] < slots[index]) {
          low = middle + 1;
        } else {
          high = middle;
        }
      }
      predecessors[index] = low > 0 ? tails[low - 1] : -1;
      tails[low] = index;
      if (low == length) {
        length++;
      }
    }
    var stable = new boolean[previousSize];
    for (var index = length > 0 ? tails[length - 1] : -1; index >= 0; index = predecessors[index]) {
      stable[slots[index]] = true;
    }
    return stable;
  }
}
