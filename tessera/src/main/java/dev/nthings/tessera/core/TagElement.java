package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// HTML element node backed by a [TagName] and a [StyleRefinement]. Authors usually create one
/// through the [Elements] factories.
///
/// Give the element an id with [#id(ElementRef)]. Features that need the element's own id
/// ([#ref()], [#interestTooltip], [#anchorName()]) bind an anonymous ref when none is bound; a
/// named ref bound before or after them gives the element its id, and every ref names the same
/// element. Two named refs asking for different ids report `conflicting-element-id`.
public final class TagElement
    implements Element,
        InteractiveElement<TagElement>,
        ParentElement<TagElement>,
        FluentBuilder<TagElement> {

  private final TagName tag;
  private final StyleRefinement style = new StyleRefinement();
  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final Map<String, IdReference> idReferences = new LinkedHashMap<>();
  private final List<Element> children = new ArrayList<>();
  private final List<ElementRef> refs = new ArrayList<>(1);
  private @Nullable TagElement interestTooltip;
  private boolean anchorSelf;
  private @Nullable ElementRef anchorTarget;
  private @Nullable TransitionKey transitionKey;
  private final @Nullable SourceLocation source;

  /// Creates an element for `tag`. Authors normally use the [Elements] factories.
  public TagElement(TagName tag) {
    this(tag, SourceProvenance.capture());
  }

  private TagElement(TagName tag, @Nullable SourceLocation source) {
    this.tag = tag;
    this.source = source;
  }

  /// Copies this builder's configuration; child elements and reference identities are shared.
  public TagElement copy() {
    var copy = new TagElement(tag, source);
    copy.style.mergeFrom(style);
    copy.attributes.putAll(attributes);
    copy.idReferences.putAll(idReferences);
    copy.children.addAll(children);
    copy.refs.addAll(refs);
    copy.interestTooltip = interestTooltip;
    copy.anchorSelf = anchorSelf;
    copy.anchorTarget = anchorTarget;
    copy.transitionKey = transitionKey;
    return copy;
  }

  @Override
  public TagElement self() {
    return this;
  }

  /// Compiler-facing: the authoring call site, present only while source tracking is active.
  public @Nullable SourceLocation source() {
    return source;
  }

  /// Compiler-facing: the emitted tag name.
  public TagName tag() {
    return tag;
  }

  @Override
  public StyleRefinement style() {
    return style;
  }

  @Override
  public Map<String, String> attributes() {
    return attributes;
  }

  @Override
  public Map<String, IdReference> idReferences() {
    return idReferences;
  }

  /// Compiler-facing: the child elements in the order they were added.
  public List<Element> childrenList() {
    return children;
  }

  /// Appends `child`. Throws IllegalStateException if this is a void element, which cannot have
  /// children.
  @Override
  public void appendChild(Element child) {
    if (tag.isVoid()) {
      throw new IllegalStateException(
          "HTML5 void element <" + tag.tagName() + "> cannot have children");
    }
    children.add(child);
  }

  /// Binds `elementRef` to this element, so its DOM id is resolved on the page and other elements
  /// can reference it. An element may carry several refs: they all resolve to one DOM id, taken
  /// from the named ref ([ElementRef#scoped] or [ElementRef#page]) if there is one, otherwise
  /// generated as `<tag>-<n>`. Two named refs asking for different ids report
  /// `conflicting-element-id`. Binding the same ref twice on one element has no further effect.
  public TagElement id(ElementRef elementRef) {
    for (var bound : refs) {
      if (bound == elementRef) {
        return this;
      }
    }
    refs.add(elementRef);
    return this;
  }

  /// Returns a ref to this element: the first one bound, or a new anonymous ref bound now. Pass it
  /// to `command`, `labelFor`, `aria*` or `href` on other elements.
  public ElementRef ref() {
    if (refs.isEmpty()) {
      refs.add(ElementRef.anonymous());
    }
    return refs.getFirst();
  }

  /// Compiler-facing: the refs bound to this element, in binding order.
  public List<ElementRef> boundRefs() {
    return Collections.unmodifiableList(refs);
  }

  /// Adds a hint tooltip that this element's `interestfor` points at, shown on hover or focus. The
  /// tooltip is emitted right after this element and positioned above it, with flip fallbacks.
  /// `tooltipBuilder` receives the default tooltip, a `<span>` with `popover="hint"` and
  /// `role="tooltip"`, and returns the one to emit. Its DOM id is this element's id followed by
  /// `--tooltip`. Binds an anonymous ref when this element has none.
  public TagElement interestTooltip(UnaryOperator<TagElement> tooltipBuilder) {
    ref();
    var base =
        Elements.span()
            .attr("popover", "hint")
            .role(Role.TOOLTIP)
            .positionArea(PositionArea.of(PositionArea.Part.BLOCK_START))
            .positionTryFallbacks(
                PositionTry.flip(PositionTry.Flip.BLOCK), PositionTry.flip(PositionTry.Flip.INLINE))
            .m(CssLength.rem(0.25));
    this.interestTooltip = tooltipBuilder.apply(base);
    return this;
  }

  /// Compiler-facing: the tooltip element, or `null` if none was added.
  public @Nullable TagElement interestTooltip() {
    return interestTooltip;
  }

  /// Declares an anchor named after this element's DOM id, so [#anchoredTo] elements can position
  /// against it. Binds an anonymous ref when this element has none.
  public TagElement anchorName() {
    ref();
    this.anchorSelf = true;
    return this;
  }

  /// Compiler-facing: whether [#anchorName()] was called.
  public boolean isAnchor() {
    return anchorSelf;
  }

  /// Positions this element against `anchor`, with `area` and the given fallbacks. The anchor must
  /// call [#anchorName()] and be laid out earlier on the page. Otherwise the position names
  /// nothing, and no error is reported. An unbound `anchor` is reported as `unbound-reference`.
  public TagElement anchoredTo(
      ElementRef anchor, PositionArea area, PositionTry firstFallback, PositionTry... fallbacks) {
    this.anchorTarget = anchor;
    return positionArea(area).positionTryFallbacks(firstFallback, fallbacks);
  }

  /// Compiler-facing: the element this one is positioned against, or `null`.
  public @Nullable ElementRef anchorTarget() {
    return anchorTarget;
  }

  /// Sets the cross-document transition key, which emits `view-transition-name`. See
  /// [TransitionKey].
  @Override
  public TagElement transitionKey(TransitionKey key) {
    this.transitionKey = key;
    return this;
  }

  /// Compiler-facing: the transition key, or `null` if none was set.
  public @Nullable TransitionKey transitionKey() {
    return transitionKey;
  }
}
