package dev.nthings.tessera.core;

/// Which user actions close a `<dialog>`, set with [TagElement#closedBy(ClosedBy)].
public enum ClosedBy {
  /// Clicking outside, Escape and close requests all close the dialog (`closedby="any"`).
  ANY("any"),
  /// Only Escape and close requests close the dialog (`closedby="closerequest"`).
  CLOSE_REQUEST("closerequest"),
  /// Only script or a command closes the dialog (`closedby="none"`).
  NONE("none");

  private final String wireValue;

  ClosedBy(String wireValue) {
    this.wireValue = wireValue;
  }

  /// The `closedby` attribute value.
  String wireValue() {
    return wireValue;
  }
}
