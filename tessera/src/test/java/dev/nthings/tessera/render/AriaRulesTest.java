package dev.nthings.tessera.render;

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
import static dev.nthings.tessera.support.HtmlAssertions.assertThatCode;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.Accessible;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Role;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ARIA authoring rules")
class AriaRulesTest {

  private static List<String> rulesOf(Element body) {
    var failure =
        catchThrowableOfType(
            CompilationException.class, () -> PageCompiler.compile(new PageDocument("Aria", body)));
    return failure.diagnostics().stream().map(CompilationDiagnostic::rule).toList();
  }

  private static void compiles(Element body) {
    assertThatCode(() -> PageCompiler.compile(new PageDocument("Aria", body)))
        .doesNotThrowAnyException();
  }

  static Stream<Arguments> violations() {
    var target = ElementRef.named("target");
    return Stream.of(
        argumentSet(
            "a region without a name",
            main().child(div().role(Role.REGION).text("Content")),
            "role-name-required"),
        argumentSet(
            "a native dialog without a name",
            main().child(dialog().text("Hello")),
            "role-name-required"),
        argumentSet(
            "a switch without a label",
            main().child(input("checkbox").role(Role.SWITCH)),
            "role-name-required"),
        argumentSet(
            "a tab list with no tabs",
            main().child(div().role(Role.TABLIST).ariaLabel("Sections")),
            "role-required-children"),
        argumentSet(
            "a custom checkbox without aria-checked",
            main().child(div().role(Role.CHECKBOX).tabIndex(0).ariaLabel("Agree")),
            "role-required-attribute"),
        argumentSet(
            "a custom heading without a level",
            main().child(div().role(Role.HEADING).text("Title")),
            "role-required-attribute"),
        argumentSet(
            "a state the role does not support",
            main().child(div().role(Role.NOTE).ariaExpanded(true).text("Note")),
            "aria-attribute-not-allowed"),
        argumentSet(
            "a positive tabindex",
            main().child(div().tabIndex(2).text("Skips the order")),
            "tabindex-positive"),
        argumentSet(
            "a label for something it cannot name",
            main().children(label().labelFor(target).text("Name"), div().id(target)),
            "label-target"),
        argumentSet(
            "a focusable control hidden from assistive technology",
            main().child(span().ariaHidden(true).child(button().text("Go"))),
            "aria-hidden-focusable"),
        argumentSet(
            "two main landmarks",
            div().children(main().text("One"), main().text("Two")),
            "multiple-main"),
        argumentSet(
            "two unnamed navigation landmarks",
            main().children(nav().text("A"), nav().text("B")),
            "landmark-name-unique"),
        argumentSet(
            "two navigation landmarks with one name",
            main().children(nav().ariaLabel("Docs").text("A"), nav().ariaLabel("Docs").text("B")),
            "landmark-name-unique"),
        argumentSet(
            "a heading level that skips",
            main().children(h1().text("Title"), h3().text("Detail")),
            "heading-level-skip"),
        argumentSet(
            "an empty heading", main().children(h1().text("Title"), h2()), "empty-heading"));
  }

  @ParameterizedTest
  @MethodSource("violations")
  @DisplayName("a violation is reported under its rule")
  void reportsViolation(Element body, String rule) {
    assertThat(rulesOf(body)).contains(rule);
  }

  @Test
  @DisplayName("a diagnostic explains the role and its remedy")
  void diagnosticNamesRoleAndFix() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                PageCompiler.compile(
                    new PageDocument("Aria", main().child(div().role(Role.TABLIST)))));

    var diagnostic = failure.diagnostics().getFirst();
    assertThat(diagnostic.message()).contains("role=\"tablist\"").contains("[tab]");
    assertThat(diagnostic.suggestion()).contains("child elements");
  }

  @Test
  @DisplayName("declared roles that meet their requirements compile")
  void acceptsSatisfiedRoles() {
    var control = ElementRef.named("control");
    compiles(
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
                    .ariaChecked(Accessible.Checked.MIXED)
                    .text("All"),
                div().role(Role.HEADING).ariaLevel(2).text("Section"),
                button().role(Role.BUTTON).ariaExpanded(false).text("Menu"),
                ul().role(Role.LIST).child(li().text("Item")),
                span().tabIndex(0).text("Focusable"),
                span().tabIndex(-1).text("Scripted focus")));
  }

  @Test
  @DisplayName("landmarks of one kind compile when their names differ")
  void acceptsDistinctlyNamedLandmarks() {
    compiles(
        div()
            .children(
                main().children(h1().text("Page"), h2().text("Part"), h3().text("Detail")),
                nav().ariaLabel("Primary").text("A"),
                nav().ariaLabel("Footer").text("B")));
  }

  @Test
  @DisplayName("a lone landmark needs no name, and a heading outline may climb back up")
  void acceptsSingleLandmarkAndDescendingHeadings() {
    compiles(
        main()
            .children(
                nav().text("Only one"),
                h3().text("Starts anywhere"),
                h2().text("Climbs back"),
                h1().text("To the top")));
  }

  @Test
  @DisplayName("a focusable control is allowed inside aria-hidden once it leaves the tab order")
  void acceptsHiddenControlsOutOfTabOrder() {
    compiles(
        main()
            .children(
                span().ariaHidden(true).child(button().tabIndex(-1).text("Go")),
                span().ariaHidden(true).child(a("/x").text("Plain link").tabIndex(-1))));
  }

  @Test
  @DisplayName("elements inside a closed dialog are not checked as part of the page outline")
  void ignoresClosedDialogContents() {
    compiles(
        main()
            .children(
                h1().text("Page"),
                dialog().ariaLabel("Details").children(h3().text("Inside"), nav().text("A")),
                nav().text("B")));
  }

  @Test
  @DisplayName("roles and aria attributes are written through typed methods, not attr")
  void genericAttributeMethodsRefuseAria() {
    assertThatIllegalArgumentException().isThrownBy(() -> div().attr("role", "note"));
    assertThatIllegalArgumentException().isThrownBy(() -> div().attr("aria-label", "x"));
    assertThatIllegalArgumentException().isThrownBy(() -> div().boolAttr("aria-hidden", true));
    assertThat(div().role(Role.NOTE).attributes()).containsEntry("role", "note");
    assertThat(div().ariaCurrent(Accessible.Current.PAGE).attributes())
        .containsEntry("aria-current", "page");
  }
}
