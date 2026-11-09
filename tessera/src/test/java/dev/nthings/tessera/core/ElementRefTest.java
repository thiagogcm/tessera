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
import dev.nthings.tessera.compiler.Diagnostic;
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
  void resolvesToBindSiteScopeFromInvokerInAnotherView() {
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

    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(
            () -> render(div().child(button().command(dangling, BuiltinCommand.CLOSE).text("x"))))
        .withMessageContainingAll("ElementRef.scoped(ghost)", "not bound");
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
            "duplicate-reference-binding"),
        argumentSet(
            "separate references with the same DOM id",
            div()
                .children(
                    p().id(ElementRef.scoped("confirm")),
                    dialog,
                    button().command(dialog.ref(), BuiltinCommand.SHOW_MODAL).text("Open"),
                    button()),
            "duplicate-id"));
  }

  @ParameterizedTest
  @MethodSource("ambiguousTargets")
  @DisplayName(
      "ambiguous targets report their binding error and independent errors without guessing command compatibility")
  void reportsAmbiguousTargetsWithoutSecondaryCommandErrors(Element body, String rule) {
    var failure = catchThrowableOfType(CompilationException.class, () -> render(body));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactlyInAnyOrder(rule, "interactive-label");
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
  void conflictingNamedRefsReport() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                render(
                    div().id(ElementRef.scoped("first")).id(ElementRef.page("second")).text("x")));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactly("conflicting-element-id");
  }

  @Test
  @DisplayName("the id attribute is refused in favour of id(ElementRef)")
  void idAttributeIsRefused() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> div().attr("id", "literal"))
        .withMessageContaining("id(ElementRef)");
  }

  @Test
  @DisplayName("scoped names are sanitized and a page name with whitespace is refused")
  void namesAreSanitized() {
    var page = renderMain(div().id(ElementRef.scoped("Save draft!!")).text("x"));

    assertThat(page.elementById("Save-draft-")).hasText("x");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ElementRef.page("two words"));
  }
}
