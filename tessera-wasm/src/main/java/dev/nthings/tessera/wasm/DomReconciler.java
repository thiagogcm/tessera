package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.internal.view.ControlSemantics;
import dev.nthings.tessera.internal.view.LinkedNode;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Retains native elements and anchored logical groups within one owned host.
///
/// A commit runs in phases, and each phase owns what it touches. [#apply] reconciles a candidate
/// against the committed tree, registering every instance it creates before that instance acquires
/// listeners or an effect lifetime, and detaching obsolete ranges; a first commit builds off the
/// host, so a failure before publication leaves the fallback nodes in place. [#publish] installs
/// the candidate as the committed tree, moving a first build into the host. [#retire] then releases
/// the resources of replaced subtrees with their application cleanups, [#handOff] moves focus and
/// the reading position from a replaced fallback, and [#activate] starts the effects the commit
/// placed. A root that closes while a phase runs settles through [ClosedRoot]: the commit publishes
/// nothing and starts nothing, and the closing root releases what the commit acquired along with
/// what it already owned.
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

  private List<Mounted> widgets = List.of();
  private final List<Mounted> widgetUpdates = new ArrayList<>();

  /// Native state of bindings already on the DOM, read before any sibling mutates their values.
  private final Map<Mounted, JSObject> previousControls = new IdentityHashMap<>();

  private List<Mounted> boundControls = List.of();
  private final List<ControlUpdate> controlUpdates = new ArrayList<>();

  private record ControlUpdate(
      Mounted instance,
      ControlProperties bindings,
      @Nullable JSObject retained,
      boolean restoreValue,
      boolean restoreChecked) {}

  private final List<RuntimeException> disposalFailures = new ArrayList<>();
  private boolean mounted;

  /// What the first commit took over from the fallback content, until [#handOff] applies it.
  private @Nullable JSObject handoff;

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
    // The previous commit drained every queue, or failed and closed the root.
    for (var instance : widgets) {
      requireOpen();
      var controller = instance.widget;
      if (controller != null) {
        controller.beforePatch();
      }
    }
    for (var control : boundControls) {
      requireOpen();
      // Other controls are captured lazily, before their own patch can change them.
      if (sharesNativeState((LinkedNode.Tag<LiveBehavior>) control.view)) {
        previousControls.put(control, Dom.captureControl(control.first));
      }
    }
    var parent = host;
    if (!mounted) {
      parent = Dom.element("div");
    }
    return new Commit(reconcile(parent, roots, snapshot.roots(), null, false));
  }

  /// Publishes a prepared commit: installs its tree as the committed tree, replacing fallback
  /// content on the first commit. Acquired instances transfer to the committed tree here; a failure
  /// during first installation releases them, restores the original fallback nodes and returns
  /// focus to the fallback element that had it. A successful first installation leaves the focus
  /// and scroll handoff to [#handOff].
  void publish(Commit commit) {
    // A root that closed after the last mutation of [#apply] already released its acquisitions.
    requireOpen();
    if (mounted) {
      finishControls();
      requireOpen();
    } else {
      // Retain the real nodes until installation succeeds, including dirty native controls.
      var fallback = Dom.snapshotChildren(host);
      var state = Dom.captureFallback(host);
      var installed = false;
      try {
        Dom.clear(host);
        for (var instance : commit.tree()) {
          Dom.moveRange(host, instance.first, instance.last, null);
        }
        finishControls();
        requireOpen();
        installed = true;
      } finally {
        if (!installed) {
          try {
            abort();
          } finally {
            Dom.restoreChildren(host, fallback);
            Dom.refocusFallback(state);
          }
        }
      }
      mounted = true;
      handoff = state;
    }
    roots = commit.tree();
    created.clear();
  }

  /// Completes the first commit's handoff from fallback content, once the committed tree, its
  /// references and its widgets are in place and before effects start: keeps the reading position
  /// of a host scrolled past its top, and moves focus from a fallback element to the committed
  /// element with the same id. Focus events it fires reach the committed handlers.
  void handOff() {
    var state = handoff;
    handoff = null;
    if (state != null) {
      Dom.handOffFallback(host, state);
    }
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
  }

  /// Applies browser-owned state once the tree and references are visible.
  void updateWidgets(BrowserRoot root) {
    var pending = List.copyOf(widgetUpdates);
    widgetUpdates.clear();
    var retained = new ArrayList<Mounted>();
    for (var instance : pending) {
      requireOpen();
      var tag = (LinkedNode.Tag<LiveBehavior>) instance.view;
      var binding = LiveBehavior.bindings(tag).widget();
      var controller = instance.widget;
      if (controller != null && (binding == null || instance.widgetKind != binding.getClass())) {
        instance.widget = null;
        controller.close();
        controller = null;
      }
      if (binding != null) {
        if (controller == null) {
          controller = binding.create(instance.first, root);
          instance.widget = controller;
          instance.widgetKind = binding.getClass();
        }
        controller.update(binding);
        retained.add(instance);
      }
    }
    requireOpen();
    widgets = List.copyOf(retained);
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
        var placement = (LinkedNode.Special<LiveBehavior>) instance.view;
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
    retiring.clear();
    clearQueued();
  }

  /// Drops the work queued for the active commit's later phases.
  private void clearQueued() {
    starting.clear();
    previousControls.clear();
    controlUpdates.clear();
    widgetUpdates.clear();
  }

  /// Whether siblings can change this control's state before its own patch: a radio unchecked by
  /// its group, or a select whose options change.
  private static boolean sharesNativeState(LinkedNode.Tag<LiveBehavior> tag) {
    return tag.tag() == HtmlTag.SELECT
        || (ControlSemantics.isCheckable(tag.tag(), tag.attributes())
            && "radio".equals(ControlSemantics.inputType(tag.attributes())));
  }

  /// Reports and clears `failures`, if any.
  private void report(String what, List<RuntimeException> failures) {
    Failures.reportAll(Failures.inRoot(what, hostId), failures);
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
    clearQueued();
    boundControls = List.of();
    widgets = List.of();
    handoff = null;
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
    var widget = instance.widget;
    instance.widget = null;
    if (widget != null) {
      Failures.attempt(failures, widget::close);
    }
    instance.events.values().forEach(binding -> Dom.release(binding.cleanup));
    instance.events = Map.of();
    instance.formListeners.forEach(Dom::release);
    instance.formListeners = List.of();
  }

  /// Releases a subtree's resources, then detaches its whole DOM range at once.
  private void dispose(Mounted instance) {
    release(instance, disposalFailures);
    Dom.removeRange(instance.first, instance.last);
  }

  /// Plans the change from `previous` to `next` with [SiblingPlan], then moves only the retained
  /// ranges outside the longest run already in order, creates new nodes in their final position,
  /// and detaches the rest for [#retire]. Matched nodes are patched in sibling order with
  /// creations, so effects start in document order. The root closing mid-mutation aborts at the
  /// next child or range boundary.
  private List<Mounted> reconcile(
      JSObject parent,
      List<Mounted> previous,
      List<LinkedNode<LiveBehavior>> next,
      @Nullable JSObject end,
      boolean retainOptionDefaults) {
    if (previous.isEmpty() && next.isEmpty()) {
      return List.of();
    }
    requireOpen();
    var plan =
        SiblingPlan.of(previous, instance -> instance.identity, next, DomReconciler::identity);
    // Walking backwards, each retained range is placed before its next retained sibling, and each
    // new child's anchor is that sibling.
    @Nullable JSObject[] anchors = new JSObject[next.size()];
    var before = end;
    for (var index = next.size() - 1; index >= 0; index--) {
      requireOpen();
      var slot = plan.match(index);
      if (slot == SiblingPlan.NEW) {
        anchors[index] = before;
        continue;
      }
      var old = previous.get(slot);
      if (plan.moves(index)) {
        Dom.moveRange(parent, old.first, old.last, before);
      }
      before = old.first;
    }
    var result = new ArrayList<Mounted>(next.size());
    for (var index = 0; index < next.size(); index++) {
      requireOpen();
      var slot = plan.match(index);
      if (slot != SiblingPlan.NEW) {
        var old = previous.get(slot);
        patch(parent, old, next.get(index), retainOptionDefaults);
        result.add(old);
      } else {
        result.add(create(parent, next.get(index), anchors[index]));
      }
    }
    for (var slot = 0; slot < previous.size(); slot++) {
      if (!plan.retains(slot)) {
        var obsolete = previous.get(slot);
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

  /// What a child matches among its previous siblings: a keyed child its key, an unkeyed child the
  /// earliest unmatched sibling of its kind (tag name, text, effect or group definition), so a
  /// conditional sibling of another kind does not shift the match. Equal identities are always
  /// compatible, so a matched node is patched and an unmatched one replaced.
  private static Object identity(LinkedNode<LiveBehavior> node) {
    return switch (node) {
      case LinkedNode.Group<LiveBehavior> group -> {
        var key = group.key();
        yield key != null ? new KeyedGroup(key, group.definition()) : group.definition();
      }
      case LinkedNode.Special<LiveBehavior> _ -> EFFECT;
      case LinkedNode.Tag<LiveBehavior> tag -> tag.tag();
      case LinkedNode.Text<LiveBehavior> _ -> TEXT;
    };
  }

  /// Creates the nodes of `view` before `before`. A new option writes its declared `selected`
  /// default even inside a retained select, so options that arrive in a later render apply the
  /// select's initial value; a choice the user already made is restored after the patch.
  private Mounted create(
      JSObject parent, LinkedNode<LiveBehavior> view, @Nullable JSObject before) {
    return switch (view) {
      case LinkedNode.Text(var text) -> {
        var node = Dom.text(text);
        Dom.insert(parent, node, before);
        yield track(view, node, node);
      }
      case LinkedNode.Tag<LiveBehavior> tag -> {
        var node = Dom.element(tag.tag().namespace().uri(), tag.tag().tagName());
        var instance = track(view, node, node);
        attributes(node, Map.of(), tag.attributes(), LinkedNode.Defaults.NONE, false);
        liveProperties(node, Map.of(), tag.liveProperties());
        formListeners(instance, tag);
        var bindings = LiveBehavior.bindings(tag);
        events(instance, bindings.events());
        instance.children = reconcile(node, List.of(), tag.children(), null, false);
        Dom.insert(parent, node, before);
        if (bindings.widget() != null) {
          widgetUpdates.add(instance);
        }
        if (bindings.controls().isControlled()) {
          controlUpdates.add(new ControlUpdate(instance, bindings.controls(), null, false, false));
        }
        yield instance;
      }
      case LinkedNode.Group<LiveBehavior> group -> {
        var first = Dom.marker();
        var last = Dom.marker();
        Dom.insert(parent, first, before);
        Dom.insert(parent, last, before);
        var instance = track(view, first, last);
        instance.children = reconcile(parent, List.of(), group.children(), last, false);
        yield instance;
      }
      case LinkedNode.Special<LiveBehavior> _ -> {
        var anchor = Dom.marker();
        Dom.insert(parent, anchor, before);
        var instance = track(view, anchor, anchor);
        instance.lifetime = new LifetimeScope();
        starting.add(instance);
        yield instance;
      }
    };
  }

  /// Registers a new instance before it acquires anything, so a failure or closure releases it.
  private Mounted track(LinkedNode<LiveBehavior> view, JSObject first, JSObject last) {
    var instance = new Mounted(view, first, last);
    created.add(instance);
    return instance;
  }

  private void patch(
      JSObject parent,
      Mounted instance,
      LinkedNode<LiveBehavior> next,
      boolean retainOptionDefaults) {
    switch (next) {
      case LinkedNode.Text(var text) -> {
        if (!((LinkedNode.Text<LiveBehavior>) instance.view).content().equals(text)) {
          Dom.textValue(instance.first, text);
        }
      }
      case LinkedNode.Tag<LiveBehavior> tag -> {
        patchTag(instance, tag, retainOptionDefaults);
      }
      case LinkedNode.Group<LiveBehavior> group -> {
        instance.children =
            reconcile(
                parent, instance.children, group.children(), instance.last, retainOptionDefaults);
      }
      case LinkedNode.Special<LiveBehavior> _ -> {}
    }
    instance.view = next;
  }

  private void patchTag(
      Mounted instance, LinkedNode.Tag<LiveBehavior> tag, boolean retainOptionDefaults) {
    var previous = (LinkedNode.Tag<LiveBehavior>) instance.view;
    var hadTextDefault = instance.defaults.text();
    var defaults = instance.keepDefaults(tag.defaults());
    var node = instance.first;
    var bindings = LiveBehavior.bindings(tag);
    if (bindings.widget() != null || instance.widget != null) {
      widgetUpdates.add(instance);
    }
    var preserveText = defaults.text() && !hadTextDefault && !instance.children.isEmpty();
    queueRetainedControl(instance, previous, tag, defaults, bindings.controls(), preserveText);

    attributes(
        node,
        previous.attributes(),
        tag.attributes(),
        defaults,
        retainOptionDefaults && tag.tag() == HtmlTag.OPTION);
    liveProperties(node, previous.liveProperties(), tag.liveProperties());
    events(instance, bindings.events());
    if (!defaults.text()) {
      instance.children =
          reconcile(
              node,
              instance.children,
              tag.children(),
              null,
              defaults.selection()
                  || (retainOptionDefaults && ControlSemantics.mayContainOptions(tag.tag())));
    } else if (preserveText) {
      // Keep the native default but release any previously mounted logical children.
      var text = Dom.defaultText(node);
      instance.children = reconcile(node, instance.children, List.of(), null, false);
      Dom.defaultText(node, text);
    }
  }

  private void queueRetainedControl(
      Mounted instance,
      LinkedNode.Tag<LiveBehavior> previous,
      LinkedNode.Tag<LiveBehavior> tag,
      LinkedNode.Defaults defaults,
      ControlProperties next,
      boolean preserveText) {
    var old = LiveBehavior.bindings(previous).controls();
    var handoff = previousControls.get(instance);
    var attributes = tag.attributes();
    var restoreValue =
        preserveText
            || (old.value() != null
                && next.value() == null
                && ControlSemantics.hasEditableValue(tag.tag(), attributes));
    var restoreChecked =
        old.checked() != null
            && next.checked() == null
            && ControlSemantics.isCheckable(tag.tag(), attributes);
    var restoreSelection =
        tag.tag() == HtmlTag.SELECT
            && next.value() == null
            && next.selectedValues() == null
            && (defaults.selection() || handoff != null);
    if ((restoreSelection || restoreValue || restoreChecked) && handoff == null) {
      handoff = Dom.captureControl(instance.first);
    }
    var retained = restoreValue || restoreChecked || restoreSelection ? handoff : null;
    if (retained != null || next.isControlled()) {
      controlUpdates.add(new ControlUpdate(instance, next, retained, restoreValue, restoreChecked));
    }
  }

  /// Restore browser-owned state before applying bindings. Connected siblings (including radios)
  /// are in their final positions, and each live property is compared with the actual DOM.
  private void finishControls() {
    for (var index = 0; index < controlUpdates.size(); index++) {
      requireOpen();
      var update = controlUpdates.get(index);
      var retained = update.retained();
      if (retained != null) {
        Dom.restoreControl(
            update.instance().first, retained, update.restoreValue(), update.restoreChecked());
      }
    }
    var bound = new ArrayList<Mounted>();
    for (var index = 0; index < controlUpdates.size(); index++) {
      requireOpen();
      var update = controlUpdates.get(index);
      if (update.bindings().isControlled()) {
        controls(update.instance(), update.bindings());
        bound.add(update.instance());
      }
    }
    boundControls = List.copyOf(bound);
    previousControls.clear();
    controlUpdates.clear();
  }

  /// Listens for what the user changes on a form control. After an edit, a control with a bound
  /// property requests a render: the application may have accepted the edit, and a bound property
  /// it left unchanged reverts the control. Text controls also track IME composition, so a bound
  /// value waits for it to end.
  private void formListeners(Mounted instance, LinkedNode.Tag<LiveBehavior> tag) {
    var text = tag.tag() == HtmlTag.INPUT || tag.tag() == HtmlTag.TEXTAREA;
    if (!text && tag.tag() != HtmlTag.SELECT) {
      return;
    }
    Consumer<JSObject> edited =
        _ -> {
          if (bindings(instance).isControlled()) {
            invalidate.run();
          }
        };
    var listeners = new ArrayList<JSObject>(4);
    listeners.add(Dom.listen(instance.first, "input", edited));
    listeners.add(Dom.listen(instance.first, "change", edited));
    if (text) {
      listeners.add(Dom.listen(instance.first, "compositionstart", _ -> instance.composing = true));
      listeners.add(
          Dom.listen(instance.first, "compositionend", event -> compositionEnded(instance, event)));
    }
    instance.formListeners = List.copyOf(listeners);
  }

  /// Browsers dispatch a composition's last `input` event while it is still composing, so an
  /// application that waits for the composition to end would never see the committed text. The
  /// element's `input` handler therefore runs once more with it, and a bound value is reconciled.
  private void compositionEnded(Mounted instance, JSObject event) {
    instance.composing = false;
    if (!open.getAsBoolean()) {
      return;
    }
    var input = instance.events.get(new EventListener.Key("input", false));
    if (input != null) {
      input.dispatch(event, instance, "input", hostId);
    }
    // Only a controlled value skipped writes during composition.
    if (bindings(instance).value() != null) {
      invalidate.run();
    }
  }

  private static ControlProperties bindings(Mounted instance) {
    return LiveBehavior.bindings((LinkedNode.Tag<LiveBehavior>) instance.view).controls();
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
      JSObject node,
      Map<String, String> previous,
      Map<String, String> next,
      LinkedNode.Defaults defaults,
      boolean retainSelected) {
    diff(
        node,
        previous,
        next,
        Dom::removeAttribute,
        Dom::attribute,
        name ->
            !(defaults.value() && "value".equals(name))
                && !(defaults.checked() && "checked".equals(name))
                && !(retainSelected && "selected".equals(name)));
  }

  /// Sets changed live custom properties through CSSOM, which a `style-src` policy without
  /// `'unsafe-inline'` allows, and removes those no longer declared.
  private static void liveProperties(
      JSObject node, Map<String, String> previous, Map<String, String> next) {
    diff(node, previous, next, Dom::removeStyleProperty, Dom::setStyleProperty, _ -> true);
  }

  private static void diff(
      JSObject node,
      Map<String, String> previous,
      Map<String, String> next,
      BiConsumer<JSObject, String> remove,
      NamedSetter set,
      Predicate<String> canWrite) {
    for (var name : previous.keySet()) {
      if (!next.containsKey(name) && canWrite.test(name)) {
        remove.accept(node, name);
      }
    }
    next.forEach((name, value) -> {
      if (!value.equals(previous.get(name)) && canWrite.test(name)) {
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
      if (listener == null || !listener.flags().equals(event.getValue().flags)) {
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
    private LinkedNode<LiveBehavior> view;

    /// Fixed for the node's life: a node is only patched with a view of the same identity.
    private final Object identity;

    private final JSObject first;
    private final JSObject last;
    private List<Mounted> children = List.of();
    private LinkedNode.Defaults defaults;

    /// Allocated on first use: most nodes bind no listeners.
    private Map<EventListener.Key, Binding> events = Map.of();

    private List<JSObject> formListeners = List.of();
    private boolean composing;
    private @Nullable LifetimeScope lifetime;
    private @Nullable WidgetController widget;
    private @Nullable Class<?> widgetKind;

    /// Set when this instance's resources are released, so each releases once.
    private boolean released;

    private Mounted(LinkedNode<LiveBehavior> view, JSObject first, JSObject last) {
      this.view = view;
      this.identity = identity(view);
      this.first = first;
      this.last = last;
      this.defaults =
          view instanceof LinkedNode.Tag<LiveBehavior> tag
              ? tag.defaults()
              : LinkedNode.Defaults.NONE;
    }

    /// A typed declaration introduced on a retained control also leaves its DOM baseline in place.
    private LinkedNode.Defaults keepDefaults(LinkedNode.Defaults next) {
      if (next == LinkedNode.Defaults.NONE || next.equals(defaults)) {
        return defaults;
      }
      defaults =
          new LinkedNode.Defaults(
              defaults.value() || next.value(),
              defaults.checked() || next.checked(),
              defaults.text() || next.text(),
              defaults.selection() || next.selection());
      return defaults;
    }
  }

  private static final class Binding {
    private Consumer<EventType.Source> handler;
    private final EventListener.Flags flags;
    private final JSObject cleanup;

    private Binding(Mounted target, EventListener listener, String hostId, BooleanSupplier open) {
      this.handler = listener.handler();
      this.flags = listener.flags();
      var type = listener.type();
      this.cleanup =
          Dom.listen(
              target.first,
              type,
              flags.capture(),
              flags.passive(),
              flags.once(),
              event -> {
                if (open.getAsBoolean()) {
                  dispatch(event, target, type, hostId);
                }
              });
    }

    /// An application handler failure is reported and does not affect the root or its tree.
    private void dispatch(JSObject event, Mounted target, String type, String hostId) {
      try {
        handler.accept(
            new NativeEvent.Source(
                event,
                target.first,
                ((LinkedNode.Tag<LiveBehavior>) target.view).tag(),
                target.composing));
      } catch (RuntimeException failure) {
        Failures.report("Java '" + type + "' handler", hostId, failure);
      }
    }
  }
}
