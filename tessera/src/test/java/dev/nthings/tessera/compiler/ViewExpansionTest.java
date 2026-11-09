package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.support.RenderedPage;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("View expansion")
class ViewExpansionTest {

  private record Box(String name, Element content) implements View {
    @Override
    public String viewId() {
      return name;
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

  private record Exploding() implements Component {
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
                Compiler.compile(
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
                Compiler.compile(
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

  /// A component that returns another component, with no `.intoElement()` in between.
  private record Wrapper(String text) implements Component {
    @Override
    public IntoElement render(RenderContext context) {
      return (Component) _ -> p().text(text);
    }
  }

  @Test
  @DisplayName("a component may return another component from render")
  void componentMayReturnAComponent() {
    var page = RenderedPage.render(main().child(new Wrapper("nested")));

    assertThat(page.element("main > p").text()).isEqualTo("nested");
  }

  static Stream<Arguments> unstableViews() {
    View lambda = _ -> p().text("lambda");
    var anonymous =
        new View() {
          @Override
          public IntoElement render(RenderContext context) {
            return p().text("anonymous");
          }
        };
    return Stream.of(argumentSet("lambda", lambda), argumentSet("anonymous class", anonymous));
  }

  @ParameterizedTest
  @MethodSource("unstableViews")
  @DisplayName("a view without a stable class name and the default viewType is reported")
  void reportsUnstableViewType(View view) {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Unstable", main().child(view))));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactly("unstable-view-type");
  }

  @Test
  @DisplayName("an anonymous view that names its viewType compiles")
  void anonymousViewWithViewTypeCompiles() {
    var named =
        new View() {
          @Override
          public String viewType() {
            return "inline-note";
          }

          @Override
          public IntoElement render(RenderContext context) {
            return p().text("named");
          }
        };

    var page = RenderedPage.render(main().child(named));

    assertThat(page.element("[data-view=inline-note]").text()).isEqualTo("named");
  }

  @Test
  @DisplayName("a component diagnostic is located at the reporting component")
  void componentDiagnosticIsLocatedAtTheComponent() {
    Component strict =
        context -> {
          context.report("hero-conflict", "Two hero images", "Keep one hero image.");
          return p().text("content");
        };

    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Strict", main().child(new Box("outer", strict.intoElement())))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("hero-conflict");
              assertThat(d.fix()).isEqualTo("Keep one hero image.");
              assertThat(d.location().components()).containsExactly("box[outer]", "Component");
            });
  }

  @Test
  @DisplayName("a registered heading whose target is never bound is reported")
  void unboundHeadingTargetIsReported() {
    Component heading =
        context -> {
          context.registerHeading(ElementRef.page("missing"), "Missing", 2);
          return p().text("No heading element");
        };

    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Headings", main().child(heading))));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactly("unbound-reference");
  }
}
