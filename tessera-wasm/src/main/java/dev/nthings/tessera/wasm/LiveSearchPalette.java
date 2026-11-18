package dev.nthings.tessera.wasm;

import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.view.WidgetAccess;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Search palette with a browser-owned query, matching results and keyboard navigation.
public final class LiveSearchPalette implements LiveComponent {
  private static final String INPUT = ".palette-input";
  private static final String ITEM = ".palette-item";
  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern SPACE = Pattern.compile("(?U)\\s+");

  private final SearchPalette palette;

  private LiveSearchPalette(SearchPalette palette) {
    this.palette = palette;
  }

  /// Adapts the portable palette's result structure and styles.
  public static LiveSearchPalette of(SearchPalette palette) {
    return new LiveSearchPalette(palette);
  }

  /// Creates the palette's native modal invoker.
  public TagElement triggerButton() {
    return palette.triggerButton();
  }

  @Override
  public LiveContent expand(LiveContext context) {
    var scope = WidgetAccess.scope(palette);
    return new WidgetScope(
        scope, inner -> LiveElement.of(WidgetAccess.search(palette, inner)).widget(Binding.SEARCH));
  }

  private enum Binding implements WidgetBinding {
    SEARCH;

    @Override
    public WidgetController create(JSObject node, BrowserRoot root) {
      return new Controller(node, root);
    }
  }

  private record Result(
      String group,
      String title,
      String href,
      @Nullable String description,
      @Nullable String badge) {}

  private record Entry(JSObject node, Result result, String text) {}

  private static final class Controller extends WidgetController {
    private @Nullable JSObject input;
    private @Nullable JSObject empty;
    private List<Entry> entries = List.of();
    private List<Entry> visible = List.of();
    private @Nullable Entry focusedBeforePatch;
    private boolean initialized;

    Controller(JSObject node, BrowserRoot root) {
      super(node, root);
    }

    @Override
    void beforePatch() {
      var active = Dom.activeElement();
      focusedBeforePatch =
          visible.stream()
              .filter(entry -> WidgetDom.same(entry.node(), active))
              .findFirst()
              .orElse(null);
    }

    @Override
    void update(WidgetBinding binding) {
      input = WidgetDom.child(node, INPUT);
      empty = WidgetDom.child(node, ".palette-empty");
      var next = new ArrayList<Entry>();
      for (var group : WidgetDom.children(node, ".palette-group")) {
        var title = WidgetDom.text(WidgetDom.child(group, ".palette-group-title"));
        for (var item : WidgetDom.children(group, ITEM)) {
          var content = WidgetDom.text(item);
          var result =
              new Result(
                  title,
                  WidgetDom.text(WidgetDom.child(item, ".palette-item-title")),
                  WidgetDom.attribute(item, "href"),
                  WidgetDom.text(item, ".palette-item-desc"),
                  WidgetDom.text(item, ".palette-item-badge"));
          next.add(new Entry(item, result, normalize(title + " " + content)));
        }
      }
      entries = next;
      if (!initialized) {
        initialized = true;
        listen(
            node,
            "input",
            event -> {
              if (WidgetDom.same(input, WidgetDom.closest(event, INPUT))) {
                filter();
              }
            });
        listen(node, "keydown", this::navigate);
        listen(node, "click", this::click);
        listen(
            node,
            "toggle",
            event -> {
              if (Dom.eventString(event, "newState").equals("open")) {
                filter();
              }
            });
      }
      var previous = focusedBeforePatch;
      focusedBeforePatch = null;
      var active = Dom.activeElement();
      var restore =
          previous != null
              && (WidgetDom.same(previous.node(), active) || WidgetDom.noFocus(active));
      filter(restore ? previous : null);
    }

    private void filter() {
      filter(null);
    }

    private void filter(@Nullable Entry previous) {
      var search = input;
      var status = empty;
      if (search == null || status == null) {
        return;
      }
      var terms =
          Arrays.stream(SPACE.split(normalize(Dom.value(search))))
              .filter(term -> !term.isEmpty())
              .toList();
      var matches = new ArrayList<Entry>();
      var active = Dom.activeElement();
      var focusHidden = false;
      for (var entry : entries) {
        var item = entry.node();
        var match = terms.stream().allMatch(entry.text()::contains);
        WidgetDom.hidden(item, !match);
        if (match) {
          matches.add(entry);
        } else if (WidgetDom.same(item, active)) {
          focusHidden = true;
        }
      }
      visible = matches;
      WidgetDom.hidden(status, !visible.isEmpty());
      if (previous != null) {
        var sameResult =
            matches.stream().filter(entry -> entry.result().equals(previous.result())).toList();
        var retained =
            sameResult.stream()
                .filter(entry -> WidgetDom.same(entry.node(), previous.node()))
                .findFirst()
                .orElse(sameResult.isEmpty() ? null : sameResult.getFirst());
        focus(retained == null ? firstMatchOr(search) : retained.node());
      } else if (focusHidden) {
        focus(firstMatchOr(search));
      }
    }

    private JSObject firstMatchOr(JSObject fallback) {
      return visible.isEmpty() ? fallback : visible.getFirst().node();
    }

    private static void focus(JSObject target) {
      if (!Dom.isActiveElement(target)) {
        Dom.focus(target);
      }
    }

    private void navigate(JSObject event) {
      if (WidgetDom.skipKey(event)) {
        return;
      }
      var item = WidgetDom.closest(event, ITEM);
      var fromInput = WidgetDom.same(input, WidgetDom.closest(event, INPUT));
      if (!fromInput && item == null) {
        return;
      }
      var key = Dom.eventString(event, "key");
      switch (key) {
        case "Escape" -> {
          Dom.preventDefault(event);
          WidgetDom.requestClose(node);
        }
        case "Enter" -> {
          if (fromInput) {
            Dom.preventDefault(event);
            if (!visible.isEmpty()) {
              WidgetDom.click(visible.getFirst().node());
            }
          }
        }
        case "ArrowDown", "ArrowUp" -> {
          if (!visible.isEmpty()) {
            Dom.preventDefault(event);
            var index = -1;
            for (var current = 0; current < visible.size(); current++) {
              if (WidgetDom.same(visible.get(current).node(), item)) {
                index = current;
                break;
              }
            }
            var forward = key.equals("ArrowDown");
            var next =
                index < 0
                    ? (forward ? 0 : visible.size() - 1)
                    : Math.floorMod(index + (forward ? 1 : -1), visible.size());
            Dom.focus(visible.get(next).node());
          }
        }
        default -> {}
      }
    }

    private void click(JSObject event) {
      if (!WidgetDom.skipKey(event)
          && !Dom.flag(event, "shiftKey")
          && Dom.number(event, "button") == 0
          && WidgetDom.closest(event, ITEM) != null) {
        WidgetDom.requestClose(node);
      }
    }

    @Override
    void dispose() {
      if (Dom.flag(node, "open")) {
        Dom.closeDialog(node, "");
      }
    }
  }

  private static String normalize(String input) {
    return MARKS
        .matcher(Normalizer.normalize(input, Normalizer.Form.NFKD))
        .replaceAll("")
        .toLowerCase(Locale.ROOT);
  }
}
