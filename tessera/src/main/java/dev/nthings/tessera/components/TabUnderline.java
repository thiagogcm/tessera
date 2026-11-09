package dev.nthings.tessera.components;

import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transition;

/// Underline shared by [NavTabs] and [ContentTabs], sliding to the active tab.
final class TabUnderline {

  private TabUnderline() {}

  /// Anchor-positioned underline sliding to the tab carrying `anchorIdent`.
  static ScopeStyle apply(ScopeStyle scope, String barSelector, Anchor anchorIdent) {
    return scope
        .select(
            barSelector,
            s ->
                s.content(CssContent.empty())
                    .absolute()
                    .positionAnchor(anchorIdent)
                    .insetBlockStart(
                        CssLength.anchor(CssLength.AnchorSide.BOTTOM).minus(CssLength.px(2)))
                    .insetInlineStart(CssLength.anchor(CssLength.AnchorSide.LEFT))
                    .insetInlineEnd(CssLength.anchor(CssLength.AnchorSide.RIGHT))
                    .blockSize(CssLength.px(2))
                    .bg(Tokens.ACCENT_PRIMARY))
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(
                    barSelector,
                    s ->
                        s.transition(
                            Transition.of(CssDuration.ms(200), Easing.ease(), Animatable.INSET))));
  }
}
