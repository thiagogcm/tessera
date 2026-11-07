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
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Background;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.css.Tokens;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Accessible Forms and Form Controls Suite backed by CSS Level 4 `:user-invalid` / `:user-valid`
/// pseudo-classes, HTML `<input type="checkbox" switch>`, `appearance: base-select` where
/// supported, and CSS Anchor Positioning tooltips.
public final class Forms {

  private Forms() {}

  /// Creates a styled form input element with default focus and validation styling.
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

  /// Creates a styled multiline `<textarea>` that grows with its content (`field-sizing:
  /// content`) from a minimum of `minRows` lines.
  public static TagElement textareaControl(String name, int minRows) {
    return controlChrome(textarea().attr("name", name))
        .fieldSizingContent()
        .minBlockSize(CssLength.lh(minRows).plus(CssLength.rem(1)))
        .resizeVertical()
        .focusVisible(Forms::controlFocusRing)
        .userInvalid(s -> s.borderColor(Tokens.STATUS_DANGER))
        .userValid(s -> s.borderColor(Tokens.STATUS_SUCCESS));
  }

  /// Creates a binary on/off toggle: a `role="switch"` checkbox drawn as a track and thumb.
  ///
  /// The target browsers have no native `<input switch>`, so the control is `appearance: none`
  /// with the thumb
  /// as a background layer that slides on `:checked`.
  public static TagElement switchInput(String name, boolean checked) {
    return input("checkbox")
        .attr("name", name)
        .role(Role.SWITCH)
        .boolAttr("checked", checked)
        .appearanceNone()
        .inlineSize(CssLength.rem(2.5))
        .blockSize(CssLength.rem(1.5))
        .rounded(Tokens.RADIUS_FULL)
        .background(switchTrack(CssLength.zero(), Tokens.BORDER_SUBTLE))
        .cursorPointer()
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

  /// Creates a full accessible field wrapper for an input or control.
  public static Field field(String labelText, TagElement control) {
    return new Field(labelText, control);
  }

  /// Creates a full accessible field wrapper for a customizable select control.
  public static Field field(String labelText, SelectControl select) {
    return new Field(labelText, select.control(), select);
  }

  /// Creates an inline row pairing a label and a switch control.
  public static TagElement switchField(String labelText, String name, boolean checked) {
    var sw = switchInput(name, checked).id("switch-" + name);
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
                .cursorPointer()
                .text(labelText))
        .child(sw);
  }

  /// Creates a customizable [SelectControl] with `appearance: base-select`.
  public static SelectControl selectControl(String name) {
    return new SelectControl(name);
  }

  /// Accessible field container providing label, format hints, and error messaging.
  public static final class Field implements RenderView {

    private final String labelText;
    private final TagElement control;
    private final IntoElement controlNode;
    private @Nullable String hintText;
    private @Nullable String errorText;
    private @Nullable String anchorTooltipText;

    public Field(String labelText, TagElement control) {
      this(labelText, control, control);
    }

    /// `control` is the labelled element; `controlNode` is what gets placed (e.g. its view).
    private Field(String labelText, TagElement control, IntoElement controlNode) {
      this.labelText = labelText;
      this.control = control;
      this.controlNode = controlNode;
    }

    /// Names the control's DOM id (scoped to this field) instead of a compiler-assigned one.
    public Field id(String name) {
      control.id(name);
      return this;
    }

    /// Sets format hint text placed above the input, preventing autocomplete overlays from
    /// obscuring it.
    public Field hint(String hintText) {
      this.hintText = hintText;
      return this;
    }

    /// Sets validation error message text revealed when the field matches `:user-invalid`.
    public Field error(String errorText) {
      this.errorText = errorText;
      return this;
    }

    /// Attaches a CSS Anchor Positioning floating tooltip to this field.
    public Field anchorTooltip(String tooltipText) {
      this.anchorTooltipText = tooltipText;
      return this;
    }

    @Override
    public String viewType() {
      return "form-field";
    }

    @Override
    public Element render() {
      var ctrl = control.ref();

      var container = div().attr("class", "field-container");

      // 1. Label
      var labelEl = label().labelFor(ctrl).attr("class", "field-label").text(labelText);
      container.child(labelEl);

      // 2. Format hint placed above input per modern guidance
      if (hintText != null) {
        var hint = ElementRef.named("hint");
        container.child(span().id(hint).attr("class", "field-hint").text(hintText));
        control.ariaDescribedBy(hint);
      }

      // 3. Optional anchor for the floating tooltip
      if (anchorTooltipText != null) {
        control.anchorName();
      }

      // 4. Control
      container.child(controlNode);

      // 5. Error message placed immediately after control for adjacent sibling styling
      if (errorText != null) {
        var error = ElementRef.named("error");
        control.ariaErrorMessage(error);
        var errEl =
            div()
                .id(error)
                .attr("class", "field-error")
                .child(span().ariaHidden(true).text("⚠ "))
                .child(span().text(errorText));
        container.child(errEl);
      }

      // 6. Anchor-positioned floating tooltip
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
                      .borderSubtle(Tokens.BORDER_SUBTLE)
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

  /// Customizable `<select>` component implementing `appearance: base-select` and top-layer option
  /// picker styling where supported, with a native select fallback elsewhere.
  public static final class SelectControl implements RenderView {

    public record OptionItem(String value, String label, boolean selected) {}

    private final TagElement select;

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
      select.id(id);
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

    /// The `<select>` element, for wiring labels and descriptions to it.
    TagElement control() {
      return select;
    }

    @Override
    public String viewType() {
      return "custom-select";
    }

    @Override
    public Element render() {
      return select;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> controlChrome(s.appearanceBaseSelect()).cursorPointer())
          .select(":scope:focus-visible", Forms::controlFocusRing)
          .select(
              ":scope::picker(select)",
              s ->
                  s.appearanceBaseSelect()
                      .bg(Tokens.SURFACE_CARD)
                      .textColor(Tokens.TEXT_PRIMARY)
                      .borderSubtle(Tokens.BORDER_SUBTLE)
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
                      .cursorPointer())
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
        .borderSubtle(Tokens.BORDER_SUBTLE)
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
