package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/// A query container: declare it on an element with [Styled#container(Container)], then style
/// descendants with its queries, such as `on(cards.atLeast(CssLength.rem(36)), ...)`.
///
/// A [#named] container is matched by name, so its queries skip nearer containers. An [#unnamed]
/// container only sets `container-type`, and its queries match the nearest ancestor container of
/// the right type. Size queries ([#atLeast], [#below]) need a [Type#INLINE_SIZE] or [Type#SIZE]
/// container; scroll-state queries ([#stuck], [#snapped]) need [Type#SCROLL_STATE].
@CheckReturnValue
public final class Container {

  /// A `container-type` value.
  public enum Type implements CssKeyword {
    /// Queries the inline size; the element no longer sizes its inline axis to its content.
    INLINE_SIZE,
    /// Queries both axes; the element no longer sizes to its content.
    SIZE,
    /// Queries scroll state: whether a sticky element is stuck or a snap target is snapped.
    SCROLL_STATE
  }

  /// An edge a sticky container can be stuck to, for [#stuck(Edge)].
  public enum Edge implements CssKeyword {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT,
    BLOCK_START,
    BLOCK_END,
    INLINE_START,
    INLINE_END
  }

  /// A scroll axis a snap target can be snapped on, for [#snapped(Axis)].
  public enum Axis implements CssKeyword {
    X,
    Y,
    BLOCK,
    INLINE,
    BOTH
  }

  private final @Nullable CssIdent ident;
  private final Set<Type> types;

  private Container(@Nullable CssIdent ident, Type first, Type... rest) {
    var all = EnumSet.of(first, rest);
    if (all.contains(Type.SIZE) && all.contains(Type.INLINE_SIZE)) {
      throw new IllegalArgumentException("A container is SIZE or INLINE_SIZE, not both");
    }
    this.ident = ident;
    this.types = all;
  }

  /// A container named `name`, of the given types. Throws IllegalArgumentException if `name` is not
  /// a CSS identifier, starts with `--`, or is `none`, `default` or a CSS-wide keyword, and if both
  /// `SIZE` and `INLINE_SIZE` are given.
  public static Container named(String name, Type first, Type... rest) {
    return new Container(CssIdent.of(name), first, rest);
  }

  /// An unnamed container of the given types. Its queries match the nearest ancestor container of
  /// the right type, whichever element declared it. Throws IllegalArgumentException if both `SIZE`
  /// and `INLINE_SIZE` are given.
  public static Container unnamed(Type first, Type... rest) {
    return new Container(null, first, rest);
  }

  /// Matches while the container's inline size is at least `minInlineSize`: `@container [name]
  /// (inline-size >= min)`. Paired with [#below(CssLength)] at the same length, the two ranges do
  /// not overlap.
  public ContainerQuery atLeast(CssLength minInlineSize) {
    return new ContainerQuery(this, "(inline-size >= " + minInlineSize.toCss() + ")");
  }

  /// Matches while the container's inline size is less than `maxInlineSize`: `@container [name]
  /// (inline-size < max)`.
  public ContainerQuery below(CssLength maxInlineSize) {
    return new ContainerQuery(this, "(inline-size < " + maxInlineSize.toCss() + ")");
  }

  /// Matches while the sticky container is stuck to `edge`: `scroll-state(stuck: edge)`.
  public ContainerQuery stuck(Edge edge) {
    return new ContainerQuery(this, "scroll-state(stuck: " + edge.toCss() + ")");
  }

  /// Matches while the container is a snap target snapped on `axis`: `scroll-state(snapped: axis)`.
  public ContainerQuery snapped(Axis axis) {
    return new ContainerQuery(this, "scroll-state(snapped: " + axis.toCss() + ")");
  }

  /// The container name, or null for an unnamed container.
  @Nullable CssIdent ident() {
    return ident;
  }

  /// The `container-type` value, such as `inline-size scroll-state`, in [Type] order.
  String typeList() {
    return types.stream().map(Type::toCss).collect(Collectors.joining(" "));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Container container
        && Objects.equals(container.ident, ident)
        && container.types.equals(types);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ident, types);
  }

  @Override
  public String toString() {
    return (ident == null ? "<unnamed>" : ident.toCss()) + " " + typeList();
  }
}
