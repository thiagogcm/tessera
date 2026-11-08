package dev.nthings.tessera.core;

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

import dev.nthings.tessera.render.CompilationDiagnostic;
import dev.nthings.tessera.render.CompilationException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ElementRef")
class ElementRefTest {

  /// View wrapping arbitrary content so a test can place elements in a nested scope.
  record Panel(String id, Element content) implements RenderView {

    @Override
    public ElementId entityId() {
      return ElementId.of(id);
    }

    @Override
    public Element render(RenderContext context) {
      return div().child(content);
    }
  }

  @Test
  @DisplayName("resolves to its bind-site scope from an invoker in another view")
  void resolvesToBindSiteScopeFromInvokerInAnotherView() {
    var target = ElementRef.named("confirm");

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
    var dangling = ElementRef.named("ghost");

    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(
            () -> render(div().child(button().command(dangling, BuiltinCommand.CLOSE).text("x"))))
        .withMessageContainingAll("ElementRef(ghost)", "not bound");
  }

  static Stream<Arguments> ambiguousTargets() {
    var shared = ElementRef.named("confirm");
    var dialog = dialog().id("confirm").ariaLabel("Confirm").text("Confirm?");
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
                    p().id("confirm"),
                    dialog,
                    button().command(dialog, BuiltinCommand.SHOW_MODAL).text("Open"),
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
        .extracting(CompilationDiagnostic::rule)
        .containsExactlyInAnyOrder(rule, "interactive-label");
  }
}
