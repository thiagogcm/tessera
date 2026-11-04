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
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Accessible Forms and Form Controls Suite backed by CSS Level 4 `:user-invalid` / `:user-valid`
/// pseudo-classes, HTML `<input type="checkbox" switch>`, Chrome 156+ `appearance: base-select`,
/// and CSS Anchor Positioning tooltips.
public final class Forms {

  private Forms() {}

  /// Creates a styled form input element with default focus and validation styling.
  public static TagElement inputControl(String name, String type) {
    return controlChrome(input(type).attr("name", name))
        .focusVisible(Forms::controlFocusRing)
        .userInvalid(
            s ->
                s.borderColor(Tokens.STATUS_DANGER)
                    .bgRaw("oklch(from " + Tokens.STATUS_DANGER.varReference() + " 97% 0.03 h)"))
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

  /// Creates a styled multiline `<textarea>` with vertical resize.
  public static TagElement textareaControl(String name, int rows) {
    return controlChrome(textarea().attr("name", name))
        .attr("rows", Integer.toString(rows))
        .resizeVertical()
        .focusVisible(Forms::controlFocusRing)
        .userInvalid(s -> s.borderColor(Tokens.STATUS_DANGER))
        .userValid(s -> s.borderColor(Tokens.STATUS_SUCCESS));
  }

  /// Creates a modern binary on/off toggle switch backed by HTML `<input type="checkbox"
  /// switch>`.
  public static TagElement switchInput(String name, boolean checked) {
    return input("checkbox")
        .attr("name", name)
        .attr("switch", "")
        .boolAttr("checked", checked)
        .accentColor(Tokens.ACCENT_PRIMARY)
        .cursorPointer()
        .focusVisible(s -> s.focusRing(Tokens.ACCENT_PRIMARY));
  }

  /// Creates a full accessible field wrapper for an input or control.
  public static Field field(String labelText, TagElement control) {
    return new Field(labelText, control);
  }

  /// Creates a full accessible field wrapper for a customizable select control.
  public static Field field(String labelText, SelectControl select) {
    return new Field(labelText, select.asTag());
  }

  /// Creates an inline row pairing a label and a switch control.
  public static TagElement switchField(String labelText, String name, boolean checked) {
    var sw = switchInput(name, checked).id("switch-" + name);
    return div()
        .flex()
        .itemsCenter()
        .justifyBetween()
        .gap(CssLength.rem(1))
        .py(CssLength.rem(0.25))
        .child(
            label()
                .labelFor(sw.ref())
                .fontSize(CssLength.rem(0.875))
                .fontWeight(500)
                .textColor(Tokens.TEXT_PRIMARY)
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
    private @Nullable String hintText;
    private @Nullable String errorText;
    private @Nullable String anchorTooltipText;

    public Field(String labelText, TagElement control) {
      this.labelText = labelText;
      this.control = control;
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
      ElementRef ctrl = control.ref();

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
      container.child(control);

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
                .role("tooltip")
                .anchoredTo(ctrl, "top span-right", "flip-block, flip-inline")
                .text(anchorTooltipText);
        container.child(tooltipEl);
      }

      return container;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.flex().flexCol().gap(CssLength.rem(0.25)).relative())
          .select(
              ":scope .field-label",
              s -> s.fontSize(CssLength.rem(0.875)).fontWeight(600).textColor(Tokens.TEXT_PRIMARY))
          .select(
              ":scope .field-hint",
              s -> s.fontSize(CssLength.rem(0.8)).textColor(Tokens.TEXT_MUTED).m(CssLength.px(0)))
          .select(
              ":scope .field-error",
              s ->
                  s.hidden()
                      .fontSize(CssLength.rem(0.8))
                      .fontWeight(500)
                      .textColor(Tokens.STATUS_DANGER)
                      .itemsCenter()
                      .gap(CssLength.rem(0.25)))
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
                      .rounded(CssLength.rem(0.375))
                      .px(CssLength.rem(0.6))
                      .py(CssLength.rem(0.3))
                      .fontSize(CssLength.rem(0.75))
                      .shadow("0 4px 12px oklch(0% 0 0 / 0.15)"))
          .select(
              ":scope:has(:focus-visible) .field-anchor-tooltip, :scope:has(:hover) .field-anchor-tooltip",
              s -> s.block());
    }
  }

  /// Customizable `<select>` component implementing Chrome 156+ `appearance: base-select` and
  /// top-layer option picker styling.
  public static final class SelectControl implements RenderView {

    public record OptionItem(String value, String label, boolean selected) {}

    public record OptionGroup(String label, List<OptionItem> options) {}

    private final ElementId entityId;
    private final String name;
    private @Nullable String id;
    private final List<OptionItem> standaloneOptions = new ArrayList<>();
    private final List<OptionGroup> groups = new ArrayList<>();

    public SelectControl(String name) {
      this.name = name;
      this.entityId = ElementId.of("select-" + Fnv1a64.sanitizeIdentifier(name));
    }

    public SelectControl id(String id) {
      this.id = id;
      return this;
    }

    public SelectControl option(String value, String label) {
      standaloneOptions.add(new OptionItem(value, label, false));
      return this;
    }

    public SelectControl option(String value, String label, boolean selected) {
      standaloneOptions.add(new OptionItem(value, label, selected));
      return this;
    }

    public SelectControl optgroup(String groupLabel, List<OptionItem> options) {
      groups.add(new OptionGroup(groupLabel, List.copyOf(options)));
      return this;
    }

    @Override
    public ElementId entityId() {
      return entityId;
    }

    @Override
    public String viewType() {
      return "custom-select";
    }

    @Override
    public Element render() {
      var sel =
          select()
              .attr("name", name)
              .id(id != null ? id : "control")
              .attr("class", "base-select-control")
              .child(
                  button()
                      .attr("class", "select-trigger")
                      .ariaLabel("Select " + name)
                      .child(selectedcontent()));

      standaloneOptions.forEach(opt -> sel.child(optionElement(opt)));
      for (var group : groups) {
        var groupEl = Elements.optgroup(group.label());
        group.options().forEach(opt -> groupEl.child(optionElement(opt)));
        sel.child(groupEl);
      }

      return sel;
    }

    public TagElement asTag() {
      return (TagElement) render();
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
                      .rounded(CssLength.rem(0.5))
                      .p(CssLength.rem(0.375))
                      .shadow("0 12px 28px -6px oklch(0% 0 0 / 0.25)"))
          .select(
              ":scope option",
              s ->
                  s.px(CssLength.rem(0.75))
                      .py(CssLength.rem(0.5))
                      .rounded(CssLength.rem(0.375))
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
        .px(CssLength.rem(0.875))
        .py(CssLength.rem(0.5))
        .rounded(CssLength.rem(0.5))
        .borderSubtle(Tokens.BORDER_SUBTLE)
        .bg(Tokens.SURFACE_CARD)
        .textColor(Tokens.TEXT_PRIMARY)
        .fontSize(CssLength.rem(0.875));
  }

  private static StyleRefinement controlFocusRing(StyleRefinement s) {
    return s.outline("2px solid " + Tokens.ACCENT_PRIMARY.varReference())
        .outlineOffset(CssLength.px(1));
  }

  private static TagElement optionElement(SelectControl.OptionItem opt) {
    return Elements.option(opt.value(), opt.label()).boolAttr("selected", opt.selected());
  }
}
