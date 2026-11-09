package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.article;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.ul;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.KeyedElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
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

  /// A list item whose heading labels it through a scoped ref, keyed by `name`.
  private static Element card(String name) {
    var title = ElementRef.scoped("title");
    return article()
        .ariaLabelledBy(title)
        .children(h2().id(title).text(name), p().id(ElementRef.anonymous()).text("Body"))
        .key(name);
  }

  private static RenderedPage cards(List<String> names) {
    return RenderedPage.render(
        main().children(names.stream().map(ViewExpansionTest::card).toList()));
  }

  @Test
  @DisplayName("keyed items keep their ids when siblings are inserted, removed and reordered")
  void keyedItemsKeepTheirIdsWhenSiblingsMove() {
    var before = cards(List.of("a", "b"));
    var after = cards(List.of("c", "b"));
    var reordered = cards(List.of("b", "d", "a"));

    var b = before.elements("article").get(1);
    assertThat(after.elements("article").get(1).outerHtml()).isEqualTo(b.outerHtml());
    assertThat(reordered.elements("article").getFirst().outerHtml()).isEqualTo(b.outerHtml());
    assertThat(reordered.elements("article").get(2).outerHtml())
        .isEqualTo(before.elements("article").getFirst().outerHtml());
    assertThat(reordered.elements("article"))
        .allSatisfy(
            card ->
                assertThat(reordered.elementById(card.attr("aria-labelledby")).parent())
                    .isEqualTo(card));
  }

  @Test
  @DisplayName("distinct keys give distinct ids, even when they differ only in punctuation or case")
  void distinctKeysGiveDistinctIds() {
    var page = cards(List.of("row-a", "row_a", "Row-a", "row a"));

    assertThat(page.elements("h2").eachAttr("id")).hasSize(4).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("keyed content is emitted in place, without a wrapper")
  void keyedContentIsEmittedInPlace() {
    var page =
        RenderedPage.render(
            main()
                .child(
                    ul().children(
                            li().text("First").key("first"),
                            fragment()
                                .children(li().text("Second"), li().text("Third"))
                                .key("pair"),
                            li().text("Fourth"))));

    assertThat(page.elements("main > ul > *").eachText())
        .containsExactly("First", "Second", "Third", "Fourth");
  }

  @Test
  @DisplayName("a key repeated among siblings is reported at the second sibling")
  void repeatedSiblingKeyIsReported() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument("Keys", main().children(p().key("a"), p().key("a")))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("duplicate-key");
              assertThat(d.message()).isEqualTo("Duplicate sibling key 'a'");
              assertThat(d.location().components()).containsExactly("[key=a]");
            });
  }

  @Test
  @DisplayName("the same key under different parents is valid and gives distinct ids")
  void sameKeyUnderDifferentParentsIsValid() {
    var page =
        RenderedPage.render(
            main()
                .children(
                    div().child(Field.of("Left", Forms.text("left")).key("same")),
                    div()
                        .child(
                            Elements.late(_ -> Field.of("Right", Forms.text("right")).key("same"))),
                    ul().child(li().id(ElementRef.scoped("item")).key("same")),
                    ul().child(li().id(ElementRef.scoped("item")).key("same"))));

    assertThat(page.elements("input, li").eachAttr("id")).hasSize(4).doesNotHaveDuplicates();
    assertThat(page.elements("label").eachAttr("for"))
        .containsExactlyElementsOf(page.elements("input").eachAttr("id"));
  }

  @Test
  @DisplayName("a blank key is rejected and a later key replaces an earlier one")
  void blankKeyIsRejectedAndTheLastKeyWins() {
    var content = p().text("Item");

    assertThatIllegalArgumentException().isThrownBy(() -> content.key(" "));
    assertThat(content.key("first").key("second")).isEqualTo(new KeyedElement("second", content));
  }

  /// A component whose scoped ref and diagnostic show which scope and path it renders in.
  private record Note(String text) implements Component {
    @Override
    public IntoElement render(RenderContext context) {
      context.report("note-rendered", "Rendered " + text, "Nothing to fix.");
      return p().id(ElementRef.scoped("note")).text(text);
    }
  }

  @Test
  @DisplayName("a keyed component takes its key on its own boundary")
  void keyedComponentTakesItsKeyOnItsOwnBoundary() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Notes", main().child(new Box("outer", new Note("A").key("a"))))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d ->
                assertThat(d.location().components()).containsExactly("box[outer]", "Note[key=a]"));
  }

  @Test
  @DisplayName("a keyed component keeps its scoped ids when siblings are inserted before it")
  void keyedComponentKeepsItsScopedIds() {
    Component quiet = _ -> p().id(ElementRef.scoped("note")).text("Quiet");
    var alone = RenderedPage.render(main().child(quiet.key("q")));
    var behind =
        RenderedPage.render(
            main().children(p().id(ElementRef.anonymous()), quiet.key("x"), quiet.key("q")));

    var id = alone.element("p").id();
    assertThat(id).endsWith("--note").doesNotContain("----");
    assertThat(behind.elements("p").last().id()).isEqualTo(id);
  }
}
