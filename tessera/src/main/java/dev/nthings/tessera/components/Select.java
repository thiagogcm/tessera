package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.selectedcontent;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Appearance;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Customizable `<select>`: where the browser supports customizable selects, the library styles its
/// button and open list; other browsers show their native list. Create one with [#of(String)], add
/// options, and wrap it with [Field#of(String, Select)] to label it.
///
/// Without an [#id(String)], the compiler numbers instances `custom-select-<n>`.
public final class Select extends BuiltinScope<Select> {

  /// An option's submitted value and visible label for [Select#optgroup(String, List)].
  public static final class Option {

    private final String value;
    private final String label;

    private Option(String value, String label) {
      this.value = value;
      this.label = label;
    }

    /// An option submitting `value` and showing `label`.
    public static Option of(String value, String label) {
      return new Option(value, label);
    }
  }

  private final TagElement select;

  private Select(TagElement select) {
    this.select = select;
  }

  private Select(String name) {
    this(
        Elements.select()
            .attr("name", name)
            .attr("class", "base-select-control")
            .child(
                button()
                    .attr("class", "select-trigger")
                    .child(span().visuallyHidden().text("Select " + name))
                    .child(selectedcontent())));
  }

  /// Creates an empty select submitted as `name`.
  public static Select of(String name) {
    return new Select(name);
  }

  /// Selects the first option whose value matches on insertion and native form reset. Use ordinary
  /// options with this default so there is one source of initial selection.
  public Select initialValue(String value) {
    select.initialValue(value);
    return this;
  }

  /// Adds an option submitting `value` and showing `label`.
  public Select option(String value, String label) {
    select.child(Elements.option(value, label));
    return this;
  }

  /// Adds an `<optgroup>` labelled `label` holding `options`.
  public Select optgroup(String label, List<Option> options) {
    var group = Elements.optgroup(label);
    options.forEach(option -> group.child(Elements.option(option.value, option.label)));
    select.child(group);
    return this;
  }

  /// The underlying `<select>`, for label and description wiring.
  TagElement control() {
    return select;
  }

  /// A select with this one's id around `control`, a wired copy of [#control()].
  Select around(TagElement control) {
    var copy = new Select(control);
    copy.id = id;
    return copy;
  }

  @Override
  public Select self() {
    return this;
  }

  @Override
  String scopeName() {
    return "custom-select";
  }

  @Override
  TagElement expand(ViewContext context) {
    return select;
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s ->
            Forms.controlChrome(s.appearance(Appearance.BASE_SELECT))
                .cursor(Cursor.POINTER)
                .on(State.FOCUS_VISIBLE, Forms::controlFocusRing))
        .select(
            ":scope::picker(select)",
            s ->
                s.appearance(Appearance.BASE_SELECT)
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .rounded(Tokens.RADIUS_LG)
                    .p(Tokens.SPACE_1_5)
                    .shadow(
                        Shadow.drop(
                            CssLength.px(12),
                            CssLength.px(28),
                            CssLength.px(-6),
                            CssColor.oklch(0, 0, 0, 0.25))))
        .select(
            ":scope option",
            s ->
                s.px(Tokens.SPACE_3)
                    .py(Tokens.SPACE_2)
                    .rounded(Tokens.RADIUS_MD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .cursor(Cursor.POINTER)
                    .on(State.HOVER, h -> h.bg(Tokens.SURFACE_SUBTLE)))
        .select(":scope option::checkmark", s -> s.textColor(Tokens.ACCENT_PRIMARY))
        .select(":scope::picker-icon", s -> s.textColor(Tokens.TEXT_MUTED));
  }
}
