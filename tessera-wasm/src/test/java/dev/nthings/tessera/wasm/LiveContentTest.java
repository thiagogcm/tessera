package dev.nthings.tessera.wasm;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewException;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.view.EmbeddedRule;
import dev.nthings.tessera.view.ViewRenderer;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Live content")
class LiveContentTest {

  private static final Effect NOTHING = _ -> {};

  /// Renders `content` as a mounted view with page-level ids, outside a browser.
  private static ViewSnapshot<LiveBehavior> render(LiveContent content) {
    return ViewRenderer.render(
        _ -> LivePlacement.capture(content), "", base -> base, LiveBehavior::validate);
  }

  private static List<String> styleRules(ViewSnapshot<LiveBehavior> snapshot) {
    return snapshot.styleRules().stream().map(EmbeddedRule::css).toList();
  }

  /// Every tag in document order, looking through groups.
  private static Stream<ViewSnapshot.Tag<LiveBehavior>> tags(
      List<ViewSnapshot.Node<LiveBehavior>> nodes) {
    return nodes.stream()
        .flatMap(node ->
            switch (node) {
              case ViewSnapshot.Tag<LiveBehavior> tag ->
                  Stream.concat(Stream.of(tag), tags(tag.children()));
              case ViewSnapshot.Group<LiveBehavior> group -> tags(group.children());
              case ViewSnapshot.Text<LiveBehavior> _, ViewSnapshot.Special<LiveBehavior> _ ->
                  Stream.empty();
            });
  }

  private static ViewSnapshot.Tag<LiveBehavior> firstTag(LiveContent content) {
    return tags(render(content).roots()).findFirst().orElseThrow();
  }

  @Test
  @DisplayName("expanding a live effect does not start it")
  void expandingALiveEffectDoesNotStartIt() {
    var started = new int[1];
    render(LiveElement.of(div()).child(Live.effect("ticker", _ -> started[0]++)));

    assertThat(started[0]).isZero();
  }

  @Test
  @DisplayName("effect keys share the sibling namespace with keyed children")
  void effectKeysShareTheSiblingNamespaceWithKeyedChildren() {
    assertThatThrownBy(() ->
            render(
                LiveElement.of(div())
                    .children(Live.effect("a", NOTHING), Live.effect("a", NOTHING))))
        .isInstanceOf(ViewException.class)
        .hasMessageContaining("duplicate-key");
    assertThatThrownBy(() ->
            render(
                LiveElement.of(div())
                    .children(
                        Live.effect("a", NOTHING), LiveContent.of(button().text("A").key("a")))))
        .isInstanceOf(ViewException.class)
        .hasMessageContaining("duplicate-key");

    var separate =
        render(
            LiveElement.of(div())
                .children(
                    LiveElement.of(div()).child(Live.effect("a", NOTHING)),
                    LiveContent.of(div().child(button().text("A").key("a")))));
    assertThat(separate.roots()).hasSize(1);
  }

  @Test
  @DisplayName("blank event types and sibling keys are rejected")
  void blankEventTypesAndSiblingKeysAreRejected() {
    assertThatThrownBy(() -> LiveElement.of(div()).on(" ", _ -> {}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Live.effect("", NOTHING)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> LiveContent.of(div()).key(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("binding events leaves the portable element valid for static output")
  void bindingEventsLeavesThePortableElementValidForStaticOutput() {
    var save = Button.of("Save").element();
    var bound = firstTag(LiveElement.of(save).onClick(() -> {}));

    assertThat(LiveBehavior.bindings(bound).events())
        .containsOnlyKeys(new EventListener.Key("click", false));
    assertThat(LiveBehavior.bindings(firstTag(LiveContent.of(save))).events()).isEmpty();
    assertThat(Compiler.compile(new PageDocument("Static", main().child(save))).html())
        .contains(">Save</button>");
  }

  @Test
  @DisplayName("live declarations keep one rule in browser mounts and compile as values statically")
  void liveDeclarationsKeepOneRuleInBrowserMountsAndCompileAsValuesStatically() {
    var classes = new HashSet<String>();
    for (var width = 1; width <= 3; width++) {
      var size = CssLength.px(width);
      var snapshot = render(LiveContent.of(div().live(style -> style.inlineSize(size))));
      var tag = (ViewSnapshot.Tag<LiveBehavior>) snapshot.roots().getFirst();

      assertThat(tag.liveProperties()).containsExactly(entry("--tl-inline-size", width + "px"));
      classes.add(tag.attributes().get("class"));
      assertThat(styleRules(snapshot))
          .anySatisfy(rule -> assertThat(rule).contains("inline-size:var(--tl-inline-size)"))
          .contains("@property --tl-inline-size{syntax:\"*\";inherits:false}")
          .noneSatisfy(rule -> assertThat(rule).contains("inline-size:" + size.toCss()));
    }
    assertThat(classes).hasSize(1);

    var css =
        Compiler.compile(
                new PageDocument(
                    "Live", main().child(div().live(style -> style.inlineSize(CssLength.px(7))))))
            .css();
    assertThat(css).contains("inline-size:7px").doesNotContain("--tl-");
  }

  @Test
  @DisplayName("conditional styles written as live styles stay ordinary rules")
  void conditionalStylesWrittenAsLiveStylesStayOrdinaryRules() {
    var snapshot =
        render(
            LiveContent.of(
                div()
                    .live(style ->
                        style
                            .inlineSize(CssLength.px(3))
                            .on(State.HOVER, hover -> hover.opacity(0.5)))));
    var tag = (ViewSnapshot.Tag<LiveBehavior>) snapshot.roots().getFirst();

    assertThat(tag.liveProperties()).containsOnlyKeys("--tl-inline-size");
    assertThat(styleRules(snapshot))
        .anySatisfy(rule -> assertThat(rule).startsWith("@layer interactive{").contains("opacity"));
  }

  @Test
  @DisplayName("a live value keeps the initial attribute of the wrapped portable control")
  void liveValueKeepsTheInitialAttributeOfTheWrappedPortableControl() {
    var control = Forms.text("name").attr("value", "Initial");
    for (var turn = 0; turn < 3; turn++) {
      var input = firstTag(LiveElement.of(control).value("Live " + turn));
      assertThat(input.attributes()).containsEntry("value", "Initial");
      assertThat(LiveBehavior.bindings(input).controls().value()).isEqualTo("Live " + turn);
    }
    assertThat(LiveBehavior.bindings(firstTag(LiveContent.of(control))).controls().isControlled())
        .isFalse();
    assertThat(Compiler.compile(new PageDocument("Initial", main().child(control))).html())
        .contains("value=\"Initial\"");
  }

  @Test
  @DisplayName("selection bindings and event data copy their selected values")
  void selectionBindingsAndEventDataCopyTheirSelectedValues() {
    var values = new ArrayList<>(List.of("a", "b"));
    var select = LiveElement.of(Elements.select().boolAttr("multiple")).selectedValues(values);
    var event = new ControlState("a", false, values, true);
    values.clear();

    assertThat(LiveBehavior.bindings(firstTag(select)).controls().selectedValues())
        .containsExactly("a", "b");
    assertThat(event.selectedValues()).containsExactly("a", "b");
    select.selectedValues(List.of());
    assertThat(LiveBehavior.bindings(firstTag(select)).controls())
        .isEqualTo(new ControlProperties(null, null, List.of()));
  }

  @ParameterizedTest
  @MethodSource("invalidHosts")
  @DisplayName("controlled properties reject incompatible native hosts")
  void controlledPropertiesRejectIncompatibleNativeHosts(LiveElement control) {
    assertThatThrownBy(() -> render(control))
        .isInstanceOf(ViewException.class)
        .hasMessageMatching("(?s).*[Cc]ontrolled.*");
  }

  private static Stream<LiveElement> invalidHosts() {
    return Stream.of(
        LiveElement.of(Elements.div()).value("text"),
        LiveElement.of(Elements.input("file")).value("path"),
        LiveElement.of(Elements.input("text")).checked(true),
        LiveElement.of(Elements.textarea()).checked(false),
        LiveElement.of(Elements.select()).selectedValues(List.of()),
        LiveElement.of(Elements.select().boolAttr("multiple")).value("a"));
  }

  @ParameterizedTest
  @MethodSource("controlledHosts")
  @DisplayName("controlled properties apply to compatible native hosts")
  void controlledPropertiesApplyToCompatibleNativeHosts(LiveElement control) {
    assertThat(LiveBehavior.bindings(firstTag(control)).controls().isControlled()).isTrue();
  }

  private static Stream<LiveElement> controlledHosts() {
    TagElement multiple = Elements.select().boolAttr("multiple");
    return Stream.of(
        LiveElement.of(Elements.input("text")).value(""),
        LiveElement.of(Elements.textarea()).value("Notes"),
        LiveElement.of(Elements.input("checkbox")).checked(false),
        LiveElement.of(Elements.input("radio")).checked(true),
        LiveElement.of(Elements.select()).value("a"),
        LiveElement.of(multiple).selectedValues(List.of()));
  }
}
