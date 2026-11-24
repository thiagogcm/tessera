package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// A [State] of an enclosing [Group], emitted as a nested `:where(.g-name:pseudo-class) &` rule.
/// Get one from [Group#state(State)].
public final class GroupState implements Condition {

  private final Group group;
  private final State state;

  GroupState(Group group, State state) {
    this.group = group;
    this.state = state;
  }

  /// The state the group must be in, which sets this condition's place in emission order.
  State state() {
    return state;
  }

  /// The prelude that opens the nested rule, such as `:where(.g-row:hover:not(:where(:disabled)))
  /// &`.
  String prelude() {
    return ":where(." + group.className() + state.pseudoClasses() + ") &";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof GroupState groupState
        && groupState.group.equals(group)
        && groupState.state == state;
  }

  @Override
  public int hashCode() {
    return 31 * group.hashCode() + state.hashCode();
  }

  @Override
  public String toString() {
    return prelude();
  }
}
