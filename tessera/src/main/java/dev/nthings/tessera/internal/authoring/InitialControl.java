package dev.nthings.tessera.internal.authoring;

import java.util.List;
import org.jspecify.annotations.Nullable;

/// Authored defaults captured with a portable control. Null fields were left to native markup.
public record InitialControl(
    @Nullable String value, @Nullable Boolean checked, @Nullable List<String> selectedValues) {

  public static final InitialControl NONE = new InitialControl(null, null, null);

  public InitialControl {
    selectedValues = selectedValues == null ? null : List.copyOf(selectedValues);
  }

  public InitialControl withValue(String value) {
    return new InitialControl(value, checked, selectedValues);
  }

  public InitialControl withChecked(boolean checked) {
    return new InitialControl(value, checked, selectedValues);
  }

  public InitialControl withSelectedValues(List<String> values) {
    return new InitialControl(value, checked, values);
  }
}
