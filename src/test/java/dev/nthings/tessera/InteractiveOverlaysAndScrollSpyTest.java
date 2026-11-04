package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.Accordion;
import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.Breadcrumbs.Crumb;
import dev.nthings.tessera.components.Carousel;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Overlays;
import dev.nthings.tessera.components.StickyHeader;
import dev.nthings.tessera.components.Tabs;
import dev.nthings.tessera.components.Tabs.NavItem;
import dev.nthings.tessera.components.Tabs.TabPanel;
import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InteractiveOverlaysAndScrollSpyTest {

  @Test
  @DisplayName("Table of contents rejects dangling fragment IDs during prepaint")
  void rejectsDanglingFragmentIds() {
    var brokenToc =
        Toc.of("broken-toc", "Contents", List.of(Toc.Item.h2("missing-section", "Missing")));
    assertThatThrownBy(
            () -> PageCompiler.compile(new PageDocument("Broken ToC", main().child(brokenToc))))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("references a non-existent fragment ID");
  }

  @Test
  @DisplayName("Compiles modal, drawer, and dropdown with starting-style transitions")
  void compilesOverlaysAndDropdown() {
    var modal =
        Overlays.modal("confirm-deploy", "Confirm Deployment")
            .description("Production rollout")
            .bodyText("Deploy to all 4 regions?");

    var drawer =
        Overlays.drawer("inspector", "Live Inspector")
            .subtitle("Cascade state")
            .body(p().text("7 cascade layers active."));

    var dropdown =
        Dropdown.create("actions-menu", "Actions")
            .linkItem("/logs", "View Logs", "G L")
            .separator()
            .commandItem("open-deploy", modal.dialogRef(), BuiltinCommand.SHOW_MODAL, "Deploy Now");

    var page =
        main()
            .children(
                modal.triggerButton("trigger-modal", "Open Modal"),
                drawer.triggerButton("trigger-drawer", "Open Drawer"),
                dropdown,
                modal.asDeferred(10),
                drawer.asDeferred(20));

    var compiled = PageCompiler.compile(new PageDocument("Overlays", page));

    assertThat(compiled.cssContent())
        .contains("@starting-style{opacity:0;transform:scale(0.95)}")
        .contains("transition-property:opacity, transform, display, overlay")
        .contains("transition-behavior:allow-discrete")
        .contains("position-area:block-end span-inline-end")
        .contains("position-try-fallbacks:flip-block, flip-inline, flip-block flip-inline");

    var doc = Jsoup.parse(compiled.html());
    var modalEl = doc.selectFirst("dialog#confirm-deploy--dialog[closedby=any]");
    var drawerEl = doc.selectFirst("dialog#inspector--dialog[closedby=any]");
    assertThat(modalEl).isNotNull();
    assertThat(drawerEl).isNotNull();

    // Dropdown popover is adjacent sibling of its trigger button without false role="menu"
    var dropdownTrigger = doc.selectFirst("[data-view^=dropdown-] > button.dropdown-trigger");
    assertThat(dropdownTrigger).isNotNull();
    assertThat(dropdownTrigger.hasAttr("aria-haspopup")).isFalse();
    var adjacentPopover = dropdownTrigger.nextElementSibling();
    assertThat(adjacentPopover).isNotNull();
    assertThat(adjacentPopover.attr("popover")).isEqualTo("auto");
  }

  @Test
  @DisplayName("Compiles accordion, tabs, breadcrumbs, sticky header, and carousel")
  void compilesInteractiveSuite() {
    var sticky = new StickyHeader("top-bar", div().text("Sticky Top Bar"));
    var crumbs =
        Breadcrumbs.of("crumbs", Crumb.of("/", "Home"), Crumb.of("/components", "Components"));
    var navTabs =
        Tabs.navTabs(
            "main-tabs",
            "Sections",
            "/components",
            List.of(new NavItem("/", "Home"), new NavItem("/components", "Components")));
    var contentTabs =
        Tabs.contentTabs(
            "code-tabs",
            List.of(
                TabPanel.of("Java 25", p().text("ScopedValue")),
                TabPanel.of("CSS 2026", p().text("@starting-style"))));
    var accordion =
        Accordion.exclusive("faq")
            .item("Question 1", "Answer 1", true)
            .item("Question 2", "Answer 2", false);
    var carousel =
        Carousel.create("slides", "Feature Carousel")
            .slide("Slide 1", "Summary 1")
            .slide("Slide 2", "Summary 2");

    var compiled =
        PageCompiler.compile(
            new PageDocument(
                "Interactive Suite",
                main().children(sticky, crumbs, navTabs, contentTabs, accordion, carousel)));

    assertThat(compiled.cssContent())
        .contains("@container sticky-top-bar scroll-state(stuck: top)")
        .contains("content:\"/\" / \"\"")
        .contains("inset-block-start:calc(anchor(bottom) - 2px)")
        .contains(":scope > details::details-content")
        .contains("scroll-snap-type:x mandatory")
        .contains("@container scroll-state(snapped: x)")
        .contains(":scope > .carousel-item::scroll-marker:target-current");

    var doc = Jsoup.parse(compiled.html());
    assertThat(doc.select("details[name=faq]")).hasSize(2);
    assertThat(doc.selectFirst("nav[aria-label=Breadcrumb] a[aria-current=page]")).isNotNull();
  }
}
