package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import dev.nthings.tessera.wasm.MountOptions;
import java.util.ArrayList;
import java.util.List;

/// A view whose next render can be invalid in several ways, plus coalesced and runaway invalidation
/// and failing handlers and updates. A rejected render must leave the rows and the draft as they
/// were, and the view must recover on the next valid render.
final class SchedulingDemo {
  private int renders;
  private boolean runaway;
  private boolean duplicate;
  private boolean fail;
  private boolean rejectWithEffect;
  private int effectStarts;
  private int partialUpdates;

  BrowserRoot mount(String host) {
    return BrowserRoot.mount(
        host, this::view, MountOptions.DEFAULT.withScheduling(new BrowserRoot.Scheduling(2, 8)));
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    renders++;
    if (runaway) {
      root.invalidate();
    }
    var rows = new ArrayList<>(List.of("Alpha", "Beta", "Gamma"));
    if (duplicate) {
      rows.add("Alpha");
    }
    var list = LiveElement.of(Elements.ul().ariaLabel("Rows"));
    rows.forEach(row -> list.child(LiveContent.of(Elements.li().text(row).key(row))));
    var section =
        LiveElement.of(
                Elements.section()
                    .ariaLabel("Render checks")
                    .children(
                        Elements.h2().text("Render checks"),
                        Elements.p().role(Role.STATUS).text("Renders: " + renders),
                        Elements.p().text("Effects started: " + effectStarts),
                        Elements.p().text("Changes before a failure: " + partialUpdates),
                        Field.of("Draft", Forms.input("draft", InputType.TEXT))))
            .child(list);
    if (fail) {
      fail = false;
      section.child(LiveContent.of(Elements.div().child(new FailingPanel())));
    }
    if (rejectWithEffect) {
      rejectWithEffect = false;
      // Rejected for its duplicate keys: its effect must never start.
      section
          .child(Live.effect("audit", _ -> effectStarts++))
          .child(LiveContent.of(Elements.div().key("audit")));
    }
    return section.children(
        Ui.button(
            "Refresh five times",
            () -> {
              for (var index = 0; index < 5; index++) {
                root.invalidate();
              }
            }),
        Ui.button("Start runaway", root.update(() -> runaway = true)),
        Ui.button("Stop runaway", root.update(() -> runaway = false)),
        Ui.button("Duplicate a row", root.update(() -> duplicate = true)),
        Ui.button("Remove duplicate", root.update(() -> duplicate = false)),
        Ui.button("Fail next render", root.update(() -> fail = true)),
        Ui.button("Reject render with effect", root.update(() -> rejectWithEffect = true)),
        Ui.button("Refresh", root.update(() -> {})),
        // The change is applied before the failure, so it still renders.
        Ui.button(
            "Fail during update",
            root.update(() -> {
              partialUpdates++;
              throw new IllegalStateException("Simulated update failure");
            })),
        Ui.button(
            "Throw from handler",
            () -> {
              throw new IllegalStateException("Simulated handler failure");
            }));
  }

  private record FailingPanel() implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      throw new IllegalStateException("Simulated render failure");
    }
  }
}
