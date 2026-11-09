package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;

import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.WhiteSpace;
import java.util.Locale;

/// Shared face of [Button] and [IconButton]; classes never alter type or invoker attributes.
final class ButtonFace {

  private ButtonFace() {}

  /// Emits rules for the given face only: icon faces get square boxes, text faces get padding.
  static void configure(ScopeStyle scope, boolean icon) {
    scope
        .root(
            s ->
                s.inlineFlex()
                    .itemsCenter()
                    .justifyCenter()
                    .gap(Tokens.SPACE_2)
                    .typography(Roles.LABEL)
                    .cursor(Cursor.POINTER)
                    .rounded(Tokens.RADIUS_MD)
                    .border(Border.hairline(CssColor.transparent()))
                    .textDecorationNone()
                    .whiteSpace(WhiteSpace.NOWRAP)
                    .on(State.FOCUS_VISIBLE, f -> f.focusRing(Tokens.ACCENT_PRIMARY)))
        .select(":scope[aria-busy=true]", s -> s.cursor(Cursor.PROGRESS))
        .select(":scope:disabled", s -> s.opacity(0.55).cursor(Cursor.NOT_ALLOWED));

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

    for (var size : Size.values()) {
      scope.select(
          ":scope.button--" + css(size),
          s -> {
            if (icon) {
              iconBox(s, size);
            } else {
              paddedBox(s, size);
            }
          });
    }

    for (var tone : Tone.values()) {
      for (var variant : Button.Variant.values()) {
        var selector = ":scope.button--" + css(tone) + ".button--" + css(variant);
        scope.select(
            selector, s -> paint(s, tone, variant).on(State.HOVER, h -> hover(h, tone, variant)));
      }
    }

    scope.on(
        Media.MOTION_SAFE,
        m -> m.select(":scope", s -> s.transition(Transition.controlFeedback())));
  }

  /// Square box for icon buttons; the icon box grows with the size.
  private static StyleRefinement iconBox(StyleRefinement style, Size size) {
    var box =
        switch (size) {
          case SM -> Tokens.SPACE_8;
          case MD -> Tokens.SPACE_10;
          case LG -> Tokens.SPACE_12;
        };
    return style.inlineSize(box).blockSize(box).p(CssLength.px(0));
  }

  /// Text buttons: padding follows density; type size does not.
  private static StyleRefinement paddedBox(StyleRefinement style, Size size) {
    return switch (size) {
      case SM -> style.fontSize(Tokens.FONT_SIZE_XS).px(Tokens.SPACE_3).py(Tokens.SPACE_1);
      case MD ->
          style
              .fontSize(Tokens.CONTROL_FONT_SIZE)
              .px(Tokens.CONTROL_PADDING_INLINE)
              .py(Tokens.SPACE_2);
      case LG -> style.fontSize(Tokens.FONT_SIZE_MD).px(Tokens.SPACE_5).py(Tokens.SPACE_3);
    };
  }

  static TagElement element(ButtonBase<?> model, boolean icon) {
    var el =
        button()
            .attr("class", classes(model, icon))
            .attr("type", model.submit ? "submit" : "button")
            .boolAttr("disabled", model.disabled);
    if (model.busy) {
      el.ariaBusy(true);
    }
    model.domRefs.forEach(el::id);
    var target = model.commandTarget;
    var command = model.command;
    if (target != null && command != null) {
      el.command(target, command);
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

  private static StyleRefinement paint(StyleRefinement style, Tone tone, Button.Variant variant) {
    var ink = ink(tone);
    return switch (variant) {
      case SOLID -> style.bg(ink).textColor(onSolid(tone));
      case OUTLINE ->
          style
              .bg(CssColor.transparent())
              .textColor(ink)
              .border(Border.hairline(tone == Tone.NEUTRAL ? Tokens.BORDER_SUBTLE : ink));
      case GHOST -> style.bg(CssColor.transparent()).textColor(ink);
    };
  }

  private static StyleRefinement hover(StyleRefinement style, Tone tone, Button.Variant variant) {
    return switch (variant) {
      case SOLID -> style.opacity(0.92);
      case OUTLINE, GHOST ->
          style.bg(tone == Tone.NEUTRAL ? Tokens.SURFACE_SUBTLE : ink(tone).mixTransparent(12));
    };
  }

  /// The tone's fill and text color; neutral buttons use primary text rather than muted.
  private static DesignToken.Color ink(Tone tone) {
    return tone == Tone.NEUTRAL ? Tokens.TEXT_PRIMARY : tone.color();
  }

  private static CssColor onSolid(Tone tone) {
    return switch (tone) {
      case NEUTRAL -> Tokens.SURFACE_CARD;
      case WARNING -> CssColor.oklch(15, 0.02, 75);
      case ACCENT, INFO, SUCCESS, DANGER -> CssColor.white();
    };
  }

  private static String css(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
