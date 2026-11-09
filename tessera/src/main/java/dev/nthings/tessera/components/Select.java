package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.selectedcontent;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
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

/// Customizable `<select>`: where the browser supports customizable selects, the library styles
/// its button and open list; other browsers show their native list. Create one with
/// [#of(String)], add options, and wrap it with [Field#of(String, Select)] to label it.
///
/// Without an [#id(String)], the compiler numbers instances `custom-select-<n>`.
public final class Select extends BuiltinScope<Select> {

  /// One option for [Select#optgroup(String, List)]: the submitted `value`, the visible
  /// `label`, and whether it starts selected. Create one with [#of(String, String)] or
  /// [#selected(String, String)].
  public static final class Option {

    private final String value;
    private final String label;
    private final boolean selected;

    private Option(String value, String label, boolean selected) {
      this.value = value;
      this.label = label;
      this.selected = selected;
    }

    /// An option submitting `value` and showing `label`.
    public static Option of(String value, String label) {
      return new Option(value, label, false);
    }

    /// An option submitting `value` and showing `label`, selected on load.
    public static Option selected(String value, String label) {
      return new Option(value, label, true);
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

  /// Adds an option submitting `value` and showing `label`.
  public Select option(String value, String label) {
    select.child(optionElement(Option.of(value, label)));
    return this;
  }

  /// Adds an option submitting `value` and showing `label`, selected on load.
  public Select selectedOption(String value, String label) {
    select.child(optionElement(Option.selected(value, label)));
    return this;
  }

  /// Adds an `<optgroup>` labelled `label` holding `options`.
  public Select optgroup(String label, List<Option> options) {
    var group = Elements.optgroup(label);
    options.forEach(option -> group.child(optionElement(option)));
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
  IntoElement expand(DocumentContext context) {
    return select;
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
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

  private static TagElement optionElement(Option option) {
    return Elements.option(option.value, option.label)
        .onlyIf(option.selected, el -> el.boolAttr("selected"));
  }
}
