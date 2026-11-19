package dev.nthings.tessera.compiler;

import dev.nthings.tessera.document.PageScript;

/// Document-level fallback for interest invokers when the browser has no native `interestfor`.
final class TooltipScript {

  static final PageScript INSTANCE =
      PageScript.fromResource(
          "tessera-interest-tooltip", TooltipScript.class, "interest-tooltip.js");

  private TooltipScript() {}
}
