package dev.nthings.tessera.css;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// The conditions in force inside a rule: those of every conditional rule it nests in. A condition
/// already in force adds nothing when nested again, so [#enter(Condition, Object, Function)] keeps
/// such styles in the current rule: `on(State.HOVER, s -> s.on(State.HOVER, ...))` emits one
/// `&:hover` rule, and a motion-safe transition inside `on(Media.MOTION_SAFE, ...)` one `@media`
/// block. [Declarations] and [ScopeStyle] both nest through it.
final class Enclosing {

  /// No condition: the conditions in force at a top-level rule.
  static final Enclosing NONE = new Enclosing(null, null);

  private final @Nullable Condition condition;
  private final @Nullable Enclosing outer;

  private Enclosing(@Nullable Condition condition, @Nullable Enclosing outer) {
    this.condition = condition;
    this.outer = outer;
  }

  /// Where styles under `condition` go, inside `current`, a rule with these conditions in force:
  /// `current` itself when `condition` is already in force, or else the rule `nested` finds or
  /// creates for it, given the conditions in force inside that rule.
  <T> T enter(Condition condition, T current, Function<Enclosing, T> nested) {
    for (var enclosing = this; enclosing != null; enclosing = enclosing.outer) {
      if (condition.equals(enclosing.condition)) {
        return current;
      }
    }
    return nested.apply(new Enclosing(condition, this));
  }
}
