package dev.nthings.tessera.css;

import java.util.function.UnaryOperator;

/// Composable fluent styling: layout, box, text, effects, scrolling, and anchoring.
///
/// Each concern is its own trait; methods take the value type their property accepts
/// ([CssLength], [CssColor], [Border], ...).
///
/// @param <SELF> concrete element or style builder type
public interface Styled<SELF extends Styled<SELF>>
    extends LayoutStyled<SELF>,
        BoxStyled<SELF>,
        TextStyled<SELF>,
        EffectStyled<SELF>,
        ScrollStyled<SELF>,
        AnchorStyled<SELF> {

  default SELF refine(UnaryOperator<StyleRefinement> mutator) {
    mutator.apply(style());
    return self();
  }

  /// Keeps content in the accessibility tree while removing it from the visual layout.
  default SELF visuallyHidden() {
    return absolute()
        .inlineSize(CssLength.px(1))
        .blockSize(CssLength.px(1))
        .p(CssLength.zero())
        .m(CssLength.px(-1))
        .overflowHidden()
        .whiteSpace(WhiteSpace.NOWRAP)
        .borderNone();
  }
}
