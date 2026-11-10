package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.article;
import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.empty;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.late;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.text;
import static dev.nthings.tessera.core.Elements.ul;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.KeyedElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.support.RenderedPage;
import dev.nthings.tessera.view.Diagnostic;
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
  @DisplayName("generated ids number within each scope")
  void numbersGeneratedIdsPerScope() {
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
    assertThat(second).isEqualTo("second--p-1");
    assertThat(grown.element("[data-scope-id=second] p").id()).isEqualTo(second);
  }

  @Test
  @DisplayName("a failing expand callback keeps its exception and names its component")
  void reportsExpandCallbackFailure() {
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
  void retainsLateFailureContext() {
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
  void reportsUnstableScopeName(Scope view) {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Unstable", main().child(view))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("unstable-scope-name");
              assertThat(d.message())
                  .contains(
                      "is an anonymous class or lambda, so its default scopeName() changes"
                          + " between builds");
              assertThat(d.fix())
                  .isEqualTo(
                      "Implement the scope as a named class or record, or override scopeName().");
              assertThat(d.location().page()).isEqualTo("Unstable");
              assertThat(d.location().components()).hasSize(1);
            });
  }

  @Test
  @DisplayName("an anonymous scope that names its scopeName compiles")
  void anonymousScopeWithNameCompiles() {
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
  void locatesComponentDiagnostic() {
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
              assertThat(d.message()).isEqualTo("Two hero images");
              assertThat(d.fix()).isEqualTo("Keep one hero image.");
              assertThat(d.location().page()).isEqualTo("Strict");
              assertThat(d.location().tag()).isEmpty();
              assertThat(d.location().components()).containsExactly("box[outer]", "Component");
            });
  }

  @Test
  @DisplayName("a heading whose outline target is never bound is reported")
  void reportsUnboundHeadingTarget() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Headings",
                        main()
                            .child(Heading.of(2, "Missing").outline(ElementRef.page("missing"))))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("unbound-reference");
              assertThat(d.message())
                  .isEqualTo(
                      "Heading \"Missing\" references ElementRef.page(missing), which is not"
                          + " bound to any element on this page");
              assertThat(d.fix())
                  .isEqualTo("Bind this reference with .id(ref) on the intended target.");
              assertThat(d.location().page()).isEqualTo("Headings");
              assertThat(d.location().tag()).isEqualTo("h2");
              assertThat(d.location().components()).containsExactly("Heading");
            });
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
  void keyedItemsKeepIds() {
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
  void emitsKeyedContentInPlace() {
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
              assertThat(d.fix())
                  .isEqualTo("Give each child in this parent a distinct stable key.");
              assertThat(d.location().page()).isEqualTo("Keys");
              assertThat(d.location().components()).containsExactly("[key=a]");
            });
  }

  @Test
  @DisplayName("the same key under different parents is valid and gives distinct ids")
  void allowsSameKeyAcrossParents() {
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
  @DisplayName("keyed ids stay stable when other keyed lists precede them")
  void keyedIdsStayStable() {
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
  @DisplayName("keyed lists sharing a key collide unless their parents are named")
  void sharedKeysNeedNamedParents() {
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
  void rejectsBlankKeyAndUsesLast() {
    var content = p().text("Item");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> content.key(" "))
        .withMessage("A sibling key must not be blank");
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
  void keysComponentAtItsBoundary() {
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
            d -> {
              assertThat(d.rule()).isEqualTo("note-rendered");
              assertThat(d.message()).isEqualTo("Rendered A");
              assertThat(d.fix()).isEqualTo("Nothing to fix.");
              assertThat(d.location().page()).isEqualTo("Notes");
              assertThat(d.location().components()).containsExactly("box[outer]", "Note[key=a]");
            });
  }

  @Test
  @DisplayName("a keyed component keeps its scoped ids when siblings are inserted before it")
  void keyedComponentKeepsScopedIds() {
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

  static Stream<Arguments> keyedContentInGeneratedScope() {
    return Stream.of(
        argumentSet(
            "keyed content inside a scope without scopeId reports generated-scope-id",
            main().child(new Open(p().key("a"))),
            "open-1",
            List.of("open[open-1]", "[key=a]")),
        argumentSet(
            "keyed content in a later generated scope reports its own counter",
            main().children(new Open(p().text("First")), new Open(p().key("a"))),
            "open-2",
            List.of("open[open-2]", "[key=a]")),
        argumentSet(
            "a generated parent id still flags keyed content in an explicit child scope",
            main().child(new Open(new Box("inner", p().key("a")))),
            "open-1--inner",
            List.of("open[open-1]", "box[open-1--inner]", "[key=a]")),
        argumentSet(
            "a generated scope whose expand result is keyed reports generated-scope-id",
            main().child(new KeyedOpen()),
            "keyed-open-1",
            List.of("keyed-open[keyed-open-1]", "[key=a]")),
        argumentSet(
            "a component that returns keyed content inside a generated scope is reported",
            main().child(new Open(new KeyedNote())),
            "open-1",
            List.of("open[open-1]", "KeyedNote", "[key=a]")),
        argumentSet(
            "deferred keyed content inside a generated scope is reported",
            main().child(new Open(Elements.deferred(p().key("a")))),
            "open-1",
            List.of("open[open-1]", "[key=a]")));
  }

  @ParameterizedTest
  @MethodSource("keyedContentInGeneratedScope")
  @DisplayName("keyed content in a generated id scope reports generated-scope-id")
  void reportsKeyedContentInGeneratedScope(
      Element body, String generatedScopeId, List<String> components) {
    var failure =
        catchThrowableOfType(
            CompilationException.class, () -> Compiler.compile(new PageDocument("Keys", body)));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("generated-scope-id");
              assertThat(d.message())
                  .isEqualTo(
                      "Keyed content is inside scope '"
                          + generatedScopeId
                          + "', whose id was generated");
              assertThat(d.fix())
                  .isEqualTo(
                      "Set scopeId() on each enclosing scope. A generated id is numbered in"
                          + " encounter order, so moving a sibling scope renames every keyed id"
                          + " inside it.");
              assertThat(d.location().page()).isEqualTo("Keys");
              assertThat(d.location().components()).containsExactlyElementsOf(components);
            });
  }

  @Test
  @DisplayName("an explicit scopeId keeps keyed content inside that scope")
  void explicitScopeIdAllowsKeyedContent() {
    var page =
        RenderedPage.render(
            main().child(new Box("cards", p().id(ElementRef.scoped("body")).key("a"))));

    assertThat(page.element("p").id()).isEqualTo("cards--key-0061--body");
  }

  @Test
  @DisplayName("a generated scope id and the same explicit scopeId share one id counter")
  void sharesScopeIdCounter() {
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
  void sharesNestedIdCounter() {
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

  /// A scope whose root is whatever the test supplies.
  private record Host(IntoElement content) implements Scope {
    @Override
    public String scopeId() {
      return "host";
    }

    @Override
    public String scopeName() {
      return "host";
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return content;
    }
  }

  /// A scope that returns another scope as its root.
  private record Outer() implements Scope {
    @Override
    public String scopeId() {
      return "outer";
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return new Box("inner", p().text("Inside"));
    }
  }

  /// A component whose result is a fragment.
  private record FragmentNote() implements Component {
    @Override
    public IntoElement expand(DocumentContext context) {
      return fragment().children(p().text("One"), p().text("Two"));
    }
  }

  /// A component whose result is a scope.
  private record NestedScope() implements Component {
    @Override
    public IntoElement expand(DocumentContext context) {
      return new Box("inner", p().text("Inside"));
    }
  }

  /// A component whose result is one list element.
  private record Listed() implements Component {
    @Override
    public Element expand(DocumentContext context) {
      return ul().children(li().text("One"), li().text("Two"));
    }
  }

  /// A scope that delegates to one unscoped list component.
  private record ItemList() implements Scope {
    @Override
    public String scopeId() {
      return "items";
    }

    @Override
    public String scopeName() {
      return "items";
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return new Listed();
    }
  }

  /// A scope whose host is the keyed element itself.
  private record KeyedHost() implements Scope {
    @Override
    public String scopeId() {
      return "cards";
    }

    @Override
    public String scopeName() {
      return "keyed";
    }

    @Override
    public Element expand(DocumentContext context) {
      return article().child(p().text("Body")).key("a");
    }
  }

  static Stream<Arguments> invalidScopeRoots() {
    return Stream.of(
        argumentSet("empty fragment", new Host(fragment())),
        argumentSet("one-child fragment", new Host(fragment().child(p().text("Only")))),
        argumentSet(
            "two-child fragment", new Host(fragment().children(p().text("One"), p().text("Two")))),
        argumentSet("text", new Host(text("Loose"))),
        argumentSet("raw HTML", new Host(rawHtml("<p>Hi</p>"))),
        argumentSet("nothing", new Host(empty())),
        argumentSet("deferred content", new Host(deferred(p().text("Moved")))),
        argumentSet("late content", new Host(late(_ -> p().text("Later")))),
        argumentSet("another scope", new Outer()),
        argumentSet("a component that returns a fragment", new Host(new FragmentNote())),
        argumentSet("a component that returns a scope", new Host(new NestedScope())));
  }

  @ParameterizedTest
  @MethodSource("invalidScopeRoots")
  @DisplayName("a scope root that is not one element host reports scope-root")
  void rejectsNonHostScopeRoot(Scope scope) {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Scopes", main().child(scope))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("scope-root");
              assertThat(d.fix()).isNotBlank();
              assertThat(d.location().page()).isEqualTo("Scopes");
              assertThat(d.location().components()).isNotEmpty();
            });
  }

  @Test
  @DisplayName("a component that returns one element is the scope host")
  void componentDelegationStampsReturnedElement() {
    var page = RenderedPage.render(main().child(new ItemList()));
    var list = page.element("main > ul");

    assertThat(list.attr("data-scope")).isEqualTo("items");
    assertThat(list.attr("data-scope-id")).isEqualTo("items");
    assertThat(list.children()).extracting(element -> element.text()).containsExactly("One", "Two");
    assertThat(page.elements("main > div")).isEmpty();
  }

  @Test
  @DisplayName("a keyed element can be the scope host")
  void keyedElementCarriesScope() {
    var page = RenderedPage.render(main().child(new KeyedHost()));
    var host = page.element("main > article");

    assertThat(host.attr("data-scope")).isEqualTo("keyed");
    assertThat(host.attr("data-scope-id")).isEqualTo("cards");
    assertThat(host.selectFirst("p").text()).isEqualTo("Body");
    assertThat(page.elements("main > div")).isEmpty();
  }
}
