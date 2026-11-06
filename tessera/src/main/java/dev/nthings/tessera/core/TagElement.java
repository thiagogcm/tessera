package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// HTML element node backed by [HtmlTag], [StyleRefinement], and [Interactivity].
///
/// An element gets a scoped DOM id by binding an [ElementRef] ([#id(ElementRef)], [#id(String)]);
/// features that need the element's own id ([#interestTooltip], [#anchorName()]) bind an
/// anonymous ref automatically.
public final class TagElement
    implements Element,
        InteractiveElement<TagElement>,
        ParentElement<TagElement>,
        FluentBuilder<TagElement> {

  private final HtmlTag tag;
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

  public TagElement(HtmlTag tag) {
    this.tag = tag;
  }

  @Override
  public TagElement self() {
    return this;
  }

  public HtmlTag tag() {
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

  public List<Element> childrenList() {
    return children;
  }

  @Override
  public void appendChild(Element child) {
    if (tag.isVoid()) {
      throw new IllegalStateException(
          "HTML5 void element <" + tag.tagName() + "> cannot have children");
    }
    children.add(child);
  }

  /// Binds `elementRef` to this element; its DOM id is resolved at the bind site.
  public TagElement id(ElementRef elementRef) {
    if (ref != null && ref != elementRef) {
      throw new IllegalStateException("Element already bound to " + ref);
    }
    this.ref = elementRef;
    return this;
  }

  /// Binds a fresh [ElementRef#named(String)] ref.
  public TagElement id(String name) {
    return id(ElementRef.named(name));
  }

  /// This element's ref, binding an anonymous one if none was set.
  public ElementRef ref() {
    if (ref == null) {
      ref = ElementRef.anonymous();
    }
    return ref;
  }

  public @Nullable ElementRef boundRef() {
    return ref;
  }

  public TagElement command(TagElement target, BuiltinCommand command) {
    return command(target.ref(), command);
  }

  public TagElement dispatchAction(TagElement target, Action action) {
    return dispatchAction(target.ref(), action);
  }

  /// Interest Invoker tooltip emitted as the next sibling (`interestfor` + `popover="hint"`),
  /// positioned above the element with flip fallbacks unless `tooltipBuilder` overrides it.
  public TagElement interestTooltip(UnaryOperator<TagElement> tooltipBuilder) {
    ref();
    var base =
        Elements.span()
            .attr("popover", "hint")
            .role("tooltip")
            .positionArea("block-start")
            .positionTryFallbacks("flip-block, flip-inline")
            .m(CssLength.rem(0.25));
    this.interestTooltip = tooltipBuilder.apply(base);
    return this;
  }

  public @Nullable TagElement interestTooltip() {
    return interestTooltip;
  }

  /// Declares `anchor-name` derived from this element's DOM id.
  public TagElement anchorName() {
    ref();
    this.anchorSelf = true;
    return this;
  }

  public boolean isAnchor() {
    return anchorSelf;
  }

  /// Positions this element against `anchor` (which must be laid out earlier on the page).
  public TagElement anchoredTo(ElementRef anchor, String positionArea, String tryFallbacks) {
    this.anchorTarget = anchor;
    style.put(CssProperty.POSITION_AREA, positionArea);
    style.put(CssProperty.POSITION_TRY_FALLBACKS, tryFallbacks);
    return this;
  }

  public @Nullable ElementRef anchorTarget() {
    return anchorTarget;
  }

  @Override
  public TagElement transitionKey(TransitionKey key) {
    this.transitionKey = key;
    return this;
  }

  public @Nullable TransitionKey transitionKey() {
    return transitionKey;
  }
}
