package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.List;
import java.util.function.UnaryOperator;

/// Native `<button>`. Tone, surface, and size are independent of submission and invoker commands.
/// An ordinary button is `type="button"`; [Button#submit] opts into form submission. Navigation
/// stays on anchors.
public final class Button implements RenderView, FluentBuilder<Button> {

  /// Semantic color. It does not choose the surface or the button's behavior.
  public enum Tone {
    NEUTRAL(Tokens.TEXT_PRIMARY, Tokens.SURFACE_CARD.varReference()),
    ACCENT(Tokens.ACCENT_PRIMARY, "#ffffff"),
    SUCCESS(Tokens.STATUS_SUCCESS, "#ffffff"),
    WARNING(Tokens.STATUS_WARNING, "oklch(15% 0.02 75)"),
    DANGER(Tokens.STATUS_DANGER, "#ffffff");

    private final DesignToken.Color token;
    private final String onSolid;

    Tone(DesignToken.Color token, String onSolid) {
      this.token = token;
      this.onSolid = onSolid;
    }

    DesignToken.Color token() {
      return token;
    }

    String onSolid() {
      return onSolid;
    }
  }

  /// Surface treatment, independent of tone and behavior.
  public enum Variant {
    SOLID,
    OUTLINE,
    GHOST
  }

  /// Control size. Padding follows density. The type size does not.
  public enum Size {
    SM(Tokens.FONT_SIZE_XS, Tokens.SPACE_3, Tokens.SPACE_1, Tokens.SPACE_8),
    MD(Tokens.CONTROL_FONT_SIZE, Tokens.CONTROL_PADDING_INLINE, Tokens.SPACE_2, Tokens.SPACE_10),
    LG(Tokens.FONT_SIZE_MD, Tokens.SPACE_5, Tokens.SPACE_3, Tokens.SPACE_12);

    private final DesignToken.Length fontSize;
    private final DesignToken.Length paddingInline;
    private final DesignToken.Length paddingBlock;
    private final DesignToken.Length iconBox;

    Size(
        DesignToken.Length fontSize,
        DesignToken.Length paddingInline,
        DesignToken.Length paddingBlock,
        DesignToken.Length iconBox) {
      this.fontSize = fontSize;
      this.paddingInline = paddingInline;
      this.paddingBlock = paddingBlock;
      this.iconBox = iconBox;
    }

    DesignToken.Length fontSize() {
      return fontSize;
    }

    DesignToken.Length paddingInline() {
      return paddingInline;
    }

    DesignToken.Length paddingBlock() {
      return paddingBlock;
    }

    DesignToken.Length iconBox() {
      return iconBox;
    }
  }

  private final String name;
  private final List<IntoElement> content;
  private final ButtonModel model = new ButtonModel(Tone.ACCENT, Variant.SOLID);

  private Button(String name, List<IntoElement> content) {
    this.name = name;
    this.content = content;
  }

  /// Button whose visible text is its accessible name.
  public static Button of(String label) {
    return new Button(label, List.of());
  }

  /// Button with custom content. `accessibleName` stays the accessible name.
  public static Button of(String accessibleName, IntoElement... content) {
    return new Button(accessibleName, List.of(content));
  }

  @Override
  public Button self() {
    return this;
  }

  public Button tone(Tone tone) {
    model.tone = tone;
    return this;
  }

  public Button variant(Variant variant) {
    model.variant = variant;
    return this;
  }

  public Button size(Size size) {
    model.size = size;
    return this;
  }

  /// Participates in native form submission (`type="submit"`).
  public Button submit() {
    model.submit = true;
    return this;
  }

  public Button command(ElementRef target, BuiltinCommand command) {
    model.commandTarget = target;
    model.command = command;
    model.action = null;
    return this;
  }

  public Button dispatchAction(ElementRef target, Action action) {
    model.commandTarget = target;
    model.command = null;
    model.action = action;
    return this;
  }

  public Button id(ElementRef ref) {
    model.domRef = ref;
    return this;
  }

  public Button disabled(boolean disabled) {
    model.disabled = disabled;
    return this;
  }

  public Button disabled() {
    return disabled(true);
  }

  public Button busy(boolean busy) {
    model.busy = busy;
    return this;
  }

  public Button busy() {
    return busy(true);
  }

  /// Hint popover shown on hover or focus (`interestfor`).
  public Button interestTooltip(UnaryOperator<TagElement> tooltip) {
    model.tooltip = tooltip;
    return this;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    ButtonFace.configure(scope);
  }

  @Override
  public Element render() {
    var el = ButtonFace.element(model, false);
    if (content.isEmpty()) {
      el.text(name);
    } else {
      el.ariaLabel(name);
      content.forEach(el::child);
    }
    return el;
  }
}
