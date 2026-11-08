package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// HTML element node backed by a [TagName], [StyleRefinement], and [Interactivity]. Authors usually
/// create one through the [Elements] factories.
///
/// Features that need the element's own id ([#interestTooltip], [#anchorName()]) bind an anonymous
/// [ElementRef] automatically. Call [#id(ElementRef)] or [#id(String)] before them, because binding
/// a different ref afterwards throws IllegalStateException.
public final class TagElement
    implements Element,
        InteractiveElement<TagElement>,
        ParentElement<TagElement>,
        FluentBuilder<TagElement> {

  private final TagName tag;
  private final StyleRefinement style = new StyleRefinement();
  private final Interactivity interactivity = new Interactivity();
  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final Map<String, IdReference> idReferences = new LinkedHashMap<>();
  private final List<Element> children = new ArrayList<>();
  private @Nullable ElementRef ref;
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
    copy.interactivity.mergeFrom(interactivity);
    copy.attributes.putAll(attributes);
    copy.idReferences.putAll(idReferences);
    copy.children.addAll(children);
    copy.ref = ref;
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
  public Interactivity interactivity() {
    return interactivity;
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

  /// Binds `elementRef` to this element; its DOM id is resolved on the page. Throws
  /// IllegalStateException if this element already has a different ref. That includes an anonymous
  /// ref bound by [#ref()], [#anchorName()], [#interestTooltip(UnaryOperator)], or by this element
  /// being the target of `command` or `dispatchAction`. Call this first.
  public TagElement id(ElementRef elementRef) {
    if (ref != null && ref != elementRef) {
      throw new IllegalStateException("Element already bound to " + ref);
    }
    this.ref = elementRef;
    return this;
  }

  /// Binds a new [ElementRef#named(String)] for `name`. Inside a view the DOM id gets the view's
  /// prefix, so `id("foo")` in a view emits `<scope>--foo`, not `foo`. Link to it with an
  /// [ElementRef] rather than a hand-written `#foo`. Calling this twice on one element throws
  /// IllegalStateException, because each call makes a different ref.
  public TagElement id(String name) {
    return id(ElementRef.named(name));
  }

  /// Returns this element's ref, binding an anonymous one if none is set. Its DOM id is
  /// `<tag>-<n>`, numbered per view scope.
  public ElementRef ref() {
    if (ref == null) {
      ref = ElementRef.anonymous();
    }
    return ref;
  }

  /// Compiler-facing: the ref bound to this element, or `null` if none is bound.
  public @Nullable ElementRef boundRef() {
    return ref;
  }

  /// Binds `target`'s ref (anonymously if unset), then behaves as
  /// [InteractiveElement#command(ElementRef, BuiltinCommand)].
  public TagElement command(TagElement target, BuiltinCommand command) {
    return command(target.ref(), command);
  }

  /// Binds `target`'s ref (anonymously if unset), then behaves as
  /// [InteractiveElement#dispatchAction(ElementRef, Action)].
  public TagElement dispatchAction(TagElement target, Action action) {
    return dispatchAction(target.ref(), action);
  }

  /// Adds a hint tooltip that this element's `interestfor` points at, shown on hover or focus. The
  /// tooltip is emitted right after this element and positioned above it, with flip fallbacks.
  /// `tooltipBuilder` receives the default tooltip, a `<span>` with `popover="hint"` and
  /// `role="tooltip"`, and returns the one to emit. Binds this element's ref.
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
  /// against it. Binds this element's ref.
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
