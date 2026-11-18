package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.el;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.input;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatNoException;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.AriaChecked;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Diagnostics.Found;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Page authoring diagnostics")
class AuthoringDiagnosticsTest {

  private static List<Found> diagnosticsOf(Element body) {
    return Diagnostics.of(() -> Compiler.compile(new PageDocument("Page", body)));
  }

  static Stream<Arguments> violations() {
    var target = ElementRef.scoped("target");
    return Stream.of(
        argumentSet(
            "a button without a label", main().child(button()), "interactive-label", "button", 1),
        argumentSet(
            "an image with neither dimensions nor aspect-ratio",
            main().child(img("/banner.jpg", "Banner")),
            "image-dimensions",
            "img",
            1),
        argumentSet(
            "a label reference bound to nothing, with no cascading missing-label report",
            main().child(button().ariaLabelledBy(ElementRef.scoped("save-label"))),
            "unbound-reference",
            "button",
            1),
        argumentSet(
            "a region without a name",
            main().child(div().role(Role.REGION).text("Content")),
            "role-name-required",
            "div",
            1),
        argumentSet(
            "a native dialog without a name",
            main().child(dialog().text("Hello")),
            "role-name-required",
            "dialog",
            1),
        argumentSet(
            "a switch without a label",
            main().child(input("checkbox").role(Role.SWITCH)),
            "role-name-required",
            "input",
            1),
        argumentSet(
            "an icon-only button declared as a tab, reported once under its role",
            main()
                .child(
                    div()
                        .role(Role.TABLIST)
                        .ariaLabel("Sections")
                        .child(button().role(Role.TAB).ariaSelected(true))),
            "role-name-required",
            "button",
            1),
        argumentSet(
            "a custom heading with no text",
            main().children(h1().text("Title"), div().role(Role.HEADING).ariaLevel(2)),
            "role-name-required",
            "div",
            1),
        argumentSet(
            "a tab list with no tabs",
            main().child(div().role(Role.TABLIST).ariaLabel("Sections")),
            "role-required-children",
            "div",
            1),
        argumentSet(
            "a custom checkbox without aria-checked",
            main().child(div().role(Role.CHECKBOX).tabIndex(0).ariaLabel("Agree")),
            "role-required-attribute",
            "div",
            1),
        argumentSet(
            "a custom heading without a level",
            main().child(div().role(Role.HEADING).text("Title")),
            "role-required-attribute",
            "div",
            1),
        argumentSet(
            "a state the role does not support",
            main().child(div().role(Role.NOTE).ariaExpanded(true).text("Note")),
            "aria-attribute-not-allowed",
            "div",
            1),
        argumentSet(
            "a positive tabindex",
            main().child(div().tabIndex(2).text("Skips the order")),
            "tabindex-positive",
            "div",
            1),
        argumentSet(
            "a label for something it cannot name",
            main().children(label().labelFor(target).text("Name"), div().id(target)),
            "label-target",
            "label",
            1),
        argumentSet(
            "a focusable control hidden from assistive technology",
            main().child(span().ariaHidden(true).child(button().text("Go"))),
            "aria-hidden-focusable",
            "span",
            1),
        argumentSet(
            "two main landmarks",
            div().children(main().text("One"), main().text("Two")),
            "multiple-main",
            "main",
            1),
        argumentSet(
            "two unnamed navigation landmarks",
            main().children(nav().text("A"), nav().text("B")),
            "landmark-name-unique",
            "nav",
            2),
        argumentSet(
            "two navigation landmarks with one name",
            main().children(nav().ariaLabel("Docs").text("A"), nav().ariaLabel("Docs").text("B")),
            "landmark-name-unique",
            "nav",
            1),
        argumentSet(
            "a heading level that skips",
            main().children(h1().text("Title"), h3().text("Detail")),
            "heading-level-skip",
            "h3",
            1),
        argumentSet(
            "an empty heading",
            main().children(h1().text("Title"), h2()),
            "empty-heading",
            "h2",
            1));
  }

  @ParameterizedTest
  @MethodSource("violations")
  @DisplayName("a violation is reported under its rule at the offending element")
  void reportsViolation(Element body, String rule, String tag, int count) {
    assertThat(diagnosticsOf(body))
        .hasSize(count)
        .allSatisfy(found -> {
          assertThat(found.rule()).isEqualTo(rule);
          assertThat(found.tag()).isEqualTo(tag);
        });
  }

  static Stream<Arguments> validPatterns() {
    var control = ElementRef.scoped("control");
    return Stream.of(
        argumentSet(
            "declared roles that meet their requirements",
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
                    span().tabIndex(-1).text("Scripted focus"))),
        argumentSet(
            "landmarks of one kind with distinct names",
            div()
                .children(
                    main().children(h1().text("Page"), h2().text("Part"), h3().text("Detail")),
                    nav().ariaLabel("Primary").text("A"),
                    nav().ariaLabel("Footer").text("B"))),
        argumentSet(
            "a lone unnamed landmark and an outline that climbs back",
            main()
                .children(
                    nav().text("Only one"),
                    h3().text("Starts anywhere"),
                    h2().text("Climbs back"),
                    h1().text("To the top"))),
        argumentSet(
            "controls inside aria-hidden taken out of the tab order",
            main()
                .children(
                    span().ariaHidden(true).child(button().tabIndex(-1).text("Go")),
                    span().ariaHidden(true).child(a("/x").text("Plain link").tabIndex(-1)))),
        argumentSet(
            "outline and landmarks inside a closed dialog",
            main()
                .children(
                    h1().text("Page"),
                    dialog().ariaLabel("Details").children(h3().text("Inside"), nav().text("A")),
                    nav().text("B"))),
        argumentSet(
            "a presentational image and a link named by aria-label",
            main()
                .children(
                    img("/deco.svg", "").role(Role.PRESENTATION).dimensions(100, 100),
                    a("/home").ariaLabel("Home page").child(span().ariaHidden(true)))),
        argumentSet(
            "a percent-encoded fragment link to its anchor",
            main()
                .id(ElementRef.page("café+intro"))
                .child(a("#caf%C3%A9%2Bintro").text("Introduction"))));
  }

  @ParameterizedTest
  @MethodSource("validPatterns")
  @DisplayName("valid patterns compile without diagnostics")
  void compilesValidPatterns(Element body) {
    assertThatNoException().isThrownBy(() -> Compiler.compile(new PageDocument("Page", body)));
  }

  record Panel(String id, DocumentContent content) implements DocumentScope {
    Panel(String id, IntoElement content) {
      this(id, DocumentContent.of(content));
    }

    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public String scopeName() {
      return "panel";
    }

    @Override
    public DocumentElement expand(DocumentContext context) {
      return DocumentElement.of(div()).child(content);
    }
  }

  @Test
  @DisplayName("independent errors carry their lexical component paths")
  void reportsErrorsWithComponentPaths() {
    var missing = ElementRef.scoped("confirmation");
    var document =
        new PageDocument(
            "Settings",
            new Panel(
                "settings",
                DocumentElement.of(div())
                    .children(
                        new Panel("profile", img("/avatar.png", "Profile photo")),
                        new Panel(
                            "actions",
                            DocumentElement.of(div())
                                .children(
                                    Documents.late(_ ->
                                        DocumentContent.of(
                                            button()
                                                .id(ElementRef.scoped("save"))
                                                .command(missing, BuiltinCommand.SHOW_MODAL)
                                                .text("Save"))),
                                    DocumentContent.of(
                                        a("#details%ZZ")
                                            .id(ElementRef.scoped("details"))
                                            .text("Details")),
                                    Documents.deferred(
                                        button().id(ElementRef.scoped("cancel")), 10))))));

    var profile = List.of("panel[settings]", "panel[settings--profile]");
    var actions = List.of("panel[settings]", "panel[settings--actions]");
    assertThat(Diagnostics.of(() -> Compiler.compile(document)))
        .extracting(Found::rule, Found::components, Found::tag, Found::domId)
        .containsExactlyInAnyOrder(
            tuple("image-dimensions", profile, "img", null),
            tuple("unbound-reference", actions, "button", "settings--actions--save"),
            tuple("invalid-url", actions, "a", "settings--actions--details"),
            tuple("interactive-label", actions, "button", "settings--actions--cancel"));
  }

  record DocumentExtras() implements DocumentScope {
    private static final ViewKey<Object> ACTIONS =
        ViewKey.of("extra-actions", Object.class, Object::new);

    @Override
    public String scopeId() {
      return "extras";
    }

    @Override
    public DocumentElement expand(DocumentContext context) {
      context.contributeHead(
          el(HtmlTag.LINK).attr("rel", "stylesheet").attr("href", "/styles%ZZ.css"));
      context.atDocumentEnd(
          ACTIONS, _ -> DocumentContent.of(button().id(ElementRef.scoped("subscribe"))));
      return DocumentElement.of(div());
    }
  }

  @Test
  @DisplayName("head and end-of-document errors identify the component that registered the content")
  void reportsContributionOrigins() {
    var document = new PageDocument("Settings", new Panel("settings", new DocumentExtras()));

    var origin = List.of("panel[settings]", "document-extras[settings--extras]");
    assertThat(Diagnostics.of(() -> Compiler.compile(document)))
        .extracting(Found::rule, Found::components, Found::tag)
        .containsExactlyInAnyOrder(
            tuple("invalid-url", origin, "link"), tuple("interactive-label", origin, "button"));
  }

  @ParameterizedTest
  @EnumSource(BuiltinCommand.class)
  @DisplayName("a native command must target an element that supports it")
  void checksNativeCommandTargets(BuiltinCommand command) {
    var plain = div().id(ElementRef.scoped("target"));
    var invalid = main().children(button().command(plain.ref(), command).text("Run"), plain);

    assertThat(diagnosticsOf(invalid))
        .extracting(Found::rule, Found::tag)
        .containsExactly(tuple("incompatible-command-target", "button"));

    var compatible =
        switch (command.target()) {
          case DIALOG -> dialog().id(ElementRef.scoped("target")).ariaLabel("Target");
          case POPOVER -> div().id(ElementRef.scoped("target")).popover(PopoverMode.AUTO);
        };
    var page =
        RenderedPage.of(
            new PageDocument(
                "Commands",
                main()
                    .children(
                        button().command(compatible.ref(), command).text("Run"), compatible)));

    var invoker = page.element("main > button");
    assertThat(invoker).hasAttribute("command", command.wireValue());
    assertThat(page.target(invoker, "commandfor")).isEqualTo(page.element("main > :last-child"));
  }
}
