package dev.nthings.tessera.wasm;

import java.util.ArrayDeque;
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
/// The reconciler applies a plan by walking the next children backwards: a moved child goes before
/// its next retained sibling, then every child that is neither retained nor moved is created before
/// the next retained sibling, which is already in its final place.
final class SiblingPlan {
  /// A next child that retains nothing and is created.
  static final int NEW = -1;

  private static final SiblingPlan EMPTY = new SiblingPlan(new int[0], new boolean[0], null);

  /// For each next child: the index of the previous child it retains, or [#NEW].
  private final int[] matches;

  /// For each previous child: whether a next child retains it.
  private final boolean[] retained;

  /// For each previous child: whether it stays in place, or `null` when every retained child does.
  private final boolean @Nullable [] stable;

  private SiblingPlan(int[] matches, boolean[] retained, boolean @Nullable [] stable) {
    this.matches = matches;
    this.retained = retained;
    this.stable = stable;
  }

  /// Plans the change from `previous` to `next`, comparing the identity each function gives. Equal
  /// identities must be interchangeable: whichever previous child a next child retains is patched
  /// to match it.
  static <P, N> SiblingPlan of(
      List<P> previous,
      Function<? super P, ?> previousIdentity,
      List<N> next,
      Function<? super N, ?> nextIdentity) {
    if (previous.isEmpty() && next.isEmpty()) {
      return EMPTY;
    }
    var matches = new int[next.size()];
    var retained = new boolean[previous.size()];
    // Built at the first child that does not match its position; until then the sibling structure
    // is unchanged and each child retains the previous child at its index.
    @Nullable Map<Object, ArrayDeque<Integer>> unmatched = null;
    var lastSlot = -1;
    var reordered = false;
    for (var index = 0; index < next.size(); index++) {
      var identity = nextIdentity.apply(next.get(index));
      var slot = NEW;
      if (unmatched == null
          && index < previous.size()
          && identity.equals(previousIdentity.apply(previous.get(index)))) {
        slot = index;
      } else {
        if (unmatched == null) {
          unmatched = new HashMap<>();
          for (var rest = index; rest < previous.size(); rest++) {
            unmatched
                .computeIfAbsent(
                    previousIdentity.apply(previous.get(rest)), _ -> new ArrayDeque<>())
                .addLast(rest);
          }
        }
        var candidates = unmatched.get(identity);
        var candidate = candidates == null ? null : candidates.pollFirst();
        if (candidate != null) {
          slot = candidate;
        }
      }
      matches[index] = slot;
      if (slot != NEW) {
        retained[slot] = true;
        reordered |= slot < lastSlot;
        lastSlot = slot;
      }
    }
    return new SiblingPlan(
        matches, retained, reordered ? stableSlots(matches, previous.size()) : null);
  }

  /// The index of the previous child next child `index` retains, or [#NEW].
  int match(int index) {
    return matches[index];
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
