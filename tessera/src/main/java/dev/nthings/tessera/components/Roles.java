package dev.nthings.tessera.components;

import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Typography;

/// Semantic roles consumed by components; values are [Tokens], so a theme can replace them
/// through the registry.
public final class Roles {

  public static final Typography LABEL =
      new Typography(
          Tokens.CONTROL_FAMILY,
          Tokens.CONTROL_FONT_SIZE,
          600,
          Tokens.CONTROL_LINE_HEIGHT,
          Tokens.CONTROL_TRACKING);

  private Roles() {}
}
