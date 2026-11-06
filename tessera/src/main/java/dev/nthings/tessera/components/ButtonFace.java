package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;

import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Tokens;
import java.util.Locale;

/// Shared face of [Button] and [IconButton]. Appearance classes never change the button type or
/// invoker attributes.
final class ButtonFace {

  private ButtonFace() {}

  /// Emits only the rules the given face can use: the icon-only face has square boxes and a
  /// decorative icon slot, the text face has padded boxes.
  static void configure(ScopeStyle scope, boolean icon) {
    scope
        .root(
            s ->
                s.inlineFlex()
                    .itemsCenter()
                    .justifyCenter()
                    .gap(Tokens.SPACE_2.length())
                    .typography(Roles.LABEL)
                    .cursorPointer()
                    .rounded(Tokens.RADIUS_MD.length())
                    .border("1px solid transparent")
                    .textDecorationNone()
                    .whiteSpace("nowrap"))
        .select(":scope:focus-visible", s -> s.focusRing(Tokens.ACCENT_PRIMARY))
        .select(":scope[aria-busy=true]", s -> s.cursor("progress"))
        .select(":scope:disabled", s -> s.opacity(0.55).cursor("not-allowed"));

    if (icon) {
      scope.select(
          ":scope > .button-icon",
          s ->
              s.inlineFlex()
                  .itemsCenter()
                  .justifyCenter()
                  .inlineSize(CssLength.em(1.25))
                  .blockSize(CssLength.em(1.25))
                  .flexShrink(0));
    }

    for (var size : Button.Size.values()) {
      scope.select(
          ":scope.button--" + css(size), s -> icon ? iconBox(s, size) : paddedBox(s, size));
    }

    for (var tone : Button.Tone.values()) {
      for (var variant : Button.Variant.values()) {
        var selector = ":scope.button--" + css(tone) + ".button--" + css(variant);
        scope
            .select(selector, s -> paint(s, tone, variant))
            .select(selector + ":hover:not(:disabled)", s -> hover(s, tone, variant));
      }
    }

    scope.motionSafe(m -> m.select(":scope", s -> s.motion(Motion.controlFeedback())));
  }

  private static StyleRefinement iconBox(StyleRefinement style, Button.Size size) {
    return style
        .inlineSize(size.iconBox().length())
        .blockSize(size.iconBox().length())
        .p(CssLength.px(0));
  }

  private static StyleRefinement paddedBox(StyleRefinement style, Button.Size size) {
    return style
        .fontSize(size.fontSize().length())
        .px(size.paddingInline().length())
        .py(size.paddingBlock().length());
  }

  static TagElement element(ButtonBase<?> model, boolean icon) {
    var el =
        button()
            .attr("class", classes(model, icon))
            .attr("type", model.submit ? "submit" : "button")
            .boolAttr("disabled", model.disabled);
    if (model.busy) {
      el.attr("aria-busy", "true");
    }
    var domRef = model.domRef;
    if (domRef != null) {
      el.id(domRef);
    }
    var target = model.commandTarget;
    var command = model.command;
    var action = model.action;
    if (target != null && command != null) {
      el.command(target, command);
    } else if (target != null && action != null) {
      el.dispatchAction(target, action);
    }
    var tooltip = model.tooltip;
    if (tooltip != null) {
      el.interestTooltip(tooltip);
    }
    return el;
  }

  private static String classes(ButtonBase<?> model, boolean icon) {
    var classes =
        "button button--"
            + css(model.tone)
            + " button--"
            + css(model.variant)
            + " button--"
            + css(model.size);
    return icon ? classes + " button--icon" : classes;
  }

  private static StyleRefinement paint(
      StyleRefinement style, Button.Tone tone, Button.Variant variant) {
    return switch (variant) {
      case SOLID ->
          style.bg(tone.token()).textColorRaw(tone.onSolid()).border("1px solid transparent");
      case OUTLINE -> style.bgRaw("transparent").textColor(tone.token()).borderSubtle(tone.edge());
      case GHOST ->
          style.bgRaw("transparent").textColor(tone.token()).border("1px solid transparent");
    };
  }

  private static StyleRefinement hover(
      StyleRefinement style, Button.Tone tone, Button.Variant variant) {
    return switch (variant) {
      case SOLID -> style.opacity(0.92);
      case OUTLINE, GHOST -> style.bgRaw(tone.wash());
    };
  }

  private static String css(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
