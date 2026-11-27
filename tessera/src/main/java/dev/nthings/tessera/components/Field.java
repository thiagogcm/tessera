package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Typography;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.components.WidgetBridge.FieldParts;
import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

/// Labelled container for a form control: a visible `<label>` that names the control, optional hint
/// text that describes it, error text shown while it is `:user-invalid`, and an optional visual
/// tooltip. Create one with [#of(String, TagElement)] for a control from [Forms] or [#of(String,
/// Select)].
///
/// The control, hint and error ids are prefixed with its [#scopeId(String)]. Without a scope id,
/// the compiler numbers instances `form-field-<n>`.
public final class Field extends BuiltinScope<Field> {

  static {
    WidgetBridge.registerField(Field::parts);
  }

  /// Application-supplied validity. An invalid control may have a visible error message.
  public record Validation(boolean invalid, @Nullable String message) {}

  private final String label;

  /// The control to render: the bare control from [#of(String, TagElement)], or the `<select>` from
  /// [#of(String, Select)].
  private final TagElement control;

  private final @Nullable Select select;
  private @Nullable String hint;
  private @Nullable String error;
  private @Nullable String tooltip;
  private @Nullable Validation validation;

  private Field(String label, TagElement control, @Nullable Select select) {
    this.label = label;
    this.control = control;
    this.select = select;
  }

  /// Wraps `control`, such as [Forms#input], in a field labelled `label`. The control gets an
  /// assigned id unless you bind one with `control.id(ref)`.
  public static Field of(String label, TagElement control) {
    return new Field(label, control, null);
  }

  /// Wraps `select` in a field labelled `label`.
  public static Field of(String label, Select select) {
    return new Field(label, select.control(), select);
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

  /// Sets explicit validity, overriding the browser-driven error configured with [#error(String)].
  public Field validation(Validation validation) {
    this.validation = validation;
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
  String scopeName() {
    return "form-field";
  }

  @Override
  TagElement expand(ViewContext context) {
    var parts = parts(null);
    var host = parts.host();
    parts.before().forEach(host::child);
    var scope = parts.controlScope();
    host.child(scope != null ? scope : parts.control());
    parts.after().forEach(host::child);
    return host;
  }

  private FieldParts parts(@Nullable Validation override) {
    var state = override == null ? validation : override;
    var control = this.control.copy();
    var ctrl = control.ref();

    var container = div().attr("class", "field-container");
    var before = new ArrayList<TagElement>();
    var after = new ArrayList<TagElement>();

    var labelEl = Elements.label().labelFor(ctrl).attr("class", "field-label").text(label);
    before.add(labelEl);

    if (hint != null) {
      var hintRef = ElementRef.scoped("hint");
      before.add(span().id(hintRef).attr("class", "field-hint").text(hint));
      control.ariaDescribedBy(hintRef);
    }

    if (tooltip != null) {
      control.anchor();
    }

    if (state != null) {
      control.ariaInvalid(state.invalid());
    }

    // Follows the control so adjacent-sibling selectors can style it.
    var message = state == null ? error : state.invalid() ? state.message() : null;
    if (message != null) {
      var errorRef = ElementRef.scoped("error");
      control.ariaErrorMessage(errorRef);
      var errEl =
          div()
              .id(errorRef)
              .attr("class", state == null ? "field-error" : "field-error field-explicit-error")
              .child(span().ariaHidden(true).text("⚠ "))
              .child(span().text(message));
      after.add(errEl);
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
      after.add(tooltipEl);
    }

    return new FieldParts(
        container,
        before,
        control,
        select == null ? null : WidgetBridge.scope(select.around(control)),
        after);
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.flex().flexCol().gap(Tokens.SPACE_1).relative())
        .select(
            ":scope .field-label", s -> s.typography(Typography.LABEL).textColor(Tokens.CONTROL_FG))
        .select(
            ":scope .field-hint",
            s -> s.fontSize(Tokens.FONT_SIZE_XS).textColor(Tokens.TEXT_MUTED).m(CssLength.px(0)))
        .select(
            ":scope .field-error",
            s ->
                s.hidden()
                    .fontSize(Tokens.FONT_SIZE_XS)
                    .fontWeight(500)
                    .textColor(Tokens.STATUS_DANGER_TEXT)
                    .itemsCenter()
                    .gap(Tokens.SPACE_1))
        .select(
            ":scope .field-explicit-error, :scope:has(:user-invalid) .field-error", s -> s.flex())
        .select(
            ":scope:has(:user-invalid:not([aria-invalid]), [aria-invalid=true]) .field-label",
            s -> s.textColor(Tokens.STATUS_DANGER_TEXT))
        .select(
            ":scope:has(:user-valid:not([aria-invalid])) .field-label",
            s -> s.textColor(Tokens.STATUS_SUCCESS_TEXT))
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
