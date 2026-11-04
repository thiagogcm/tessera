package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.Accordion;
import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.Breadcrumbs.Crumb;
import dev.nthings.tessera.components.Carousel;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Tabs;
import dev.nthings.tessera.components.Tabs.NavItem;
import dev.nthings.tessera.components.Tabs.TabPanel;
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
        pager());
  }

  private static Demo breadcrumbs() {
    return Demo.of("breadcrumbs", "Breadcrumbs")
        .summary(
            "An ordered list in a labelled <nav>; separators are generated content with empty "
                + "alt text, so screen readers skip them, and the last item is aria-current.")
        .features("content: \"/\" / \"\"", "aria-current=\"page\"")
        .example(
            Breadcrumbs.of(
                "demo-crumbs",
                Crumb.of("/", "Tessera"),
                Crumb.of("/navigation", "Navigation"),
                Crumb.of("/navigation#toc-layout--breadcrumbs", "Breadcrumbs")))
        .code(
            "Breadcrumbs.of(\"crumbs\", Crumb.of(\"/\", \"Home\"), Crumb.of(\"/docs\", \"Docs\"));");
  }

  private static Demo navTabs() {
    return Demo.of("nav-tabs", "Navigation tabs")
        .summary(
            "Link tabs for page-level navigation (the site header uses them). The active "
                + "underline is a single pseudo-element anchored to the current tab.")
        .features("anchor-name", "position-anchor", "anchor()", "aria-current")
        .example(
            Tabs.navTabs(
                "demo-nav",
                "Section navigation",
                "/navigation",
                List.of(
                    new NavItem("/layout", "Layout"),
                    new NavItem("/navigation", "Navigation"),
                    new NavItem("/overlays", "Overlays"))))
        .code(
            "Tabs.navTabs(\"nav\", \"Section navigation\", activePath,",
            "    List.of(new NavItem(\"/layout\", \"Layout\"), new NavItem(\"/navigation\", \"Navigation\")));");
  }

  private static Demo contentTabs() {
    return Demo.of("content-tabs", "Content tabs")
        .summary(
            "In-page tab panels driven by a radio group: the checked label anchors the sliding "
                + "indicator and :has() reveals the matching panel.")
        .features(":has(:checked)", "anchor positioning", "radio group")
        .example(
            Tabs.contentTabs(
                "demo-tabs",
                List.of(
                    TabPanel.of(
                        "Java", p().text("Components are plain Java records and builders.")),
                    TabPanel.of("HTML", p().text("Output is semantic, validated HTML.")),
                    TabPanel.of("CSS", p().text("Styles compile into one layered stylesheet.")))))
        .code(
            "Tabs.contentTabs(\"tabs\", List.of(",
            "    TabPanel.of(\"Java\", p().text(\"…\")), TabPanel.of(\"HTML\", p().text(\"…\"))));");
  }

  private static Demo accordion() {
    return Demo.of("accordion", "Accordion")
        .summary(
            "<details> elements sharing a name open exclusively; ::details-content animates to "
                + "auto height, and collapsed text stays findable with Ctrl+F.")
        .features("<details name>", "::details-content", "interpolate-size", ":open")
        .example(
            Accordion.exclusive("demo-faq")
                .item("Does it need JavaScript?", "No. Disclosure is native HTML.", true)
                .item("Can several items be open?", "Use Accordion.multiOpen(id) for that.", false)
                .item(
                    "Is hidden text searchable?",
                    "Yes, find-in-page opens the matching item.",
                    false))
        .code(
            "Accordion.exclusive(\"faq\")",
            "    .item(\"Does it need JavaScript?\", \"No.\", true)",
            "    .item(\"Can several items be open?\", \"Use multiOpen.\", false);");
  }

  private static Demo carousel() {
    var carousel = Carousel.create("demo-carousel", "Platform highlights");
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
            "Carousel.create(\"highlights\", \"Platform highlights\")",
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
            Dropdown.create("demo-menu", "Jump to")
                .linkItem("/forms", "Forms", "G F")
                .linkItem("/data", "Data & code", "G D")
                .separator()
                .linkItem("/", "Overview", null))
        .code(
            "Dropdown.create(\"menu\", \"Jump to\")",
            "    .linkItem(\"/forms\", \"Forms\", \"G F\")",
            "    .separator()",
            "    .commandItem(\"open\", drawer.dialogRef(), BuiltinCommand.SHOW_MODAL, \"Inspect\");");
  }

  private static Demo pager() {
    return Demo.of("pager", "Pager")
        .summary("Previous/next links between documents; every page of this site ends with one.")
        .features("<nav aria-label>", "rel-style links")
        .example(
            DocPager.of(
                new DocPager.PageRef("/layout", "Layout"),
                new DocPager.PageRef("/overlays", "Overlays")))
        .code(
            "DocPager.of(new PageRef(\"/layout\", \"Layout\"), new PageRef(\"/overlays\", \"Overlays\"));");
  }
}
