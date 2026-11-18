package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.view.ViewExpansion.Reporter;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Backend-facing: the live form properties an element controls, set with the browser module's
/// `LiveElement.value`, `checked` and `selectedValues`. A `null` property is left to the browser.
record ControlProperties(
    @Nullable String value, @Nullable Boolean checked, @Nullable List<String> selectedValues) {

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

  /// Controlled properties need a native host that has them.
  void validate(TagName tag, Map<String, String> attributes, Location location, Reporter reporter) {
    if (!isControlled()) {
      return;
    }
    var type = attributes.getOrDefault("type", "text");
    var multiple = attributes.containsKey("multiple");
    if (value() != null
        && !(tag == HtmlTag.TEXTAREA
            || tag == HtmlTag.SELECT && !multiple
            || tag == HtmlTag.INPUT && !"file".equalsIgnoreCase(type))) {
      reporter.report(
          location,
          "control-value",
          "A controlled value requires an input other than file, a textarea or a single select",
          "Use selectedValues for a multiple select and do not bind file input values.");
    }
    if (checked() != null
        && !(tag == HtmlTag.INPUT
            && ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type)))) {
      reporter.report(
          location,
          "control-checked",
          "A controlled checked state requires a checkbox or radio input",
          "Use checked only on checkable inputs.");
    }
    if (selectedValues() != null && !(tag == HtmlTag.SELECT && multiple)) {
      reporter.report(
          location,
          "control-selection",
          "Controlled selected values require a multiple select",
          "Use value for a single select, or enable multiple selection.");
    }
  }
}
