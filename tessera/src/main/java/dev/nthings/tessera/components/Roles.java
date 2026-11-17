package dev.nthings.tessera.components;

import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Typography;

/// Typography roles that components share. Each role is a [Typography] built from design tokens, so
/// overriding a token in the theme restyles every component that uses the role. These are text
/// roles, not ARIA roles.
public final class Roles {

  /// Label text for form fields and switches, buttons, and callout titles: the control font tokens
  /// at weight 600.
  public static final Typography LABEL =
      new Typography(
          Tokens.CONTROL_FAMILY,
          Tokens.CONTROL_FONT_SIZE,
          600,
          Tokens.CONTROL_LINE_HEIGHT,
          Tokens.CONTROL_TRACKING);

  private Roles() {}
}
