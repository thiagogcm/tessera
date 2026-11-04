package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.Accordion;
import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.Breadcrumbs.Crumb;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Overlays;
import dev.nthings.tessera.components.Tabs;
import dev.nthings.tessera.components.Tabs.NavItem;
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
  @DisplayName("Modals and drawers light-dismiss; the dropdown is a toggled popover")
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

    var doc = Jsoup.parse(compiled.html());
    assertThat(doc.select("dialog[closedby=any]"))
        .extracting(e -> e.id())
        .containsExactly("confirm-deploy--dialog", "inspector--dialog");

    // Dropdown: trigger toggles the adjacent popover; no false menu semantics
    var dropdownTrigger = doc.selectFirst("button.dropdown-trigger");
    assertThat(dropdownTrigger.hasAttr("aria-haspopup")).isFalse();
    var popover = dropdownTrigger.nextElementSibling();
    assertThat(popover.attr("popover")).isEqualTo("auto");
    assertThat(dropdownTrigger.attr("commandfor")).isEqualTo(popover.id());
    assertThat(dropdownTrigger.attr("command")).isEqualTo("toggle-popover");

    // A command item inside the menu opens the modal declared elsewhere on the page
    assertThat(popover.selectFirst("button[command=show-modal]").attr("commandfor"))
        .isEqualTo("confirm-deploy--dialog");
  }

  @Test
  @DisplayName("Exclusive accordion, breadcrumb and nav-tab current-page markers")
  void compilesInteractiveSuite() {
    var crumbs =
        Breadcrumbs.of("crumbs", Crumb.of("/", "Home"), Crumb.of("/components", "Components"));
    var navTabs =
        Tabs.navTabs(
            "main-tabs",
            "Sections",
            "/components",
            List.of(new NavItem("/", "Home"), new NavItem("/components", "Components")));
    var accordion =
        Accordion.exclusive("faq")
            .item("Question 1", "Answer 1", true)
            .item("Question 2", "Answer 2", false);

    var compiled =
        PageCompiler.compile(
            new PageDocument("Interactive Suite", main().children(crumbs, navTabs, accordion)));

    var doc = Jsoup.parse(compiled.html());
    assertThat(doc.select("details[name=faq]")).hasSize(2);
    assertThat(doc.select("details[name=faq][open]")).hasSize(1);
    assertThat(doc.select("nav[aria-label=Breadcrumb] li"))
        .extracting(li -> li.selectFirst("a").hasAttr("aria-current"))
        .containsExactly(false, true);
    assertThat(doc.select("[data-view-id=main-tabs] a[aria-current=page]"))
        .extracting(e -> e.attr("href"))
        .containsExactly("/components");
  }
}
