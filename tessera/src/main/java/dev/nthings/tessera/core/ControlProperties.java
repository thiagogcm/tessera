package dev.nthings.tessera.core;

import java.util.List;
import org.jspecify.annotations.Nullable;

/// Backend-facing: the live form properties an element controls, set with
/// [TagElement#value(String)], [TagElement#checked(boolean)] and [TagElement#selectedValues(List)].
/// A `null` property is left to the browser.
public record ControlProperties(
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
}
