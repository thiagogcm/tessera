package dev.nthings.tessera.wasm;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.view.WidgetAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Adaptive tabs with retained selection and focus. Give the portable tabs an explicit scope id so
/// their keyed tabs and panels keep their identity across edits.
public final class LiveTabs implements LiveComponent {
  private static final String TAB = "[role=tab]";
  private static final String TABS = ":scope > [role=tablist] > " + TAB;
  private static final String PANELS = ":scope > [role=tabpanel]";

  private final ContentTabs tabs;
  private boolean controlled;
  private @Nullable String selected;
  private Consumer<String> request = _ -> {};

  private LiveTabs(ContentTabs tabs) {
    this.tabs = tabs;
  }

  /// Adapts the portable labels, panels, enabled state and activation mode.
  public static LiveTabs of(ContentTabs tabs) {
    return new LiveTabs(tabs);
  }

  /// Binds selection to an enabled key, or empty when there are no enabled tabs. An invalid
  /// selection rejects the candidate render.
  public LiveTabs bindSelection(Optional<String> selection) {
    controlled = true;
    selected = selection.orElse(null);
    return this;
  }

  /// Receives a user-requested selection. A bound selection changes only when a render supplies the
  /// new key. The browser root requests that render after this callback.
  public LiveTabs onSelectionRequest(Consumer<String> handler) {
    request = handler;
    return this;
  }

  @Override
  public LiveContent expand(LiveContext context) {
    var binding = new Binding(controlled, selected, request);
    return new WidgetScope(
        WidgetAccess.scope(tabs),
        scope ->
            LiveElement.of(
                    WidgetAccess.tabs(tabs, scope, binding.controlled(), binding.selection()))
                .widget(binding));
  }

  private record Binding(boolean controlled, @Nullable String selection, Consumer<String> request)
      implements WidgetBinding {
    @Override
    public WidgetController create(JSObject node, BrowserRoot root) {
      return new Controller(node, root, this);
    }
  }

  private record Tab(String key, JSObject button, JSObject panel, boolean enabled) {}

  private static final class Controller extends WidgetController {
    private Binding binding;
    private List<Tab> tabs = List.of();
    private @Nullable String selected;
    private @Nullable String roving;
    private @Nullable Tab focusedBeforePatch;
    private boolean initialized;

    Controller(JSObject node, BrowserRoot root, Binding binding) {
      super(node, root);
      this.binding = binding;
    }

    @Override
    void beforePatch() {
      var active = Dom.activeElement();
      focusedBeforePatch =
          tabs.stream()
              .filter(tab -> WidgetDom.same(tab.button(), active))
              .findFirst()
              .orElse(null);
    }

    @Override
    void update(WidgetBinding next) {
      binding = (Binding) next;
      var previous = tabs;
      var buttons = WidgetDom.children(node, TABS);
      var panels = WidgetDom.children(node, PANELS);
      var current = new ArrayList<Tab>(buttons.size());
      for (var index = 0; index < buttons.size(); index++) {
        var button = buttons.get(index);
        current.add(
            new Tab(
                WidgetDom.attribute(button, "data-tab-key"),
                button,
                panels.get(index),
                !Dom.flag(button, "disabled")));
      }
      tabs = current;
      if (binding.controlled()) {
        selected = binding.selection();
      } else if (!initialized) {
        selected =
            tabs.stream()
                .filter(tab -> WidgetDom.attribute(tab.button(), "aria-selected").equals("true"))
                .map(Tab::key)
                .findFirst()
                .orElse(null);
      } else if (!enabled(selected)) {
        selected = fallback(previous, selected);
      }
      if (!initialized) {
        initialized = true;
        listen(node, "click", this::click);
        listen(node, "keydown", this::navigate);
        listen(
            node,
            "focusout",
            event -> {
              var target = WidgetDom.related(event);
              if (tabs.stream().noneMatch(tab -> WidgetDom.same(tab.button(), target))) {
                roving = selected;
                apply();
              }
            });
      }

      var before = focusedBeforePatch;
      focusedBeforePatch = null;
      var active = Dom.activeElement();
      var focusKey =
          before != null && (WidgetDom.same(before.button(), active) || WidgetDom.noFocus(active))
              ? before.key()
              : null;
      roving = selected;
      if (focusKey != null) {
        roving = enabled(focusKey) ? focusKey : fallback(previous, focusKey);
      }
      apply();
      if (focusKey != null) {
        var tab = tab(roving);
        if (tab != null && !Dom.isActiveElement(tab.button())) {
          Dom.focus(tab.button());
        }
      }
    }

    private @Nullable Tab tab(@Nullable String key) {
      return tabs.stream().filter(tab -> tab.key().equals(key)).findFirst().orElse(null);
    }

    private boolean enabled(@Nullable String key) {
      var tab = tab(key);
      return tab != null && tab.enabled();
    }

    private @Nullable String fallback(List<Tab> previous, @Nullable String key) {
      var start = indexOf(tabs, key);
      if (start < 0) {
        start = 0;
        var removed = indexOf(previous, key);
        if (removed >= 0) {
          start = Math.min(removed, tabs.size());
          for (var index = removed + 1; index < previous.size(); index++) {
            var successor = indexOf(tabs, previous.get(index).key());
            if (successor >= 0) {
              start = successor;
              break;
            }
          }
        }
      }
      for (var index = start; index < tabs.size(); index++) {
        if (tabs.get(index).enabled()) {
          return tabs.get(index).key();
        }
      }
      for (var index = start - 1; index >= 0; index--) {
        if (tabs.get(index).enabled()) {
          return tabs.get(index).key();
        }
      }
      return null;
    }

    private static int indexOf(List<Tab> tabs, @Nullable String key) {
      for (var index = 0; index < tabs.size(); index++) {
        if (tabs.get(index).key().equals(key)) {
          return index;
        }
      }
      return -1;
    }

    private void click(JSObject event) {
      if (!Dom.flag(event, "defaultPrevented")) {
        var tab = eventTab(event);
        if (tab != null) {
          select(tab);
        }
      }
    }

    private @Nullable Tab eventTab(JSObject event) {
      var button = WidgetDom.closest(event, TAB);
      return tabs.stream()
          .filter(tab -> tab.enabled() && WidgetDom.same(tab.button(), button))
          .findFirst()
          .orElse(null);
    }

    private void navigate(JSObject event) {
      if (WidgetDom.skipKey(event)) {
        return;
      }
      var from = eventTab(event);
      if (from == null) {
        return;
      }
      var enabled = tabs.stream().filter(Tab::enabled).toList();
      var index = indexOf(enabled, from.key());
      var next =
          switch (Dom.eventString(event, "key")) {
            case "Home" -> 0;
            case "End" -> enabled.size() - 1;
            case "ArrowLeft" ->
                Math.floorMod(index + (WidgetDom.rtl(node) ? 1 : -1), enabled.size());
            case "ArrowRight" ->
                Math.floorMod(index + (WidgetDom.rtl(node) ? -1 : 1), enabled.size());
            default -> -1;
          };
      if (next < 0) {
        return;
      }

      Dom.preventDefault(event);
      var target = enabled.get(next);
      roving = target.key();
      Dom.focus(target.button());
      if (!WidgetDom.attribute(node, "data-activation").equals("manual")) {
        select(target);
      } else {
        apply();
      }
    }

    private void select(Tab tab) {
      roving = tab.key();
      if (!Objects.equals(selected, tab.key())) {
        if (!binding.controlled()) {
          selected = tab.key();
        }
        apply();
        try {
          binding.request().accept(tab.key());
        } finally {
          root.invalidate();
        }
      } else {
        apply();
      }
    }

    private void apply() {
      for (var tab : tabs) {
        var active = tab.key().equals(selected);
        Dom.attribute(tab.button(), "aria-selected", Boolean.toString(active));
        Dom.attribute(tab.button(), "tabindex", tab.key().equals(roving) ? "0" : "-1");
        WidgetDom.hidden(tab.panel(), !active);
      }
    }
  }
}
