package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.internal.view.ControlSemantics;
import dev.nthings.tessera.internal.view.DiagnosticSink;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Backend-facing: the live form properties an element controls, set with the browser module's
/// `LiveElement.bindValue`, `bindChecked` and `bindSelectedValues`. A `null` property is left to
/// the browser.
record ControlProperties(
    @Nullable String value, @Nullable Boolean checked, @Nullable List<String> selectedValues) {

  enum Kind {
    VALUE,
    CHECKED,
    SELECTION
  }

  /// No property controlled.
  public static final ControlProperties UNCONTROLLED = new ControlProperties(null, null, null);

  public ControlProperties {
    selectedValues = selectedValues == null ? null : List.copyOf(selectedValues);
  }

  /// These properties with `value` controlled.
  ControlProperties withValue(String value) {
    return new ControlProperties(value, checked, selectedValues);
  }

  /// These properties with `checked` controlled.
  ControlProperties withChecked(boolean checked) {
    return new ControlProperties(value, checked, selectedValues);
  }

  /// These properties with `selectedValues` controlled.
  ControlProperties withSelectedValues(List<String> selectedValues) {
    return new ControlProperties(value, checked, selectedValues);
  }

  /// Whether any property is controlled.
  public boolean isControlled() {
    return value != null || checked != null || selectedValues != null;
  }

  /// Bindings and typed change callbacks require a native host with the corresponding property.
  void validate(
      TagName tag,
      Map<String, String> attributes,
      Location location,
      DiagnosticSink reporter,
      Collection<EventListener> listeners) {
    var multiple = attributes.containsKey("multiple");
    if ((value != null || observes(listeners, Kind.VALUE))
        && !ControlSemantics.hasEditableValue(tag, attributes)) {
      reporter.report(
          location,
          "control-value",
          "Live value handling requires a value-editable input, textarea or single select",
          "Use a compatible input or textarea, or bind selected values on a multiple select.");
    }
    if ((checked != null || observes(listeners, Kind.CHECKED))
        && !ControlSemantics.isCheckable(tag, attributes)) {
      reporter.report(
          location,
          "control-checked",
          "Live checked state requires a checkbox or radio input",
          "Attach bindChecked or onCheckedChange to a checkable input.");
    }
    if ((selectedValues != null || observes(listeners, Kind.SELECTION))
        && !(tag == HtmlTag.SELECT && multiple)) {
      reporter.report(
          location,
          "control-selection",
          "Live selected values require a multiple select",
          "Use bindValue or onValueChange for a single select, or enable multiple selection.");
    }
  }

  private static boolean observes(Collection<EventListener> listeners, Kind kind) {
    return listeners.stream().anyMatch(listener -> listener.observed() == kind);
  }
}
