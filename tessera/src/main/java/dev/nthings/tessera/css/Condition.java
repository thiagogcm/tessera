package dev.nthings.tessera.css;

/// When conditional styles apply, passed to [Styled#on(Condition, StyleBlock)]: an element [State]
/// such as hover, a [Media] feature such as reduced motion, or a [ContainerQuery] from a
/// [Container]. Conditions nest: styles written inside one `on` callback can call `on` again, and
/// apply only while every enclosing condition holds.
public sealed interface Condition permits State, Condition.Query {

  /// A condition evaluated outside the element, as an `@media` or `@container` rule. Only these can
  /// group whole selector rules, with `ScopeStyle.on(Query, ScopeBlock)`.
  sealed interface Query extends Condition permits Media, ContainerQuery {}
}
