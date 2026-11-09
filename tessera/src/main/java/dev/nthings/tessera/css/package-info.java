/// Typed CSS authoring. Element builders and [StyleRefinement] implement [Styled], the one style
/// trait: its methods take typed [CssValue]s such as [CssLength], [CssColor], [Border] and
/// [TrackList], built with each type's static factories. The last call wins, and writing a
/// shorthand removes the longhands it covers. Reusable presets are `Consumer<StyleRefinement>`
/// functions applied with [Styled#apply]. Design tokens ([Tokens], [DesignToken]) are custom
/// properties usable wherever a value of their category is accepted; override one with
/// `tokens.register(Tokens.X.withValue(...))`. CSS limited to a view scope goes through
/// [ScopeStyle] in `View#configureScope`. Value factories that take constrained input validate it
/// eagerly, throwing IllegalArgumentException. [StyleSheet] is the compiler-facing stylesheet
/// builder.
@NullMarked
package dev.nthings.tessera.css;

import org.jspecify.annotations.NullMarked;
