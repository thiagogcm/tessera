package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.InteractiveState;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.List;
import java.util.function.UnaryOperator;

/// Shared interactive, attribute, id-reference, container-query, and scroll-driven animation
/// capabilities of element builders.
///
/// @param <SELF> concrete element type
public interface InteractiveElement<
        SELF extends InteractiveElement<SELF> & Styled<SELF> & FluentBuilder<SELF>>
    extends Styled<SELF>, Accessible<SELF>, FluentBuilder<SELF> {

  Interactivity interactivity();

  /// Sets an arbitrary attribute. Roles and `aria-*` attributes have typed methods ([Accessible]),
  /// so this refuses them.
  default SELF attr(String name, String value) {
    requireNotAria(name);
    attributes().put(name, value);
    return self();
  }

  default SELF boolAttr(String name, boolean enabled) {
    requireNotAria(name);
    if (enabled) {
      attributes().put(name, "");
    } else {
      attributes().remove(name);
    }
    return self();
  }

  /// Sets `attribute` to the DOM ids of `targets` once they are bound on the page.
  default SELF idRef(String attribute, ElementRef... targets) {
    return idRef(attribute, new IdReference("", List.of(targets)));
  }

  private SELF idRef(String attribute, IdReference reference) {
    requireNotAria(attribute);
    // Reserve the attribute's position so resolved ids keep declaration order.
    attributes().putIfAbsent(attribute, "");
    idReferences().put(attribute, reference);
    return self();
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

  default SELF onContainer(
      CssIdent containerName, CssLength minInlineSize, UnaryOperator<StyleRefinement> mutator) {
    interactivity().onContainerMinWidth(containerName, minInlineSize, mutator);
    return self();
  }

  default SELF scrollReveal(ScrollReveal preset) {
    return scrollReveal(preset, TimelineAxis.BLOCK);
  }

  default SELF scrollReveal(ScrollReveal preset, TimelineAxis axis) {
    return scrollDriven(preset, axis, false);
  }

  /// Plays `preset` once, the first time any part of the element scrolls into view (CSS
  /// scroll-triggered animation; elements already visible on load play immediately). Unlike
  /// [#scrollReveal(ScrollReveal)], progress is time-based and does not reverse on scroll back.
  default SELF revealOnce(ScrollReveal preset) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity().on(InteractiveState.MOTION_SAFE, preset::revealOnce);
    return self();
  }

  default SELF scrollExit(ScrollReveal preset) {
    return scrollExit(preset, TimelineAxis.BLOCK);
  }

  default SELF scrollExit(ScrollReveal preset, TimelineAxis axis) {
    return scrollDriven(preset, axis, true);
  }

  private SELF scrollDriven(ScrollReveal preset, TimelineAxis axis, boolean exit) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity().on(InteractiveState.MOTION_SAFE, s -> preset.scrollDriven(s, axis, exit));
    return self();
  }

  default SELF staggerBySiblingIndex(int stepMilliseconds) {
    interactivity()
        .on(InteractiveState.MOTION_SAFE, s -> s.animationStaggerBySiblingIndex(stepMilliseconds));
    return self();
  }

  private static void requireNotAria(String attribute) {
    if (attribute.equals("role") || attribute.startsWith("aria-")) {
      throw new IllegalArgumentException(
          "Use the typed Accessible method (role(Role), aria...(...)) instead of the attribute '"
              + attribute
              + "'");
    }
  }
}
