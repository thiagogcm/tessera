package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Appearance;
import dev.nthings.tessera.css.Background;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.Resize;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.Typography;
import dev.nthings.tessera.internal.css.Tint;

/// Form control functions: styled text-like inputs, a textarea and a switch, returned as plain
/// elements you can refine further. The controls are bare: wrap one with [Field#of(String,
/// TagElement)] to add a visible label, hint and error text. [#switchField(String, String)] and
/// [#checkedSwitchField(String, String)] are labelled switches and need no wrapper; [Select] is the
/// customizable select. Validation styling follows the browser's `:user-invalid` and `:user-valid`
/// states.
public final class Forms {

  private Forms() {}

  /// Styled `<input>` of `type` submitted as `name`. Wrap it with [Field#of(String, TagElement)] to
  /// label it.
  public static TagElement input(String name, InputType type) {
    return controlChrome(Elements.input(type).attr("name", name))
        .on(State.FOCUS_VISIBLE, Forms::controlFocusRing)
        .on(
            State.USER_INVALID,
            s ->
                // A tint of the field's own surface, so the text stays legible in dark mode.
                s.borderColor(Tokens.STATUS_DANGER).bg(Tint.INVALID_FIELD.fill(Tone.DANGER)))
        .on(State.USER_VALID, s -> s.borderColor(Tokens.STATUS_SUCCESS));
  }

  /// Styled `<input type="text">` submitted as `name`.
  public static TagElement text(String name) {
    return input(name, InputType.TEXT);
  }

  /// Styled `<input type="email">` submitted as `name`.
  public static TagElement email(String name) {
    return input(name, InputType.EMAIL);
  }

  /// Styled `<input type="password">` submitted as `name`.
  public static TagElement password(String name) {
    return input(name, InputType.PASSWORD);
  }

  /// Styled `<input type="number">` submitted as `name`.
  public static TagElement number(String name) {
    return input(name, InputType.NUMBER);
  }

  /// Styled `<input type="search">` submitted as `name`.
  public static TagElement search(String name) {
    return input(name, InputType.SEARCH);
  }

  /// Styled `<textarea>` submitted as `name` with a minimum height of `minRows` lines plus 1rem. It
  /// grows with its content where the browser supports that. Wrap it with [Field#of(String,
  /// TagElement)] to label it.
  public static TagElement textarea(String name, int minRows) {
    return controlChrome(Elements.textarea().attr("name", name))
        .fieldSizingContent()
        .minBlockSize(CssLength.lh(minRows).plus(CssLength.rem(1)))
        .resize(Resize.VERTICAL)
        .on(State.FOCUS_VISIBLE, Forms::controlFocusRing)
        .on(State.USER_INVALID, s -> s.borderColor(Tokens.STATUS_DANGER))
        .on(State.USER_VALID, s -> s.borderColor(Tokens.STATUS_SUCCESS));
  }

  /// On/off switch submitted as `name`: a checkbox with `role="switch"`, drawn as a track and a
  /// thumb.
  ///
  /// Chromium and Firefox have no native switch, so the checkbox is restyled and the thumb is a
  /// background layer that slides when checked. In forced colors, which would hide that drawing, it
  /// renders as the native checkbox. It starts off; add `initialChecked(true)` to start it on. Use
  /// [#switchField(String, String)] to add a label.
  public static TagElement switchInput(String name) {
    return Elements.input(InputType.CHECKBOX)
        .attr("name", name)
        .role(Role.SWITCH)
        .appearance(Appearance.NONE)
        .inlineSize(CssLength.rem(2.5))
        .blockSize(CssLength.rem(1.5))
        .rounded(Tokens.RADIUS_FULL)
        .background(switchTrack(CssLength.zero(), Tokens.BORDER_SUBTLE))
        .cursor(Cursor.POINTER)
        .on(State.FOCUS_VISIBLE, s -> s.focusRing(Tokens.ACCENT_PRIMARY))
        .on(
            State.CHECKED,
            s -> s.background(switchTrack(CssLength.pct(100), Tokens.ACCENT_PRIMARY)))
        .on(
            Media.MOTION_SAFE,
            s -> s.transition(Transition.controlFeedback().properties(Animatable.BACKGROUND)))
        // Forced colors drop the drawn track and thumb, so fall back to the native checkbox.
        .on(
            Media.FORCED_COLORS,
            s ->
                s.appearance(Appearance.AUTO)
                    .inlineSize(CssLength.auto())
                    .blockSize(CssLength.auto()));
  }

  private static Background switchTrack(CssLength thumbX, DesignToken.Color trackColor) {
    return new Background(
        new Background.Dot(CssColor.white(), 80, 88),
        thumbX,
        CssLength.pct(50),
        CssLength.rem(1.5),
        CssLength.rem(1.5),
        trackColor);
  }

  /// Labelled switch row, off on load: the visible `label` names the switch. The switch's id is
  /// `switch-<name>` in the enclosing id scope, so two switches with the same name in one id scope
  /// report `duplicate-id`.
  public static TagElement switchField(String label, String name) {
    return switchRow(label, name, switchInput(name));
  }

  /// Labelled switch row that is on (checked) on load; otherwise the same as [#switchField(String,
  /// String)].
  public static TagElement checkedSwitchField(String label, String name) {
    return switchRow(label, name, switchInput(name).initialChecked(true));
  }

  private static TagElement switchRow(String label, String name, TagElement input) {
    var sw = input.id(ElementRef.scoped("switch-" + name));
    return div()
        .flex()
        .itemsCenter()
        .justifyBetween()
        .gap(Tokens.SPACE_4)
        .py(Tokens.SPACE_1)
        .child(
            Elements.label()
                .labelFor(sw.ref())
                .typography(Typography.LABEL)
                .textColor(Tokens.CONTROL_FG)
                .cursor(Cursor.POINTER)
                .text(label))
        .child(sw);
  }

  /// Shared padding, border, surface and type styling for text-like form controls.
  static <S extends Styled<S>> S controlChrome(S control) {
    return control
        .px(Tokens.CONTROL_PADDING_INLINE)
        .py(Tokens.SPACE_2)
        .rounded(Tokens.RADIUS_LG)
        .border(Border.hairline(Tokens.BORDER_SUBTLE))
        .bg(Tokens.SURFACE_CARD)
        .textColor(Tokens.TEXT_PRIMARY)
        .fontSize(Tokens.FONT_SIZE_SM);
  }

  static Declarations controlFocusRing(Declarations s) {
    return s.outline(Border.solid(CssLength.px(2), Tokens.ACCENT_PRIMARY))
        .outlineOffset(CssLength.px(1));
  }
}
