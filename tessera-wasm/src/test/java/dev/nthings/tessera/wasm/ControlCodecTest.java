package dev.nthings.tessera.wasm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Control codec")
class ControlCodecTest {

  @Test
  @DisplayName("control state keeps values containing separators and digits intact")
  void controlStateKeepsValuesContainingSeparatorsAndDigitsIntact() {
    var encoded = "10" + ControlCodec.values(List.of("a:1", "", "2:b", "é😀"));

    assertThat(ControlCodec.state(encoded, false))
        .isEqualTo(new ControlState("a:1", true, List.of("", "2:b", "é😀"), false));
    assertThat(ControlCodec.state("01" + ControlCodec.values(List.of("")), false))
        .isEqualTo(new ControlState("", false, List.of(), true));
    assertThat(ControlCodec.state("00" + ControlCodec.values(List.of("x")), true).composing())
        .isTrue();
  }
}
