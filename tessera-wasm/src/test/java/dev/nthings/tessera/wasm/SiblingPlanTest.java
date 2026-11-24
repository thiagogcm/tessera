package dev.nthings.tessera.wasm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Sibling matching and minimal movement, checked against independent oracles on seeded random
/// sibling lists and on a model of the DOM operations the reconciler performs with a plan.
@DisplayName("Sibling plan")
class SiblingPlanTest {
  private static final int SEEDS = 4_000;

  /// Unkeyed identities: tag names, text and group definitions shared by several siblings.
  private static final List<String> KINDS = List.of("div", "span", "#text", "Field");

  @Test
  @DisplayName("an unchanged list retains every child in place")
  void unchangedListsStayInPlace() {
    var children = List.of("key:a", "div", "key:b", "div", "#text");
    var plan = plan(children, children);

    for (var index = 0; index < children.size(); index++) {
      assertThat(plan.match(index)).isEqualTo(index);
      assertThat(plan.moves(index)).isFalse();
      assertThat(plan.retains(index)).isTrue();
    }
  }

  @Test
  @DisplayName("a reversed keyed list keeps one child in place and moves the rest")
  void reversedListsMoveAllButOne() {
    var previous = List.of("key:a", "key:b", "key:c", "key:d");
    var plan = plan(previous, previous.reversed());

    assertThat(moves(plan, previous.size())).isEqualTo(3);
    assertThat(apply(previous, previous.reversed(), plan))
        .isEqualTo(expected(previous.size(), plan));
  }

  @Test
  @DisplayName("moving one keyed child to the front moves only that child")
  void movingOneChildMovesOnlyIt() {
    var previous = List.of("key:a", "key:b", "key:c", "key:d", "key:e");
    var next = List.of("key:e", "key:a", "key:b", "key:c", "key:d");
    var plan = plan(previous, next);

    assertThat(moves(plan, next.size())).isEqualTo(1);
    assertThat(plan.moves(0)).isTrue();
  }

  @Test
  @DisplayName("an optional sibling of another kind does not shift unkeyed matches")
  void optionalSiblingsDoNotShiftMatches() {
    var previous = List.of("label", "input", "span", "button");
    var next = List.of("label", "input", "p", "span", "button");
    var plan = plan(previous, next);

    assertThat(plan.match(0)).isZero();
    assertThat(plan.match(1)).isEqualTo(1);
    assertThat(plan.match(2)).isEqualTo(SiblingPlan.NEW);
    assertThat(plan.match(3)).isEqualTo(2);
    assertThat(plan.match(4)).isEqualTo(3);
    assertThat(moves(plan, next.size())).isZero();
  }

  @Test
  @DisplayName("random sibling changes match the k-th occurrence of each identity in order")
  void matchesFollowOccurrenceOrder() {
    for (var seed = 0; seed < SEEDS; seed++) {
      var random = new Random(seed);
      var previous = siblings(random, List.of());
      var next = siblings(random, previous);
      var plan = plan(previous, next);
      var message = "seed " + seed + ": " + previous + " -> " + next;

      var previousSeen = new HashMap<String, Integer>();
      var occurrence = new int[previous.size()];
      for (var slot = 0; slot < previous.size(); slot++) {
        occurrence[slot] = previousSeen.merge(previous.get(slot), 1, Integer::sum) - 1;
      }
      var nextSeen = new HashMap<String, Integer>();
      var retained = new boolean[previous.size()];
      for (var index = 0; index < next.size(); index++) {
        var identity = next.get(index);
        var k = nextSeen.merge(identity, 1, Integer::sum) - 1;
        var slot = plan.match(index);
        if (k < previousSeen.getOrDefault(identity, 0)) {
          assertThat(slot).as(message).isNotEqualTo(SiblingPlan.NEW);
          assertThat(previous.get(slot)).as(message).isEqualTo(identity);
          assertThat(occurrence[slot]).as(message).isEqualTo(k);
          assertThat(retained[slot]).as(message).isFalse();
          retained[slot] = true;
        } else {
          assertThat(slot).as(message).isEqualTo(SiblingPlan.NEW);
        }
      }
      for (var slot = 0; slot < previous.size(); slot++) {
        assertThat(plan.retains(slot)).as(message).isEqualTo(retained[slot]);
      }
    }
  }

  @Test
  @DisplayName("random sibling changes move only children off one longest increasing run")
  void movesAreMinimal() {
    for (var seed = 0; seed < SEEDS; seed++) {
      var random = new Random(seed);
      var previous = siblings(random, List.of());
      var next = siblings(random, previous);
      var plan = plan(previous, next);
      var message = "seed " + seed + ": " + previous + " -> " + next;

      var slots = new ArrayList<Integer>();
      var lastStable = -1;
      for (var index = 0; index < next.size(); index++) {
        var slot = plan.match(index);
        if (slot == SiblingPlan.NEW) {
          assertThat(plan.moves(index)).as(message).isFalse();
          continue;
        }
        slots.add(slot);
        if (!plan.moves(index)) {
          assertThat(slot).as(message).isGreaterThan(lastStable);
          lastStable = slot;
        }
      }
      var stay = slots.size() - moves(plan, next.size());
      assertThat(stay).as(message).isEqualTo(longestIncreasingRun(slots));
    }
  }

  @Test
  @DisplayName("applying a random plan to a modelled DOM yields the next order")
  void applyingPlansYieldsTheNextOrder() {
    for (var seed = 0; seed < SEEDS; seed++) {
      var random = new Random(seed);
      var previous = siblings(random, List.of());
      var next = siblings(random, previous);
      var plan = plan(previous, next);

      assertThat(apply(previous, next, plan))
          .as("seed %d: %s -> %s", seed, previous, next)
          .isEqualTo(expected(next.size(), plan));
    }
  }

  private static SiblingPlan plan(List<String> previous, List<String> next) {
    return SiblingPlan.of(previous, Function.identity(), next, Function.identity());
  }

  private static int moves(SiblingPlan plan, int nextSize) {
    var moves = 0;
    for (var index = 0; index < nextSize; index++) {
      if (plan.moves(index)) {
        moves++;
      }
    }
    return moves;
  }

  /// Up to 12 siblings with unique keys and repeated unkeyed kinds. With `previous` children, the
  /// list derives from them as a render would: keyed children are dropped, reordered and joined by
  /// new keys, and unkeyed kinds come and go.
  private static List<String> siblings(Random random, List<String> previous) {
    var children = new ArrayList<String>();
    if (previous.isEmpty() || random.nextInt(10) == 0) {
      var size = random.nextInt(13);
      for (var index = 0; index < size; index++) {
        children.add(random.nextBoolean() ? "key:" + index : kind(random));
      }
      return children;
    }
    for (var child : previous) {
      if (random.nextInt(5) != 0) {
        children.add(child);
      }
    }
    switch (random.nextInt(4)) {
      case 0 -> Collections.shuffle(children, random);
      case 1 -> Collections.reverse(children);
      case 2 -> {
        if (children.size() > 1) {
          children.add(
              random.nextInt(children.size()), children.remove(random.nextInt(children.size())));
        }
      }
      default -> {}
    }
    var additions = random.nextInt(4);
    for (var index = 0; index < additions && children.size() < 12; index++) {
      var child = random.nextBoolean() ? "key:new" + index : kind(random);
      children.add(random.nextInt(children.size() + 1), child);
    }
    return children;
  }

  private static String kind(Random random) {
    return KINDS.get(random.nextInt(KINDS.size()));
  }

  /// The length of the longest strictly increasing subsequence of `values`, by quadratic search.
  private static int longestIncreasingRun(List<Integer> values) {
    var lengths = new int[values.size()];
    var longest = 0;
    for (var index = 0; index < values.size(); index++) {
      lengths[index] = 1;
      for (var before = 0; before < index; before++) {
        if (values.get(before) < values.get(index)) {
          lengths[index] = Math.max(lengths[index], lengths[before] + 1);
        }
      }
      longest = Math.max(longest, lengths[index]);
    }
    return longest;
  }

  /// The node each next child should own: the previous node it retains (`p<slot>`), or a new node
  /// (`n<index>`).
  private static List<String> expected(int nextSize, SiblingPlan plan) {
    var nodes = new ArrayList<String>();
    for (var index = 0; index < nextSize; index++) {
      var slot = plan.match(index);
      nodes.add(slot == SiblingPlan.NEW ? "n" + index : "p" + slot);
    }
    return nodes;
  }

  /// Performs a plan's DOM operations, in the reconciler's order, on a list of previous nodes
  /// followed by an end marker, then checks that nothing retained in place ever moved.
  private static List<String> apply(List<String> previous, List<String> next, SiblingPlan plan) {
    var dom = new LinkedList<String>();
    for (var slot = 0; slot < previous.size(); slot++) {
      dom.add("p" + slot);
    }
    dom.add("end");
    var stable = new ArrayList<String>(dom);
    for (var index = next.size() - 1; index >= 0; index--) {
      if (plan.moves(index)) {
        var node = "p" + plan.match(index);
        dom.remove(node);
        dom.add(dom.indexOf(anchor(plan, index)), node);
        stable.remove(node);
      }
    }
    for (var index = 0; index < next.size(); index++) {
      if (plan.match(index) == SiblingPlan.NEW) {
        dom.add(dom.indexOf(anchor(plan, index)), "n" + index);
      }
    }
    for (var slot = 0; slot < previous.size(); slot++) {
      if (!plan.retains(slot)) {
        dom.remove("p" + slot);
        stable.remove("p" + slot);
      }
    }
    // Children that did not move keep their previous relative order.
    assertThat(dom).containsSubsequence(stable);
    assertThat(dom.removeLast()).isEqualTo("end");
    return dom;
  }

  /// The node next child `index` is placed before.
  private static String anchor(SiblingPlan plan, int index) {
    var slot = plan.anchor(index);
    return slot == SiblingPlan.END ? "end" : "p" + slot;
  }
}
