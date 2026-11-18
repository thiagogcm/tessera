package dev.nthings.tessera.wasm.example;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.ContentTabs.Activation;
import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.Select;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveDialog;
import dev.nthings.tessera.wasm.LiveDialog.Mode;
import dev.nthings.tessera.wasm.LiveElement;
import dev.nthings.tessera.wasm.LiveField;
import dev.nthings.tessera.wasm.LiveSearchPalette;
import dev.nthings.tessera.wasm.LiveTabs;
import dev.nthings.tessera.wasm.LiveTooltip;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Fixtures for browser-owned and app-bound widgets, all mounted in the compiled image.
final class WidgetDemo {
  private final ElementRef dialog = ElementRef.scoped("adaptive-modal");
  private final ElementRef drawerRef = ElementRef.scoped("adaptive-drawer");
  private final ElementRef fallback = ElementRef.scoped("return-target");
  private int tabsStep;
  private boolean bindTabs;
  private @Nullable String active = "left";
  private String requested = "";
  private @Nullable String region;
  private boolean invalid;
  private boolean modalOpen;
  private boolean rejectDismissal;
  private boolean showInvoker = true;
  private boolean showModal = true;
  private int dismissals;
  private int updates;
  private boolean nonModalInvoker;
  private boolean initialDrawer;
  private boolean tip = true;

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new WidgetDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var tabs = ContentTabs.of("Views").id("views").initialSelected("center");
    if (tabsStep == 0 || tabsStep > 4) {
      tabs.tab("before", "Before", p().text("Before panel"));
    }
    if (tabsStep < 2) {
      tabs.tab("center", "Center", p().text("Center panel"));
    }
    if (tabsStep < 4) {
      tabs.disabledTab("disabled", "Disabled", p().text("Disabled panel"));
    }
    if (tabsStep < 3 || tabsStep == 5) {
      tabs.tab("after", "After", p().text("After panel"));
    }
    var manual =
        LiveTabs.of(
                ContentTabs.of("Controlled views")
                    .id("controlled-views")
                    .activation(Activation.MANUAL)
                    .tab("left", "Left", p().text("Left panel"))
                    .tab("right", "Right", p().text("Right panel")))
            .bindSelection(Optional.ofNullable(active))
            .onSelectionRequest(key -> {
              requested = key;
              if (bindTabs) {
                active = key;
              }
            });

    var field =
        LiveField.of(
                Field.of(
                        "Region", Select.of("region").option("ca", "Canada").option("br", "Brazil"))
                    .hint("Choose a region")
                    .id("region"),
                control -> {
                  if (region != null) {
                    control.bindValue(region);
                  }
                  control.onValueChange(value -> region = value);
                })
            .validation(invalid, "Region is unavailable");

    var modal =
        LiveDialog.of(Modal.of("Adaptive modal").dialogRef(dialog).body(p().text("Modal body")))
            .bindOpen(modalOpen)
            .onOpenRequest(() -> modalOpen = true)
            .onDismiss(() -> {
              dismissals++;
              if (!rejectDismissal) {
                modalOpen = false;
              }
            })
            .focusFallback(fallback);
    var drawer =
        LiveDialog.of(
            Drawer.of("Adaptive drawer").dialogRef(drawerRef).body(p().text("Drawer body")));
    if (nonModalInvoker) {
      drawer.mode(Mode.NON_MODAL);
    }
    var palette = SearchPalette.of().triggerLabel("Find pages");
    if (updates > 0) {
      palette.item("Guides", "Café three", "#widgets");
    }
    palette.item("Guides", "Café one", "#widgets");
    if (updates < 2) {
      palette.item("Guides", "Café two", "#widgets");
    }
    palette.item("Reference", "API", "#widgets");

    var first = SearchPalette.Item.of("Shared page", "#widgets").description("a").badge("bc");
    var second = SearchPalette.Item.of("Shared page", "#widgets").description("ab").badge("c");
    var duplicates = updates % 2 == 0 ? List.of(first, second) : List.of(second, first);
    duplicates.forEach(item -> palette.item("Duplicates", item));
    var search = LiveSearchPalette.of(palette);

    var host =
        LiveElement.of(div())
            .child(LiveTabs.of(tabs))
            .child(manual)
            .child(Ui.button("Change tabs", root.update(() -> tabsStep++)))
            .child(Ui.button("Accept tab requests", root.update(() -> bindTabs = true)))
            .child(Ui.button("Supply invalid selection", root.update(() -> active = "absent")))
            .child(Ui.button("Reset selection", root.update(() -> active = "left")))
            .child(div().attr("data-tab-request", requested))
            .child(field)
            .child(
                LiveField.of(
                        Field.of("Required name", Forms.text("required-name").boolAttr("required"))
                            .id("required-name"),
                        _ -> {})
                    .validation(invalid, "Enter a name"))
            .child(Ui.button("Toggle region error", root.update(() -> invalid = !invalid)))
            .child(
                Ui.button(
                    "Toggle dismissal rejection",
                    root.update(() -> rejectDismissal = !rejectDismissal)))
            .child(Ui.button("Remove modal invoker", root.update(() -> showInvoker = false)))
            .child(Ui.button("Remove modal", root.update(() -> showModal = false)))
            .child(Ui.button("Open modal from app", root.update(() -> modalOpen = true)))
            .child(Elements.button().id(fallback).text("Return focus here"))
            .child(div().attr("data-dismissals", Integer.toString(dismissals)))
            .child(Ui.button("Show drawer through host", () -> root.host().show(drawerRef)))
            .child(Ui.button("Use non-modal invoker", root.update(() -> nonModalInvoker = true)))
            .child(Ui.button("Mount open drawer", root.update(() -> initialDrawer = true)))
            .child(drawer.triggerButton("Open adaptive drawer"))
            .child(drawer)
            .child(search.triggerButton())
            .child(search)
            .child(div().attr("data-widget-updates", Integer.toString(updates)))
            .child(Ui.button("Update widgets", root.update(() -> updates++)))
            .child(Ui.button("Remove tooltip", root.update(() -> tip = false)))
            .child(Ui.button("Close widgets root", root::close));
    if (initialDrawer) {
      var preview =
          LiveDialog.of(Drawer.of("Initially open drawer").id("initial").opened()).mode(Mode.MODAL);
      host.child(preview.triggerButton("Reopen drawer modally")).child(preview);
    }
    if (showModal) {
      if (showInvoker) {
        host.child(modal.triggerButton("Open adaptive modal"));
      }
      host.child(modal);
    }
    if (tip) {
      host.child(
          LiveTooltip.of(
              Elements.button()
                  .interestTooltip(hint -> hint.text("More search details"))
                  .text("Search tip")));
    }
    return host;
  }
}
