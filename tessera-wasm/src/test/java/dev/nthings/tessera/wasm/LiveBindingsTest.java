package dev.nthings.tessera.wasm;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.internal.view.LinkedNode;
import dev.nthings.tessera.view.ViewException;
import dev.nthings.tessera.view.ViewRenderer;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Live bindings")
class LiveBindingsTest {

  private static final Effect NOTHING = _ -> {};

  /// Renders `content` as a mounted view with page-level ids, outside a browser.
  private static ViewSnapshot<LiveBehavior> render(LiveContent content) {
    return ViewRenderer.render(
        _ -> LivePlacement.capture(content), "", base -> base, LiveBehavior::validate);
  }

  private static List<String> styleRules(ViewSnapshot<LiveBehavior> snapshot) {
    return snapshot.styleRules().stream().map(StyleSheet.Rule::css).toList();
  }

  private static LinkedNode.Tag<LiveBehavior> firstTag(LiveContent content) {
    return (LinkedNode.Tag<LiveBehavior>) render(content).roots().getFirst();
  }

  /// The rules a render of `content` reports; fails if it renders.
  private static List<String> rules(LiveContent content) {
    try {
      render(content);
    } catch (ViewException failure) {
      return failure.diagnostics().stream().map(Diagnostic::rule).toList();
    }
    throw new AssertionError("Expected reported diagnostics but the render succeeded");
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
    assertThat(
            rules(
                LiveElement.of(div())
                    .children(Live.effect("a", NOTHING), Live.effect("a", NOTHING))))
        .containsExactly("duplicate-key");
    assertThat(
            rules(
                LiveElement.of(div())
                    .children(
                        Live.effect("a", NOTHING), LiveContent.of(button().text("A").key("a")))))
        .containsExactly("duplicate-key");

    var separate =
        firstTag(
            LiveElement.of(div())
                .children(
                    LiveElement.of(div()).child(Live.effect("a", NOTHING)),
                    LiveContent.of(div().child(button().text("A").key("a")))));
    assertThat(separate.children())
        .hasSize(2)
        .allSatisfy(
            child -> assertThat(((LinkedNode.Tag<LiveBehavior>) child).children()).hasSize(1));
  }

  @Test
  @DisplayName("binding events leaves the wrapped portable element unmodified")
  void bindingEventsLeavesThePortableElementUnmodified() {
    var save = Button.of("Save").element();
    var bound = firstTag(LiveElement.of(save).onClick(() -> {}));
    var plain = firstTag(LiveContent.of(save));

    assertThat(LiveBehavior.bindings(bound).events()).hasSize(1);
    assertThat(LiveBehavior.bindings(plain).events()).isEmpty();
    assertThat(plain.children()).containsExactly(new LinkedNode.Text<>("Save"));
  }

  @Test
  @DisplayName("live declarations keep one rule across values in a mount")
  void liveDeclarationsKeepOneRule() {
    var classes = new HashSet<String>();
    for (var width = 1; width <= 3; width++) {
      var size = CssLength.px(width);
      var snapshot = render(LiveContent.of(div().live(style -> style.inlineSize(size))));
      var tag = (LinkedNode.Tag<LiveBehavior>) snapshot.roots().getFirst();

      assertThat(tag.liveProperties()).containsExactly(entry("--tl-inline-size", width + "px"));
      classes.add(tag.attributes().get("class"));
      assertThat(styleRules(snapshot)).noneSatisfy(rule -> assertThat(rule).contains(size.toCss()));
    }
    assertThat(classes).hasSize(1);
  }

  @Test
  @DisplayName("conditional styles written as live styles stay ordinary rules")
  void conditionalStylesWrittenAsLiveStylesStayOrdinaryRules() {
    var tag =
        firstTag(
            LiveContent.of(
                div()
                    .live(style ->
                        style
                            .inlineSize(CssLength.px(3))
                            .on(State.HOVER, hover -> hover.opacity(0.5)))));

    assertThat(tag.liveProperties()).containsOnlyKeys("--tl-inline-size");
    assertThat(
            styleRules(
                render(
                    LiveContent.of(
                        div().live(style -> style.on(State.HOVER, hover -> hover.opacity(0.5)))))))
        .anySatisfy(rule -> assertThat(rule).contains("opacity:0.5"));
  }

  @Test
  @DisplayName("a live value keeps the initial attribute of the wrapped portable control")
  void liveValueKeepsTheInitialAttributeOfTheWrappedPortableControl() {
    var control = Forms.text("name").initialValue("Initial");
    for (var turn = 0; turn < 3; turn++) {
      var input = firstTag(LiveElement.of(control).bindValue("Live " + turn));
      assertThat(input.attributes()).containsEntry("value", "Initial");
      assertThat(LiveBehavior.bindings(input).controls().value()).isEqualTo("Live " + turn);
    }
    assertThat(firstTag(LiveContent.of(control)).attributes()).containsEntry("value", "Initial");
  }

  static Stream<Arguments> incompatibleHosts() {
    return Stream.of(
        argumentSet("value on a div", LiveElement.of(div()).bindValue("text"), "control-value"),
        argumentSet(
            "value on a file input",
            LiveElement.of(Elements.input("file")).bindValue("path"),
            "control-value"),
        argumentSet(
            "value on a checkbox",
            LiveElement.of(Elements.input("checkbox")).bindValue("on"),
            "control-value"),
        argumentSet(
            "value on a hidden input",
            LiveElement.of(Elements.input("hidden")).bindValue("token"),
            "control-value"),
        argumentSet(
            "value on a multiple select",
            LiveElement.of(Elements.select().boolAttr("multiple")).bindValue("a"),
            "control-value"),
        argumentSet(
            "checked on a text input",
            LiveElement.of(Elements.input("text")).onCheckedChange(_ -> {}),
            "control-checked"),
        argumentSet(
            "checked on a textarea",
            LiveElement.of(Elements.textarea()).bindChecked(false),
            "control-checked"),
        argumentSet(
            "selection on a single select",
            LiveElement.of(Elements.select()).bindSelectedValues(List.of()),
            "control-selection"),
        argumentSet(
            "selection changes on a single select",
            LiveElement.of(Elements.select()).onSelectedValuesChange(_ -> {}),
            "control-selection"));
  }

  @ParameterizedTest
  @MethodSource("incompatibleHosts")
  @DisplayName("controlled properties on an incompatible native host are reported")
  void controlledPropertiesRejectIncompatibleHosts(LiveElement control, String rule) {
    assertThat(rules(control)).containsExactly(rule);
  }

  static Stream<LiveElement> compatibleHosts() {
    return Stream.of(
        LiveElement.of(Elements.input("text")).bindValue("").onValueChange(_ -> {}),
        LiveElement.of(Elements.textarea()).bindValue("Notes"),
        LiveElement.of(Elements.input("checkbox")).bindChecked(false),
        LiveElement.of(Elements.input("radio")).bindChecked(true).onCheckedChange(_ -> {}),
        LiveElement.of(Elements.select()).bindValue("a"),
        LiveElement.of(Elements.select().boolAttr("multiple")).bindSelectedValues(List.of()));
  }

  @ParameterizedTest
  @MethodSource("compatibleHosts")
  @DisplayName("controlled properties on a compatible native host render")
  void controlledPropertiesRenderOnCompatibleHosts(LiveElement control) {
    render(control);
  }
}
