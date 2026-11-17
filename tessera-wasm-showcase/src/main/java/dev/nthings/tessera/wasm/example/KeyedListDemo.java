package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.wasm.BrowserRoot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

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

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    var version = revision;
    var entries = new ArrayList<>(keys);
    if (duplicate && !entries.isEmpty()) {
      entries.add(entries.getFirst());
    }
    return Elements.aside()
        .ariaLabel("Keyed list example")
        .p(Tokens.SPACE_6)
        .children(
            Elements.h2().text("Keyed entries"),
            Elements.p()
                .attr("data-list-state", "")
                .text("Revision: " + revision + "; choices: " + choices + "; last: " + lastChoice),
            Elements.div()
                .flex()
                .gap(Tokens.SPACE_2)
                .children(
                    Button.of("Prepend entry")
                        .onClick(root.update(() -> keys.addFirst("item-" + nextKey++))),
                    Button.of("Reverse entries")
                        .onClick(root.update(() -> Collections.reverse(keys))),
                    Button.of("Remove first entry")
                        .onClick(
                            root.update(() -> {
                              if (!keys.isEmpty()) {
                                keys.removeFirst();
                              }
                            })),
                    Button.of("Refresh entries").onClick(root.update(() -> revision++)),
                    Button.of("Toggle details").onClick(root.update(() -> details = !details)),
                    Button.of("Replace entry type")
                        .onClick(root.update(() -> alternate = !alternate)),
                    Button.of("Duplicate keys").onClick(root.update(() -> duplicate = true)),
                    Button.of("Recover entries").onClick(root.update(() -> duplicate = false))),
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

  /// The same content can be compiled statically by omitting the Java callback.
  public static Element content(
      List<String> keys, boolean details, boolean alternate, @Nullable Consumer<String> choose) {
    var section =
        Elements.section()
            .ariaLabel("Editable entries")
            .attr("data-entries", "")
            .flex()
            .flexCol()
            .gap(Tokens.SPACE_2);
    section.child(
        Field.of("Collection name", Forms.text("collection-name")).hint("A shared labelled field"));
    for (var key : keys) {
      IntoElement row =
          alternate && key.equals("b") ? new ReplacementEntry(key, choose) : new Entry(key, choose);
      section.child(row.key(key));
    }
    if (details) {
      section.child(
          Elements.fragment()
              .children(
                  Elements.p().text("Additional details"),
                  Elements.p().text("Rows remain independent"))
              .key("details"));
    }
    return section;
  }

  private record Entry(String key, @Nullable Consumer<String> choose) implements Component {
    @Override
    public IntoElement expand(DocumentContext context) {
      return entry(key, choose, "original");
    }
  }

  private record ReplacementEntry(String key, @Nullable Consumer<String> choose)
      implements Component {
    @Override
    public IntoElement expand(DocumentContext context) {
      return entry(key, choose, "replacement");
    }
  }

  private static Element entry(String key, @Nullable Consumer<String> choose, String variant) {
    var hint = ElementRef.scoped("hint");
    var input =
        Forms.text("entry-" + key)
            .id(ElementRef.scoped("value"))
            .attr("data-entry", key)
            .attr("data-variant", variant)
            .ariaDescribedBy(hint);
    var button = Button.of("Choose " + key);
    if (choose != null) {
      button.onClick(() -> choose.accept(key));
    }
    return Elements.fragment()
        .children(
            Elements.label().labelFor(input.ref()).text("Entry " + key), input,
            Elements.span().id(hint).text("Description for " + key), button);
  }
}
