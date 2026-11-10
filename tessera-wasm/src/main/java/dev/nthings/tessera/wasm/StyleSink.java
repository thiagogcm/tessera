package dev.nthings.tessera.wasm;

import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.css.Theme;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// One constructed stylesheet shared by every root in the document.
///
/// Rules are content-addressed and reference-counted by [StyleLedger]: a rule is inserted once,
/// before the first tree that uses it is committed, and survives while any committed tree uses it.
/// Up to [#RETAINED_UNUSED] unused rules are kept for reuse. The layer-order statement is pinned
/// first, so re-inserted rules never change the cascade order.
final class StyleSink {
  static final int RETAINED_UNUSED = 512;

  private static @Nullable StyleSink shared;

  private final JSObject sheet = Dom.adoptStyleSheet();
  private final StyleLedger ledger = new StyleLedger(RETAINED_UNUSED);
  private final Map<String, JSObject> inserted = new HashMap<>();

  private StyleSink() {
    Dom.insertRule(sheet, StyleSheet.LAYER_ORDER);
  }

  static StyleSink shared() {
    var sink = shared;
    if (sink == null) {
      sink = new StyleSink();
      shared = sink;
    }
    return sink;
  }

  /// A root owns its base rules until close and its render rules until the next successful commit.
  Root root(Theme theme) {
    return new Root(StyleSheet.embeddedBaseRules(theme));
  }

  final class Root implements AutoCloseable {
    private final List<String> baseRules;
    /// The render rules this root holds, by key, with the text built for each. Includes rules
    /// acquired for a failed commit, which remain owned until close.
    private final Map<String, String> held = new LinkedHashMap<>();
    private boolean mounted;

    private Root(List<String> baseRules) {
      this.baseRules = baseRules;
    }

    void mount() {
      if (!mounted) {
        mounted = true;
        acquire(baseRules);
      }
    }

    /// Acquires styles before native mutation and releases old styles after a successful commit.
    /// Rules are compared by key, so a rule this root holds is neither rebuilt nor rehashed.
    void update(List<StyleSheet.EmbeddedRule> rules, Runnable commit) {
      var keys = new HashSet<String>(rules.size() * 2);
      var added = new ArrayList<String>();
      for (var rule : rules) {
        var key = rule.key();
        if (keys.add(key) && !held.containsKey(key)) {
          var css = rule.css();
          held.put(key, css);
          added.add(css);
        }
      }
      acquire(added);
      commit.run();
      var unused = new ArrayList<String>();
      for (var iterator = held.entrySet().iterator(); iterator.hasNext(); ) {
        var entry = iterator.next();
        if (!keys.contains(entry.getKey())) {
          unused.add(entry.getValue());
          iterator.remove();
        }
      }
      release(unused);
    }

    @Override
    public void close() {
      var released = new ArrayList<>(held.values());
      held.clear();
      if (mounted) {
        mounted = false;
        released.addAll(0, baseRules);
      }
      release(released);
    }
  }

  /// Counts one use of each rule, inserting the rules that are new or were evicted.
  private void acquire(Collection<String> rules) {
    for (var rule : ledger.acquire(rules)) {
      var cssRule = Dom.insertRule(sheet, rule);
      if (cssRule != null) {
        inserted.put(rule, cssRule);
      }
    }
  }

  /// Ends one use of each rule, deleting unused rules beyond the retention limit.
  private void release(Collection<String> rules) {
    JSObject deleted = null;
    for (var rule : ledger.release(rules)) {
      var cssRule = inserted.remove(rule);
      if (cssRule != null) {
        if (deleted == null) {
          deleted = Dom.ruleSet();
        }
        Dom.addRule(deleted, cssRule);
      }
    }
    if (deleted != null) {
      Dom.deleteRules(sheet, deleted);
    }
  }
}
