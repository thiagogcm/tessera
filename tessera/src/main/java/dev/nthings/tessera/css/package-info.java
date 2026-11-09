/// Typed CSS authoring. Element builders and [StyleRefinement] implement [Styled], the one style
/// trait: its methods take typed [CssValue]s such as [CssLength], [CssColor], [Border] and
/// [TrackList], built with each type's static factories. The last call wins, and writing a
/// shorthand removes the longhands it covers. Reusable presets are `Consumer<StyleRefinement>`
/// functions applied with [Styled#apply].
///
/// Conditional styles go through [Styled#on(Condition, java.util.function.Consumer)] with a
/// [State], a [Media] feature or a [ContainerQuery] from a [Container] handle. Motion is a
/// [Transition] or an [Animation] of typed [Keyframes]; animations apply only without a
/// reduced-motion preference and register their keyframes themselves. [Tone] is the shared
/// semantic color.
///
/// Design tokens ([Tokens], [DesignToken]) are custom properties usable wherever a value of their
/// category is accepted; an immutable [Theme] sets their values with type-checked
/// `Theme.defaults().with(token, value)`. CSS limited to a view scope goes through [ScopeStyle] in
/// `View#configureScope`. Value factories that take constrained input validate it eagerly, throwing
/// IllegalArgumentException. [StyleSheet] is the compiler-facing stylesheet builder.
@NullMarked
package dev.nthings.tessera.css;

import org.jspecify.annotations.NullMarked;
