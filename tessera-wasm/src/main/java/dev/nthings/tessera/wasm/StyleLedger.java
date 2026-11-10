package dev.nthings.tessera.wasm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;

/// Reference counts for content-addressed style rules shared by mounted roots.
///
/// A rule stays inserted while any committed tree uses it. Unused rules remain available for
/// reuse by later renders and mounts, up to `retained` of them; beyond that the least recently
/// released are evicted. Style values that change on every render therefore cost at most
/// `retained` stale rules instead of growing without bound.
final class StyleLedger {
  private final int retained;
  private final Map<String, Integer> users = new HashMap<>();
  private final SequencedSet<String> unused = new LinkedHashSet<>();

  StyleLedger(int retained) {
    if (retained < 0) {
      throw new IllegalArgumentException("retained must not be negative");
    }
    this.retained = retained;
  }

  /// Adds one user to each rule; returns the rules not currently inserted, in order.
  List<String> acquire(Collection<String> rules) {
    var inserted = new ArrayList<String>();
    for (var rule : rules) {
      var count = users.get(rule);
      if (count == null) {
        inserted.add(rule);
        users.put(rule, 1);
      } else {
        if (count == 0) {
          unused.remove(rule);
        }
        users.put(rule, count + 1);
      }
    }
    return inserted;
  }

  /// Removes one user from each rule; returns the unused rules to delete, oldest first.
  List<String> release(Collection<String> rules) {
    for (var rule : rules) {
      var count = users.get(rule);
      if (count == null || count == 0) {
        throw new IllegalStateException("Released a style rule that has no users: " + rule);
      }
      users.put(rule, count - 1);
      if (count == 1) {
        unused.addLast(rule);
      }
    }
    var evicted = new ArrayList<String>();
    while (unused.size() > retained) {
      var rule = unused.removeFirst();
      users.remove(rule);
      evicted.add(rule);
    }
    return evicted;
  }

  int inserted() {
    return users.size();
  }
}
