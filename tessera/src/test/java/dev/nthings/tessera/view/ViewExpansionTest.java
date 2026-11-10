package dev.nthings.tessera.view;

import static dev.nthings.tessera.core.Elements.article;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.support.Snapshots.styleRules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Footnote;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.components.Select;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.css.Tokens;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Shared view expansion")
class ViewExpansionTest {
  private static final DocumentKey<Object> END = DocumentKey.of("end", Object.class, Object::new);

  @Test
  @DisplayName("static and live expansion preserve component semantics and styles")
  void staticAndLiveExpansionPreserveComponentSemanticsAndStyles() {
    var field = Field.of("Name", Forms.text("name")).hint("Full name");
    var content =
        main()
            .children(
                Button.of("Save"),
                field,
                fragment().children(p().text("Visible"), p().text("Details")),
                Elements.ul()
                    .role(Role.LIST)
                    .child(fragment().child(Elements.li().role(Role.LISTITEM).text("Entry"))));
    var live = render(content);
    var page = Compiler.compile(new PageDocument("Editor", content));

    assertThat(semantics(dom(live))).isEqualTo(semantics(Jsoup.parse(page.html())));
    assertThat(styleRules(live))
        .filteredOn(
            rule -> rule.startsWith("@layer utilities{") || rule.startsWith("@layer components{"))
        .isNotEmpty()
        .allSatisfy(
            rule ->
                assertThat(page.css())
                    .contains(rule.substring(rule.indexOf('{') + 1, rule.length() - 1)));
  }

  @ParameterizedTest
  @MethodSource("invalidPortableContent")
  @DisplayName("static and mounted content reject the same element and reference errors")
  void staticAndMountedContentRejectTheSameErrors(IntoElement content, List<String> rules) {
    var live = catchThrowableOfType(ViewException.class, () -> render(content));
    var page =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Invalid", content)));
    assertThat(live.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactlyInAnyOrderElementsOf(rules);
    assertThat(page.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactlyInAnyOrderElementsOf(rules);
  }

  private static Stream<Arguments> invalidPortableContent() {
    var command = ElementRef.scoped("target");
    var label = ElementRef.scoped("label-target");
    return Stream.of(
        Arguments.of(
            main().child(Elements.el(HtmlTag.IMG).attr("src", "/image.png")),
            List.of("image-alt", "image-dimensions")),
        Arguments.of(
            main()
                .children(
                    Elements.button().text("Open").command(command, BuiltinCommand.SHOW_MODAL),
                    fragment().child(div().id(command))),
            List.of("incompatible-command-target")),
        Arguments.of(
            main()
                .children(
                    Elements.label().labelFor(label).text("Name"),
                    fragment().child(div().id(label))),
            List.of("label-target")));
  }

  @Test
  @DisplayName("embedded rules style the mount host without restyling the host page")
  void embeddedRulesStyleTheMountHostWithoutRestylingTheHostPage() {
    var theme = Theme.defaults();
    var rules = styleRules(render(main().child(Button.of("Save"))));
    var base = StyleSheet.embeddedBaseRules(theme);

    assertThat(rules)
        .isNotEmpty()
        .doesNotContain(StyleSheet.LAYER_ORDER)
        .noneMatch(rule -> rule.startsWith("@layer reset{") || rule.startsWith("@layer theme{"));
    assertThat(base)
        .noneSatisfy(rule -> assertThat(rule).containsAnyOf(":root", "body{", "@view-transition"))
        .anySatisfy(
            rule ->
                assertThat(rule)
                    .startsWith(
                        "@layer theme{[data-tessera-theme=\""
                            + StyleSheet.themeKey(theme)
                            + "\"]{"))
        .anySatisfy(rule -> assertThat(rule).startsWith("@layer reset{[data-tessera-root]{"));
    var custom = theme.with(Tokens.ACCENT_PRIMARY, CssColor.oklch(55, 0.18, 150));
    assertThat(StyleSheet.themeKey(theme))
        .isEqualTo(StyleSheet.themeKey(Theme.defaults()))
        .isNotEqualTo(StyleSheet.themeKey(custom));
  }

  @Test
  @DisplayName("live renders report document-only contributions as diagnostics")
  void liveRendersReportDocumentOnlyContributionsAsDiagnostics() {
    Component documentOnly =
        context -> {
          context.contributeHead(Elements.el(HtmlTag.META).attr("name", "x"));
          context.atDocumentEnd(END, _ -> p());
          context.contributeScript(new PageScript("widget", "export {}"));
          return div()
              .children(
                  Elements.late(_ -> p()),
                  Elements.deferred(p()),
                  Elements.rawHtml("<b>raw</b>"),
                  Button.of("Help").interestTooltip(tooltip -> tooltip.text("More")));
        };

    var failure = catchThrowableOfType(ViewException.class, () -> render(documentOnly));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactlyInAnyOrder(
            "native-head-contribution",
            "native-document-end",
            "native-page-script",
            "native-document-placement",
            "native-document-placement",
            "native-raw-html",
            "native-page-script");
    assertThat(failure.diagnostics())
        .allSatisfy(diagnostic -> assertThat(diagnostic.location().components()).isNotEmpty());
  }

  @Test
  @DisplayName("repeated fields drop hints and errors without mutating the shared control")
  void repeatedFieldsDropHintsAndErrorsWithoutMutatingTheSharedControl() {
    var control = Forms.text("name");

    for (var turn = 0; turn < 6; turn++) {
      var enabled = turn % 2 == 0;
      var field = Field.of("Name", control).id("name").tooltip("Enter your name");
      if (enabled) {
        field.hint("Full name").error("Name is required");
      }
      var view = dom(render(field));
      var input = view.selectFirst("input");
      assertThat(input).isNotNull();
      assertThat(view.select("label").attr("for")).isEqualTo(input.id());
      assertThat(view.select(".field-hint")).hasSize(enabled ? 1 : 0);
      assertThat(view.select(".field-error")).hasSize(enabled ? 1 : 0);
      assertThat(input.hasAttr("aria-describedby")).isEqualTo(enabled);
      assertThat(input.hasAttr("aria-errormessage")).isEqualTo(enabled);
      if (enabled) {
        assertThat(view.getElementById(input.attr("aria-describedby")).text())
            .isEqualTo("Full name");
        assertThat(view.getElementById(input.attr("aria-errormessage")).text())
            .contains("Name is required");
      }
      var page = Compiler.compile(new PageDocument("Editor", main().child(field)));
      assertThat(Jsoup.parse(page.html()).select("input").attr("aria-describedby"))
          .isEqualTo(input.attr("aria-describedby"));
    }
    assertThat(control.idReferences()).isEmpty();
    assertThat(control.isAnchor()).isFalse();
  }

  @Test
  @DisplayName("repeated select fields preserve options and link their descriptions")
  void repeatedSelectFieldsPreserveOptionsAndLinkTheirDescriptions() {
    var select = Select.of("size").option("small", "Small").selectedOption("large", "Large");
    var field = Field.of("Size", select).hint("Choose a size");

    for (var turn = 0; turn < 3; turn++) {
      var view = dom(render(field));
      assertThat(view.select("option")).hasSize(2);
      assertThat(view.select("option[selected]").attr("value")).isEqualTo("large");
      assertThat(view.select("label").attr("for")).isEqualTo(view.select("select").attr("id"));
      assertThat(view.getElementById(view.select("select").attr("aria-describedby")).text())
          .isEqualTo("Choose a size");
    }
  }

  @Test
  @DisplayName("independent renders keep reference scopes separate")
  void independentRendersKeepReferenceScopesSeparate() {
    var field = Field.of("Name", Forms.text("name")).hint("Full name");
    var left = dom(ViewRenderer.render(_ -> field, "left"));
    var right = dom(ViewRenderer.render(_ -> field, "right"));

    assertThat(left.select("input").attr("id")).startsWith("left--");
    assertThat(right.select("input").attr("id"))
        .startsWith("right--")
        .isNotEqualTo(left.select("input").attr("id"));
    assertThat(left.select("label").attr("for")).isEqualTo(left.select("input").attr("id"));
    assertThat(right.select("label").attr("for")).isEqualTo(right.select("input").attr("id"));
  }

  @Test
  @DisplayName("live document rejection does not leak footnote registrations into compilations")
  void liveDocumentRejectionDoesNotLeakFootnoteRegistrationsIntoCompilations() {
    var content = main().child(p().child(Footnote.of("Reference text").reference()));

    assertThatThrownBy(() -> render(content))
        .isInstanceOf(ViewException.class)
        .hasMessageContaining("native-document-end");
    for (var turn = 0; turn < 2; turn++) {
      var page = Jsoup.parse(Compiler.compile(new PageDocument("Notes", content)).html());
      assertThat(page.select("[role=doc-noteref]").text()).isEqualTo("[1]");
      assertThat(page.select("[role=doc-footnote]")).hasSize(1);
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"style", "render"})
  @DisplayName("failed nested callbacks identify their scope without affecting the next render")
  void failedNestedCallbacksIdentifyTheirScopeWithoutAffectingTheNextRender(String phase) {
    var broken =
        new Panel(
            "outer",
            new Panel("inner", p(), phase.equals("style"), phase.equals("render")),
            false,
            false);
    var failure = catchThrowable(() -> render(broken));

    assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("Callback failed");
    assertThat(failure.getSuppressed())
        .singleElement()
        .isInstanceOf(ViewException.class)
        .satisfies(
            context ->
                assertThat(context)
                    .hasMessageContaining(
                        "panel[outer]", "panel[outer--inner]", "callback failed"));

    var next = dom(render(new Panel("fresh", p().text("Ready"), false, false)));
    assertThat(next.select("[data-scope-id]").attr("data-scope-id")).isEqualTo("fresh");
    assertThat(next.text()).isEqualTo("Ready");
  }

  @Test
  @DisplayName("transition names remain unique when transition classes differ")
  void transitionNamesRemainUniqueWhenTransitionClassesDiffer() {
    var content =
        main()
            .children(
                div().transitionKey(TransitionKey.of("shared", "first")),
                div().transitionKey(TransitionKey.of("shared", "second")));

    assertThatThrownBy(() -> render(content))
        .isInstanceOf(ViewException.class)
        .hasMessageContaining("Duplicate TransitionKey name 'shared'");
  }

  private record Panel(String id, IntoElement child, boolean failStyle, boolean failRender)
      implements Scope {
    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      if (failStyle) {
        throw new IllegalStateException("Callback failed");
      }
      scope.root(style -> style.fontWeight(600));
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      if (failRender) {
        throw new IllegalStateException("Callback failed");
      }
      return div().child(child);
    }
  }

  @Test
  @DisplayName("keyed fields keep their ids when siblings are inserted and reordered")
  void keyedFieldsKeepTheirIdsWhenSiblingsAreInsertedAndReordered() {
    var first = dom(render(keyedFields(List.of("a", "b"))));
    var next = dom(render(keyedFields(List.of("c", "b", "a"))));

    for (var name : List.of("a", "b")) {
      assertThat(next.select("input[name=" + name + "]").attr("id"))
          .isEqualTo(first.select("input[name=" + name + "]").attr("id"));
    }
    assertThat(next.select("label"))
        .allSatisfy(label -> assertThat(next.getElementById(label.attr("for"))).isNotNull());
  }

  @Test
  @DisplayName("keys are exact sibling identities rather than normalized DOM identifiers")
  void keysAreExactSiblingIdentitiesRatherThanNormalizedDomIdentifiers() {
    var view = dom(render(keyedFields(List.of("row-a", "row_a", "Row-a"))));

    assertThat(view.select("input").eachAttr("id")).hasSize(3).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("duplicate sibling keys fail but the same key under separate parents is valid")
  void duplicateSiblingKeysFailButTheSameKeyUnderSeparateParentsIsValid() {
    var invalid = keyedFields(List.of("a", "a"));
    assertThatThrownBy(() -> render(invalid))
        .isInstanceOf(ViewException.class)
        .hasMessageContaining("Duplicate sibling key 'a'");
    assertThatThrownBy(() -> Compiler.compile(new PageDocument("Editor", invalid)))
        .isInstanceOf(CompilationException.class)
        .hasMessageContaining("duplicate-key")
        .hasMessageContaining("Duplicate sibling key 'a'");

    var valid =
        main()
            .children(
                div().child(Field.of("Left", Forms.text("left")).key("same")),
                div().child(Field.of("Right", Forms.text("right")).key("same")));
    var view = dom(render(valid));
    assertThat(view.select("input").eachAttr("id")).hasSize(2).doesNotHaveDuplicates();
    assertThat(view.select("label").eachAttr("for"))
        .containsExactlyElementsOf(view.select("input").eachAttr("id"));
  }

  @Test
  @DisplayName("keyed groups carry their key and the definition of their content")
  void keyedGroupsCarryTheirKeyAndTheDefinitionOfTheirContent() {
    var snapshot =
        render(
            main()
                .children(
                    Field.of("Name", Forms.text("name")).key("field"),
                    fragment().children(p().text("A"), p().text("B")).key("pair")));
    var root = (ViewSnapshot.Tag) snapshot.roots().getFirst();

    assertThat(root.children())
        .extracting(node -> (ViewSnapshot.Group) node)
        .extracting(ViewSnapshot.Group::key, ViewSnapshot.Group::definition)
        .containsExactly(tuple("field", "form-field"), tuple("pair", FragmentElement.class));
  }

  @Test
  @DisplayName("keyed fragments preserve direct children in static and live output")
  void keyedFragmentsPreserveDirectChildrenInStaticAndLiveOutput() {
    var content =
        main()
            .child(
                Elements.ul()
                    .children(
                        fragment()
                            .children(Elements.li().text("First"), Elements.li().text("Second"))
                            .key("pair"),
                        Elements.li().text("Third").key("third")));
    var live = dom(render(content));
    var page = Jsoup.parse(Compiler.compile(new PageDocument("List", content)).html());

    assertThat(semantics(live)).isEqualTo(semantics(page));
    assertThat(page.select("ul > li").eachText()).containsExactly("First", "Second", "Third");
    assertThat(page.select("ul > *")).hasSize(3);
  }

  private static IntoElement keyedFields(List<String> names) {
    var container = main();
    names.forEach(name -> container.child(Field.of("Field " + name, Forms.text(name)).key(name)));
    return container;
  }

  private record Children(FragmentElement children) implements Scope {
    @Override
    public String scopeId() {
      return "notes";
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return children;
    }
  }

  @Test
  @DisplayName("a fragment scope root reports scope-root in static and live expansion")
  void fragmentScopeRootReportsScopeRoot() {
    var content = main().child(new Children(fragment().child(p().text("Only"))));

    assertThat(
            rules(catchThrowableOfType(ViewException.class, () -> render(content)).diagnostics()))
        .containsExactly("scope-root");
    assertThat(rules(compileFailure(content).diagnostics())).containsExactly("scope-root");
  }

  @Test
  @DisplayName("direct scope delegation reports scope-root")
  void directScopeDelegationReportsScopeRoot() {
    var content = main().child(new OuterScope(new InnerScope()));

    assertThat(
            rules(catchThrowableOfType(ViewException.class, () -> render(content)).diagnostics()))
        .containsExactly("scope-root");
    assertThat(rules(compileFailure(content).diagnostics())).containsExactly("scope-root");
  }

  @Test
  @DisplayName("a component host and a nested scope keep their own markers")
  void componentHostAndNestedScopeKeepTheirOwnMarkers() {
    var content = main().children(new ItemList(), new OuterScope(div().child(new InnerScope())));
    var live = dom(render(content));
    var page = Jsoup.parse(Compiler.compile(new PageDocument("Scopes", content)).html());

    for (var document : List.of(live, page)) {
      var list = document.selectFirst("main > ul");
      var outer = document.selectFirst("main > div[data-scope-id=outer]");
      var inner = document.selectFirst("p[data-scope-id=outer--inner]");
      assertThat(list).isNotNull();
      assertThat(list.attr("data-scope")).isEqualTo("items");
      assertThat(list.attr("data-scope-id")).isEqualTo("items");
      assertThat(list.children()).extracting(Element::text).containsExactly("One", "Two");
      assertThat(outer).isNotNull();
      assertThat(inner).isNotNull();
      assertThat(inner.parent()).isEqualTo(outer);
      assertThat(outer.attr("data-scope")).isNotEqualTo(inner.attr("data-scope"));
      assertThat(document.select("main > div[data-scope-id=items]")).isEmpty();
    }

    var compiled = Compiler.compile(new PageDocument("Scopes", content));
    assertScopeIsolation(compiled.css(), page);
    assertScopeIsolation(String.join("\n", styleRules(render(content))), live);
  }

  @Test
  @DisplayName("a keyed element is the scope host and the scope group keeps its name")
  void keyedElementIsScopeHostWithStableScopeName() {
    var content = main().child(new KeyedHost("cards"));
    var first = scopeGroup(render(content));
    var second = scopeGroup(render(content));
    var live = dom(render(content));
    var page = Jsoup.parse(Compiler.compile(new PageDocument("Keyed", content)).html());

    assertThat(first.definition()).isEqualTo("keyed").isEqualTo(second.definition());
    assertThat(first.key()).isNull();
    assertThat(first.children()).singleElement().isInstanceOf(ViewSnapshot.Group.class);
    var keyed = (ViewSnapshot.Group) first.children().getFirst();
    assertThat(keyed.key()).isEqualTo("a");
    assertThat(keyed.definition()).isEqualTo(TagElement.class);
    for (var document : List.of(live, page)) {
      var host = document.selectFirst("main > article");
      assertThat(host).isNotNull();
      assertThat(host.attr("data-scope")).isEqualTo("keyed");
      assertThat(host.attr("data-scope-id")).isEqualTo("cards");
      assertThat(host.selectFirst("p").text()).isEqualTo("Body");
      assertThat(document.select("main > div")).isEmpty();
    }
  }

  @Test
  @DisplayName("a generated scope id does not turn a keyed host into scope-root")
  void generatedScopeIdDoesNotRejectKeyedHost() {
    var content = main().child(new KeyedHost(null));

    assertThat(
            rules(catchThrowableOfType(ViewException.class, () -> render(content)).diagnostics()))
        .containsExactly("generated-scope-id");
    assertThat(rules(compileFailure(content).diagnostics())).containsExactly("generated-scope-id");
  }

  @Test
  @DisplayName("static and live expansion collect the same outline metadata")
  void outlineMetadataMatchesAcrossBackends() {
    var target = ElementRef.page("intro");
    var content =
        main().children(Heading.of(2, "Intro").id(target).outline(target), new OutlineEcho());
    var live = dom(render(content));
    var page = Jsoup.parse(Compiler.compile(new PageDocument("Outline", content)).html());

    for (var document : List.of(live, page)) {
      assertThat(document.select("h2").text()).isEqualTo("Intro");
      assertThat(document.select("h2").attr("id")).isEqualTo("intro");
      assertThat(document.select("p").text()).isEqualTo("Intro");
    }
  }

  @Test
  @DisplayName("an unbound outline fails static linking and still renders live")
  void unboundOutlineFailsStaticLinking() {
    var content = main().child(Heading.of(2, "Missing").outline(ElementRef.page("missing")));

    assertThat(rules(compileFailure(content).diagnostics())).containsExactly("unbound-reference");
    var live = dom(render(content));
    assertThat(live.select("h2").text()).isEqualTo("Missing");
    assertThat(live.select("h2").hasAttr("id")).isFalse();
  }

  private static List<String> rules(List<Diagnostic> diagnostics) {
    return diagnostics.stream().map(Diagnostic::rule).toList();
  }

  private static CompilationException compileFailure(IntoElement content) {
    return catchThrowableOfType(
        CompilationException.class, () -> Compiler.compile(new PageDocument("Scopes", content)));
  }

  private static ViewSnapshot.Group scopeGroup(ViewSnapshot snapshot) {
    var root = (ViewSnapshot.Tag) snapshot.roots().getFirst();
    return (ViewSnapshot.Group) root.children().getFirst();
  }

  private static void assertScopeIsolation(String css, Document document) {
    var outer = document.selectFirst("[data-scope-id=outer]").attr("data-scope");
    var inner = document.selectFirst("[data-scope-id=outer--inner]").attr("data-scope");
    assertThat(css)
        .contains(scopeRule(outer))
        .contains(scopeRule(inner))
        .contains("font-weight:600")
        .contains("font-weight:700");
    assertThat(css).containsSubsequence(scopeRule(outer), "font-weight:600");
    assertThat(css).containsSubsequence(scopeRule(inner), "font-weight:700");
  }

  private static String scopeRule(String scope) {
    return "@scope ([data-scope=\"" + scope + "\"]) to (:scope [data-scope] > *)";
  }

  /// A scope whose root is the supplied content, styled so its selector differs from its child.
  private record OuterScope(IntoElement content) implements Scope {
    @Override
    public String scopeId() {
      return "outer";
    }

    @Override
    public String scopeName() {
      return "outer";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(style -> style.fontWeight(600));
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return content;
    }
  }

  /// A nested scope with its own host and selector.
  private record InnerScope() implements Scope {
    @Override
    public String scopeId() {
      return "inner";
    }

    @Override
    public String scopeName() {
      return "inner";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(style -> style.fontWeight(700));
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return p().text("Inside");
    }
  }

  /// A component whose result is one list element.
  private record Listed() implements Component {
    @Override
    public IntoElement expand(DocumentContext context) {
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

  /// A scope whose host is the keyed element itself. A null id is generated.
  private record KeyedHost(@Nullable String id) implements Scope {
    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public String scopeName() {
      return "keyed";
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return article().child(p().text("Body")).key("a");
    }
  }

  /// Renders the titles collected before this component expands.
  private static final class OutlineEcho implements Component {
    @Override
    public IntoElement expand(DocumentContext context) {
      var titles = context.headings().stream().map(DocumentHeading::title).toList();
      return p().text(String.join(",", titles));
    }
  }

  @Test
  @DisplayName("late keyed fields retain their separate parent identity scopes")
  void lateKeyedFieldsRetainTheirSeparateParentIdentityScopes() {
    var content =
        main()
            .children(
                div().child(Elements.late(_ -> Field.of("Left", Forms.text("left")).key("same"))),
                div()
                    .child(Elements.late(_ -> Field.of("Right", Forms.text("right")).key("same"))));
    var page = Jsoup.parse(Compiler.compile(new PageDocument("Editor", content)).html());

    assertThat(page.select("input").eachAttr("id")).hasSize(2).doesNotHaveDuplicates();
    assertThat(page.select("label").eachAttr("for"))
        .containsExactlyElementsOf(page.select("input").eachAttr("id"));
  }

  private record SemanticElement(String tag, Map<String, String> attributes, String text) {}

  private static List<SemanticElement> semantics(Document document) {
    return document.body().getAllElements().stream()
        .map(
            element -> {
              var attributes = new HashMap<String, String>();
              element
                  .attributes()
                  .forEach(
                      attribute -> {
                        if (!List.of("class", "data-scope", "data-scope-id")
                            .contains(attribute.getKey())) {
                          attributes.put(attribute.getKey(), attribute.getValue());
                        }
                      });
              return new SemanticElement(element.tagName(), attributes, element.ownText());
            })
        .toList();
  }

  private static Document dom(ViewSnapshot snapshot) {
    var document = Document.createShell("");
    append(document.body(), snapshot.roots());
    return document;
  }

  private static void append(Element parent, List<ViewSnapshot.Node> nodes) {
    for (var node : nodes) {
      switch (node) {
        case ViewSnapshot.Tag tag -> {
          var element = parent.appendElement(tag.tag().tagName());
          tag.attributes().forEach(element::attr);
          append(element, tag.children());
        }
        case ViewSnapshot.Text(var text) -> parent.appendChild(new TextNode(text));
        case ViewSnapshot.Raw(var html) -> parent.append(html);
        case ViewSnapshot.Group group -> append(parent, group.children());
        case ViewSnapshot.Effect _ -> {}
      }
    }
  }
}
