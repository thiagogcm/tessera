package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssDeclaration;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.StyleRefinement;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Identity-bearing HTML element unlocked via {@link TagElement#id(ElementId)}.
 *
 * <p>Provides compile-time access to Chrome 156+ Invoker Commands ({@code commandfor}/{@code
 * command}), Interest Invokers ({@code interestfor} + {@code <div popover="hint">}), {@code <dialog
 * closedby>}, CSS Anchor Positioning, and Cross-Document View Transitions while retaining all
 * {@link InteractiveElement} and {@link ParentElement} methods.
 */
public final class StatefulTagElement
    implements Element,
        InteractiveElement<StatefulTagElement>,
        ParentElement<StatefulTagElement>,
        FluentBuilder<StatefulTagElement> {

  private final ElementId elementId;
  private final TagElement delegate;

  public StatefulTagElement(ElementId elementId, TagElement delegate) {
    this.elementId = elementId;
    this.delegate = delegate;
  }

  @Override
  public StatefulTagElement self() {
    return this;
  }

  public ElementId elementId() {
    return elementId;
  }

  public TagElement delegate() {
    return delegate;
  }

  @Override
  public StyleRefinement style() {
    return delegate.style();
  }

  @Override
  public Interactivity interactivity() {
    return delegate.interactivity();
  }

  @Override
  public Map<String, String> attributes() {
    return delegate.attributes();
  }

  public List<Element> childrenList() {
    return delegate.childrenList();
  }

  @Override
  public void appendChild(Element child) {
    delegate.appendChild(child);
  }

  public StatefulTagElement command(StatefulTagElement target, BuiltinCommand command) {
    return command(target.elementId(), command);
  }

  public StatefulTagElement command(ElementId targetId, BuiltinCommand command) {
    interactivity().setCommandTarget(targetId, command.wireValue());
    return this;
  }

  public StatefulTagElement command(String targetName, BuiltinCommand command) {
    return command(ElementId.of(targetName), command);
  }

  public StatefulTagElement dispatchAction(StatefulTagElement target, Action action) {
    return dispatchAction(target.elementId(), action);
  }

  public StatefulTagElement dispatchAction(ElementId targetId, Action action) {
    interactivity().setCommandTarget(targetId, action.toCustomCommand());
    action.payloadAttributes().forEach(delegate::attr);
    return this;
  }

  public StatefulTagElement interestTooltip(UnaryOperator<TagElement> tooltipBuilder) {
    var tooltip =
        tooltipBuilder.apply(Elements.span().attr("popover", "hint").attr("role", "tooltip"));
    interactivity().setInterestTooltip(tooltip);
    return this;
  }

  public StatefulTagElement popover(PopoverMode mode) {
    delegate.attr("popover", mode.wireValue());
    return this;
  }

  public StatefulTagElement dialogClosedByAny() {
    delegate.attr("closedby", "any");
    return this;
  }

  public StatefulTagElement dialogClosedByCloseRequest() {
    delegate.attr("closedby", "closerequest");
    return this;
  }

  public StatefulTagElement dialogClosedByNone() {
    delegate.attr("closedby", "none");
    return this;
  }

  public StatefulTagElement anchorName() {
    style().put(new CssDeclaration.AnchorName("--anchor-" + elementId.slug()));
    return this;
  }

  public StatefulTagElement anchoredTo(
      StatefulTagElement anchor, String positionArea, String tryFallbacks) {
    style().put(new CssDeclaration.PositionAnchor("--anchor-" + anchor.elementId().slug()));
    style().put(new CssDeclaration.PositionArea(positionArea));
    style().put(new CssDeclaration.PositionTryFallbacks(tryFallbacks));
    return this;
  }

  public StatefulTagElement viewTransitionName(String transitionClass) {
    String safeTransition = Fnv1a64.sanitizeIdentifier(transitionClass);
    style().put(new CssDeclaration.ViewTransitionName(safeTransition + "-" + elementId.slug()));
    style().put(new CssDeclaration.ViewTransitionClass(safeTransition));
    return this;
  }
}
