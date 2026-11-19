package dev.nthings.tessera.wasm;

import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.components.WidgetBridge.SearchLink;
import dev.nthings.tessera.internal.components.WidgetBridge.SearchParts;
import dev.nthings.tessera.internal.components.WidgetBridge.SearchResult;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Search palette with a browser-owned query, matching results and keyboard navigation.
public final class LiveSearchPalette extends BuiltinLiveComponent {
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
  LiveContent expand(LiveContext context) {
    return new WidgetScope(
        WidgetBridge.scope(palette),
        scope -> {
          var parts = WidgetBridge.search(palette, scope);
          return LiveElement.of(parts.host()).widget(new Binding(parts));
        });
  }

  private record Binding(SearchParts parts) implements WidgetBinding {
    @Override
    public WidgetController create(JSObject node, BrowserRoot root) {
      return new Controller(node, root, parts);
    }
  }

  /// A resolved result, its normalized search text and the `hidden` state last written to it.
  private static final class Entry {
    final SearchLink link;
    final JSObject node;
    final String text;
    private @Nullable Boolean hidden;

    Entry(SearchLink link, JSObject node) {
      this.link = link;
      this.node = node;
      this.text =
          normalize(
              Stream.of(link.group(), link.title(), link.description(), link.badge())
                  .filter(Objects::nonNull)
                  .collect(Collectors.joining(" ")));
    }

    /// Hides or shows the result; returns whether that changed what the DOM shows.
    boolean hide(boolean hide) {
      if (Objects.equals(hidden, hide)) {
        return false;
      }
      WidgetDom.hidden(node, hide);
      hidden = hide;
      return true;
    }
  }

  private static final class Controller extends WidgetController {
    private List<SearchLink> links;
    private JSObject input;
    private JSObject empty;
    private @Nullable Boolean emptyHidden;
    private List<Entry> entries;
    private List<Entry> visible = List.of();
    private @Nullable Entry focusedBeforePatch;

    Controller(JSObject node, BrowserRoot root, SearchParts parts) {
      super(node, root);
      links = links(parts);
      input = root.resolve(parts.input());
      empty = root.resolve(parts.empty());
      entries = entries(parts);
      listen(
          node,
          "input",
          event -> {
            if (WidgetDom.same(input, WidgetDom.target(event))) {
              filter(null);
            }
          });
      listen(node, "keydown", this::navigate);
      listen(node, "click", this::click);
      listen(
          node,
          "toggle",
          event -> {
            if (Dom.eventString(event, "newState").equals("open")) {
              filter(null);
            }
          });
      filter(null);
    }

    @Override
    void beforePatch() {
      focusedBeforePatch = focusedItem(visible, entry -> entry.node);
    }

    @Override
    void update(WidgetBinding binding) {
      var parts = ((Binding) binding).parts();
      var before = focusToRestore(focusedBeforePatch, entry -> entry.node);
      focusedBeforePatch = null;
      var next = links(parts);
      if (next.equals(links)) {
        // The patch kept every result and the query, so only focus can need restoring.
        if (before != null) {
          WidgetDom.focus(before.node);
        }
        return;
      }
      links = next;
      input = root.resolve(parts.input());
      empty = root.resolve(parts.empty());
      emptyHidden = null;
      entries = entries(parts);
      filter(before);
    }

    private static List<SearchLink> links(SearchParts parts) {
      return parts.results().stream().map(SearchResult::link).toList();
    }

    private List<Entry> entries(SearchParts parts) {
      return parts.results().stream()
          .map(result -> new Entry(result.link(), root.resolve(result.ref())))
          .toList();
    }

    /// Shows the results that match every query term. Focus returns to `previous`, the result
    /// focused before a patch, or to a result like it; it leaves a result that becomes hidden.
    private void filter(@Nullable Entry previous) {
      var terms =
          Arrays.stream(SPACE.split(normalize(Dom.value(input))))
              .filter(term -> !term.isEmpty())
              .toList();
      var matches = new ArrayList<Entry>();
      var focusHidden = false;
      for (var entry : entries) {
        var match = terms.stream().allMatch(entry.text::contains);
        if (match) {
          matches.add(entry);
        }
        // Only a result that was visible can have focus.
        if (entry.hide(!match) && !match && Dom.isActiveElement(entry.node)) {
          focusHidden = true;
        }
      }
      visible = matches;
      var none = visible.isEmpty();
      if (!Objects.equals(emptyHidden, !none)) {
        WidgetDom.hidden(empty, !none);
        emptyHidden = !none;
      }
      if (previous != null) {
        var sameLink = matches.stream().filter(entry -> entry.link.equals(previous.link)).toList();
        var retained =
            sameLink.stream()
                .filter(entry -> WidgetDom.same(entry.node, previous.node))
                .findFirst()
                .orElse(sameLink.isEmpty() ? null : sameLink.getFirst());
        WidgetDom.focus(retained == null ? firstMatchOrInput() : retained.node);
      } else if (focusHidden) {
        WidgetDom.focus(firstMatchOrInput());
      }
    }

    private JSObject firstMatchOrInput() {
      return visible.isEmpty() ? input : visible.getFirst().node;
    }

    private int visibleIndex(@Nullable JSObject node) {
      for (var index = 0; index < visible.size(); index++) {
        if (WidgetDom.same(visible.get(index).node, node)) {
          return index;
        }
      }
      return -1;
    }

    private void navigate(JSObject event) {
      if (WidgetDom.skipKey(event)) {
        return;
      }
      var target = WidgetDom.target(event);
      var fromInput = WidgetDom.same(input, target);
      var index = fromInput ? -1 : visibleIndex(target);
      if (!fromInput && index < 0) {
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
              WidgetDom.click(visible.getFirst().node);
            }
          }
        }
        case "ArrowDown", "ArrowUp" -> {
          if (!visible.isEmpty()) {
            Dom.preventDefault(event);
            var forward = key.equals("ArrowDown");
            var next =
                index < 0
                    ? (forward ? 0 : visible.size() - 1)
                    : Math.floorMod(index + (forward ? 1 : -1), visible.size());
            Dom.focus(visible.get(next).node);
          }
        }
        default -> {}
      }
    }

    private void click(JSObject event) {
      if (!WidgetDom.skipKey(event)
          && !Dom.flag(event, "shiftKey")
          && Dom.number(event, "button") == 0
          && visibleIndex(WidgetDom.closest(event, "a[href]")) >= 0) {
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
