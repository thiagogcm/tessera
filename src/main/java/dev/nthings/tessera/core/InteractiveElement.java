package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssDeclaration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.css.TimelineAxis;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Shared interactive, attribute, container-query, and scroll-driven animation capabilities
 * implemented by both {@link TagElement} and {@link StatefulTagElement}.
 *
 * @param <SELF> concrete element type
 */
public interface InteractiveElement<
        SELF extends InteractiveElement<SELF> & Styled<SELF> & FluentBuilder<SELF>>
    extends Styled<SELF>, FluentBuilder<SELF> {

  Interactivity interactivity();

  Map<String, String> attributes();

  default SELF attr(String name, String value) {
    attributes().put(name, value);
    return self();
  }

  default SELF boolAttr(String name, boolean enabled) {
    if (enabled) {
      attributes().put(name, "");
    } else {
      attributes().remove(name);
    }
    return self();
  }

  default SELF ariaLabel(String label) {
    return attr("aria-label", label);
  }

  default SELF ariaLabelledBy(String elementId) {
    return attr("aria-labelledby", elementId);
  }

  default SELF ariaHidden(boolean hidden) {
    return attr("aria-hidden", Boolean.toString(hidden));
  }

  default SELF role(String role) {
    return attr("role", role);
  }

  default SELF title(String title) {
    return attr("title", title);
  }

  default SELF tabIndex(int index) {
    return attr("tabindex", Integer.toString(index));
  }

  default SELF hover(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onHover(mutator);
    return self();
  }

  default SELF focusVisible(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onFocusVisible(mutator);
    return self();
  }

  default SELF active(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onActive(mutator);
    return self();
  }

  default SELF userInvalid(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onUserInvalid(mutator);
    return self();
  }

  default SELF userValid(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onUserValid(mutator);
    return self();
  }

  default SELF group(String groupName) {
    return attr("data-group", Fnv1a64.sanitizeIdentifier(groupName));
  }

  default SELF groupHover(String groupName, UnaryOperator<StyleRefinement> mutator) {
    interactivity().onGroupHover(Fnv1a64.sanitizeIdentifier(groupName), mutator);
    return self();
  }

  default SELF whenHas(String relativeSelector, UnaryOperator<StyleRefinement> mutator) {
    interactivity().onHas(relativeSelector, mutator);
    return self();
  }

  default SELF container(String containerName) {
    String safeName = Fnv1a64.sanitizeIdentifier(containerName);
    style().put(new CssDeclaration.ContainerName(safeName));
    style().put(new CssDeclaration.ContainerType(CssDeclaration.ContainerKind.INLINE_SIZE));
    return self();
  }

  default SELF onContainer(
      String containerName, CssLength minInlineSize, UnaryOperator<StyleRefinement> mutator) {
    interactivity()
        .onContainerMinWidth(Fnv1a64.sanitizeIdentifier(containerName), minInlineSize, mutator);
    return self();
  }

  default SELF onContainerStyle(
      String containerName,
      String customProperty,
      String expectedValue,
      UnaryOperator<StyleRefinement> mutator) {
    interactivity()
        .onContainerStyleQuery(
            Fnv1a64.sanitizeIdentifier(containerName), customProperty, expectedValue, mutator);
    return self();
  }

  default SELF scrollReveal(ScrollReveal preset) {
    return scrollReveal(preset, TimelineAxis.BLOCK);
  }

  default SELF scrollReveal(ScrollReveal preset, TimelineAxis axis) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity()
        .onMotionSafe(
            s ->
                s.put(
                        new CssDeclaration.Animation(
                            preset.keyframeName() + " auto linear backwards"))
                    .put(new CssDeclaration.AnimationTimeline("view(" + axis.css() + ")"))
                    .put(new CssDeclaration.AnimationRange("entry 0% entry 100%")));
    return self();
  }

  default SELF scrollExit(ScrollReveal preset) {
    return scrollExit(preset, TimelineAxis.BLOCK);
  }

  default SELF scrollExit(ScrollReveal preset, TimelineAxis axis) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity()
        .onMotionSafe(
            s ->
                s.put(new CssDeclaration.Animation(preset.keyframeName() + " auto linear forwards"))
                    .put(new CssDeclaration.AnimationTimeline("view(" + axis.css() + ")"))
                    .put(new CssDeclaration.AnimationRange("exit 0% exit 100%")));
    return self();
  }

  default SELF viewTimeline(String name, TimelineAxis axis) {
    String ident = "--" + Fnv1a64.sanitizeIdentifier(name);
    style().put(new CssDeclaration.ViewTimeline(ident + " " + axis.css()));
    return self();
  }

  default SELF staggerBySiblingIndex(int stepMilliseconds) {
    if (stepMilliseconds < 0) {
      throw new IllegalArgumentException("stepMilliseconds must be >= 0");
    }
    interactivity()
        .onMotionSafe(
            s ->
                s.put(
                    new CssDeclaration.AnimationDelay(
                        "calc((sibling-index() - 1) * " + stepMilliseconds + "ms)")));
    return self();
  }
}
