package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveComponent;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/// Editable keyed siblings with wrapper-free rows and replaceable component definitions.
public final class KeyedListDemo {
  private final List<String> keys = new ArrayList<>(List.of("a", "b", "c"));
  private int nextKey = 4;
  private int revision;
  private int choices;
  private String lastChoice = "none";
  private boolean details;
  private boolean alternate;
  private boolean duplicate;

  private KeyedListDemo() {}

  public static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new KeyedListDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var version = revision;
    var entries = new ArrayList<>(keys);
    if (duplicate && !entries.isEmpty()) {
      entries.add(entries.getFirst());
    }
    return LiveElement.of(
            Elements.aside()
                .ariaLabel("Keyed list example")
                .p(Tokens.SPACE_6)
                .children(
                    Elements.h2().text("Keyed entries"),
                    Elements.p()
                        .attr("data-list-state", "")
                        .text(
                            "Revision: "
                                + revision
                                + "; choices: "
                                + choices
                                + "; last: "
                                + lastChoice)))
        .child(
            LiveElement.of(Elements.div().flex().gap(Tokens.SPACE_2))
                .children(
                    Ui.button(
                        "Prepend entry", root.update(() -> keys.addFirst("item-" + nextKey++))),
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
                    Ui.button("Replace entry type", root.update(() -> alternate = !alternate)),
                    Ui.button("Duplicate keys", root.update(() -> duplicate = true)),
                    Ui.button("Recover entries", root.update(() -> duplicate = false))))
        .child(
            content(
                entries,
                details,
                alternate,
                key ->
                    root.update(
                            () -> {
                              choices++;
                              lastChoice = key + "@" + version;
                            })
                        .run()));
  }

  /// The entries as portable content, which a static page compiles without Java callbacks. The
  /// browser root renders the same structure with live choose buttons.
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
      List<String> keys, boolean details, boolean alternate, Consumer<String> choose) {
    var section = LiveElement.of(section());
    for (var key : keys) {
      LiveContent row =
          alternate && key.equals("b")
              ? new LiveReplacementEntry(key, choose)
              : new LiveEntry(key, choose);
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
        .attr("data-entries", "")
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
      return Elements.fragment().children(fields(key, "original"), Button.of("Choose " + key));
    }
  }

  private record ReplacementEntry(String key) implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      return Elements.fragment().children(fields(key, "replacement"), Button.of("Choose " + key));
    }
  }

  private record LiveEntry(String key, Consumer<String> choose) implements LiveComponent {
    @Override
    public LiveContent expand(LiveContext context) {
      return Live.fragment(
          LiveContent.of(fields(key, "original")),
          Ui.button("Choose " + key, () -> choose.accept(key)));
    }
  }

  private record LiveReplacementEntry(String key, Consumer<String> choose)
      implements LiveComponent {
    @Override
    public LiveContent expand(LiveContext context) {
      return Live.fragment(
          LiveContent.of(fields(key, "replacement")),
          Ui.button("Choose " + key, () -> choose.accept(key)));
    }
  }

  /// The label, input and hint of one entry.
  private static Element fields(String key, String variant) {
    var hint = ElementRef.scoped("hint");
    var input =
        Forms.text("entry-" + key)
            .id(ElementRef.scoped("value"))
            .attr("data-entry", key)
            .attr("data-variant", variant)
            .ariaDescribedBy(hint);
    return Elements.fragment()
        .children(
            Elements.label().labelFor(input.ref()).text("Entry " + key),
            input,
            Elements.span().id(hint).text("Description for " + key));
  }
}
