package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.article;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatNoException;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowable;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
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
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Document expansion")
class ExpansionTest {

  private record Box(String name, IntoElement content) implements Scope {
    @Override
    public String scopeId() {
      return name;
    }

    @Override
    public String scopeName() {
      return "box";
    }

    @Override
    public TagElement expand(ViewContext context) {
      return div().child(content);
    }
  }

  /// A document scope around document `content`.
  private record DocBox(String name, DocumentContent content) implements DocumentScope {
    @Override
    public String scopeId() {
      return name;
    }

    @Override
    public String scopeName() {
      return "doc-box";
    }

    @Override
    public DocumentElement expand(DocumentContext context) {
      return DocumentElement.of(div()).child(content);
    }
  }

  /// A scope that leaves `scopeId` unset, so the compiler numbers it.
  private record Open(IntoElement content) implements Scope {
    @Override
    public TagElement expand(ViewContext context) {
      return div().child(content);
    }
  }

  @Test
  @DisplayName("generated ids in one scope do not shift when an earlier scope grows")
  void numbersGeneratedIdsPerScope() {
    var single =
        RenderedPage.renderMain(
            new Box("first", p().id(ElementRef.anonymous())),
            new Box("second", p().id(ElementRef.anonymous())));
    var grown =
        RenderedPage.renderMain(
            new Box(
                "first",
                fragment()
                    .children(p().id(ElementRef.anonymous()), p().id(ElementRef.anonymous()))),
            new Box("second", p().id(ElementRef.anonymous())));

    assertThat(grown.element("[data-scope-id=second] p").id())
        .isEqualTo(single.element("[data-scope-id=second] p").id());
  }

  private record Exploding() implements Component {
    @Override
    public Element expand(ViewContext context) {
      throw new IllegalStateException("Broken render");
    }
  }

  static Stream<Arguments> callbackFailures() {
    IntoElement brokenConversion =
        () -> {
          throw new IllegalStateException("Broken render");
        };
    return Stream.of(
        argumentSet(
            "an expand callback",
            DocumentElement.of(main()).child(new Box("outer", new Exploding())),
            List.of("box[outer]", "Exploding")),
        argumentSet(
            "a late callback",
            DocumentElement.of(main())
                .child(
                    new DocBox("outer", Documents.late(_ -> DocumentContent.of(brokenConversion)))),
            List.of("doc-box[outer]")));
  }

  @ParameterizedTest
  @MethodSource("callbackFailures")
  @DisplayName("a failing callback keeps its exception and names its page and component path")
  void reportsCallbackFailureContext(DocumentElement body, List<String> components) {
    var failure = catchThrowable(() -> Compiler.forPage().compile(PageDocument.of("Broken", body)));

    assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("Broken render");
    assertThat(failure.getSuppressed())
        .singleElement()
        .isInstanceOfSatisfying(
            CompilationException.class,
            context ->
                assertThat(context.diagnostics())
                    .extracting(
                        d -> d.rule(), d -> d.location().page(), d -> d.location().components())
                    .containsExactly(tuple("callback-failure", "Broken", components)));
  }

  static Stream<Arguments> unstableScopes() {
    Scope lambda = _ -> p().text("lambda");
    var anonymous =
        new Scope() {
          @Override
          public TagElement expand(ViewContext context) {
            return p().text("anonymous");
          }
        };
    return Stream.of(argumentSet("lambda", lambda), argumentSet("anonymous class", anonymous));
  }

  @ParameterizedTest
  @MethodSource("unstableScopes")
  @DisplayName("a scope whose class name changes between builds must name its scopeName")
  void reportsUnstableScopeName(Scope scope) {
    assertThat(
            Diagnostics.rules(
                () -> Compiler.forPage().compile(PageDocument.of("Unstable", main(scope)))))
        .containsExactly("unstable-scope-name");
  }

  @Test
  @DisplayName("an anonymous scope with its own scopeName compiles")
  void acceptsAnonymousScopeWithName() {
    var named =
        new Scope() {
          @Override
          public String scopeName() {
            return "inline-note";
          }

          @Override
          public TagElement expand(ViewContext context) {
            return p().text("named");
          }
        };

    assertThat(RenderedPage.renderMain(named).element("[data-scope=inline-note]").text())
        .isEqualTo("named");
  }

  @Test
  @DisplayName("a component's own diagnostic is reported as written at the reporting component")
  void locatesComponentDiagnostic() {
    Component strict =
        context -> {
          context.report("hero-conflict", "Two hero images", "Keep one hero image.");
          return p().text("content");
        };

    var failure =
        catchThrowable(() ->
            Compiler.forPage().compile(PageDocument.of("Strict", main(new Box("outer", strict)))));

    assertThat(failure)
        .isInstanceOfSatisfying(
            CompilationException.class,
            compilation ->
                assertThat(compilation.diagnostics())
                    .extracting(
                        d -> d.rule(),
                        d -> d.message(),
                        d -> d.fix(),
                        d -> d.location().components())
                    .containsExactly(
                        tuple(
                            "hero-conflict",
                            "Two hero images",
                            "Keep one hero image.",
                            List.of("box[outer]", "Component"))));
  }

  @Test
  @DisplayName("a heading whose outline target is never bound is reported at the heading")
  void reportsUnboundHeadingTarget() {
    var heading = Heading.of(2, "Missing").outline(ElementRef.page("missing"));

    assertThat(
            Diagnostics.of(
                () -> Compiler.forPage().compile(PageDocument.of("Headings", main(heading)))))
        .extracting(Found::rule, Found::tag, Found::components)
        .containsExactly(tuple("unbound-reference", "h2", List.of("Heading")));
  }

  /// A list item whose heading labels it through a scoped ref, keyed by `name`.
  private static Element card(String name) {
    var title = ElementRef.scoped("title");
    return article()
        .ariaLabelledBy(title)
        .children(h2().id(title).text(name), p().id(ElementRef.anonymous()).text("Body"))
        .key(name);
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
    var alone = RenderedPage.renderMain(list(List.of("a", "b")));
    var behindAnother =
        RenderedPage.renderMain(
            list(List.of("x", "y")), p().id(ElementRef.anonymous()), list(List.of("a", "b")));
    var nested =
        RenderedPage.renderMain(
            list(List.of("x"), ElementRef.scoped("drafts")), div().child(list(List.of("a", "b"))));

    var ids = alone.elements("h2").eachAttr("id").toArray(String[]::new);
    assertThat(behindAnother.elements("h2").eachAttr("id")).endsWith(ids);
    assertThat(nested.elements("h2").eachAttr("id")).endsWith(ids);
  }

  @Test
  @DisplayName("keyed lists sharing a key collide unless their parents are named")
  void sharedKeysNeedNamedParents() {
    assertThat(
            Diagnostics.rules(() ->
                Compiler.forPage()
                    .compile(
                        PageDocument.of("Lists", main(list(List.of("a")), list(List.of("a")))))))
        .isNotEmpty()
        .containsOnly("duplicate-id");

    var named =
        RenderedPage.renderMain(
            list(List.of("a"), ElementRef.scoped("posts")),
            list(List.of("a"), ElementRef.page("drafts")));
    var alone = RenderedPage.renderMain(list(List.of("a"), ElementRef.scoped("posts")));

    assertThat(named.elements("h2").eachAttr("id")).hasSize(2).doesNotHaveDuplicates();
    assertThat(named.elements("h2").first().id()).isEqualTo(alone.element("h2").id());
  }

  @Test
  @DisplayName("a keyed component keeps its scoped ids when siblings are inserted before it")
  void keyedComponentKeepsScopedIds() {
    Component quiet = _ -> p().id(ElementRef.scoped("note")).text("Quiet");
    var alone = RenderedPage.renderMain(quiet.key("q"));
    var behind =
        RenderedPage.renderMain(p().id(ElementRef.anonymous()), quiet.key("x"), quiet.key("q"));

    assertThat(behind.elements("p").last().id()).isEqualTo(alone.element("p").id());
  }

  static Stream<Arguments> keyedContentInGeneratedScope() {
    return Stream.of(
        argumentSet("directly inside", main().child(new Open(p().key("a")))),
        argumentSet(
            "inside an explicit child scope",
            main().child(new Open(new Box("inner", p().key("a"))))));
  }

  @ParameterizedTest
  @MethodSource("keyedContentInGeneratedScope")
  @DisplayName("keyed content under a scope with a generated id is reported")
  void reportsKeyedContentInGeneratedScope(Element body) {
    assertThat(Diagnostics.rules(() -> Compiler.forPage().compile(PageDocument.of("Keys", body))))
        .containsExactly("generated-scope-id");
  }

  @Test
  @DisplayName("keyed content under scopes with explicit ids compiles")
  void acceptsKeyedContentInExplicitScope() {
    var body = main().child(new Box("cards", new Box("inner", p().key("a"))));

    assertThatNoException()
        .isThrownBy(() -> Compiler.forPage().compile(PageDocument.of("Keys", body)));
  }

  @Test
  @DisplayName("a generated scope id and the same explicit scopeId never give colliding ids")
  void generatedAndExplicitScopeIdsDoNotCollide() {
    var page =
        RenderedPage.renderMain(
            new Open(p().id(ElementRef.anonymous())),
            new Box("open-1", p().id(ElementRef.anonymous())),
            new Open(new Box("inner", p().id(ElementRef.anonymous()))),
            new Box("open-2", new Box("inner", p().id(ElementRef.anonymous()))));

    assertThat(page.elements("p").eachAttr("id")).hasSize(4).doesNotHaveDuplicates();
  }
}
