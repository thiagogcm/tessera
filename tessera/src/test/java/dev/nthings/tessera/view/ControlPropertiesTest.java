package dev.nthings.tessera.view;

import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.support.Snapshots.tags;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.ControlProperties;
import dev.nthings.tessera.core.ControlState;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.TagElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Live form properties")
class ControlPropertiesTest {

  @Test
  @DisplayName("field copies retain controlled values independently of initial attributes")
  void fieldCopiesRetainControlledValuesIndependentlyOfInitialAttributes() {
    var control = Forms.text("name").attr("value", "Initial").value("Live");
    for (var turn = 0; turn < 3; turn++) {
      var field = Field.of("Name", control);
      if (turn % 2 == 0) {
        field.hint("Hint");
      }
      var input =
          tags(render(field).roots())
              .filter(tag -> tag.tag().tagName().equals("input"))
              .findFirst()
              .orElseThrow();
      assertThat(input.attributes()).containsEntry("value", "Initial");
      assertThat(input.controlProperties().value()).isEqualTo("Live");
    }
  }

  @Test
  @DisplayName("clearing live bindings leaves initial attributes and existing copies unchanged")
  void clearingLiveBindingsLeavesInitialAttributesAndExistingCopiesUnchanged() {
    var control =
        Elements.input("checkbox")
            .attr("type", "checkbox")
            .boolAttr("checked")
            .value("yes")
            .checked(false);
    var copy = control.copy();
    control.uncontrolled();

    assertThat(control.controlProperties().isControlled()).isFalse();
    assertThat(control.attributes()).containsEntry("checked", "");
    assertThat(copy.controlProperties()).isEqualTo(new ControlProperties("yes", false, null));
    var snapshot = tags(render(copy).roots()).findFirst().orElseThrow();
    assertThat(snapshot.controlProperties()).isEqualTo(copy.controlProperties());
    assertThat(Compiler.compile(new PageDocument("Initial", control)).html()).contains("checked");
  }

  @Test
  @DisplayName("selection bindings and event data copy their selected values")
  void selectionBindingsAndEventDataCopyTheirSelectedValues() {
    var values = new ArrayList<>(List.of("a", "b"));
    var select = Elements.select().boolAttr("multiple").selectedValues(values);
    var event = new ControlState("a", false, values, true);
    values.clear();

    assertThat(select.controlProperties().selectedValues()).containsExactly("a", "b");
    assertThat(event.selectedValues()).containsExactly("a", "b");
    select.selectedValues(List.of());
    assertThat(select.controlProperties().isControlled()).isTrue();
    assertThat(
            tags(render(select).roots())
                .findFirst()
                .orElseThrow()
                .controlProperties()
                .selectedValues())
        .isEmpty();
  }

  @ParameterizedTest
  @MethodSource("invalidHosts")
  @DisplayName("controlled properties reject incompatible native hosts")
  void controlledPropertiesRejectIncompatibleNativeHosts(TagElement control) {
    assertThatThrownBy(() -> render(control))
        .isInstanceOf(ViewException.class)
        .hasMessageMatching("(?s).*[Cc]ontrolled.*");
  }

  private static Stream<TagElement> invalidHosts() {
    return Stream.of(
        Elements.div().value("text"),
        Elements.input("file").value("path"),
        Elements.input("text").checked(true),
        Elements.textarea().checked(false),
        Elements.select().selectedValues(List.of()),
        Elements.select().boolAttr("multiple").value("a"));
  }

  @ParameterizedTest
  @MethodSource("controlledHosts")
  @DisplayName(
      "static output rejects live bindings rather than treating them as initial attributes")
  void staticOutputRejectsLiveBindingsRatherThanTreatingThemAsInitialAttributes(
      TagElement control) {
    assertThat(
            tags(render(control).roots())
                .findFirst()
                .orElseThrow()
                .controlProperties()
                .isControlled())
        .isTrue();
    assertThatThrownBy(() -> Compiler.compile(new PageDocument("Form", control)))
        .isInstanceOf(CompilationException.class)
        .hasMessageContaining("native-control-properties");
  }

  private static Stream<TagElement> controlledHosts() {
    return Stream.of(
        Elements.input("text").value(""),
        Elements.textarea().value("Notes"),
        Elements.input("checkbox").checked(false),
        Elements.input("radio").checked(true),
        Elements.select().value("a"),
        Elements.select().boolAttr("multiple").selectedValues(List.of()));
  }
}
