package dev.nthings.tessera.core;

import java.util.List;

/// The state of a form control copied during an event, from [BrowserEvent#control()]: its
/// `value`, whether a checkbox or radio is `checked`, the values of a multiple select's
/// selected options in document order, and whether an input method (IME) composition is in
/// progress. While `composing` is true the text is provisional; wait for the composition to end
/// before validating or reformatting it.
public record ControlState(
    String value, boolean checked, List<String> selectedValues, boolean composing) {

  public ControlState {
    selectedValues = List.copyOf(selectedValues);
  }
}
