package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.BundledBehavior;
import dev.nthings.tessera.internal.components.Icons;

/// Icon button that cycles the page's color scheme through system, light and dark, and remembers
/// the choice in `localStorage` for every page of the site. Create one with [#of()] and place it in
/// the site header.
///
/// The choice is the `data-color-scheme` attribute of `<html>`, `light` or `dark`, or none for the
/// system preference; the stylesheet sets `color-scheme` from it, so every token follows. In a
/// static document the toggle's script switches the scheme, and a short inline script in `<head>`
/// restores the saved choice before first paint, so a page without the toggle keeps the system
/// scheme. The button shows a monitor, sun or moon for the current choice and is named for it, as
/// in `Color scheme: dark`.
public final class ColorSchemeToggle extends BuiltinScope<ColorSchemeToggle> {

  private ColorSchemeToggle() {}

  /// Creates a toggle.
  public static ColorSchemeToggle of() {
    return new ColorSchemeToggle();
  }

  @Override
  public ColorSchemeToggle self() {
    return this;
  }

  @Override
  String scopeName() {
    return "color-scheme-toggle";
  }

  @Override
  TagElement expand(ViewContext context) {
    var icons =
        span()
            .attr("class", "scheme-icons")
            .child(Icons.monitor().attr("class", "scheme-icon scheme-system"))
            .child(Icons.sun().attr("class", "scheme-icon scheme-light"))
            .child(Icons.moon().attr("class", "scheme-icon scheme-dark"));
    var root =
        div()
            .inlineFlex()
            .child(
                IconButton.of("Color scheme: system", icons)
                    .toTagElement()
                    .attr("data-color-scheme-toggle", ""));
    AuthoringAccess.tag(root).behavior = BundledBehavior.COLOR_SCHEME;
    return root;
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .select(":scope .scheme-icons", s -> s.flex())
        .select(":scope .scheme-icon", s -> s.hidden())
        .select(
            ":root:not([data-color-scheme=\"light\"],[data-color-scheme=\"dark\"]) :scope"
                + " .scheme-system,"
                + ":root[data-color-scheme=\"light\"] :scope .scheme-light,"
                + ":root[data-color-scheme=\"dark\"] :scope .scheme-dark",
            s -> s.block());
  }
}
