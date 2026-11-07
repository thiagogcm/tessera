package dev.nthings.tessera.render;

import dev.nthings.tessera.core.ClientBehavior;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Document-level fallback for interest invokers when the browser has no native `interestfor`.
final class TooltipBehavior {

  static final ClientBehavior INSTANCE =
      new ClientBehavior("tessera-interest-tooltip", readScript());

  private TooltipBehavior() {}

  private static String readScript() {
    try (var input =
        Objects.requireNonNull(
            TooltipBehavior.class.getResourceAsStream("interest-tooltip.js"),
            "Bundled interest tooltip module")) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
