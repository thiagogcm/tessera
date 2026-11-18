package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.view.LiveNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/// A portable host element with live behavior: Java event handlers, controlled form properties and
/// live children. Wrapping does not change the portable element, which stays valid for static
/// pages; the live behavior belongs to this wrapper. The host keeps its own attributes, styles and
/// children; children added here follow them.
///
/// ```java
/// LiveElement.of(Button.of("Save").element()).onClick(root.update(this::save));
/// LiveElement.of(Forms.text("name")).value(name).on("input", e -> name = e.control().value());
/// ```
public final class LiveElement implements LiveContent {

  private final TagElement host;
  private final Map<EventListener.Key, EventListener> events = new LinkedHashMap<>();
  private ControlProperties controls = ControlProperties.UNCONTROLLED;
  private final List<LiveNode<LiveBehavior>> children = new ArrayList<>();

  private LiveElement(TagElement host) {
    this.host = host;
  }

  /// Wraps `host`, whose attributes, styles and children are read on each render.
  public static LiveElement of(TagElement host) {
    return new LiveElement(host);
  }

  /// Runs `handler` synchronously when the browser dispatches `event` (such as `click`, `input` or
  /// `keydown`) to the host, bubble phase. A handler for the same type and phase replaces the
  /// previous one. Re-rendering rebinds the current handler, so it may capture the state of the
  /// render that created it. Throws IllegalArgumentException if `event` is blank.
  public LiveElement on(String event, Consumer<BrowserEvent> handler) {
    return on(event, EventOptions.DEFAULT, handler);
  }

  /// Runs `handler` for `event` with native listener `options`: capture phase, passive, or once. A
  /// handler for the same type and phase replaces the previous one. Throws IllegalArgumentException
  /// if `event` is blank.
  public LiveElement on(String event, EventOptions options, Consumer<BrowserEvent> handler) {
    var listener = new EventListener(event, options, handler);
    events.put(listener.key(), listener);
    return this;
  }

  /// Runs `handler` synchronously when the host is clicked, within the click's user activation, so
  /// it may open a modal dialog.
  public LiveElement onClick(Runnable handler) {
    return on("click", _ -> handler.run());
  }

  /// Controls the live `value` of an input, textarea or single select: each render writes it to the
  /// control unless the user is composing text with an input method. The initial `value` attribute
  /// is unaffected. Another host (a file input, a multiple select, any other element) reports
  /// `control-value`.
  public LiveElement value(String value) {
    controls = controls.withValue(value);
    return this;
  }

  /// Controls the live checked state of a checkbox or radio input. Another host reports
  /// `control-checked`.
  public LiveElement checked(boolean checked) {
    controls = controls.withChecked(checked);
    return this;
  }

  /// Controls the selected options of a multiple select; an empty list deselects every option.
  /// Another host reports `control-selection`.
  public LiveElement selectedValues(List<String> values) {
    controls = controls.withSelectedValues(values);
    return this;
  }

  /// Adds portable `child` as the next child. Throws IllegalStateException for a void host.
  public LiveElement child(IntoElement child) {
    return append(new LiveNode.Portable<>(child.intoElement()));
  }

  /// Adds live `child` as the next child. Throws IllegalStateException for a void host.
  public LiveElement child(LiveContent child) {
    return append(LivePlacement.capture(child));
  }

  /// Adds a text child, escaped on output.
  public LiveElement text(String text) {
    return child(Elements.text(text));
  }

  /// Adds each live child in order.
  public LiveElement children(LiveContent... children) {
    for (var child : children) {
      append(LivePlacement.capture(child));
    }
    return this;
  }

  /// Adds each live child in iteration order.
  public LiveElement children(Collection<? extends LiveContent> children) {
    for (var child : children) {
      append(LivePlacement.capture(child));
    }
    return this;
  }

  private LiveElement append(LiveNode<LiveBehavior> child) {
    LiveNode.Host.requireChildren(host);
    children.add(child);
    return this;
  }

  LiveNode.Host<LiveBehavior> capture() {
    return new LiveNode.Host<>(host, children, new LiveBehavior.Bindings(events, controls));
  }
}
