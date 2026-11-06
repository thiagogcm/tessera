package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.components.Lists.BulletList;
import dev.nthings.tessera.components.Overlays;
import dev.nthings.tessera.components.Overlays.DrawerSide;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.css.CssLength;

/// Modal dialogs, drawers, popovers, interest tooltips, and the search palette.
final class OverlaysPage {

  private OverlaysPage() {}

  static Page page() {
    return Page.ofDemos(
        "/overlays",
        "Overlays",
        "Overlays & top layer",
        "Dialogs, drawers, popovers and tooltips rendered in the browser's top layer, opened by "
            + "declarative invoker commands and animated in and out with @starting-style.",
        modal(),
        drawer(),
        popover(),
        tooltip(),
        searchPalette());
  }

  private static Demo modal() {
    var modal =
        Overlays.modal("demo-modal", "Publish release?")
            .description("A native <dialog> opened with command=\"show-modal\".")
            .bodyText(
                "Press Escape or click the backdrop to dismiss — closedby=\"any\" handles both.")
            .closeLabel("Got it");
    return Demo.of("modal", "Modal dialog")
        .summary(
            "A modal <dialog> with light dismiss. The trigger is a plain button whose commandfor "
                + "is resolved by the compiler to the dialog's scoped id.")
        .features(
            "command=\"show-modal\"", "<dialog closedby=\"any\">", "@starting-style", "::backdrop")
        .example(Demo.row(modal.triggerButton("open-modal", "Open modal")), modal.asDeferred(10))
        .code(
            "var modal = Overlays.modal(\"publish\", \"Publish release?\").bodyText(\"…\");",
            "main().children(modal.triggerButton(\"open\", \"Open modal\"), modal.asDeferred(10));");
  }

  private static Demo drawer() {
    var start =
        Overlays.drawer("demo-drawer-start", "Filters", DrawerSide.INLINE_START)
            .subtitle("Slides in from the start edge")
            .body(BulletList.ofStrings("Status", "Owner", "Region"));
    var end =
        Overlays.drawer("demo-drawer-end", "Details", DrawerSide.INLINE_END)
            .subtitle("Slides in from the end edge")
            .body(p().text("Drawers are modal dialogs docked to an edge."));
    return Demo.of("drawer", "Drawer")
        .summary("Edge-docked dialogs that slide in with translate transitions and allow-discrete.")
        .features("transition-behavior: allow-discrete", "overlay", "translate")
        .example(
            div()
                .flex()
                .gap(CssLength.rem(0.75))
                .children(
                    start.triggerButton("open-start", "Open start drawer"),
                    end.triggerButton("open-end", "Open end drawer")),
            start.asDeferred(10),
            end.asDeferred(10))
        .code(
            "var drawer = Overlays.drawer(\"filters\", \"Filters\", DrawerSide.INLINE_START);",
            "drawer.triggerButton(\"open\", \"Open drawer\");");
  }

  private static Demo popover() {
    var panel = ElementRef.named("demo-popover");
    return Demo.of("popover", "Popover")
        .summary(
            "Any element can become a light-dismiss popover. The button references it through an "
                + "ElementRef, so the id link is checked at compile time.")
        .features("popover=\"auto\"", "command=\"toggle-popover\"", "ElementRef")
        .example(
            Demo.row(Demo.button("Toggle popover").command(panel, BuiltinCommand.TOGGLE_POPOVER)),
            Demo.surface(
                    div().id(panel).popover(PopoverMode.AUTO).p(CssLength.rem(1)),
                    CssLength.rem(0.75))
                .text("Click outside or press Escape to close."))
        .code(
            "var panel = ElementRef.named(\"panel\");",
            "button().command(panel, BuiltinCommand.TOGGLE_POPOVER).text(\"Toggle\");",
            "div().id(panel).popover(PopoverMode.AUTO).text(\"…\");");
  }

  private static Demo tooltip() {
    return Demo.of("tooltip", "Interest tooltip")
        .summary(
            "Hover or focus the button: interestfor shows a hint popover after a short delay, "
                + "anchored above the button and flipped when space runs out.")
        .features("interestfor", "popover=\"hint\"", "position-try-fallbacks")
        .example(
            Demo.row(
                Demo.button("Hover me")
                    .interestTooltip(
                        tip ->
                            Demo.surface(tip.p(CssLength.rem(0.5)), CssLength.rem(0.375))
                                .text("Saved 2 minutes ago"))))
        .code(
            "button().interestTooltip(tip -> tip.text(\"Saved 2 minutes ago\")).text(\"Hover me\");");
  }

  private static Demo searchPalette() {
    return Demo.of("search-palette", "Search palette")
        .summary(
            "The header's search button opens a command palette dialog with grouped results and "
                + "native <datalist> suggestions inside a <search> landmark.")
        .features("<search>", "<datalist>", "accesskey", "<dialog>")
        .example(
            p().textPretty()
                .children(
                    Formatting.inlineCode("Search"),
                    text(" in the header, or press "),
                    Formatting.keybinding("Alt+K"),
                    text(". Every page and demo on this site is indexed.")))
        .code(
            "var search = SearchPalette.create(\"search\", \"Search…\")",
            "    .item(\"Pages\", \"Forms\", \"/forms\", \"Inputs and validation\");",
            "header.children(search.triggerButton(\"search-btn\", \"Search\"), search.asDeferred(30));");
  }
}
