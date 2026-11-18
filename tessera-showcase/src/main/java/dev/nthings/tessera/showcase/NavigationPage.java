package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.Accordion;
import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.Carousel;
import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.components.Link;
import dev.nthings.tessera.components.NavTabs;
import dev.nthings.tessera.css.CssLength;
import java.util.List;

/// Breadcrumbs, tabs, accordions, carousels, dropdown menus, and pagers.
final class NavigationPage {

  private NavigationPage() {}

  static Page page() {
    return Page.ofDemos(
        "/navigation",
        "Navigation",
        "Navigation & disclosure",
        "Wayfinding and progressive disclosure built on native elements: links, radio groups, "
            + "<details>, scroll snapping, and popovers.",
        breadcrumbs(),
        navTabs(),
        contentTabs(),
        accordion(),
        carousel(),
        dropdown(),
        pager(),
        Catalog.OVERFLOW.documentation());
  }

  private static Demo breadcrumbs() {
    return Demo.of("breadcrumbs", "Breadcrumbs")
        .summary(
            "An ordered list in a labelled <nav>; separators are generated content with empty "
                + "alt text, so screen readers skip them, and the last item is aria-current.")
        .features("content: \"/\" / \"\"", "aria-current=\"page\"")
        .example(
            Breadcrumbs.of(
                    new Link("/", "Tessera"),
                    new Link("/navigation", "Navigation"),
                    new Link("/navigation#breadcrumbs", "Breadcrumbs"))
                .label("Breadcrumb example"))
        .code("Breadcrumbs.of(new Link(\"/\", \"Home\"), new Link(\"/docs\", \"Docs\"));");
  }

  private static Demo navTabs() {
    return Demo.of("nav-tabs", "Navigation tabs")
        .summary(
            "Link tabs for page-level navigation (the site header uses them). The active "
                + "underline is a single pseudo-element anchored to the current tab.")
        .features("anchor-name", "position-anchor", "anchor()", "aria-current")
        .example(
            NavTabs.of(
                    "Section navigation",
                    List.of(
                        new Link("/layout", "Layout"),
                        new Link("/navigation", "Navigation"),
                        new Link("/overlays", "Overlays")))
                .active("/navigation"))
        .code(
            "NavTabs.of(\"Section navigation\",",
            "        List.of(new Link(\"/layout\", \"Layout\"), new Link(\"/navigation\", \"Navigation\")))",
            "    .active(activePath);");
  }

  private static Demo contentTabs() {
    return Demo.of("content-tabs", "Content tabs")
        .summary(
            "In-page tab panels follow the ARIA tabs pattern with roving tabindex and arrow-key "
                + "navigation. HTML has no tab widget, so a page script switches the panels.")
        .features("role=tablist", "roving tabindex", "Arrow keys")
        .example(
            ContentTabs.of("Pipeline stages")
                .id("demo-tabs")
                .tab("java", "Java", p().text("Components are plain Java records and builders."))
                .tab("html", "HTML", p().text("Output is semantic, validated HTML."))
                .tab("css", "CSS", p().text("Styles compile into one layered stylesheet.")))
        .code(
            "ContentTabs.of(\"Pipeline stages\")",
            "    .tab(\"java\", \"Java\", p().text(\"…\"))",
            "    .tab(\"html\", \"HTML\", p().text(\"…\"));");
  }

  private static Demo accordion() {
    return Demo.of("accordion", "Accordion")
        .summary(
            "<details> elements sharing a name open exclusively; ::details-content animates to "
                + "auto height, and collapsed text stays findable with Ctrl+F. Each card below "
                + "builds its FAQ with the same code and id, and the two stay independent.")
        .features("<details name>", "::details-content", "interpolate-size", ":open")
        .example(
            Grid.autoFit(
                    CssLength.rem(18),
                    Card.of("Getting started")
                        .body(
                            faq()
                                .openItem(
                                    "Does it need JavaScript?", "No. Disclosure is native HTML.")
                                .item(
                                    "Can several items be open?", "Leave out exclusive() for that.")
                                .item(
                                    "Is hidden text searchable?",
                                    "Yes, find-in-page opens the matching item.")),
                    Card.of("Styling")
                        .body(
                            faq()
                                .openItem("Can I theme it?", "Yes, through the design tokens.")
                                .item("Does it animate?", "Only when motion is allowed.")))
                .gap(CssLength.rem(1)))
        .code(
            "Accordion.of().id(\"faq\").exclusive()",
            "    .openItem(\"Does it need JavaScript?\", \"No.\")",
            "    .item(\"Can several items be open?\", \"Leave out exclusive().\");");
  }

  /// An exclusive FAQ; every card calls this, so the ids repeat and their scopes keep them apart.
  private static Accordion faq() {
    return Accordion.of().id("faq").exclusive();
  }

  private static Demo carousel() {
    var platform =
        Carousel.of("Platform highlights")
            .id("slides")
            .slide("Scroll snapping", "Slides snap to the center of the track.")
            .slide("Scroll markers", "The dots are ::scroll-marker pseudo-elements.")
            .slide("Scroll buttons", "Previous and next are ::scroll-button() pseudo-elements.")
            .slide(
                "Snapped state",
                "@container scroll-state(snapped: x) highlights the current slide.")
            .slide("Keyboard", "Markers and buttons are focusable and work with the keyboard.");
    var specials =
        Carousel.of("Café specials")
            .id("slides")
            .slide("Crème brûlée", "Caramelized sugar over vanilla custard.")
            .slide("Thé à la menthe", "Green tea with fresh mint.")
            .slide("Pão de queijo", "Cheese bread, warm from the oven.")
            .slide("Smørrebrød", "Open sandwiches on rye.");
    return Demo.of("carousel", "Carousel")
        .summary(
            "A scroll-snap track whose pagination dots and previous/next buttons are generated by "
                + "the browser — no script, fully keyboard accessible. Each card holds its own "
                + "carousel, and each carousel's buttons scroll only its own track.")
        .features(
            "::scroll-marker",
            "::scroll-button()",
            "scroll-marker-group",
            ":target-current",
            "scroll-state(snapped)")
        .example(
            Grid.autoFit(
                    CssLength.rem(18),
                    Card.of("Platform").body(platform),
                    Card.of("Café").body(specials))
                .gap(CssLength.rem(1)))
        .code(
            "Carousel.of(\"Platform highlights\")",
            "    .slide(\"Scroll snapping\", \"Slides snap to the center.\")",
            "    .slide(\"Scroll markers\", \"Dots are ::scroll-marker.\");");
  }

  private static Demo dropdown() {
    return Demo.of("dropdown", "Dropdown")
        .summary(
            "A popover menu toggled by an invoker command, positioned next to its trigger with "
                + "anchor positioning and flipped when it would overflow.")
        .features(
            "popover", "command=\"toggle-popover\"", "position-area", "position-try-fallbacks")
        .example(
            Dropdown.of("Jump to")
                .id("demo-menu")
                .item(Dropdown.Item.link("/forms", "Forms").shortcut("G F"))
                .item(Dropdown.Item.link("/data", "Data & code").shortcut("G D"))
                .separator()
                .item(Dropdown.Item.link("/", "Overview")))
        .code(
            "Dropdown.of(\"Jump to\")",
            "    .item(Dropdown.Item.link(\"/forms\", \"Forms\").shortcut(\"G F\"))",
            "    .separator()",
            "    .item(Dropdown.Item.command(",
            "        \"open\", drawer.dialogRef(), BuiltinCommand.SHOW_MODAL, \"Inspect\"));");
  }

  private static Demo pager() {
    return Demo.of("pager", "Pager")
        .summary("Previous/next links between documents; every page of this site ends with one.")
        .features("<nav aria-label>", "rel-style links")
        .example(
            DocPager.of()
                .previous(new Link("/layout", "Layout"))
                .next(new Link("/overlays", "Overlays"))
                .label("Pager example"))
        .code(
            "DocPager.of()",
            "    .previous(new Link(\"/layout\", \"Layout\"))",
            "    .next(new Link(\"/overlays\", \"Overlays\"));");
  }
}
