package dev.nthings.tessera.render;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("View expansion")
class ViewExpansionTest {

  private record Box(String name, Element content) implements RenderView {
    @Override
    public ElementId entityId() {
      return ElementId.of(name);
    }

    @Override
    public String viewType() {
      return "box";
    }

    @Override
    public Element render(RenderContext context) {
      return div().child(content);
    }
  }

  private record Exploding() implements RenderOnce {
    @Override
    public Element render(RenderContext context) {
      throw new IllegalStateException("Broken render");
    }
  }

  @Test
  @DisplayName("numbers generated ids within each view, so one view never renumbers another")
  void numbersGeneratedIdsWithinEachView() {
    var single =
        RenderedPage.render(
            main()
                .children(
                    new Box("first", p().id(ElementRef.anonymous())),
                    new Box("second", p().id(ElementRef.anonymous()))));
    var grown =
        RenderedPage.render(
            main()
                .children(
                    new Box(
                        "first",
                        fragment()
                            .children(
                                p().id(ElementRef.anonymous()), p().id(ElementRef.anonymous()))),
                    new Box("second", p().id(ElementRef.anonymous()))));

    var second = single.element("[data-view-id=second] p").id();
    assertThat(second).endsWith("-p-1");
    assertThat(grown.element("[data-view-id=second] p").id()).isEqualTo(second);
  }

  @Test
  @DisplayName("a failing render callback keeps its exception and names its component")
  void aFailingRenderCallbackKeepsItsExceptionAndNamesItsComponent() {
    var failure =
        catchThrowable(
            () ->
                PageCompiler.compile(
                    new PageDocument(
                        "Broken", main().child(new Box("outer", new Exploding().intoElement())))));

    assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("Broken render");
    assertThat(failure.getSuppressed())
        .singleElement()
        .satisfies(
            context ->
                assertThat(context)
                    .hasMessageContaining(
                        "[callback-failure]",
                        "page \"Broken\"",
                        "box[outer] > Exploding",
                        "Render callback failed: Broken render"));
  }

  @Test
  @DisplayName("late conversion failures retain their page and component context")
  void lateConversionFailuresRetainContext() {
    IntoElement broken =
        () -> {
          throw new IllegalStateException("Broken conversion");
        };
    var failure =
        catchThrowable(
            () ->
                PageCompiler.compile(
                    new PageDocument(
                        "Late page", main().child(new Box("outer", Elements.late(_ -> broken))))));

    assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("Broken conversion");
    assertThat(failure.getSuppressed())
        .singleElement()
        .satisfies(
            context ->
                assertThat(context)
                    .hasMessageContaining(
                        "[callback-failure]",
                        "page \"Late page\"",
                        "box[outer]",
                        "Late callback failed: Broken conversion"));
  }
}
