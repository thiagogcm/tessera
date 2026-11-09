/// Typed CSS authoring: element builders and [StyleRefinement] implement [Styled], whose traits
/// ([LayoutStyled], [BoxStyled], [TextStyled], [EffectStyled], [ScrollStyled], [AnchorStyled])
/// take typed [CssValue]s such as [CssLength], [CssColor], [Border] and [TrackList]. Design tokens
/// ([Tokens], [DesignToken]) are custom properties usable wherever a value of their category is
/// accepted; override one with `tokens.register(Tokens.X.withValue(...))`. CSS limited to a view
/// scope goes through [ScopeStyle] in `View#configureScope`, and value factories that take
/// constrained input validate it eagerly, throwing IllegalArgumentException.
@NullMarked
package dev.nthings.tessera.css;

import org.jspecify.annotations.NullMarked;
