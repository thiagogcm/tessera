package dev.nthings.tessera.components;

import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Typography;

/// Semantic roles consumed by components. The values are tokens in [Tokens], so a theme replaces
/// them through the registry without editing the component.
public final class Roles {

  public static final DesignToken.Color FOREGROUND = Tokens.CONTROL_FG;

  public static final Typography LABEL =
      new Typography(
          Tokens.CONTROL_FAMILY,
          Tokens.CONTROL_FONT_SIZE,
          600,
          Tokens.CONTROL_LINE_HEIGHT,
          Tokens.CONTROL_TRACKING);

  private Roles() {}
}
