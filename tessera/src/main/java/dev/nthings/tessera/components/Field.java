package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// Labelled container for a form control: a visible `<label>` that names the control, optional
/// hint text that describes it, error text shown while it is `:user-invalid`, and an optional
/// visual tooltip. Create one with [#of(String, TagElement)] for a control from [Forms] or
/// [#of(String, Select)].
///
/// The control, hint and error ids are prefixed with its [#id(String)]. Without an id, the
/// compiler numbers instances `form-field-<n>`.
public final class Field extends BuiltinView<Field> {

  private final String label;

  /// Exactly one is set: the bare control from [#of(String, TagElement)], or the select from
  /// [#of(String, Select)], whose `<select>` is the control.
  private final @Nullable TagElement bareControl;

  private final @Nullable Select select;
  private @Nullable String hint;
  private @Nullable String error;
  private @Nullable String tooltip;

  private Field(String label, @Nullable TagElement bareControl, @Nullable Select select) {
    this.label = label;
    this.bareControl = bareControl;
    this.select = select;
  }

  /// Wraps `control`, such as [Forms#text(String)], in a field labelled `label`. The control gets
  /// an assigned id unless you bind one with `control.id(ref)`.
  public static Field of(String label, TagElement control) {
    return new Field(label, control, null);
  }

  /// Wraps `select` in a field labelled `label`.
  public static Field of(String label, Select select) {
    return new Field(label, null, select);
  }

  /// Sets hint text shown above the control and linked to it with `aria-describedby`.
  public Field hint(String hint) {
    this.hint = hint;
    return this;
  }

  /// Sets error text shown while the control is `:user-invalid`. The control references the text
  /// through `aria-errormessage`.
  public Field error(String error) {
    this.error = error;
    return this;
  }

  /// Sets a tooltip anchored above the control while it is hovered or focused. The tooltip is
  /// visual only: the control does not reference it, so assistive technology does not read it.
  public Field tooltip(String tooltip) {
    this.tooltip = tooltip;
    return this;
  }

  @Override
  public Field self() {
    return this;
  }

  @Override
  String viewType() {
    return "form-field";
  }

  @Override
  IntoElement render(RenderContext context) {
    var wrapper = select;
    var control =
        (wrapper != null ? wrapper.control() : Objects.requireNonNull(bareControl)).copy();
    var ctrl = control.ref();

    var container = div().attr("class", "field-container");

    var labelEl = Elements.label().labelFor(ctrl).attr("class", "field-label").text(label);
    container.child(labelEl);

    if (hint != null) {
      var hintRef = ElementRef.scoped("hint");
      container.child(span().id(hintRef).attr("class", "field-hint").text(hint));
      control.ariaDescribedBy(hintRef);
    }

    if (tooltip != null) {
      control.anchorName();
    }

    container.child(wrapper != null ? wrapper.around(control) : control);

    // Follows the control so adjacent-sibling selectors can style it.
    if (error != null) {
      var errorRef = ElementRef.scoped("error");
      control.ariaErrorMessage(errorRef);
      var errEl =
          div()
              .id(errorRef)
              .attr("class", "field-error")
              .child(span().ariaHidden(true).text("⚠ "))
              .child(span().text(error));
      container.child(errEl);
    }

    if (tooltip != null) {
      var tooltipEl =
          div()
              .attr("class", "field-anchor-tooltip")
              .role(Role.TOOLTIP)
              .anchoredTo(
                  ctrl,
                  PositionArea.of(PositionArea.Part.TOP, PositionArea.Part.SPAN_RIGHT),
                  PositionTry.flip(PositionTry.Flip.BLOCK),
                  PositionTry.flip(PositionTry.Flip.INLINE))
              .text(tooltip);
      container.child(tooltipEl);
    }

    return container;
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.flex().flexCol().gap(Tokens.SPACE_1).relative())
        .select(":scope .field-label", s -> s.typography(Roles.LABEL).textColor(Tokens.CONTROL_FG))
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
