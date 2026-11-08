package dev.nthings.tessera.components;

/// Shorthand for the pill shape of [Badge]: each factory is the matching `Badge` factory followed
/// by [Badge#pill()]. [#of(String)] is [Badge#pill(String)].
public final class Pill {

  private Pill() {}

  public static Badge of(String text) {
    return Badge.pill(text);
  }

  public static Badge neutral(String text) {
    return Badge.neutral(text).pill();
  }

  public static Badge accent(String text) {
    return Badge.accent(text).pill();
  }

  public static Badge success(String text) {
    return Badge.success(text).pill();
  }

  public static Badge warning(String text) {
    return Badge.warning(text).pill();
  }

  public static Badge danger(String text) {
    return Badge.danger(text).pill();
  }

  public static Badge info(String text) {
    return Badge.info(text).pill();
  }

  public static Badge dot(String text, Badge.Tone tone) {
    return Badge.dot(text, tone).pill();
  }
}
