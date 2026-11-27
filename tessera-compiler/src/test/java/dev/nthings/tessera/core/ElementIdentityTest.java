package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static dev.nthings.tessera.support.RenderedPage.render;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Diagnostics.Found;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Element identity and linking")
class ElementIdentityTest {

  /// Wraps content in a nested id scope.
  record Panel(String id, IntoElement content) implements Scope {

    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public TagElement expand(ViewContext context) {
      return div().child(content);
    }
  }

  @Test
  @DisplayName("a scoped ref resolves to its bind-site scope from an invoker in another scope")
  void resolvesToBindSiteScope() {
    var target = ElementRef.scoped("confirm");

    var page =
        renderMain(
            new Panel("toolbar", button().command(target, BuiltinCommand.SHOW_MODAL).text("Open")),
            new Panel("dialogs", dialog().id(target).ariaLabel("Confirm").text("Sure?")));

    assertThat(page.target(page.element("button"), "commandfor"))
        .hasTagName("dialog")
        .hasText("Sure?");
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
    menu.id(named);

    var page =
        renderMain(
            new Panel(
                "nav",
                div()
                    .children(
                        button().command(early, BuiltinCommand.TOGGLE_POPOVER).text("Open"),
                        button().command(named, BuiltinCommand.HIDE_POPOVER).text("Close"),
                        menu)));

    var buttons = page.elements("button");
    assertThat(page.target(buttons.get(0), "commandfor")).hasText("Menu");
    assertThat(buttons.eachAttr("commandfor")).containsOnly(buttons.get(0).attr("commandfor"));
  }

  @Test
  @DisplayName("scoped names with spaces or punctuation still link, and an empty name is refused")
  void scopedNamesWithPunctuationStillLink() {
    var name = ElementRef.scoped("Full name!!");
    var confirm = ElementRef.scoped("save  draft?");

    var page =
        renderMain(
            label().labelFor(name).text("Name"),
            Forms.input("name", InputType.TEXT).id(name),
            button().command(confirm, BuiltinCommand.SHOW_MODAL).text("Save"),
            dialog().id(confirm).ariaLabel("Save draft").text("Saved"));

    var input = page.target(page.element("label"), "for");
    assertThat(input).hasTagName("input");
    assertThat(page.target(page.element("button"), "commandfor")).hasTagName("dialog");
    assertThat(List.of(input.id(), page.element("dialog").id()))
        .allSatisfy(id -> assertThat(id).doesNotContainAnyWhitespaces());
    assertThatIllegalArgumentException().isThrownBy(() -> ElementRef.scoped(""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "two words"})
  @DisplayName("a page name must be non-empty and free of whitespace")
  void rejectsInvalidPageNames(String name) {
    assertThatIllegalArgumentException().isThrownBy(() -> ElementRef.page(name));
  }

  @Test
  @DisplayName("an unbound ref is reported at the element that uses it")
  void reportsUnboundRef() {
    var dangling = ElementRef.scoped("ghost");

    assertThat(
            Diagnostics.of(() ->
                render(div().child(button().command(dangling, BuiltinCommand.CLOSE).text("x")))))
        .extracting(Found::rule, Found::tag)
        .containsExactly(tuple("unbound-reference", "button"));
  }

  static Stream<Arguments> ambiguousTargets() {
    var shared = ElementRef.scoped("confirm");
    var dialog = dialog().id(ElementRef.scoped("confirm")).ariaLabel("Confirm").text("Confirm?");
    return Stream.of(
        argumentSet(
            "one ref bound to two elements",
            div()
                .children(
                    p().id(shared),
                    dialog().id(shared).ariaLabel("Confirm").text("Confirm?"),
                    button().command(shared, BuiltinCommand.SHOW_MODAL).text("Open")),
            "duplicate-reference-binding",
            null),
        argumentSet(
            "two refs asking for the same DOM id",
            div()
                .children(
                    p().id(ElementRef.scoped("confirm")),
                    dialog,
                    button().command(dialog.ref(), BuiltinCommand.SHOW_MODAL).text("Open")),
            "duplicate-id",
            "confirm"));
  }

  @ParameterizedTest
  @MethodSource("ambiguousTargets")
  @DisplayName("an ambiguous command target reports only its binding error")
  void reportsAmbiguousTargets(IntoElement body, String rule, String domId) {
    assertThat(Diagnostics.of(() -> render(body)))
        .singleElement()
        .satisfies(found -> {
          assertThat(found.rule()).isEqualTo(rule);
          assertThat(found.tag()).isEqualTo("dialog");
          assertThat(found.domId()).isEqualTo(domId);
        });
  }

  @Test
  @DisplayName("two named refs asking for different ids on one element are reported")
  void reportsConflictingElementId() {
    assertThat(
            Diagnostics.of(() ->
                render(
                    div().id(ElementRef.scoped("first")).id(ElementRef.page("second")).text("x"))))
        .singleElement()
        .satisfies(found -> {
          assertThat(found.rule()).isEqualTo("conflicting-element-id");
          assertThat(found.tag()).isEqualTo("div");
          assertThat(found.domId()).isEqualTo("first");
        });
  }

  private static final Pattern ANCHOR = Pattern.compile("anchor-name:([^;}]+)");
  private static final Pattern POSITION = Pattern.compile("position-anchor:([^;}]+)");

  @ParameterizedTest
  @ValueSource(strings = {"signup.email", "menü", "a:b", "x#1"})
  @DisplayName("an anchored element pairs with a target bound to any page name")
  void anchorsPairWithAnyPageName(String name) {
    var target = ElementRef.page(name);
    var page =
        renderMain(
            div().id(target).anchorName().text("Anchor"),
            p().id(ElementRef.page("floating"))
                .anchoredTo(
                    target,
                    PositionArea.of(PositionArea.Part.BOTTOM, PositionArea.Part.CENTER),
                    PositionTry.flip(PositionTry.Flip.BLOCK))
                .text("Floating"));

    var anchor = match(ANCHOR, page.ruleOf(page.elementById(name), "u-"));
    assertThat(anchor).matches("--[A-Za-z0-9_-]+");
    assertThat(match(POSITION, page.ruleOf(page.elementById("floating"), "u-"))).isEqualTo(anchor);
  }

  @Test
  @DisplayName("distinct page names never share an anchor name")
  void distinctPageNamesKeepDistinctAnchors() {
    var page =
        renderMain(
            div().id(ElementRef.page("a.b")).anchorName().text("dot"),
            div().id(ElementRef.page("a:b")).anchorName().text("colon"),
            div().id(ElementRef.page("a-b")).anchorName().text("dash"));

    assertThat(
            Stream.of("a.b", "a:b", "a-b")
                .map(id -> match(ANCHOR, page.ruleOf(page.elementById(id), "u-"))))
        .doesNotHaveDuplicates();
  }

  @ParameterizedTest
  @ValueSource(strings = {"c#", "50%", "café+intro", "a[1]", "x\"y", "q?a/b"})
  @DisplayName("a link to a page ref with URL-reserved characters is a valid URL to its target")
  void fragmentLinksToAnyPageNameResolve(String name) {
    var target = ElementRef.page(name);
    var content = Elements.main().children(div().id(target).text("Target"), a(target).text("Go"));

    var page = render(content);

    var href = page.element("a").attr("href");
    assertThat(URI.create(href).getFragment()).isEqualTo(name);
    assertThat(page.elementById(name)).hasText("Target");
  }

  @Test
  @DisplayName("#top needs no target, other in-page fragments must exist")
  void topFragmentNeedsNoTarget() {
    renderMain(a("#top").text("Back to top"), a("#TOP").text("Back to top"));

    assertThat(
            Diagnostics.of(
                () -> renderMain(Elements.el(HtmlTag.A).attr("HREF", "#missing").text("Jump"))))
        .extracting(Found::rule)
        .containsExactly("missing-fragment");
  }

  private static String match(Pattern pattern, String css) {
    var matcher = pattern.matcher(css);
    assertThat(matcher.find()).as("%s in %s", pattern, css).isTrue();
    return matcher.group(1);
  }
}
