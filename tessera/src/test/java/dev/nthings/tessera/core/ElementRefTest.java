package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.RenderedPage.render;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.view.Diagnostic;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ElementRef")
class ElementRefTest {

  /// Wraps content in a nested scope for tests.
  record Panel(String id, Element content) implements Scope {

    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public Element expand(DocumentContext context) {
      return div().child(content);
    }
  }

  @Test
  @DisplayName("resolves to its bind-site scope from an invoker in another scope")
  void resolvesToBindSiteScope() {
    var target = ElementRef.scoped("confirm");

    var page =
        renderMain(
            new Panel("toolbar", button().command(target, BuiltinCommand.SHOW_MODAL).text("Open")),
            new Panel("dialogs", dialog().id(target).ariaLabel("Confirm").text("Sure?")));

    assertThat(page.element("button")).hasAttribute("commandfor", "dialogs--confirm");
    assertThat(page.elementById("dialogs--confirm")).hasTagName("dialog");
  }

  @Test
  @DisplayName("an unbound ref fails compilation")
  void unboundRefFailsCompilation() {
    var dangling = ElementRef.scoped("ghost");

    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> render(div().child(button().command(dangling, BuiltinCommand.CLOSE).text("x"))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("unbound-reference");
              assertThat(d.message())
                  .isEqualTo(
                      "<button commandfor> references ElementRef.scoped(ghost), which is not bound to any element on this page");
              assertThat(d.fix())
                  .isEqualTo("Bind this reference with .id(ref) on the intended target.");
              assertThat(d.location().page()).isEqualTo("Test");
              assertThat(d.location().tag()).isEqualTo("button");
              assertThat(d.location().domId()).isNull();
            });
  }

  static Stream<Arguments> ambiguousTargets() {
    var shared = ElementRef.scoped("confirm");
    var dialog = dialog().id(ElementRef.scoped("confirm")).ariaLabel("Confirm").text("Confirm?");
    return Stream.of(
        argumentSet(
            "same reference bound twice",
            div()
                .children(
                    p().id(shared),
                    dialog().id(shared).ariaLabel("Confirm").text("Confirm?"),
                    button().command(shared, BuiltinCommand.SHOW_MODAL).text("Open"),
                    button()),
            "duplicate-reference-binding",
            "ElementRef.scoped(confirm) is bound to more than one element on the same page",
            "Bind this ElementRef once; use a separate reference for the other element.",
            "dialog",
            null),
        argumentSet(
            "separate references with the same DOM id",
            div()
                .children(
                    p().id(ElementRef.scoped("confirm")),
                    dialog,
                    button().command(dialog.ref(), BuiltinCommand.SHOW_MODAL).text("Open"),
                    button()),
            "duplicate-id",
            "Duplicate DOM id 'confirm' on the same page",
            "Give each element a distinct ID within its id scope.",
            "dialog",
            "confirm"));
  }

  @ParameterizedTest
  @MethodSource("ambiguousTargets")
  @DisplayName("ambiguous targets report their binding error without secondary command errors")
  void reportsAmbiguousTargets(
      Element body, String rule, String message, String fix, String tag, String domId) {
    var failure = catchThrowableOfType(CompilationException.class, () -> render(body));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactlyInAnyOrder(rule, "interactive-label");
    assertThat(failure.diagnostics())
        .filteredOn(d -> d.rule().equals(rule))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.message()).isEqualTo(message);
              assertThat(d.fix()).isEqualTo(fix);
              assertThat(d.location().page()).isEqualTo("Test");
              assertThat(d.location().tag()).isEqualTo(tag);
              assertThat(d.location().domId()).isEqualTo(domId);
            });
  }

  @Test
  @DisplayName("a page ref keeps its literal id inside an id scope")
  void pageRefIgnoresIdScope() {
    var section = ElementRef.page("installation");

    var page =
        renderMain(
            new Panel("docs", div().id(section).text("Install")),
            div().child(a(section).text("Install")));

    assertThat(page.elementById("installation")).hasText("Install");
    assertThat(page.element("a")).hasAttribute("href", "#installation");
  }

  @Test
  @DisplayName("refs taken before and after naming an element alias one DOM id")
  void refsAliasOneElement() {
    var named = ElementRef.scoped("menu");
    var menu = div().popover(PopoverMode.AUTO).text("Menu");
    var early = menu.ref();
    menu.anchorName().id(named);

    var page =
        renderMain(
            new Panel(
                "nav",
                div()
                    .children(
                        button().command(early, BuiltinCommand.TOGGLE_POPOVER).text("Open"),
                        button().command(named, BuiltinCommand.HIDE_POPOVER).text("Close"),
                        menu)));

    assertThat(page.elements("button"))
        .extracting(button -> button.attr("commandfor"))
        .containsOnly("nav--menu");
    assertThat(page.elementById("nav--menu")).hasText("Menu");
  }

  @Test
  @DisplayName("two named refs asking for different ids report conflicting-element-id")
  void reportsConflictingElementId() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                render(
                    div().id(ElementRef.scoped("first")).id(ElementRef.page("second")).text("x")));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("conflicting-element-id");
              assertThat(d.message())
                  .isEqualTo(
                      "<div> is bound to ElementRef.scoped(first) and ElementRef.page(second), which ask for different ids ('first' and 'second')");
              assertThat(d.fix())
                  .isEqualTo(
                      "Bind one named ElementRef per element and pass that ref around; extra handles can use element.ref() or ElementRef.anonymous().");
              assertThat(d.location().page()).isEqualTo("Test");
              assertThat(d.location().tag()).isEqualTo("div");
              assertThat(d.location().domId()).isEqualTo("first");
            });
  }

  @Test
  @DisplayName("the id attribute is refused in favour of id(ElementRef)")
  void idAttributeIsRefused() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> div().attr("id", "literal"))
        .withMessage(
            "Use id(ElementRef) instead of the attribute 'id', so the compiler can scope and link it");
  }

  @Test
  @DisplayName("scoped names are sanitized and empty scoped names are refused")
  void scopedNamesAreSanitized() {
    var page =
        renderMain(
            div()
                .children(
                    div().id(ElementRef.scoped("Save draft!!")).text("x"),
                    div().id(ElementRef.scoped("a  b")).text("y")));

    assertThat(page.elementById("Save-draft-")).hasText("x");
    assertThat(page.elementById("a-b")).hasText("y");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ElementRef.scoped(""))
        .withMessage("Identifier cannot be empty");
  }

  static Stream<Arguments> invalidPageNames() {
    return Stream.of(
        argumentSet("empty", ""),
        argumentSet("space", "two words"),
        argumentSet("tab", "two\twords"));
  }

  @ParameterizedTest
  @MethodSource("invalidPageNames")
  @DisplayName("page names must be non-empty and contain no whitespace")
  void rejectsInvalidPageNames(String name) {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ElementRef.page(name))
        .withMessage("Page-level id must be non-empty and contain no whitespace: '" + name + "'");
  }
}
