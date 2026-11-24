package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.Nullable;

/// A named group whose state its descendants style themselves by: mark an element with
/// `TagElement.group(Group)`, then give a descendant styles for one of its states, such as
/// `on(row.state(State.HOVER), s -> s.opacity(1))`, so hovering anywhere on the row reveals the
/// descendant. The conditions are plain CSS, so they behave the same in static pages and live
/// roots.
///
/// A descendant matches while any enclosing element marked with the group is in the state, not only
/// the nearest one, so give nested groups distinct names. Marking an element adds the class
/// `g-<name>` and no specificity: an element's own [State] styles override its group styles.
@CheckReturnValue
public final class Group {

  private final CssIdent ident;

  private Group(CssIdent ident) {
    this.ident = ident;
  }

  /// The group `name`. Throws IllegalArgumentException if `name` is not a CSS identifier, starts
  /// with `--`, or is `none`, `default` or a CSS-wide keyword.
  public static Group named(String name) {
    return new Group(CssIdent.of(name));
  }

  /// Matches while an enclosing element marked with this group is in `state`, such as
  /// `:where(.g-row:hover:not(:where(:disabled))) &` for [State#HOVER].
  public GroupState state(State state) {
    return new GroupState(this, state);
  }

  /// The class that marks an element as part of this group, such as `g-row`.
  String className() {
    return "g-" + ident.toCss();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Group group && group.ident.equals(ident);
  }

  @Override
  public int hashCode() {
    return ident.hashCode();
  }

  @Override
  public String toString() {
    return className();
  }
}
