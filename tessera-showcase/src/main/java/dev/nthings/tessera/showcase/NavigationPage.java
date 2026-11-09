package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.Accordion;
import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.Carousel;
import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Link;
import dev.nthings.tessera.components.NavTabs;
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
                + "navigation; a bundled client behavior switches the server-rendered panels.")
        .features("role=tablist", "roving tabindex", "Arrow keys")
        .example(
            ContentTabs.of("Pipeline stages")
                .id("demo-tabs")
                .tab("Java", p().text("Components are plain Java records and builders."))
                .tab("HTML", p().text("Output is semantic, validated HTML."))
                .tab("CSS", p().text("Styles compile into one layered stylesheet.")))
        .code(
            "ContentTabs.of(\"Pipeline stages\")",
            "    .tab(\"Java\", p().text(\"…\"))",
            "    .tab(\"HTML\", p().text(\"…\"));");
  }

  private static Demo accordion() {
    return Demo.of("accordion", "Accordion")
        .summary(
            "<details> elements sharing a name open exclusively; ::details-content animates to "
                + "auto height, and collapsed text stays findable with Ctrl+F.")
        .features("<details name>", "::details-content", "interpolate-size", ":open")
        .example(
            Accordion.of()
                .id("demo-faq")
                .exclusive()
                .item("Does it need JavaScript?", "No. Disclosure is native HTML.", true)
                .item("Can several items be open?", "Leave out exclusive() for that.", false)
                .item(
                    "Is hidden text searchable?",
                    "Yes, find-in-page opens the matching item.",
                    false))
        .code(
            "Accordion.of().exclusive()",
            "    .item(\"Does it need JavaScript?\", \"No.\", true)",
            "    .item(\"Can several items be open?\", \"Leave out exclusive().\", false);");
  }

  private static Demo carousel() {
    var carousel = Carousel.of("Platform highlights").id("demo-carousel");
    carousel
        .slide("Scroll snapping", "Slides snap to the center of the track.")
        .slide("Scroll markers", "The dots are ::scroll-marker pseudo-elements.")
        .slide("Scroll buttons", "Previous and next are ::scroll-button() pseudo-elements.")
        .slide("Snapped state", "@container scroll-state(snapped: x) highlights the current slide.")
        .slide("Keyboard", "Markers and buttons are focusable and work with the keyboard.");
    return Demo.of("carousel", "Carousel")
        .summary(
            "A scroll-snap track whose pagination dots and previous/next buttons are generated by "
                + "the browser — no script, fully keyboard accessible.")
        .features(
            "::scroll-marker",
            "::scroll-button()",
            "scroll-marker-group",
            ":target-current",
            "scroll-state(snapped)")
        .example(carousel)
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
                .linkItem("/forms", "Forms", "G F")
                .linkItem("/data", "Data & code", "G D")
                .separator()
                .linkItem("/", "Overview", null))
        .code(
            "Dropdown.of(\"Jump to\")",
            "    .linkItem(\"/forms\", \"Forms\", \"G F\")",
            "    .separator()",
            "    .commandItem(\"open\", drawer.dialogRef(), BuiltinCommand.SHOW_MODAL, \"Inspect\");");
  }

  private static Demo pager() {
    return Demo.of("pager", "Pager")
        .summary("Previous/next links between documents; every page of this site ends with one.")
        .features("<nav aria-label>", "rel-style links")
        .example(
            DocPager.of(new Link("/layout", "Layout"), new Link("/overlays", "Overlays"))
                .label("Pager example"))
        .code(
            "DocPager.of(new Link(\"/layout\", \"Layout\"), new Link(\"/overlays\", \"Overlays\"));");
  }
}
