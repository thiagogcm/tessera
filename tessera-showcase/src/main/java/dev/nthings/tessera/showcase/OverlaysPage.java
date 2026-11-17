package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.BulletList;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.TextWrap;

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
        Catalog.MODAL.documentation(),
        drawer(),
        Catalog.POPOVER.documentation(),
        tooltip(),
        searchPalette());
  }

  private static Demo drawer() {
    var start =
        Drawer.of("Filters")
            .id("demo-drawer-start")
            .side(Drawer.Side.INLINE_START)
            .description("Slides in from the start edge")
            .body(BulletList.of("Status", "Owner", "Region"));
    var end =
        Drawer.of("Details")
            .id("demo-drawer-end")
            .description("Slides in from the end edge")
            .body(p().text("Drawers are modal dialogs docked to an edge."));
    return Demo.of("drawer", "Drawer")
        .summary("Edge-docked dialogs that slide in with translate transitions and allow-discrete.")
        .features("transition-behavior: allow-discrete", "overlay", "translate")
        .example(
            div()
                .flex()
                .gap(CssLength.rem(0.75))
                .children(
                    Button.of("Open start drawer")
                        .command(start.dialogRef(), BuiltinCommand.SHOW_MODAL),
                    Button.of("Open end drawer")
                        .command(end.dialogRef(), BuiltinCommand.SHOW_MODAL)),
            deferred(start, 10),
            deferred(end, 10))
        .code(
            "var drawer = Drawer.of(\"Filters\").side(Drawer.Side.INLINE_START);",
            "Button.of(\"Open drawer\").command(drawer.dialogRef(), BuiltinCommand.SHOW_MODAL);");
  }

  private static Demo tooltip() {
    return Demo.of("tooltip", "Interest tooltip")
        .summary(
            "Hover or focus the button: interestfor shows a hint popover after a short delay, "
                + "anchored above the button and flipped when space runs out.")
        .features("interestfor", "popover=\"hint\"", "position-try-fallbacks")
        .example(
            Button.of("Hover me")
                .interestTooltip(tip ->
                    Demo.surface(tip.p(CssLength.rem(0.5)), CssLength.rem(0.375))
                        .text("Saved 2 minutes ago")))
        .code("Button.of(\"Hover me\").interestTooltip(tip -> tip.text(\"Saved 2 minutes ago\"));");
  }

  private static Demo searchPalette() {
    return Demo.of("search-palette", "Search palette")
        .summary(
            "The header's search button opens a command palette dialog with grouped results and "
                + "live filtering and keyboard selection inside a <search> landmark.")
        .features("<search>", "automatic ES module", "accesskey", "<dialog>")
        .example(
            p().textWrap(TextWrap.PRETTY)
                .children(
                    Formatting.inlineCode("Search"),
                    text(" in the header, or press "),
                    Formatting.keybinding("Alt+K"),
                    text(". Every page and demo on this site is indexed.")))
        .code(
            "var search = SearchPalette.of().placeholder(\"Search…\").triggerLabel(\"Search\")",
            "    .item(\"Pages\", \"Forms\", \"/forms\");",
            "header.children(search.triggerButton(), Elements.deferred(search, 30));");
  }
}
