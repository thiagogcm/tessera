package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Events;
import dev.nthings.tessera.wasm.KeyboardEvent;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveComponent;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Editable keyed rows without wrapper elements. A live feed prepends entries every minute,
/// Alt+ArrowUp and Alt+ArrowDown move the focused row, and rows can be reversed, removed, chosen or
/// replaced by another component type. The static list route compiles the same rows.
public final class KeyedListDemo {
  private final List<String> keys = new ArrayList<>(List.of("a", "b", "c"));
  private int nextKey = 4;
  private int revision;
  private int choices;
  private String lastChoice = "none";
  private boolean details;
  private boolean alternate;
  private boolean feed;

  private KeyedListDemo() {}

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new KeyedListDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var version = revision;
    var actions = new Actions(root, version);
    return LiveElement.of(
            Elements.aside()
                .ariaLabel("Keyed list example")
                .p(Tokens.SPACE_6)
                .children(
                    Elements.h2().text("Keyed entries"),
                    Elements.p()
                        .role(Role.STATUS)
                        .text(
                            "Revision: "
                                + revision
                                + "; choices: "
                                + choices
                                + "; last: "
                                + lastChoice)))
        .child(
            feed
                ? Live.effect(
                    "feed",
                    lifetime ->
                        root.host()
                            .within(lifetime)
                            .interval(
                                Duration.ofMinutes(1), () -> keys.addFirst("item-" + nextKey++)))
                : Live.fragment())
        .child(
            LiveElement.of(Elements.div().flex().gap(Tokens.SPACE_2))
                .children(
                    LiveElement.of(Elements.label())
                        .child(
                            LiveElement.of(Elements.input(InputType.CHECKBOX))
                                .bindChecked(feed)
                                .onCheckedChange(value -> root.update(() -> feed = value).run()))
                        .text("Live feed"),
                    Ui.button("Reverse entries", root.update(() -> Collections.reverse(keys))),
                    Ui.button(
                        "Remove first entry",
                        root.update(() -> {
                          if (!keys.isEmpty()) {
                            keys.removeFirst();
                          }
                        })),
                    Ui.button("Refresh entries", root.update(() -> revision++)),
                    Ui.button("Toggle details", root.update(() -> details = !details)),
                    Ui.button("Replace entry type", root.update(() -> alternate = !alternate))))
        .child(content(keys, details, alternate, actions));
  }

  /// Row callbacks bound to the render that created them.
  private final class Actions {
    private final BrowserRoot root;
    private final int version;

    Actions(BrowserRoot root, int version) {
      this.root = root;
      this.version = version;
    }

    void choose(String key) {
      root.update(
              () -> {
                choices++;
                lastChoice = key + "@" + version;
              })
          .run();
    }

    void keydown(String key, KeyboardEvent event) {
      var step =
          !event.altKey()
              ? 0
              : switch (event.key()) {
                case "ArrowUp" -> -1;
                case "ArrowDown" -> 1;
                default -> 0;
              };
      var from = keys.indexOf(key);
      var to = from + step;
      if (step != 0 && to >= 0 && to < keys.size()) {
        event.preventDefault();
        root.update(() -> Collections.swap(keys, from, to)).run();
      }
    }
  }

  /// The entries as portable content, which a static page compiles without Java callbacks. The
  /// browser root renders the same structure with live inputs and choose buttons.
  public static Element content(List<String> keys, boolean details, boolean alternate) {
    var section = section();
    for (var key : keys) {
      IntoElement row = alternate && key.equals("b") ? new ReplacementEntry(key) : new Entry(key);
      section.child(row.key(key));
    }
    if (details) {
      section.child(details());
    }
    return section;
  }

  private static LiveContent content(
      List<String> keys, boolean details, boolean alternate, Actions actions) {
    var section = LiveElement.of(section());
    for (var key : keys) {
      LiveContent row =
          alternate && key.equals("b")
              ? new LiveReplacementEntry(key, actions)
              : new LiveEntry(key, actions);
      section.child(row.key(key));
    }
    if (details) {
      section.child(details());
    }
    return section;
  }

  private static TagElement section() {
    return Elements.section()
        .ariaLabel("Editable entries")
        .flex()
        .flexCol()
        .gap(Tokens.SPACE_2)
        .child(
            Field.of("Collection name", Forms.text("collection-name"))
                .hint("A shared labelled field"))
        // A bundled icon: structured SVG in the static page and in the browser root alike.
        .child(Callout.of(Callout.Kind.NOTE, Elements.p().text("Entries keep their state")));
  }

  private static Element details() {
    return Elements.fragment()
        .children(
            Elements.p().text("Additional details"), Elements.p().text("Rows remain independent"))
        .key("details");
  }

  private record Entry(String key) implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      var parts = new RowParts(key, "Description");
      return Elements.fragment()
          .children(parts.label(), parts.input(), parts.hint(), Button.of("Choose " + key));
    }
  }

  private record ReplacementEntry(String key) implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      var parts = new RowParts(key, "Replacement");
      return Elements.fragment()
          .children(parts.label(), parts.input(), parts.hint(), Button.of("Choose " + key));
    }
  }

  private record LiveEntry(String key, Actions actions) implements LiveComponent {
    @Override
    public LiveContent expand(LiveContext context) {
      return row(new RowParts(key, "Description"), actions);
    }
  }

  private record LiveReplacementEntry(String key, Actions actions) implements LiveComponent {
    @Override
    public LiveContent expand(LiveContext context) {
      return row(new RowParts(key, "Replacement"), actions);
    }
  }

  private static LiveContent row(RowParts parts, Actions actions) {
    var key = parts.key();
    return Live.fragment(
        LiveContent.of(parts.label()),
        LiveElement.of(parts.input()).on(Events.KEYDOWN, event -> actions.keydown(key, event)),
        LiveContent.of(parts.hint()),
        Ui.button("Choose " + key, () -> actions.choose(key)));
  }

  /// The label, input and hint of one entry, linked by refs created once per row.
  private record RowParts(String key, String kind, ElementRef value, ElementRef description) {
    RowParts(String key, String kind) {
      this(key, kind, ElementRef.scoped("value"), ElementRef.scoped("hint"));
    }

    TagElement label() {
      return Elements.label().labelFor(value).text("Entry " + key);
    }

    TagElement input() {
      return Forms.text("entry-" + key).id(value).ariaDescribedBy(description);
    }

    TagElement hint() {
      return Elements.span().id(description).text(kind + " for " + key);
    }
  }
}
