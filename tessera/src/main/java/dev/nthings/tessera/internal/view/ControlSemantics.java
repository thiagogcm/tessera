package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.TagName;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Control-element classification shared by initial-state lowering and live control bindings.
public final class ControlSemantics {
  // The value property reflects the attribute in these modes, or cannot be set for file inputs.
  private static final Set<String> NON_EDITABLE_VALUE_TYPES =
      Set.of("hidden", "button", "submit", "reset", "image", "checkbox", "radio", "file");

  private ControlSemantics() {}

  /// The ASCII case-insensitive input keyword, defaulting to text for absent or non-ASCII types.
  public static String inputType(Map<String, String> attributes) {
    var type = attributes.get("type");
    return type == null || type.chars().anyMatch(character -> character > 0x7f)
        ? "text"
        : type.toLowerCase(Locale.ROOT);
  }

  /// Whether this element's value is user-edited state rather than a reflection of its `value`
  /// attribute: a textarea, a single select, or an input in value mode other than file. Live value
  /// bindings require it; an initial value only needs a settable `value` attribute.
  public static boolean hasEditableValue(TagName tag, Map<String, String> attributes) {
    return tag == HtmlTag.TEXTAREA
        || (tag == HtmlTag.SELECT && !attributes.containsKey("multiple"))
        || (tag == HtmlTag.INPUT && !NON_EDITABLE_VALUE_TYPES.contains(inputType(attributes)));
  }

  /// Whether options beneath this element can belong to an enclosing select's option list.
  public static boolean mayContainOptions(TagName tag) {
    return tag != HtmlTag.SELECT
        && tag != HtmlTag.DATALIST
        && tag != HtmlTag.HR
        && tag != HtmlTag.TEMPLATE
        && tag != HtmlTag.OPTION;
  }

  /// Whether this element has a native checked property.
  public static boolean isCheckable(TagName tag, Map<String, String> attributes) {
    if (tag != HtmlTag.INPUT) {
      return false;
    }
    var type = inputType(attributes);
    return "checkbox".equals(type) || "radio".equals(type);
  }
}
