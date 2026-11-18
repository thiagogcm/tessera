package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Retains native elements and anchored logical groups within one owned host.
///
/// A commit runs in phases, and each phase owns what it touches. [#apply] reconciles a candidate
/// against the committed tree, registering every instance it creates before that instance acquires
/// listeners or an effect lifetime, and detaching obsolete ranges; a first commit builds off the
/// host, so a failure before publication leaves the fallback nodes in place. [#publish] installs
/// the candidate as the committed tree, moving a first build into the host. [#retire] then releases
/// the resources of replaced subtrees with their application cleanups, and [#activate] starts the
/// effects the commit placed. A root that closes while a phase runs settles through [ClosedRoot]:
/// the commit releases everything it acquired, publishes nothing and starts nothing, and the
/// closing root releases what it already owned.
final class DomReconciler {
  /// Identity of every unkeyed text node; keyed groups are [KeyedGroup]s, kinds are tag names and
  /// group definitions.
  private static final Object TEXT = new Object();

  /// Identity of every effect. `Elements.effect` places each effect alone in a keyed group, so the
  /// group's key restarts it.
  private static final Object EFFECT = new Object();

  /// A keyed group matches only a previous group with the same key and definition.
  private record KeyedGroup(String key, Object definition) {}

  /// One prepared commit: the candidate tree [#publish] installs as the committed tree.
  record Commit(List<Mounted> tree) {}

  private final JSObject host;
  private final String hostId;
  private final Runnable invalidate;

  /// The closing boundary of the owning root: `false` once the root starts closing.
  private final BooleanSupplier open;

  private List<Mounted> roots = List.of();

  /// Acquired by the active commit; released by [#abort] when the commit fails or the root closes.
  private final List<Mounted> created = new ArrayList<>();

  /// Obsolete subtrees the active commit detached; released with their cleanups by [#retire].
  private final List<Mounted> retiring = new ArrayList<>();

  /// Effects the active commit placed; started by [#activate] while the root remains open.
  private final List<Mounted> starting = new ArrayList<>();

  private final List<RuntimeException> disposalFailures = new ArrayList<>();
  private boolean mounted;

  DomReconciler(JSObject host, String hostId, Runnable invalidate, BooleanSupplier open) {
    this.host = host;
    this.hostId = hostId;
    this.invalidate = invalidate;
    this.open = open;
  }

  /// Applies a candidate without publishing it, returning the commit for [#publish].
  ///
  /// A first commit builds off the host, so failure leaves the fallback nodes intact. Instances the
  /// commit creates are registered before they acquire resources, obsolete ranges are detached and
  /// queued for [#retire], and no application callback runs during the phase. The caller settles a
  /// failure or closure: the closing root has already released the created instances, and a native
  /// failure closes the root, whose teardown releases the committed tree and the acquisitions.
  Commit apply(ViewSnapshot<LiveBehavior> snapshot) {
    starting.clear();
    disposalFailures.clear();
    created.clear();
    retiring.clear();
    var parent = host;
    if (!mounted) {
      parent = Dom.element("div");
    }
    return new Commit(reconcile(parent, roots, snapshot.roots(), null));
  }

  /// Publishes a prepared commit: installs its tree as the committed tree, replacing fallback
  /// content on the first commit. Acquired instances transfer to the committed tree here; a failure
  /// while moving a first build into the host releases them before it propagates.
  void publish(Commit commit) {
    if (!open.getAsBoolean()) {
      abort();
      throw new ClosedRoot();
    }
    if (!mounted) {
      var installed = false;
      try {
        Dom.clear(host);
        for (var instance : commit.tree()) {
          Dom.moveRange(host, instance.first, instance.last, null);
        }
        installed = true;
      } finally {
        if (!installed) {
          abort();
        }
      }
      mounted = true;
    }
    roots = commit.tree();
    created.clear();
  }

  /// Releases the obsolete subtrees the last commit replaced, descendant first and each resource
  /// once, running their application cleanups. A cleanup that closes the root joins the teardown
  /// already in progress; the remaining cleanups still run and are still reported.
  void retire() {
    var obsolete = List.copyOf(retiring);
    retiring.clear();
    for (var instance : obsolete) {
      dispose(instance);
    }
    report("Effect cleanup failed", disposalFailures);
    disposalFailures.clear();
  }

  /// Starts the effects the last commit placed, in document order, while the root remains open. A
  /// start that closes the root stops the batch: no later effect starts. Start failures are
  /// collected and reported without closing the root.
  void activate() {
    var pending = List.copyOf(starting);
    starting.clear();
    var started = new ArrayList<RuntimeException>();
    for (var instance : pending) {
      if (!open.getAsBoolean()) {
        break;
      }
      var lifetime = instance.lifetime;
      if (lifetime != null && !lifetime.disposed()) {
        var placement = (ViewSnapshot.Special<LiveBehavior>) instance.view;
        var effect = ((LiveBehavior.Reaction) placement.payload()).effect();
        Failures.attempt(started, () -> effect.start(lifetime));
      }
    }
    report("Effect failed to start", started);
  }

  /// Releases what an aborted commit acquired: newest first, so descendants release before their
  /// parents, detaching what the commit had attached. A closing root has already released the
  /// committed tree; a native failure closes the root next.
  private void abort() {
    for (var index = created.size() - 1; index >= 0; index--) {
      dispose(created.get(index));
    }
    created.clear();
    starting.clear();
    retiring.clear();
  }

  private void report(String what, List<RuntimeException> failures) {
    if (!failures.isEmpty()) {
      Failures.reportAll(Failures.inRoot(what, hostId), failures);
    }
  }

  /// Releases every listener and effect, closes open dialogs and popovers, then empties the owned
  /// host. A commit interrupted by this close owns what it acquired and what it retired; each is
  /// released here once. Application cleanup failures are collected for one report. Clearing the
  /// host rather than removing retained ranges also disposes a tree left inconsistent by a failed
  /// update.
  void close(List<RuntimeException> failures) {
    roots.forEach(instance -> release(instance, failures));
    roots = List.of();
    retiring.forEach(instance -> release(instance, failures));
    retiring.clear();
    for (var index = created.size() - 1; index >= 0; index--) {
      release(created.get(index), failures);
    }
    created.clear();
    starting.clear();
    if (mounted) {
      Dom.closeTransient(host);
      Dom.clear(host);
    }
  }

  /// Releases the resources of `instance` and its descendants, innermost first, without touching
  /// the DOM. Each instance releases once, so repeated requests join the same teardown.
  private static void release(Mounted instance, List<RuntimeException> failures) {
    if (instance.released) {
      return;
    }
    instance.released = true;
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

  /// Releases a subtree's resources, then detaches its whole DOM range at once.
  private void dispose(Mounted instance) {
    release(instance, disposalFailures);
    Dom.removeRange(instance.first, instance.last);
  }

  /// Matches `next` against `previous`, then moves only the retained ranges outside the longest run
  /// already in order, creates new nodes in their final position, and detaches the rest for
  /// [#retire]. Matched nodes are patched in sibling order with creations, so effects start in
  /// document order. The root closing mid-mutation aborts at the next child or range boundary.
  private List<Mounted> reconcile(
      JSObject parent,
      List<Mounted> previous,
      List<ViewSnapshot.Node<LiveBehavior>> next,
      @Nullable JSObject end) {
    if (previous.isEmpty() && next.isEmpty()) {
      return List.of();
    }
    requireOpen();
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
      requireOpen();
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
      requireOpen();
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
        var obsolete = previous.get(index);
        Dom.removeRange(obsolete.first, obsolete.last);
        retiring.add(obsolete);
      }
    }
    return result;
  }

  /// The root closed mid-mutation: settle at this boundary, releasing what was acquired.
  private void requireOpen() {
    if (!open.getAsBoolean()) {
      throw new ClosedRoot();
    }
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

  /// What a child matches among its previous siblings: a keyed child its key, an unkeyed child the
  /// earliest unmatched sibling of its kind (tag name, text, effect or group definition), so a
  /// conditional sibling of another kind does not shift the match. Equal identities are always
  /// compatible, so a matched node is patched and an unmatched one replaced.
  private static Object identity(ViewSnapshot.Node<LiveBehavior> node) {
    return switch (node) {
      case ViewSnapshot.Group<LiveBehavior> group -> {
        var key = group.key();
        yield key != null ? new KeyedGroup(key, group.definition()) : group.definition();
      }
      case ViewSnapshot.Special<LiveBehavior> _ -> EFFECT;
      case ViewSnapshot.Tag<LiveBehavior> tag -> tag.tag();
      case ViewSnapshot.Text<LiveBehavior> _ -> TEXT;
    };
  }

  private Mounted create(
      JSObject parent, ViewSnapshot.Node<LiveBehavior> view, @Nullable JSObject before) {
    return switch (view) {
      case ViewSnapshot.Text(var text) -> {
        var node = Dom.text(text);
        Dom.insert(parent, node, before);
        var instance = new Mounted(view, node, node);
        created.add(instance);
        yield instance;
      }
      case ViewSnapshot.Tag<LiveBehavior> tag -> {
        var node = Dom.element(tag.tag().namespace().uri(), tag.tag().tagName());
        var instance = new Mounted(view, node, node);
        // Registered before any acquisition, so a failure or closure releases this subtree.
        created.add(instance);
        attributes(node, Map.of(), tag.attributes());
        liveProperties(node, Map.of(), tag.liveProperties());
        composition(instance, tag);
        var bindings = LiveBehavior.bindings(tag);
        events(instance, bindings.events());
        instance.children = reconcile(node, List.of(), tag.children(), null);
        Dom.insert(parent, node, before);
        controls(instance, bindings.controls());
        yield instance;
      }
      case ViewSnapshot.Group<LiveBehavior> group -> {
        var first = Dom.marker();
        var last = Dom.marker();
        Dom.insert(parent, first, before);
        Dom.insert(parent, last, before);
        var instance = new Mounted(view, first, last);
        created.add(instance);
        instance.children = reconcile(parent, List.of(), group.children(), last);
        yield instance;
      }
      case ViewSnapshot.Special<LiveBehavior> _ -> {
        var anchor = Dom.marker();
        Dom.insert(parent, anchor, before);
        var instance = new Mounted(view, anchor, anchor);
        created.add(instance);
        instance.lifetime = new LifetimeScope();
        starting.add(instance);
        yield instance;
      }
    };
  }

  private void patch(JSObject parent, Mounted instance, ViewSnapshot.Node<LiveBehavior> next) {
    switch (next) {
      case ViewSnapshot.Text(var text) -> {
        if (!((ViewSnapshot.Text<LiveBehavior>) instance.view).content().equals(text)) {
          Dom.textValue(instance.first, text);
        }
      }
      case ViewSnapshot.Tag<LiveBehavior> tag -> {
        var previous = (ViewSnapshot.Tag<LiveBehavior>) instance.view;
        attributes(instance.first, previous.attributes(), tag.attributes());
        liveProperties(instance.first, previous.liveProperties(), tag.liveProperties());
        var bindings = LiveBehavior.bindings(tag);
        events(instance, bindings.events());
        instance.children = reconcile(instance.first, instance.children, tag.children(), null);
        controls(instance, bindings.controls());
      }
      case ViewSnapshot.Group<LiveBehavior> group -> {
        instance.children = reconcile(parent, instance.children, group.children(), instance.last);
      }
      case ViewSnapshot.Special<LiveBehavior> _ -> {}
    }
    instance.view = next;
  }

  /// Tracks provisional text so controlled values can wait until composition ends.
  private void composition(Mounted instance, ViewSnapshot.Tag<LiveBehavior> tag) {
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
                  if (LiveBehavior.bindings((ViewSnapshot.Tag<LiveBehavior>) instance.view)
                          .controls()
                          .value()
                      != null) {
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
    next.forEach((name, value) -> {
      if (!value.equals(previous.get(name))) {
        set.apply(node, name, value);
      }
    });
  }

  @FunctionalInterface
  private interface NamedSetter {
    void apply(JSObject node, String name, String value);
  }

  /// Keeps one native listener per type and phase. A re-render with the same options only swaps the
  /// Java handler it calls, so the previous callback is no longer referenced; different options
  /// register a new listener. A dispatched `once` listener stays spent while it remains bound and
  /// is re-armed only after a render removes it. A root that has started closing dispatches
  /// nothing, so a detached node cannot run application handlers after teardown.
  private void events(Mounted instance, Map<EventListener.Key, EventListener> next) {
    if (instance.events.isEmpty() && next.isEmpty()) {
      return;
    }
    // The root is closing: released instances keep no bindings and acquire none.
    if (!open.getAsBoolean()) {
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
    next.forEach((key, listener) -> {
      var binding = instance.events.get(key);
      if (binding != null) {
        binding.handler = listener.handler();
      } else {
        instance.events.put(key, new Binding(instance, listener, hostId, open));
      }
    });
  }

  private static final class Mounted {
    private ViewSnapshot.Node<LiveBehavior> view;

    /// Fixed for the node's life: a node is only patched with a view of the same identity.
    private final Object identity;

    private final JSObject first;
    private final JSObject last;
    private List<Mounted> children = List.of();
    private int slot;

    /// Allocated on first use: most nodes bind no listeners.
    private Map<EventListener.Key, Binding> events = Map.of();

    private List<JSObject> composition = List.of();
    private boolean composing;
    private @Nullable LifetimeScope lifetime;

    /// Set when this instance's resources are released, so each releases once.
    private boolean released;

    private Mounted(ViewSnapshot.Node<LiveBehavior> view, JSObject first, JSObject last) {
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

    private Binding(Mounted target, EventListener listener, String hostId, BooleanSupplier open) {
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
              event -> {
                if (open.getAsBoolean()) {
                  dispatch(event, target, type, hostId);
                }
              });
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
      var tag = ((ViewSnapshot.Tag<LiveBehavior>) target.view).tag();
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
