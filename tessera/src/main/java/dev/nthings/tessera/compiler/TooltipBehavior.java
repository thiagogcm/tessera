package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.ClientBehavior;

/// Document-level fallback for interest invokers when the browser has no native `interestfor`.
final class TooltipBehavior {

  static final ClientBehavior INSTANCE =
      ClientBehavior.fromResource(
          "tessera-interest-tooltip", TooltipBehavior.class, "interest-tooltip.js");

  private TooltipBehavior() {}
}
