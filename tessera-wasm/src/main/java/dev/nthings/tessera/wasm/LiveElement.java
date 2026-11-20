package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagHost;
import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.view.LiveNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/// A portable host element with live behavior: Java event handlers, controlled form properties and
/// live children. Wrapping does not change the portable element, which stays valid for static
/// pages; the live behavior belongs to this wrapper. The host keeps its own attributes, styles and
/// children; children added here follow them.
///
/// ```java
/// LiveElement.of(Button.of("Save")).onClick(root.update(this::save));
/// LiveElement.of(Forms.text("name")).bindValue(name).onValueChange(value -> name = value);
/// ```
public final class LiveElement implements LiveTree, FluentBuilder<LiveElement> {

  private final TagElement host;
  private final Map<EventListener.Key, EventListener> events = new LinkedHashMap<>();
  private ControlProperties controls = ControlProperties.UNCONTROLLED;
  private @Nullable WidgetBinding widget;
  private final List<LiveNode<LiveBehavior>> children = new ArrayList<>();

  private LiveElement(TagElement host) {
    this.host = host;
  }

  /// Wraps `host`, whose attributes, styles and children are read on each render. Accepts
  /// [TagElement] and tag-backed components such as `Button`, `IconButton`, and `Image`.
  public static LiveElement of(TagHost host) {
    return new LiveElement(host.toTagElement());
  }

  /// Wraps `host` and runs `configure` on the resulting live element, allowing mixed portable and
  /// live children and bindings to be attached in one block.
  public static LiveElement of(TagHost host, Consumer<LiveElement> configure) {
    var element = of(host);
    configure.accept(element);
    return element;
  }

  @Override
  public LiveElement self() {
    return this;
  }

  /// Runs `handler` synchronously when the browser dispatches `event` to the host, with optional
  /// native listener `options` ([EventOption#CAPTURE], [EventOption#PASSIVE], [EventOption#ONCE]).
  /// A handler for the same type and phase replaces the previous one. Re-rendering rebinds the
  /// current handler, so it may capture the state of the render that created it.
  ///
  /// On an input or textarea, the [Events#INPUT] handler also runs when an IME composition ends,
  /// with the event type `compositionend` and the committed text: browsers dispatch the
  /// composition's last `input` event while [ControlState#composing()] is still true.
  public <E extends BrowserEvent> LiveElement on(
      EventType<E> event, Consumer<E> handler, EventOption... options) {
    return listen(EventListener.of(event, options, handler, null));
  }

  private LiveElement listen(EventListener listener) {
    events.put(listener.key(), listener);
    return this;
  }

  /// Runs `handler` synchronously when the host is clicked, within the click's user activation, so
  /// it may open a modal dialog.
  public LiveElement onClick(Runnable handler) {
    return on(Events.CLICK, _ -> handler.run());
  }

  /// Binds the current value of a value-editable input, textarea or single select to application
  /// state. Renders apply changes after attributes and children, and text writes wait for IME
  /// composition to finish. A user edit requests a render, so an edit the application does not
  /// accept into the bound value is reverted. The native reset default remains the one set at
  /// insertion.
  public LiveElement bindValue(String value) {
    controls = controls.withValue(value);
    return this;
  }

  /// Controls the live checked state of a checkbox or radio input; a user change the application
  /// does not accept is reverted. Another host reports `control-checked`.
  public LiveElement bindChecked(boolean checked) {
    controls = controls.withChecked(checked);
    return this;
  }

  /// Controls the selected options of a multiple select; an empty list deselects every option, and
  /// a user change the application does not accept is reverted. Another host reports
  /// `control-selection`.
  public LiveElement bindSelectedValues(List<String> values) {
    controls = controls.withSelectedValues(values);
    return this;
  }

  /// Reports the requested value on a native `input` event. The application can update its state
  /// and trigger a render to accept the edit.
  public LiveElement onValueChange(Consumer<String> handler) {
    return listen(
        EventListener.of(
            Events.INPUT,
            new EventOption[0],
            event -> handler.accept(event.control().value()),
            ControlProperties.Kind.VALUE));
  }

  /// Reports the checked state on a native `change` event from a checkbox or radio.
  public LiveElement onCheckedChange(Consumer<Boolean> handler) {
    return listen(
        EventListener.of(
            Events.CHANGE,
            new EventOption[0],
            event -> handler.accept(event.control().checked()),
            ControlProperties.Kind.CHECKED));
  }

  /// Reports the selected values of a multiple select in option order on a native `input` event.
  public LiveElement onSelectedValuesChange(Consumer<List<String>> handler) {
    return listen(
        EventListener.of(
            Events.INPUT,
            new EventOption[0],
            event -> handler.accept(event.control().selectedValues()),
            ControlProperties.Kind.SELECTION));
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

  /// Adds portable children in order.
  public LiveElement children(IntoElement first, IntoElement... rest) {
    child(first);
    for (var child : rest) {
      child(child);
    }
    return this;
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

  /// Adds each live child from `stream` in order. The stream is consumed.
  public LiveElement children(Stream<? extends LiveContent> stream) {
    stream.forEach(this::child);
    return this;
  }

  private LiveElement append(LiveNode<LiveBehavior> child) {
    LiveNode.Host.requireChildren(host);
    children.add(child);
    return this;
  }

  LiveElement widget(WidgetBinding binding) {
    widget = binding;
    return this;
  }

  LiveNode.Host<LiveBehavior> capture() {
    return new LiveNode.Host<>(host, children, new LiveBehavior.Bindings(events, controls, widget));
  }
}
