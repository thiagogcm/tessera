package dev.nthings.tessera.view;

import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.internal.view.ControlSemantics;
import java.util.Map;

/// Control-element classification exposed to the browser backend for live control bindings.
public final class ControlLowering {

  private ControlLowering() {}

  /// The ASCII case-insensitive input keyword, defaulting to text for absent or non-ASCII types.
  public static String inputType(Map<String, String> attributes) {
    return ControlSemantics.inputType(attributes);
  }

  /// Whether this element's value is user-edited state rather than a reflection of its `value`
  /// attribute: a textarea, a single select, or an input in value mode other than file.
  public static boolean hasEditableValue(TagName tag, Map<String, String> attributes) {
    return ControlSemantics.hasEditableValue(tag, attributes);
  }

  /// Whether options beneath this element can belong to an enclosing select's option list.
  public static boolean mayContainOptions(TagName tag) {
    return ControlSemantics.mayContainOptions(tag);
  }

  /// Whether this element has a native checked property.
  public static boolean isCheckable(TagName tag, Map<String, String> attributes) {
    return ControlSemantics.isCheckable(tag, attributes);
  }
}
