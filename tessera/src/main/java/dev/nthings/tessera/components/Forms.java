package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.input;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.select;
import static dev.nthings.tessera.core.Elements.selectedcontent;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.textarea;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Appearance;
import dev.nthings.tessera.css.Background;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.Resize;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.css.Tokens;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Form controls: text-like inputs, a textarea, a switch and a customizable select. The
/// controls are bare: wrap one with [#field(String, TagElement)] (or
/// [#field(String, SelectControl)]) to add a visible label, hint and error text. [#switchField]
/// is a labelled switch and needs no wrapper. Validation styling follows the browser's
/// `:user-invalid` and `:user-valid` states.
public final class Forms {

  private Forms() {}

  /// Styled `<input>` of `type` named `name`. Wrap it with [#field(String, TagElement)] to label
  /// it.
  public static TagElement inputControl(String name, String type) {
    return controlChrome(input(type).attr("name", name))
        .focusVisible(Forms::controlFocusRing)
        .userInvalid(
            s ->
                s.borderColor(Tokens.STATUS_DANGER)
                    .bg(CssColor.sameHue(Tokens.STATUS_DANGER, 97, 0.03)))
        .userValid(s -> s.borderColor(Tokens.STATUS_SUCCESS));
  }

  public static TagElement text(String name) {
    return inputControl(name, "text");
  }

  public static TagElement email(String name) {
    return inputControl(name, "email");
  }

  public static TagElement password(String name) {
    return inputControl(name, "password");
  }

  public static TagElement number(String name) {
    return inputControl(name, "number");
  }

  public static TagElement search(String name) {
    return inputControl(name, "search");
  }

  /// Styled `<textarea>` named `name` with a minimum height of `minRows` lines plus 1rem. It
  /// grows with its content where the browser supports that. Wrap it with
  /// [#field(String, TagElement)] to label it.
  public static TagElement textareaControl(String name, int minRows) {
    return controlChrome(textarea().attr("name", name))
        .fieldSizingContent()
        .minBlockSize(CssLength.lh(minRows).plus(CssLength.rem(1)))
        .resize(Resize.VERTICAL)
        .focusVisible(Forms::controlFocusRing)
        .userInvalid(s -> s.borderColor(Tokens.STATUS_DANGER))
        .userValid(s -> s.borderColor(Tokens.STATUS_SUCCESS));
  }

  /// On/off switch named `name`: a checkbox with `role="switch"`, drawn as a track and a thumb.
  ///
  /// Chromium and Firefox have no native switch, so the checkbox is restyled and the thumb is a
  /// background layer that slides when checked. Use [#switchField] to add a label.
  public static TagElement switchInput(String name, boolean checked) {
    return input("checkbox")
        .attr("name", name)
        .role(Role.SWITCH)
        .boolAttr("checked", checked)
        .appearance(Appearance.NONE)
        .inlineSize(CssLength.rem(2.5))
        .blockSize(CssLength.rem(1.5))
        .rounded(Tokens.RADIUS_FULL)
        .background(switchTrack(CssLength.zero(), Tokens.BORDER_SUBTLE))
        .cursor(Cursor.POINTER)
        .focusVisible(s -> s.focusRing(Tokens.ACCENT_PRIMARY))
        .checked(s -> s.background(switchTrack(CssLength.pct(100), Tokens.ACCENT_PRIMARY)))
        .motionSafe(s -> s.motion(Motion.controlFeedback().properties(Animatable.BACKGROUND)));
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

  /// Wraps a text-like control in a labelled [Field].
  public static Field field(String labelText, TagElement control) {
    return new Field(labelText, control);
  }

  /// Wraps a select in a labelled [Field].
  public static Field field(String labelText, SelectControl select) {
    return new Field(labelText, select.control(), SelectControl::new);
  }

  /// Labelled switch row: the label names the switch. The switch's id is `switch-<name>` in the
  /// enclosing view scope, so two switches with the same name in one view scope report
  /// `duplicate-id`.
  public static TagElement switchField(String labelText, String name, boolean checked) {
    var sw = switchInput(name, checked).id(ElementRef.scoped("switch-" + name));
    return div()
        .flex()
        .itemsCenter()
        .justifyBetween()
        .gap(Tokens.SPACE_4)
        .py(Tokens.SPACE_1)
        .child(
            label()
                .labelFor(sw.ref())
                .typography(Roles.LABEL)
                .textColor(Tokens.CONTROL_FG)
                .cursor(Cursor.POINTER)
                .text(labelText))
        .child(sw);
  }

  /// Creates a customizable [SelectControl] named `name`. Wrap it with
  /// [#field(String, SelectControl)] to label it.
  public static SelectControl selectControl(String name) {
    return new SelectControl(name);
  }

  /// Labelled container for a control: a `<label>`, optional hint text that describes the
  /// control, error text shown while the control is `:user-invalid`, and an optional
  /// [#anchorTooltip(String)] tooltip. Create one with [Forms#field(String, TagElement)] or
  /// [Forms#field(String, SelectControl)].
  public static final class Field implements View {

    private final String labelText;
    private final TagElement control;
    private final Function<TagElement, IntoElement> controlView;
    private @Nullable String hintText;
    private @Nullable String errorText;
    private @Nullable String anchorTooltipText;

    public Field(String labelText, TagElement control) {
      this(labelText, control, element -> element);
    }

    private Field(
        String labelText, TagElement control, Function<TagElement, IntoElement> controlView) {
      this.labelText = labelText;
      this.control = control;
      this.controlView = controlView;
    }

    /// Sets the DOM id of the control, replacing the compiler-assigned one.
    public Field id(String name) {
      control.id(ElementRef.scoped(name));
      return this;
    }

    /// Sets hint text shown above the control and linked to it with `aria-describedby`, or
    /// removes it when `null`.
    public Field hint(@Nullable String hintText) {
      this.hintText = hintText;
      return this;
    }

    /// Sets error text shown while the control is `:user-invalid`, or removes it when `null`.
    /// The control references the text through `aria-errormessage`.
    public Field error(@Nullable String errorText) {
      this.errorText = errorText;
      return this;
    }

    /// Sets a tooltip anchored above the control while it is hovered or focused. The tooltip
    /// is visual only: the control does not reference it, so assistive technology does not
    /// read it.
    public Field anchorTooltip(String tooltipText) {
      this.anchorTooltipText = tooltipText;
      return this;
    }

    @Override
    public String viewType() {
      return "form-field";
    }

    @Override
    public Element render(RenderContext context) {
      var control = this.control.copy();
      var ctrl = control.ref();

      var container = div().attr("class", "field-container");

      var labelEl = label().labelFor(ctrl).attr("class", "field-label").text(labelText);
      container.child(labelEl);

      if (hintText != null) {
        var hint = ElementRef.scoped("hint");
        container.child(span().id(hint).attr("class", "field-hint").text(hintText));
        control.ariaDescribedBy(hint);
      }

      if (anchorTooltipText != null) {
        control.anchorName();
      }

      container.child(controlView.apply(control));

      // Follows the control so adjacent-sibling selectors can style it.
      if (errorText != null) {
        var error = ElementRef.scoped("error");
        control.ariaErrorMessage(error);
        var errEl =
            div()
                .id(error)
                .attr("class", "field-error")
                .child(span().ariaHidden(true).text("⚠ "))
                .child(span().text(errorText));
        container.child(errEl);
      }

      if (anchorTooltipText != null) {
        var tooltipEl =
            div()
                .attr("class", "field-anchor-tooltip")
                .role(Role.TOOLTIP)
                .anchoredTo(
                    ctrl,
                    PositionArea.of(PositionArea.Part.TOP, PositionArea.Part.SPAN_RIGHT),
                    PositionTry.flip(PositionTry.Flip.BLOCK),
                    PositionTry.flip(PositionTry.Flip.INLINE))
                .text(anchorTooltipText);
        container.child(tooltipEl);
      }

      return container;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.flex().flexCol().gap(Tokens.SPACE_1).relative())
          .select(
              ":scope .field-label", s -> s.typography(Roles.LABEL).textColor(Tokens.CONTROL_FG))
          .select(
              ":scope .field-hint",
              s -> s.fontSize(Tokens.FONT_SIZE_XS).textColor(Tokens.TEXT_MUTED).m(CssLength.px(0)))
          .select(
              ":scope .field-error",
              s ->
                  s.hidden()
                      .fontSize(Tokens.FONT_SIZE_XS)
                      .fontWeight(500)
                      .textColor(Tokens.STATUS_DANGER)
                      .itemsCenter()
                      .gap(Tokens.SPACE_1))
          .select(
              ":scope:has(:user-invalid) .field-error, :scope :user-invalid + .field-error",
              s -> s.flex())
          .select(":scope:has(:user-invalid) .field-label", s -> s.textColor(Tokens.STATUS_DANGER))
          .select(":scope:has(:user-valid) .field-label", s -> s.textColor(Tokens.STATUS_SUCCESS))
          .select(
              ":scope .field-anchor-tooltip",
              s ->
                  s.hidden()
                      .absolute()
                      .bg(Tokens.SURFACE_CARD)
                      .textColor(Tokens.TEXT_PRIMARY)
                      .border(Border.hairline(Tokens.BORDER_SUBTLE))
                      .rounded(Tokens.RADIUS_MD)
                      .px(CssLength.rem(0.6))
                      .py(CssLength.rem(0.3))
                      .fontSize(Tokens.FONT_SIZE_XS)
                      .shadow(
                          Shadow.drop(
                              CssLength.px(4), CssLength.px(12), CssColor.oklch(0, 0, 0, 0.15))))
          .select(
              ":scope:has(:focus-visible) .field-anchor-tooltip, :scope:has(:hover) .field-anchor-tooltip",
              s -> s.block());
    }
  }

  /// Select whose open list the library styles where the browser supports customizable
  /// selects; other browsers show their native list. Create one with
  /// [Forms#selectControl(String)].
  public static final class SelectControl implements View {

    public record OptionItem(String value, String label, boolean selected) {}

    private final TagElement select;

    private SelectControl(TagElement select) {
      this.select = select;
    }

    public SelectControl(String name) {
      this.select =
          select()
              .attr("name", name)
              .attr("class", "base-select-control")
              .child(
                  button()
                      .attr("class", "select-trigger")
                      .child(span().visuallyHidden().text("Select " + name))
                      .child(selectedcontent()));
    }

    public SelectControl id(String id) {
      select.id(ElementRef.scoped(id));
      return this;
    }

    public SelectControl option(String value, String label) {
      return option(value, label, false);
    }

    public SelectControl option(String value, String label, boolean selected) {
      select.child(optionElement(new OptionItem(value, label, selected)));
      return this;
    }

    public SelectControl optgroup(String groupLabel, List<OptionItem> options) {
      var group = Elements.optgroup(groupLabel);
      options.forEach(opt -> group.child(optionElement(opt)));
      select.child(group);
      return this;
    }

    /// Underlying `<select>`, for label and description wiring.
    TagElement control() {
      return select;
    }

    @Override
    public String viewType() {
      return "custom-select";
    }

    @Override
    public Element render(RenderContext context) {
      return select;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> controlChrome(s.appearance(Appearance.BASE_SELECT)).cursor(Cursor.POINTER))
          .select(":scope:focus-visible", Forms::controlFocusRing)
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
                      .cursor(Cursor.POINTER))
          .select(":scope option:hover", s -> s.bg(Tokens.SURFACE_SUBTLE))
          .select(":scope option::checkmark", s -> s.textColor(Tokens.ACCENT_PRIMARY))
          .select(":scope::picker-icon", s -> s.textColor(Tokens.TEXT_MUTED));
    }
  }

  /// Shared padding, border, surface and type styling for text-like form controls.
  private static <S extends Styled<S>> S controlChrome(S control) {
    return control
        .px(Tokens.CONTROL_PADDING_INLINE)
        .py(Tokens.SPACE_2)
        .rounded(Tokens.RADIUS_LG)
        .border(Border.hairline(Tokens.BORDER_SUBTLE))
        .bg(Tokens.SURFACE_CARD)
        .textColor(Tokens.TEXT_PRIMARY)
        .fontSize(Tokens.FONT_SIZE_SM);
  }

  private static StyleRefinement controlFocusRing(StyleRefinement s) {
    return s.outline(Border.solid(CssLength.px(2), Tokens.ACCENT_PRIMARY))
        .outlineOffset(CssLength.px(1));
  }

  private static TagElement optionElement(SelectControl.OptionItem opt) {
    return Elements.option(opt.value(), opt.label()).boolAttr("selected", opt.selected());
  }
}
