package dev.nthings.tessera.wasm;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.view.WidgetAccess;
import dev.nthings.tessera.view.WidgetAccess.TabsParts;
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
    var controlled = this.controlled;
    var selection = selected;
    var request = this.request;
    return new WidgetScope(
        WidgetAccess.scope(tabs),
        scope -> {
          var parts = WidgetAccess.tabs(tabs, scope, controlled, selection);
          return LiveElement.of(parts.host())
              .widget(new Binding(parts, controlled, selection, request));
        });
  }

  private record Binding(
      TabsParts parts, boolean controlled, @Nullable String selection, Consumer<String> request)
      implements WidgetBinding {
    @Override
    public WidgetController create(JSObject node, BrowserRoot root) {
      return new Controller(node, root, this);
    }
  }

  private record Tab(String key, boolean enabled, JSObject button, JSObject panel) {}

  private static final class Controller extends WidgetController {
    private Binding binding;
    private List<Tab> tabs = List.of();
    private @Nullable String selected;
    private @Nullable String roving;
    private @Nullable Tab focusedBeforePatch;
    // The selection and tab stop the DOM shows. A changed model may have rewritten the rendered
    // attributes of any tab, so `stale` makes the next apply write every tab.
    private @Nullable String shownSelected;
    private @Nullable String shownRoving;
    private boolean stale = true;

    Controller(JSObject node, BrowserRoot root, Binding binding) {
      super(node, root);
      this.binding = binding;
      selected = binding.parts().selected();
      listen(node, "click", this::click);
      listen(node, "keydown", this::navigate);
      listen(
          node,
          "focusout",
          event -> {
            if (tabOf(WidgetDom.related(event)) == null) {
              roving = selected;
              apply();
            }
          });
    }

    @Override
    void beforePatch() {
      focusedBeforePatch = focusedItem(tabs, Tab::button);
    }

    @Override
    void update(WidgetBinding next) {
      var parts = ((Binding) next).parts();
      var previous = tabs;
      if (stale || changed(parts)) {
        var current = new ArrayList<Tab>(parts.tabs().size());
        for (var tab : parts.tabs()) {
          current.add(
              new Tab(
                  tab.key(), tab.enabled(), root.resolve(tab.tab()), root.resolve(tab.panel())));
        }
        tabs = current;
        stale = true;
      }
      binding = (Binding) next;
      if (binding.controlled()) {
        selected = binding.selection();
      } else if (!enabled(selected)) {
        selected = fallback(previous, selected);
      }

      var before = focusToRestore(focusedBeforePatch, Tab::button);
      focusedBeforePatch = null;
      roving = selected;
      if (before != null) {
        roving = enabled(before.key()) ? before.key() : fallback(previous, before.key());
      }
      apply();
      if (before != null) {
        var tab = tab(roving);
        if (tab != null) {
          WidgetDom.focus(tab.button());
        }
      }
    }

    /// Whether `parts` differ from the tabs of the previous binding. Equal parts render the same
    /// keyed elements with the same attributes, so the patch left them as [#apply] wrote them; a
    /// changed rendered selection means the patch rewrote the selection attributes.
    private boolean changed(TabsParts parts) {
      if (!Objects.equals(parts.selected(), binding.parts().selected())
          || parts.tabs().size() != tabs.size()) {
        return true;
      }
      for (var index = 0; index < tabs.size(); index++) {
        var tab = tabs.get(index);
        var rendered = parts.tabs().get(index);
        if (!tab.key().equals(rendered.key()) || tab.enabled() != rendered.enabled()) {
          return true;
        }
      }
      return false;
    }

    private @Nullable Tab tab(@Nullable String key) {
      return tabs.stream().filter(tab -> tab.key().equals(key)).findFirst().orElse(null);
    }

    private @Nullable Tab tabOf(@Nullable JSObject button) {
      if (button == null) {
        return null;
      }
      return tabs.stream()
          .filter(tab -> WidgetDom.same(tab.button(), button))
          .findFirst()
          .orElse(null);
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
      var tab = tabOf(WidgetDom.closest(event, "[role=tab]"));
      return tab != null && tab.enabled() ? tab : null;
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
      if (binding.parts().manual()) {
        apply();
      } else {
        select(target);
      }
    }

    private void select(Tab tab) {
      roving = tab.key();
      var changed = !Objects.equals(selected, tab.key());
      if (changed && !binding.controlled()) {
        selected = tab.key();
      }
      apply();
      if (changed) {
        notifyApplication(() -> binding.request().accept(tab.key()));
      }
    }

    /// Shows the selection and tab stop, writing only the tabs whose state changed.
    private void apply() {
      if (!stale
          && Objects.equals(shownSelected, selected)
          && Objects.equals(shownRoving, roving)) {
        return;
      }
      for (var tab : tabs) {
        var key = tab.key();
        var active = key.equals(selected);
        if (stale || active != key.equals(shownSelected)) {
          Dom.attribute(tab.button(), "aria-selected", Boolean.toString(active));
          WidgetDom.hidden(tab.panel(), !active);
        }
        var stop = key.equals(roving);
        if (stale || stop != key.equals(shownRoving)) {
          Dom.attribute(tab.button(), "tabindex", stop ? "0" : "-1");
        }
      }
      shownSelected = selected;
      shownRoving = roving;
      stale = false;
    }
  }
}
