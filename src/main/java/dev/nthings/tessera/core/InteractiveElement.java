package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.InteractiveState;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.css.TimelineAxis;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/// Shared interactive, attribute, id-reference, container-query, and scroll-driven animation
/// capabilities of element builders.
///
/// @param <SELF> concrete element type
public interface InteractiveElement<
        SELF extends InteractiveElement<SELF> & Styled<SELF> & FluentBuilder<SELF>>
    extends Styled<SELF>, FluentBuilder<SELF> {

  Interactivity interactivity();

  Map<String, String> attributes();

  /// Attributes whose values are DOM ids of [ElementRef] targets, resolved by the compiler.
  Map<String, IdReference> idReferences();

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

  /// Sets `attribute` to the DOM ids of `targets` once they are bound on the page.
  default SELF idRef(String attribute, ElementRef... targets) {
    return idRef(attribute, new IdReference("", List.of(targets)));
  }

  private SELF idRef(String attribute, IdReference reference) {
    // Reserve the attribute's position so resolved ids keep declaration order.
    attributes().putIfAbsent(attribute, "");
    idReferences().put(attribute, reference);
    return self();
  }

  default SELF ariaLabelledBy(ElementRef... labels) {
    return idRef("aria-labelledby", labels);
  }

  default SELF ariaDescribedBy(ElementRef... descriptions) {
    return idRef("aria-describedby", descriptions);
  }

  default SELF ariaErrorMessage(ElementRef message) {
    return idRef("aria-errormessage", message);
  }

  default SELF labelFor(ElementRef control) {
    return idRef("for", control);
  }

  default SELF list(ElementRef datalist) {
    return idRef("list", datalist);
  }

  /// Same-page fragment link: `href="#<target dom id>"`.
  default SELF href(ElementRef target) {
    return idRef("href", new IdReference("#", List.of(target)));
  }

  /// Chrome 156+ Invoker Command: `commandfor` + `command`.
  default SELF command(ElementRef target, BuiltinCommand command) {
    return idRef("commandfor", target).attr("command", command.wireValue());
  }

  /// Custom or native Invoker Command: `commandfor` + `command`.
  default SELF command(ElementRef target, String customCommand) {
    return idRef("commandfor", target).attr("command", customCommand);
  }

  /// Custom `--namespace-name` Invoker Command plus the action's payload attributes.
  default SELF dispatchAction(ElementRef target, Action action) {
    idRef("commandfor", target).attr("command", action.toCustomCommand());
    action.payloadAttributes().forEach(this::attr);
    return self();
  }

  default SELF popover(PopoverMode mode) {
    return attr("popover", mode.wireValue());
  }

  default SELF dialogClosedByAny() {
    return attr("closedby", "any");
  }

  default SELF dialogClosedByCloseRequest() {
    return attr("closedby", "closerequest");
  }

  default SELF dialogClosedByNone() {
    return attr("closedby", "none");
  }

  default SELF dimensions(int width, int height) {
    return attr("width", Integer.toString(width)).attr("height", Integer.toString(height));
  }

  /// Cross-document view transition identity: emits `view-transition-name` (and class), checked
  /// to be unique per page and present on at least two routes of a site.
  SELF transitionKey(TransitionKey key);

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
    interactivity().on(InteractiveState.HOVER, mutator);
    return self();
  }

  default SELF focusVisible(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.FOCUS_VISIBLE, mutator);
    return self();
  }

  default SELF active(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.ACTIVE, mutator);
    return self();
  }

  /// Styles applied only under `prefers-reduced-motion: no-preference`.
  default SELF motionSafe(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.MOTION_SAFE, mutator);
    return self();
  }

  default SELF checked(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.CHECKED, mutator);
    return self();
  }

  default SELF userInvalid(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.USER_INVALID, mutator);
    return self();
  }

  default SELF userValid(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.USER_VALID, mutator);
    return self();
  }

  default SELF group(String groupName) {
    return attr("data-group", Fnv1a64.sanitizeIdentifier(groupName));
  }

  default SELF groupHover(String groupName, UnaryOperator<StyleRefinement> mutator) {
    interactivity()
        .on(InteractiveState.GROUP_HOVER, Fnv1a64.sanitizeIdentifier(groupName), mutator);
    return self();
  }

  default SELF whenHas(String relativeSelector, UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.HAS, relativeSelector, mutator);
    return self();
  }

  default SELF container(String containerName) {
    var safeName = Fnv1a64.sanitizeIdentifier(containerName);
    style().put(CssProperty.CONTAINER_NAME, safeName);
    style().put(CssProperty.CONTAINER_TYPE, "inline-size");
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
    return scrollDriven(preset, axis, "backwards", "entry");
  }

  /// Plays `preset` once, the first time any part of the element scrolls into view (CSS
  /// scroll-triggered animation; elements already visible on load play immediately). Unlike
  /// [#scrollReveal(ScrollReveal)], progress is time-based and does not reverse on scroll back.
  default SELF revealOnce(ScrollReveal preset) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity()
        .on(
            InteractiveState.MOTION_SAFE,
            s ->
                s.put(CssProperty.ANIMATION, preset.keyframeName() + " 0.5s ease-out both")
                    .put(
                        CssProperty.TIMELINE_TRIGGER, "--tessera-reveal view() cover 0% cover 100%")
                    .put(CssProperty.ANIMATION_TRIGGER, "--tessera-reveal play-once"));
    return self();
  }

  default SELF scrollExit(ScrollReveal preset) {
    return scrollExit(preset, TimelineAxis.BLOCK);
  }

  default SELF scrollExit(ScrollReveal preset, TimelineAxis axis) {
    return scrollDriven(preset, axis, "forwards", "exit");
  }

  private SELF scrollDriven(ScrollReveal preset, TimelineAxis axis, String fill, String phase) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity()
        .on(
            InteractiveState.MOTION_SAFE,
            s ->
                s.put(CssProperty.ANIMATION, preset.keyframeName() + " auto linear " + fill)
                    .put(CssProperty.ANIMATION_TIMELINE, "view(" + axis.css() + ")")
                    .put(CssProperty.ANIMATION_RANGE, phase + " 0% " + phase + " 100%"));
    return self();
  }

  default SELF viewTimeline(String name, TimelineAxis axis) {
    var ident = "--" + Fnv1a64.sanitizeIdentifier(name);
    style().put(CssProperty.VIEW_TIMELINE, ident + " " + axis.css());
    return self();
  }

  default SELF staggerBySiblingIndex(int stepMilliseconds) {
    if (stepMilliseconds < 0) {
      throw new IllegalArgumentException("stepMilliseconds must be >= 0");
    }
    interactivity()
        .on(
            InteractiveState.MOTION_SAFE,
            s ->
                s.put(
                    CssProperty.ANIMATION_DELAY,
                    "calc((sibling-index() - 1) * " + stepMilliseconds + "ms)"));
    return self();
  }
}
