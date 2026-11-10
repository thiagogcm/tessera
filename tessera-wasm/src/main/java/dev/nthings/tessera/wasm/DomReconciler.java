package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.BrowserEvent;
import dev.nthings.tessera.core.ControlProperties;
import dev.nthings.tessera.core.ControlState;
import dev.nthings.tessera.core.EventListener;
import dev.nthings.tessera.core.EventOptions;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.KeyboardData;
import dev.nthings.tessera.core.PointerData;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Retains native elements and anchored logical groups within one owned host.
final class DomReconciler {
  /// Identity of every unkeyed text node; keyed groups are [KeyedGroup]s, kinds are tag names
  /// and group definitions.
  private static final Object TEXT = new Object();

  /// Identity of every effect. `Elements.effect` places each effect alone in a keyed group, so
  /// the group's key restarts it.
  private static final Object EFFECT = new Object();

  /// A keyed group matches only a previous group with the same key and definition.
  private record KeyedGroup(String key, Object definition) {}

  private final JSObject host;
  private final String hostId;
  private final Runnable invalidate;
  private List<Mounted> roots = List.of();
  private final List<Mounted> starting = new ArrayList<>();
  private final List<RuntimeException> disposalFailures = new ArrayList<>();
  private boolean mounted;

  DomReconciler(JSObject host, String hostId, Runnable invalidate) {
    this.host = host;
    this.hostId = hostId;
    this.invalidate = invalidate;
  }

  /// Replaces fallback content on the first successful update. A browser failure while applying
  /// the update propagates so the owner can close the root.
  ///
  /// Effects placed by the update start after it is committed; effects it removes are disposed
  /// during reconciliation. Application failures in either are reported without affecting the tree.
  void update(ViewSnapshot snapshot) {
    starting.clear();
    disposalFailures.clear();
    if (!mounted) {
      Dom.clear(host);
      mounted = true;
    }
    roots = reconcile(host, roots, snapshot.roots(), null);
    report("Effect cleanup failed", disposalFailures);
    if (starting.isEmpty()) {
      return;
    }
    var started = new ArrayList<RuntimeException>();
    for (var instance : List.copyOf(starting)) {
      var lifetime = instance.lifetime;
      if (lifetime != null && !lifetime.disposed()) {
        var effect = ((ViewSnapshot.Effect) instance.view).effect();
        Failures.attempt(started, () -> effect.start(lifetime));
      }
    }
    starting.clear();
    report("Effect failed to start", started);
  }

  private void report(String what, List<RuntimeException> failures) {
    if (!failures.isEmpty()) {
      Failures.reportAll(Failures.inRoot(what, hostId), failures);
    }
  }

  /// Releases every listener and effect, closes open dialogs and popovers, then
  /// empties the owned host. Application cleanup failures are collected for one report.
  /// Clearing the host rather than removing retained ranges also disposes a tree left
  /// inconsistent by a failed update.
  void close(List<RuntimeException> failures) {
    roots.forEach(instance -> release(instance, failures));
    roots = List.of();
    starting.clear();
    if (mounted) {
      Dom.closeTransient(host);
      Dom.clear(host);
    }
  }

  /// Releases the listeners and effects of `instance` and its descendants, innermost first,
  /// without touching the DOM. Application cleanup failures do not interrupt disposal.
  private static void release(Mounted instance, List<RuntimeException> failures) {
    instance.children.forEach(child -> release(child, failures));
    var lifetime = instance.lifetime;
    if (lifetime != null) {
      lifetime.dispose(failures);
    }
    instance.events.values().forEach(binding -> Dom.release(binding.cleanup));
    instance.events = Map.of();
    instance.composition.forEach(Dom::release);
    instance.composition = List.of();
  }

  /// Releases a subtree, then detaches its whole DOM range at once.
  private void dispose(Mounted instance) {
    release(instance, disposalFailures);
    Dom.removeRange(instance.first, instance.last);
  }

  /// Matches `next` against `previous`, then moves only the retained ranges outside the
  /// longest run already in order, creates new nodes in their final position, and disposes the
  /// rest. Matched nodes are patched in sibling order with creations, so effects start in
  /// document order.
  private List<Mounted> reconcile(
      JSObject parent,
      List<Mounted> previous,
      List<ViewSnapshot.Node> next,
      @Nullable JSObject end) {
    if (previous.isEmpty() && next.isEmpty()) {
      return List.of();
    }
    for (var index = 0; index < previous.size(); index++) {
      previous.get(index).slot = index;
    }
    var consumed = new boolean[previous.size()];
    @Nullable Mounted[] matched = new Mounted[next.size()];
    // Built at the first child that does not match its position; until then the sibling structure
    // is unchanged and each child matches the previous child at its index.
    @Nullable Map<Object, ArrayDeque<Mounted>> unmatched = null;
    var lastSlot = -1;
    var reordered = false;
    for (var index = 0; index < next.size(); index++) {
      var identity = identity(next.get(index));
      Mounted old;
      if (unmatched == null
          && index < previous.size()
          && identity.equals(previous.get(index).identity)) {
        old = previous.get(index);
      } else {
        if (unmatched == null) {
          unmatched = new HashMap<>();
          for (var rest = index; rest < previous.size(); rest++) {
            var instance = previous.get(rest);
            unmatched.computeIfAbsent(instance.identity, _ -> new ArrayDeque<>()).addLast(instance);
          }
        }
        var candidates = unmatched.get(identity);
        old = candidates == null ? null : candidates.pollFirst();
      }
      if (old != null) {
        consumed[old.slot] = true;
        matched[index] = old;
        reordered |= old.slot < lastSlot;
        lastSlot = old.slot;
      }
    }
    // Walking backwards, each retained range is placed before its next retained sibling, and each
    // new child's anchor is that sibling.
    var stable = reordered ? stableSlots(matched, previous.size()) : null;
    @Nullable JSObject[] anchors = new JSObject[next.size()];
    var before = end;
    for (var index = next.size() - 1; index >= 0; index--) {
      var old = matched[index];
      if (old == null) {
        anchors[index] = before;
        continue;
      }
      if (stable != null && !stable[old.slot]) {
        Dom.moveRange(parent, old.first, old.last, before);
      }
      before = old.first;
    }
    var result = new ArrayList<Mounted>(next.size());
    for (var index = 0; index < next.size(); index++) {
      var old = matched[index];
      if (old != null) {
        patch(parent, old, next.get(index));
        result.add(old);
      } else {
        result.add(create(parent, next.get(index), anchors[index]));
      }
    }
    for (var index = 0; index < previous.size(); index++) {
      if (!consumed[index]) {
        dispose(previous.get(index));
      }
    }
    return result;
  }

  /// Marks the previous slots on one longest increasing run of matched slots in `matched` order;
  /// those ranges are already in relative order and stay where they are.
  private static boolean[] stableSlots(@Nullable Mounted[] matched, int previousSize) {
    var slots = new int[matched.length];
    var count = 0;
    for (var instance : matched) {
      if (instance != null) {
        slots[count++] = instance.slot;
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

  /// What a child matches among its previous siblings: a keyed child its key, an unkeyed child
  /// the earliest unmatched sibling of its kind (tag name, text, effect or group definition), so a
  /// conditional sibling of another kind does not shift the match. Equal identities are always
  /// compatible, so a matched node is patched and an unmatched one replaced.
  private static Object identity(ViewSnapshot.Node node) {
    return switch (node) {
      case ViewSnapshot.Group group -> {
        var key = group.key();
        yield key != null ? new KeyedGroup(key, group.definition()) : group.definition();
      }
      case ViewSnapshot.Effect _ -> EFFECT;
      case ViewSnapshot.Tag tag -> tag.tag();
      case ViewSnapshot.Text _ -> TEXT;
      case ViewSnapshot.Raw _ -> throw rawHtml();
    };
  }

  private Mounted create(JSObject parent, ViewSnapshot.Node view, @Nullable JSObject before) {
    return switch (view) {
      case ViewSnapshot.Text(var text) -> {
        var node = Dom.text(text);
        Dom.insert(parent, node, before);
        yield new Mounted(view, node, node);
      }
      case ViewSnapshot.Tag tag -> {
        var node = Dom.element(tag.tag().tagName());
        var instance = new Mounted(view, node, node);
        attributes(node, Map.of(), tag.attributes());
        liveProperties(node, Map.of(), tag.liveProperties());
        composition(instance, tag);
        events(instance, tag.events());
        instance.children = reconcile(node, List.of(), tag.children(), null);
        Dom.insert(parent, node, before);
        controls(instance, tag.controlProperties());
        yield instance;
      }
      case ViewSnapshot.Group group -> {
        var first = Dom.marker();
        var last = Dom.marker();
        Dom.insert(parent, first, before);
        Dom.insert(parent, last, before);
        var instance = new Mounted(view, first, last);
        instance.children = reconcile(parent, List.of(), group.children(), last);
        yield instance;
      }
      case ViewSnapshot.Effect _ -> {
        var anchor = Dom.marker();
        Dom.insert(parent, anchor, before);
        var instance = new Mounted(view, anchor, anchor);
        instance.lifetime = new LifetimeScope();
        starting.add(instance);
        yield instance;
      }
      case ViewSnapshot.Raw _ -> throw rawHtml();
    };
  }

  private void patch(JSObject parent, Mounted instance, ViewSnapshot.Node next) {
    switch (next) {
      case ViewSnapshot.Text(var text) -> {
        if (!((ViewSnapshot.Text) instance.view).content().equals(text)) {
          Dom.textValue(instance.first, text);
        }
      }
      case ViewSnapshot.Tag tag -> {
        var previous = (ViewSnapshot.Tag) instance.view;
        attributes(instance.first, previous.attributes(), tag.attributes());
        liveProperties(instance.first, previous.liveProperties(), tag.liveProperties());
        events(instance, tag.events());
        instance.children = reconcile(instance.first, instance.children, tag.children(), null);
        controls(instance, tag.controlProperties());
      }
      case ViewSnapshot.Group group -> {
        instance.children = reconcile(parent, instance.children, group.children(), instance.last);
      }
      case ViewSnapshot.Effect _ -> {}
      case ViewSnapshot.Raw _ -> throw rawHtml();
    }
    instance.view = next;
  }

  /// `ViewRenderer.render` reports raw HTML as `native-raw-html` before a snapshot exists.
  private static IllegalStateException rawHtml() {
    return new IllegalStateException("Native snapshots never contain raw HTML");
  }

  private void composition(Mounted instance, ViewSnapshot.Tag tag) {
    if (tag.tag() != HtmlTag.INPUT && tag.tag() != HtmlTag.TEXTAREA) {
      return;
    }
    instance.composition =
        List.of(
            Dom.listen(instance.first, "compositionstart", _ -> instance.composing = true),
            Dom.listen(
                instance.first,
                "compositionend",
                _ -> {
                  instance.composing = false;
                  // Only a controlled value skipped writes during composition.
                  if (((ViewSnapshot.Tag) instance.view).controlProperties().value() != null) {
                    invalidate.run();
                  }
                }));
  }

  private static void controls(Mounted instance, ControlProperties properties) {
    var node = instance.first;
    var value = properties.value();
    if (value != null && !instance.composing && !value.equals(Dom.value(node))) {
      Dom.value(node, value);
    }
    var checked = properties.checked();
    if (checked != null && checked != Dom.flag(node, "checked")) {
      Dom.checked(node, checked);
    }
    var selectedValues = properties.selectedValues();
    if (selectedValues != null) {
      Dom.selectValues(node, ControlCodec.values(selectedValues));
    }
  }

  private static void attributes(
      JSObject node, Map<String, String> previous, Map<String, String> next) {
    diff(node, previous, next, Dom::removeAttribute, Dom::attribute);
  }

  /// Sets changed live custom properties through CSSOM, which a `style-src` policy without
  /// `'unsafe-inline'` allows, and removes those no longer declared.
  private static void liveProperties(
      JSObject node, Map<String, String> previous, Map<String, String> next) {
    diff(node, previous, next, Dom::removeStyleProperty, Dom::setStyleProperty);
  }

  private static void diff(
      JSObject node,
      Map<String, String> previous,
      Map<String, String> next,
      BiConsumer<JSObject, String> remove,
      NamedSetter set) {
    for (var name : previous.keySet()) {
      if (!next.containsKey(name)) {
        remove.accept(node, name);
      }
    }
    next.forEach(
        (name, value) -> {
          if (!value.equals(previous.get(name))) {
            set.apply(node, name, value);
          }
        });
  }

  @FunctionalInterface
  private interface NamedSetter {
    void apply(JSObject node, String name, String value);
  }

  /// Keeps one native listener per type and phase. A re-render with the same options only swaps
  /// the Java handler it calls, so the previous callback is no longer referenced; different
  /// options register a new listener. A dispatched `once` listener stays spent while it remains
  /// bound and is re-armed only after a render removes it.
  private void events(Mounted instance, Map<EventListener.Key, EventListener> next) {
    if (instance.events.isEmpty() && next.isEmpty()) {
      return;
    }
    var iterator = instance.events.entrySet().iterator();
    while (iterator.hasNext()) {
      var event = iterator.next();
      var listener = next.get(event.getKey());
      if (listener == null || !listener.options().equals(event.getValue().options)) {
        Dom.release(event.getValue().cleanup);
        iterator.remove();
      }
    }
    if (instance.events.isEmpty() && !next.isEmpty()) {
      instance.events = new LinkedHashMap<>();
    }
    next.forEach(
        (key, listener) -> {
          var binding = instance.events.get(key);
          if (binding != null) {
            binding.handler = listener.handler();
          } else {
            instance.events.put(key, new Binding(instance, listener, hostId));
          }
        });
  }

  private static final class Mounted {
    private ViewSnapshot.Node view;
    /// Fixed for the node's life: a node is only patched with a view of the same identity.
    private final Object identity;
    private final JSObject first;
    private final JSObject last;
    private List<Mounted> children = List.of();
    private int slot;
    // Allocated on first use: most nodes bind no listeners.
    private Map<EventListener.Key, Binding> events = Map.of();
    private List<JSObject> composition = List.of();
    private boolean composing;
    private @Nullable LifetimeScope lifetime;

    private Mounted(ViewSnapshot.Node view, JSObject first, JSObject last) {
      this.view = view;
      this.identity = identity(view);
      this.first = first;
      this.last = last;
    }
  }

  private static final class Binding {
    private Consumer<BrowserEvent> handler;
    private final EventOptions options;
    private final JSObject cleanup;

    private Binding(Mounted target, EventListener listener, String hostId) {
      this.handler = listener.handler();
      this.options = listener.options();
      var type = listener.type();
      this.cleanup =
          Dom.listen(
              target.first,
              type,
              options.capture(),
              options.passive(),
              options.once(),
              event -> dispatch(event, target, type, hostId));
    }

    /// An application handler failure is reported and does not affect the root or its tree.
    private void dispatch(JSObject event, Mounted target, String type, String hostId) {
      try {
        handler.accept(new NativeEvent(event, target));
      } catch (RuntimeException failure) {
        Failures.report("Java '" + type + "' handler", hostId, failure);
      }
    }
  }

  private record NativeEvent(JSObject event, Mounted target) implements BrowserEvent {
    @Override
    public ControlState control() {
      var tag = ((ViewSnapshot.Tag) target.view).tag();
      if (tag != HtmlTag.INPUT && tag != HtmlTag.TEXTAREA && tag != HtmlTag.SELECT) {
        throw new IllegalStateException(
            "Form state requires an input, textarea or select current target");
      }
      return ControlCodec.state(Dom.controlState(target.first, event), target.composing);
    }

    @Override
    public String type() {
      return Dom.eventString(event, "type");
    }

    @Override
    public KeyboardData keyboard() {
      if (!Dom.isKeyboardEvent(event)) {
        throw new IllegalStateException("Keyboard data requires a keyboard event, not " + type());
      }
      return new KeyboardData(
          Dom.eventString(event, "key"),
          Dom.eventString(event, "code"),
          Dom.flag(event, "altKey"),
          Dom.flag(event, "ctrlKey"),
          Dom.flag(event, "metaKey"),
          Dom.flag(event, "shiftKey"),
          Dom.flag(event, "repeat"));
    }

    @Override
    public PointerData pointer() {
      if (!Dom.isMouseEvent(event)) {
        throw new IllegalStateException(
            "Pointer data requires a mouse or pointer event, not " + type());
      }
      return new PointerData(
          (int) Dom.number(event, "button"),
          Dom.number(event, "clientX"),
          Dom.number(event, "clientY"),
          (int) Dom.number(event, "detail"),
          Dom.flag(event, "altKey"),
          Dom.flag(event, "ctrlKey"),
          Dom.flag(event, "metaKey"),
          Dom.flag(event, "shiftKey"));
    }

    @Override
    public void preventDefault() {
      Dom.preventDefault(event);
    }

    @Override
    public boolean defaultPrevented() {
      return Dom.flag(event, "defaultPrevented");
    }

    @Override
    public void stopPropagation() {
      Dom.stopPropagation(event);
    }

    @Override
    public void stopImmediatePropagation() {
      Dom.stopImmediatePropagation(event);
    }
  }
}
