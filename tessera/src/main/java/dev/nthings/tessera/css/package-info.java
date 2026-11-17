/// Typed CSS authoring. Element builders and [Declarations] implement [Styled], the one style
/// trait: its methods take typed [CssValue]s such as [CssLength], [CssColor], [Border] and
/// [TrackList], built with each type's static factories. The last call wins, and writing a
/// shorthand removes the longhands it covers. Style callbacks and reusable presets are
/// [StyleBlock]s, applied to any target with [Styled#styles(StyleBlock)]; [ScopeBlock] groups scope
/// rules.
///
/// Conditional styles go through [Styled#on(Condition, StyleBlock)] with a [State], a [Media]
/// feature or a [ContainerQuery] from a [Container] handle. Motion is a [Transition] or an
/// [Animation] of typed [Keyframes]; animations apply only without a reduced-motion preference and
/// register their keyframes themselves. [Tone] is the shared semantic color.
///
/// Design tokens ([Tokens], [DesignToken]) are custom properties usable wherever a value of their
/// category is accepted; an immutable [Theme] sets their values with type-checked
/// `Theme.defaults().with(token, value)`. CSS limited to an id scope goes through [ScopeStyle] in
/// `Scope#configureScope`. Value factories that take constrained input validate it eagerly and
/// throw IllegalArgumentException. [StyleSheet] is the compiler-facing stylesheet builder.
@NullMarked
package dev.nthings.tessera.css;

import org.jspecify.annotations.NullMarked;
