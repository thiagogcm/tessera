package dev.nthings.tessera.wasm;

import static dev.nthings.tessera.core.Elements.div;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.view.ViewException;
import dev.nthings.tessera.view.ViewRenderer;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Live bindings")
class LiveBindingsTest {

  /// Renders `content` as a mounted view with page-level ids, outside a browser.
  private static ViewSnapshot<LiveBehavior> render(LiveContent content) {
    return ViewRenderer.render(
        _ -> LivePlacement.capture(content), "", base -> base, LiveBehavior::validate);
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

  static Stream<Arguments> incompatibleHosts() {
    return Stream.of(
        argumentSet("value on a div", LiveElement.of(div()).bindValue("text"), "control-value"),
        argumentSet(
            "value on a file input",
            LiveElement.of(Elements.input(InputType.FILE)).bindValue("path"),
            "control-value"),
        argumentSet(
            "value on a checkbox",
            LiveElement.of(Elements.input(InputType.CHECKBOX)).bindValue("on"),
            "control-value"),
        argumentSet(
            "value on a hidden input",
            LiveElement.of(Elements.input(InputType.HIDDEN)).bindValue("token"),
            "control-value"),
        argumentSet(
            "value on a multiple select",
            LiveElement.of(Elements.select().boolAttr("multiple")).bindValue("a"),
            "control-value"),
        argumentSet(
            "checked on a text input",
            LiveElement.of(Elements.input(InputType.TEXT)).onCheckedChange(_ -> {}),
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
        LiveElement.of(Elements.input(InputType.TEXT)).bindValue("").onValueChange(_ -> {}),
        LiveElement.of(Elements.textarea()).bindValue("Notes"),
        LiveElement.of(Elements.input(InputType.CHECKBOX)).bindChecked(false),
        LiveElement.of(Elements.input(InputType.RADIO)).bindChecked(true).onCheckedChange(_ -> {}),
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
