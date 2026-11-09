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
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.KeyedElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Scope expansion")
class ExpansionTest {

  private record Box(String name, Element content) implements Scope {
    @Override
    public String scopeId() {
      return name;
    }

    @Override
    public String scopeName() {
      return "box";
    }

    @Override
    public Element expand(DocumentContext context) {
      return div().child(content);
    }
  }

  private record Exploding() implements Component {
    @Override
    public Element expand(DocumentContext context) {
      throw new IllegalStateException("Broken render");
    }
  }

  @Test
  @DisplayName("numbers generated ids within each scope, so one scope never renumbers another")
  void numbersGeneratedIdsWithinEachScope() {
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

    var second = single.element("[data-scope-id=second] p").id();
    assertThat(second).endsWith("-p-1");
    assertThat(grown.element("[data-scope-id=second] p").id()).isEqualTo(second);
  }

  @Test
  @DisplayName("a failing expand callback keeps its exception and names its component")
  void aFailingExpandCallbackKeepsItsExceptionAndNamesItsComponent() {
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
                        "Expand callback failed: Broken render"));
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
    public IntoElement expand(DocumentContext context) {
      return (Component) _ -> p().text(text);
    }
  }

  @Test
  @DisplayName("a component may return another component from expand")
  void componentMayReturnAComponent() {
    var page = RenderedPage.render(main().child(new Wrapper("nested")));

    assertThat(page.element("main > p").text()).isEqualTo("nested");
  }

  static Stream<Arguments> unstableViews() {
    Scope lambda = _ -> p().text("lambda");
    var anonymous =
        new Scope() {
          @Override
          public IntoElement expand(DocumentContext context) {
            return p().text("anonymous");
          }
        };
    return Stream.of(argumentSet("lambda", lambda), argumentSet("anonymous class", anonymous));
  }

  @ParameterizedTest
  @MethodSource("unstableViews")
  @DisplayName("a scope without a stable class name and the default scopeName is reported")
  void reportsUnstableViewType(Scope view) {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Unstable", main().child(view))));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactly("unstable-scope-name");
  }

  @Test
  @DisplayName("an anonymous scope that names its scopeName compiles")
  void anonymousViewWithViewTypeCompiles() {
    var named =
        new Scope() {
          @Override
          public String scopeName() {
            return "inline-note";
          }

          @Override
          public IntoElement expand(DocumentContext context) {
            return p().text("named");
          }
        };

    var page = RenderedPage.render(main().child(named));

    assertThat(page.element("[data-scope=inline-note]").text()).isEqualTo("named");
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
    return RenderedPage.render(main().children(names.stream().map(ExpansionTest::card).toList()));
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
                    ul().id(ElementRef.scoped("first"))
                        .child(li().id(ElementRef.scoped("item")).key("same")),
                    ul().id(ElementRef.scoped("second"))
                        .child(li().id(ElementRef.scoped("item")).key("same"))));

    assertThat(page.elements("input, li").eachAttr("id")).hasSize(4).doesNotHaveDuplicates();
    assertThat(page.elements("label").eachAttr("for"))
        .containsExactlyElementsOf(page.elements("input").eachAttr("id"));
  }

  /// A list of [#card]s whose parent is bound to the given refs, if any.
  private static Element list(List<String> names, ElementRef... parent) {
    var list = Elements.section().children(names.stream().map(ExpansionTest::card).toList());
    for (var ref : parent) {
      list.id(ref);
    }
    return list;
  }

  @Test
  @DisplayName("keyed ids stay put when another keyed list is added or removed before them")
  void keyedIdsIgnoreOtherKeyedLists() {
    var alone = RenderedPage.render(main().child(list(List.of("a", "b"))));
    var behindAnother =
        RenderedPage.render(
            main()
                .children(
                    list(List.of("x", "y")),
                    p().id(ElementRef.anonymous()),
                    list(List.of("a", "b"))));
    var nested =
        RenderedPage.render(
            main()
                .children(
                    list(List.of("x"), ElementRef.scoped("drafts")),
                    div().child(list(List.of("a", "b")))));

    var ids = alone.elements("h2").eachAttr("id");
    assertThat(behindAnother.elements("h2").eachAttr("id")).endsWith(ids.toArray(String[]::new));
    assertThat(nested.elements("h2").eachAttr("id")).endsWith(ids.toArray(String[]::new));
  }

  @Test
  @DisplayName(
      "keyed lists in one scope that reuse a key collide, and naming their parents fixes it")
  void keyedListsSharingKeysNeedNamedParents() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Lists", main().children(list(List.of("a")), list(List.of("a"))))));

    assertThat(failure.diagnostics()).extracting(Diagnostic::rule).containsOnly("duplicate-id");
    assertThat(failure.diagnostics().getFirst().fix())
        .contains("bind a named ElementRef")
        .contains("each list's parent");

    var named =
        RenderedPage.render(
            main()
                .children(
                    list(List.of("a"), ElementRef.scoped("posts")),
                    list(List.of("a"), ElementRef.page("drafts"))));
    var alone = RenderedPage.render(main().child(list(List.of("a"), ElementRef.scoped("posts"))));

    assertThat(named.elements("h2").eachAttr("id")).hasSize(2).doesNotHaveDuplicates();
    assertThat(named.elements("h2").first().id()).isEqualTo(alone.element("h2").id());
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
    public IntoElement expand(DocumentContext context) {
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

  /// A scope that leaves `scopeId` unset, so the compiler numbers it.
  private record Open(IntoElement content) implements Scope {
    @Override
    public Element expand(DocumentContext context) {
      return div().child(content);
    }
  }

  @Test
  @DisplayName("keyed content inside a scope without scopeId reports generated-scope-id")
  void keyedContentInsideAGeneratedScopeIsReported() {
    var alone =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Keys", main().child(new Open(p().key("a"))))));

    assertThat(alone.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("generated-scope-id");
              assertThat(d.message()).contains("open-1");
              assertThat(d.fix()).contains("scopeId()");
            });

    var behind =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Keys",
                        main().children(new Open(p().text("First")), new Open(p().key("a"))))));

    assertThat(behind.diagnostics())
        .singleElement()
        .satisfies(d -> assertThat(d.message()).contains("open-2"));
  }

  @Test
  @DisplayName("an explicit scopeId keeps keyed content inside that scope")
  void explicitScopeIdAllowsKeyedContent() {
    var page =
        RenderedPage.render(
            main().child(new Box("cards", p().id(ElementRef.scoped("body")).key("a"))));

    assertThat(page.element("p").id()).startsWith("cards--");
  }

  @Test
  @DisplayName("a generated parent id still numbers keyed content in an explicit child scope")
  void generatedParentIdReachesKeyedContent() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Keys", main().child(new Open(new Box("inner", p().key("a")))))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("generated-scope-id");
              assertThat(d.message()).contains("open-1--inner");
            });
  }

  /// A scope whose expand result is the keyed element, with nothing wrapped around the key.
  private record KeyedOpen() implements Scope {
    @Override
    public Element expand(DocumentContext context) {
      return article().key("a");
    }
  }

  /// A component whose expand result is keyed.
  private record KeyedNote() implements Component {
    @Override
    public Element expand(DocumentContext context) {
      return p().key("a");
    }
  }

  @Test
  @DisplayName("a generated scope whose expand result is keyed reports generated-scope-id")
  void keyedExpandResultInsideAGeneratedScopeIsReported() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Keys", main().child(new KeyedOpen()))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("generated-scope-id");
              assertThat(d.message()).contains("keyed-open-1");
            });
  }

  @Test
  @DisplayName("a component that returns keyed content inside a generated scope is reported")
  void componentReturningKeyedContentInsideAGeneratedScopeIsReported() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument("Keys", main().child(new Open(new KeyedNote())))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("generated-scope-id");
              assertThat(d.message()).contains("open-1");
            });
  }

  @Test
  @DisplayName("deferred keyed content inside a generated scope is reported")
  void deferredKeyedContentInsideAGeneratedScopeIsReported() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Keys", main().child(new Open(Elements.deferred(p().key("a")))))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("generated-scope-id");
              assertThat(d.message()).contains("open-1");
            });
  }

  @Test
  @DisplayName("a generated scope id and the same explicit scopeId share one id counter")
  void generatedAndExplicitScopeIdsShareOneCounter() {
    var page =
        RenderedPage.render(
            main()
                .children(
                    new Open(p().id(ElementRef.anonymous())),
                    new Box("open-1", p().id(ElementRef.anonymous()))));

    assertThat(page.elements("p").eachAttr("id")).containsExactly("open-1--p-1", "open-1--p-2");
  }

  @Test
  @DisplayName("a generated parent and an explicit scope with the same id share the nested counter")
  void generatedParentAndExplicitScopeShareTheNestedCounter() {
    var page =
        RenderedPage.render(
            main()
                .children(
                    new Open(new Box("inner", p().id(ElementRef.anonymous()))),
                    new Box(
                        "open-1", new Box("inner", p().id(ElementRef.anonymous())).intoElement())));

    assertThat(page.elements("p").eachAttr("id"))
        .containsExactly("open-1--inner--p-1", "open-1--inner--p-2");
  }
}
