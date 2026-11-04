package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.RenderedPage.render;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.render.AccessibilityViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ElementRef")
class ElementRefTest {

  /// View wrapping arbitrary content so a test can place elements in a nested scope.
  record Panel(String id, Element content) implements RenderView {

    @Override
    public ElementId entityId() {
      return ElementId.of(id);
    }

    @Override
    public Element render() {
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
            new Panel("dialogs", dialog().id(target).text("Sure?")));

    assertThat(page.element("button")).hasAttribute("commandfor", "dialogs--confirm");
    assertThat(page.elementById("dialogs--confirm")).hasTagName("dialog");
  }

  @Test
  @DisplayName("an unbound ref fails compilation")
  void unboundRefFailsCompilation() {
    var dangling = ElementRef.named("ghost");

    assertThatExceptionOfType(AccessibilityViolationException.class)
        .isThrownBy(
            () -> render(div().child(button().command(dangling, BuiltinCommand.CLOSE).text("x"))))
        .withMessageContainingAll("ElementRef(ghost)", "not bound");
  }

  @Test
  @DisplayName("a ref bound to more than one element fails compilation")
  void refBoundTwiceFailsCompilation() {
    var shared = ElementRef.named("once");

    assertThatExceptionOfType(AccessibilityViolationException.class)
        .isThrownBy(() -> render(div().children(p().id(shared), p().id(shared))))
        .withMessageContaining("bound to more than one element");
  }
}
