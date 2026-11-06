package dev.nthings.tessera.css;

import java.util.function.UnaryOperator;

/// Composable fluent styling: layout, box, text, effects, scrolling, and anchoring.
///
/// Each concern is its own trait; this interface composes them. Every method takes values of the
/// type its property accepts ([CssLength], [CssColor], [Border], ...).
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
}
