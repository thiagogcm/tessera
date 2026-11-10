package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.input;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.AriaChecked;
import dev.nthings.tessera.core.AriaCurrent;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.support.RenderedPage;
import dev.nthings.tessera.view.Diagnostic;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ARIA authoring rules")
class AriaRulesTest {

  private static List<Diagnostic> diagnosticsOf(Element body) {
    var failure =
        catchThrowableOfType(
            CompilationException.class, () -> Compiler.compile(new PageDocument("Aria", body)));
    assertThat(failure).as("compilation failure").isNotNull();
    return failure.diagnostics();
  }

  static Stream<Arguments> violations() {
    var target = ElementRef.scoped("target");
    return Stream.of(
        argumentSet(
            "a region without a name",
            main().child(div().role(Role.REGION).text("Content")),
            "role-name-required",
            "<div> with role=\"region\" has no accessible name",
            "Add .ariaLabel(...) or .ariaLabelledBy(...).",
            "div",
            1),
        argumentSet(
            "a native dialog without a name",
            main().child(dialog().text("Hello")),
            "role-name-required",
            "<dialog> with role=\"dialog\" has no accessible name",
            "Add .ariaLabel(...) or .ariaLabelledBy(...).",
            "dialog",
            1),
        argumentSet(
            "a switch without a label",
            main().child(input("checkbox").role(Role.SWITCH)),
            "role-name-required",
            "<input> with role=\"switch\" has no accessible name",
            "Add .ariaLabel(...) or .ariaLabelledBy(...), or readable content.",
            "input",
            1),
        argumentSet(
            "a tab list with no tabs",
            main().child(div().role(Role.TABLIST).ariaLabel("Sections")),
            "role-required-children",
            "<div> with role=\"tablist\" must contain at least one element with role [tab]",
            "Add the required child elements, or choose a role that does not require them.",
            "div",
            1),
        argumentSet(
            "a custom checkbox without aria-checked",
            main().child(div().role(Role.CHECKBOX).tabIndex(0).ariaLabel("Agree")),
            "role-required-attribute",
            "<div> with role=\"checkbox\" must declare aria-checked",
            "Declare aria-checked with its typed aria method, or use a native element that exposes it.",
            "div",
            1),
        argumentSet(
            "a custom heading without a level",
            main().child(div().role(Role.HEADING).text("Title")),
            "role-required-attribute",
            "<div> with role=\"heading\" must declare aria-level",
            "Declare aria-level with its typed aria method, or use a native element that exposes it.",
            "div",
            1),
        argumentSet(
            "a state the role does not support",
            main().child(div().role(Role.NOTE).ariaExpanded(true).text("Note")),
            "aria-attribute-not-allowed",
            "<div> with role=\"note\" does not support aria-expanded",
            "Remove aria-expanded, or use a role that supports it (see Role#supportedAria for the role).",
            "div",
            1),
        argumentSet(
            "a positive tabindex",
            main().child(div().tabIndex(2).text("Skips the order")),
            "tabindex-positive",
            "tabindex=\"2\" on <div> overrides the natural tab order",
            "Use tabindex=\"0\" to make an element focusable, or \"-1\" for scripted focus, and order the document instead.",
            "div",
            1),
        argumentSet(
            "a label for something it cannot name",
            main().children(label().labelFor(target).text("Name"), div().id(target)),
            "label-target",
            "<label for=\"target\"> points at <div>, which a label cannot name",
            "Point the label at an <input>, <select>, <textarea>, <button>, or <output>, or use aria-labelledby on the element instead.",
            "label",
            1),
        argumentSet(
            "a focusable control hidden from assistive technology",
            main().child(span().ariaHidden(true).child(button().text("Go"))),
            "aria-hidden-focusable",
            "<span aria-hidden=\"true\"> contains a focusable <button>, which keyboard users can still reach",
            "Remove aria-hidden, take the control out of the tab order with tabindex=\"-1\", or disable it.",
            "span",
            1),
        argumentSet(
            "two main landmarks",
            div().children(main().text("One"), main().text("Two")),
            "multiple-main",
            "A page has one main landmark; this is a second <main>",
            "Keep a single <main> (or role=\"main\") per page.",
            "main",
            1),
        argumentSet(
            "two unnamed navigation landmarks",
            main().children(nav().text("A"), nav().text("B")),
            "landmark-name-unique",
            "This page has 2 navigation landmarks, and this one has no accessible name to tell them apart",
            "Give each one a distinct .ariaLabel(...) or .ariaLabelledBy(...).",
            "nav",
            2),
        argumentSet(
            "two navigation landmarks with one name",
            main().children(nav().ariaLabel("Docs").text("A"), nav().ariaLabel("Docs").text("B")),
            "landmark-name-unique",
            "Two navigation landmarks share the accessible name \"Docs\"",
            "Give each one a distinct accessible name.",
            "nav",
            1),
        argumentSet(
            "a heading level that skips",
            main().children(h1().text("Title"), h3().text("Detail")),
            "heading-level-skip",
            "Heading level jumps from 1 to 3 (<h3>)",
            "Use level 2 here, or add the intermediate heading so the outline has no gaps.",
            "h3",
            1),
        argumentSet(
            "an empty heading",
            main().children(h1().text("Title"), h2()),
            "empty-heading",
            "<h2> heading has no readable text",
            "Give the heading text or an aria-label, or use an element that is not a heading.",
            "h2",
            1));
  }

  @ParameterizedTest
  @MethodSource("violations")
  @DisplayName("each rule violation carries its message, remedy, and location")
  void reportsViolation(
      Element body, String rule, String message, String fix, String tag, int count) {
    assertThat(diagnosticsOf(body))
        .hasSize(count)
        .allSatisfy(
            d -> {
              assertThat(d.rule()).isEqualTo(rule);
              assertThat(d.message()).isEqualTo(message);
              assertThat(d.fix()).isEqualTo(fix);
              assertThat(d.location().page()).isEqualTo("Aria");
              assertThat(d.location().components()).isEmpty();
              assertThat(d.location().tag()).isEqualTo(tag);
              assertThat(d.location().domId()).isNull();
            });
  }

  static Stream<Arguments> unnamedDeclaredRoles() {
    return Stream.of(
        argumentSet(
            "an icon-only button declared as a tab",
            main()
                .child(
                    div()
                        .role(Role.TABLIST)
                        .ariaLabel("Sections")
                        .child(button().role(Role.TAB).ariaSelected(true))),
            "<button> with role=\"tab\" has no accessible name",
            "Add .ariaLabel(...) or .ariaLabelledBy(...), or readable content.",
            "button"),
        argumentSet(
            "a custom heading with no text",
            main().children(h1().text("Title"), div().role(Role.HEADING).ariaLevel(2)),
            "<div> with role=\"heading\" has no accessible name",
            "Add .ariaLabel(...) or .ariaLabelledBy(...), or readable content.",
            "div"));
  }

  @ParameterizedTest
  @MethodSource("unnamedDeclaredRoles")
  @DisplayName("a missing name is reported once, under the declared role")
  void missingNameIsReportedOnce(Element body, String message, String fix, String tag) {
    var diagnostics = diagnosticsOf(body);
    assertThat(diagnostics)
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("role-name-required");
              assertThat(d.message()).isEqualTo(message);
              assertThat(d.fix()).isEqualTo(fix);
              assertThat(d.location().page()).isEqualTo("Aria");
              assertThat(d.location().tag()).isEqualTo(tag);
              assertThat(d.location().domId()).isNull();
            });
  }

  @Test
  @DisplayName("declared roles that meet their requirements compile with their states")
  void acceptsSatisfiedRoles() {
    var control = ElementRef.scoped("control");
    var page =
        RenderedPage.of(
            new PageDocument(
                "Aria",
                main()
                    .children(
                        div().role(Role.REGION).ariaLabel("Results").text("Content"),
                        dialog().ariaLabel("Confirm").text("Sure?"),
                        label().labelFor(control).text("Notifications"),
                        input("checkbox").id(control).role(Role.SWITCH),
                        label().child(input("checkbox").role(Role.SWITCH)).text("Updates"),
                        div()
                            .role(Role.TABLIST)
                            .ariaLabel("Sections")
                            .child(div().role(Role.TAB).ariaSelected(true).text("One")),
                        div()
                            .role(Role.CHECKBOX)
                            .tabIndex(0)
                            .ariaChecked(AriaChecked.MIXED)
                            .text("All"),
                        div().role(Role.HEADING).ariaLevel(2).text("Section"),
                        button().role(Role.BUTTON).ariaExpanded(false).text("Menu"),
                        ul().role(Role.LIST).child(li().text("Item")),
                        span().tabIndex(0).text("Focusable"),
                        span().tabIndex(-1).text("Scripted focus"))));

    assertThat(page.element("main > div[role=region]"))
        .hasAttribute("aria-label", "Results")
        .hasText("Content");
    assertThat(page.element("main > dialog"))
        .hasAttribute("aria-label", "Confirm")
        .hasText("Sure?");
    assertThat(page.element("main > label[for=control]")).hasText("Notifications");
    assertThat(page.element("main > input[role=switch]")).hasAttribute("id", "control");
    assertThat(page.elements("main > label"))
        .extracting(e -> e.text())
        .containsExactly("Notifications", "Updates");
    assertThat(page.element("main > div[role=tablist]")).hasAttribute("aria-label", "Sections");
    assertThat(page.element("main > div[role=tablist] > div[role=tab]"))
        .hasAttribute("aria-selected", "true")
        .hasText("One");
    assertThat(page.element("main > div[role=checkbox]"))
        .hasAttribute("aria-checked", "mixed")
        .hasAttribute("tabindex", "0")
        .hasText("All");
    assertThat(page.element("main > div[role=heading]"))
        .hasAttribute("aria-level", "2")
        .hasText("Section");
    assertThat(page.element("main > button[role=button]"))
        .hasAttribute("aria-expanded", "false")
        .hasText("Menu");
    assertThat(page.element("main > ul[role=list] > li")).hasText("Item");
    assertThat(page.elements("main > span[tabindex]"))
        .extracting(e -> e.attr("tabindex"))
        .containsExactly("0", "-1");
  }

  static Stream<Arguments> acceptedStructures() {
    return Stream.of(
        argumentSet(
            "landmarks of one kind compile when their names differ",
            div()
                .children(
                    main().children(h1().text("Page"), h2().text("Part"), h3().text("Detail")),
                    nav().ariaLabel("Primary").text("A"),
                    nav().ariaLabel("Footer").text("B")),
            (Consumer<RenderedPage>)
                page -> {
                  assertThat(page.elements("main > h1, main > h2, main > h3"))
                      .extracting(e -> e.tagName())
                      .containsExactly("h1", "h2", "h3");
                  assertThat(page.element("nav[aria-label=Primary]")).hasText("A");
                  assertThat(page.element("nav[aria-label=Footer]")).hasText("B");
                }),
        argumentSet(
            "a lone landmark needs no name, and a heading outline may climb back",
            main()
                .children(
                    nav().text("Only one"),
                    h3().text("Starts anywhere"),
                    h2().text("Climbs back"),
                    h1().text("To the top")),
            (Consumer<RenderedPage>)
                page -> {
                  assertThat(page.element("main > nav")).hasText("Only one");
                  assertThat(page.elements("main > h1, main > h2, main > h3"))
                      .extracting(e -> e.tagName())
                      .containsExactly("h3", "h2", "h1");
                }));
  }

  @ParameterizedTest
  @MethodSource("acceptedStructures")
  @DisplayName("landmark and heading structures that meet their rules compile")
  void acceptsLandmarkAndHeadingStructures(Element body, Consumer<RenderedPage> verify) {
    verify.accept(RenderedPage.of(new PageDocument("Aria", body)));
  }

  @Test
  @DisplayName("a focusable control is allowed inside aria-hidden once it leaves the tab order")
  void acceptsUntabbableHiddenControls() {
    var page =
        RenderedPage.of(
            new PageDocument(
                "Aria",
                main()
                    .children(
                        span().ariaHidden(true).child(button().tabIndex(-1).text("Go")),
                        span().ariaHidden(true).child(a("/x").text("Plain link").tabIndex(-1)))));

    assertThat(page.element("main > span[aria-hidden=true] > button"))
        .hasAttribute("tabindex", "-1")
        .hasText("Go");
    assertThat(page.element("main > span[aria-hidden=true] > a"))
        .hasAttribute("href", "/x")
        .hasAttribute("tabindex", "-1")
        .hasText("Plain link");
  }

  @Test
  @DisplayName("elements inside a closed dialog are not checked as part of the page outline")
  void ignoresClosedDialogContents() {
    var page =
        RenderedPage.of(
            new PageDocument(
                "Aria",
                main()
                    .children(
                        h1().text("Page"),
                        dialog()
                            .ariaLabel("Details")
                            .children(h3().text("Inside"), nav().text("A")),
                        nav().text("B"))));

    assertThat(page.element("main > dialog")).hasAttribute("aria-label", "Details");
    assertThat(page.element("main > dialog > h3")).hasText("Inside");
    assertThat(page.element("main > dialog > nav")).hasText("A");
    assertThat(page.element("main > nav")).hasText("B");
  }

  @Test
  @DisplayName("roles and aria attributes are written through typed methods, not attr")
  void genericAttributeMethodsRefuseAria() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> div().attr("role", "note"))
        .withMessageContaining("attribute 'role'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> div().attr("aria-label", "x"))
        .withMessageContaining("attribute 'aria-label'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> div().boolAttr("aria-hidden"))
        .withMessageContaining("attribute 'aria-hidden'");
    assertThat(div().role(Role.NOTE).attributes()).containsEntry("role", "note");
    assertThat(div().ariaCurrent(AriaCurrent.PAGE).attributes())
        .containsEntry("aria-current", "page");
  }
}
