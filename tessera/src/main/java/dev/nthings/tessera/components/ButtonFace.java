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

  static void configure(ScopeStyle scope) {
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
        .select(":scope:disabled", s -> s.opacity(0.55).cursor("not-allowed"))
        .select(
            ":scope > .button-icon",
            s ->
                s.inlineFlex()
                    .itemsCenter()
                    .justifyCenter()
                    .inlineSize(CssLength.em(1.25))
                    .blockSize(CssLength.em(1.25))
                    .flexShrink(0));

    for (var size : Button.Size.values()) {
      var name = css(size);
      scope
          .select(
              ":scope.button--" + name + ":not(.button--icon)",
              s ->
                  s.fontSize(size.fontSize().length())
                      .px(size.paddingInline().length())
                      .py(size.paddingBlock().length()))
          .select(
              ":scope.button--icon.button--" + name,
              s ->
                  s.inlineSize(size.iconBox().length())
                      .blockSize(size.iconBox().length())
                      .p(CssLength.px(0)));
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

  static TagElement element(ButtonModel model, boolean icon) {
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
    return el;
  }

  private static String classes(ButtonModel model, boolean icon) {
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
      case OUTLINE ->
          style.bgRaw("transparent").textColor(tone.token()).border("1px solid " + edge(tone));
      case GHOST ->
          style.bgRaw("transparent").textColor(tone.token()).border("1px solid transparent");
    };
  }

  private static StyleRefinement hover(
      StyleRefinement style, Button.Tone tone, Button.Variant variant) {
    return switch (variant) {
      case SOLID -> style.opacity(0.92);
      case OUTLINE, GHOST ->
          style.bgRaw(
              tone == Button.Tone.NEUTRAL
                  ? Tokens.SURFACE_SUBTLE.varReference()
                  : tone.token().mixTransparent(12));
    };
  }

  private static String edge(Button.Tone tone) {
    return (tone == Button.Tone.NEUTRAL ? Tokens.BORDER_SUBTLE : tone.token()).varReference();
  }

  private static String css(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
