package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.ControlState;
import java.util.ArrayList;
import java.util.Collection;

/// The text form of form-control state that crosses the JavaScript boundary in one call: each
/// string as `<length>:<text>`, with lengths in UTF-16 units, as both languages count them.
final class ControlCodec {

  private ControlCodec() {}

  /// Decodes [Dom#controlState]: the checked and composing flags, the value, then the selected
  /// option values. `composing` is the composition the reconciler already tracks for the control.
  static ControlState state(String encoded, boolean composing) {
    var texts = new ArrayList<String>();
    for (var at = 2; at < encoded.length(); ) {
      var colon = encoded.indexOf(':', at);
      var end = colon + 1 + Integer.parseInt(encoded, at, colon, 10);
      texts.add(encoded.substring(colon + 1, end));
      at = end;
    }
    return new ControlState(
        texts.getFirst(),
        encoded.charAt(0) == '1',
        texts.subList(1, texts.size()),
        composing || encoded.charAt(1) == '1');
  }

  /// Encodes option values for [Dom#selectValues].
  static String values(Collection<String> values) {
    var encoded = new StringBuilder();
    values.forEach(value -> encoded.append(value.length()).append(':').append(value));
    return encoded.toString();
  }
}
