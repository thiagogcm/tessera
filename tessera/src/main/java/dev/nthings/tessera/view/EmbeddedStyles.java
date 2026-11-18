package dev.nthings.tessera.view;

import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.internal.css.StyleSheet;
import java.util.List;

/// Stylesheet information needed to mount a live root in a host document.
public final class EmbeddedStyles {
  public static final String ROOT_ATTRIBUTE = StyleSheet.ROOT_ATTRIBUTE;
  public static final String THEME_ATTRIBUTE = StyleSheet.THEME_ATTRIBUTE;
  public static final String LAYER_ORDER = StyleSheet.LAYER_ORDER;

  private EmbeddedStyles() {}

  public static List<String> baseRules(Theme theme) {
    return StyleSheet.embeddedBaseRules(theme);
  }

  public static String themeKey(Theme theme) {
    return StyleSheet.themeKey(theme);
  }
}
