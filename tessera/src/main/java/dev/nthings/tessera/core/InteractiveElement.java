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

/// Shared interactive, attribute, id-reference, container-query and scroll-animation capabilities
/// of element builders. [TagElement] implements it.
///
/// @param <SELF> concrete element type
public interface InteractiveElement<
        SELF extends InteractiveElement<SELF> & Styled<SELF> & FluentBuilder<SELF>>
    extends Styled<SELF>, Accessible<SELF>, FluentBuilder<SELF> {

  Interactivity interactivity();

  /// Sets an arbitrary attribute. Roles and `aria-*` have typed methods ([Accessible]) and are
  /// refused here.
  default SELF attr(String name, String value) {
    requireNotAria(name);
    attributes().put(name, value);
    return self();
  }

  /// Sets a boolean attribute: present with an empty value when `enabled`, removed otherwise.
  default SELF boolAttr(String name, boolean enabled) {
    requireNotAria(name);
    if (enabled) {
      attributes().put(name, "");
    } else {
      attributes().remove(name);
    }
    return self();
  }

  /// Sets `attribute` to the DOM ids of `targets` once they are bound on the page. Throws
  /// IllegalArgumentException if no target is given.
  default SELF idRef(String attribute, ElementRef... targets) {
    return idRef(attribute, new IdReference("", List.of(targets)));
  }

  private SELF idRef(String attribute, IdReference reference) {
    requireNotAria(attribute);
    // Reserve the slot so resolved ids keep declaration order.
    attributes().putIfAbsent(attribute, "");
    idReferences().put(attribute, reference);
    return self();
  }

  /// Sets `for` to the DOM id of `control`, on a `<label>`.
  default SELF labelFor(ElementRef control) {
    return idRef("for", control);
  }

  /// Sets `list` to the DOM id of a `<datalist>` that offers suggestions for this input.
  default SELF list(ElementRef datalist) {
    return idRef("list", datalist);
  }

  /// Same-page fragment link: `href="#<target dom id>"`.
  default SELF href(ElementRef target) {
    return idRef("href", new IdReference("#", List.of(target)));
  }

  /// Sets `commandfor` and `command`, so activating this element runs `command` on `target`.
  /// `show-modal`, `close` and `request-close` need a `<dialog>` target, and `toggle-popover`,
  /// `show-popover` and `hide-popover` need a target with a `popover` attribute. Otherwise the
  /// compiler reports `incompatible-command-target`. See [BuiltinCommand].
  default SELF command(ElementRef target, BuiltinCommand command) {
    return idRef("commandfor", target).attr("command", command.wireValue());
  }

  /// Sets the custom `--namespace-name` Invoker Command for `action`, plus its payload attributes.
  /// Custom commands are not checked against the target.
  default SELF dispatchAction(ElementRef target, Action action) {
    idRef("commandfor", target).attr("command", action.toCustomCommand());
    action.payloadAttributes().forEach(this::attr);
    return self();
  }

  /// Sets the `popover` attribute to `mode`.
  default SELF popover(PopoverMode mode) {
    return attr("popover", mode.wireValue());
  }

  /// Sets `closedby="any"`: clicking outside, Escape and close requests all close the dialog.
  default SELF dialogClosedByAny() {
    return attr("closedby", "any");
  }

  /// Sets `closedby="closerequest"`: only Escape and close requests close the dialog.
  default SELF dialogClosedByCloseRequest() {
    return attr("closedby", "closerequest");
  }

  /// Sets `closedby="none"`: only script or a command closes the dialog.
  default SELF dialogClosedByNone() {
    return attr("closedby", "none");
  }

  default SELF dimensions(int width, int height) {
    return attr("width", Integer.toString(width)).attr("height", Integer.toString(height));
  }

  /// Emits `view-transition-name` (and class) for a cross-document transition. Keys are checked
  /// per page (`duplicate-transition-key`) and across the site (`unshared-transition-key`). See
  /// [TransitionKey].
  SELF transitionKey(TransitionKey key);

  default SELF title(String title) {
    return attr("title", title);
  }

  /// Sets `tabindex`. A positive value is reported as `tabindex-positive`; use `0` or `-1`.
  default SELF tabIndex(int index) {
    return attr("tabindex", Integer.toString(index));
  }

  /// Applies `mutator`'s styles on hover, while the element is not disabled.
  default SELF hover(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.HOVER, mutator);
    return self();
  }

  /// Applies `mutator`'s styles under `:focus-visible`, for keyboard focus.
  default SELF focusVisible(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.FOCUS_VISIBLE, mutator);
    return self();
  }

  /// Applies `mutator`'s styles while the element is pressed, and not disabled.
  default SELF active(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.ACTIVE, mutator);
    return self();
  }

  /// Styles applied only under `prefers-reduced-motion: no-preference`.
  default SELF motionSafe(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.MOTION_SAFE, mutator);
    return self();
  }

  /// Applies `mutator`'s styles while the element is `:checked`.
  default SELF checked(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.CHECKED, mutator);
    return self();
  }

  /// Applies `mutator`'s styles when the user has changed a control to an invalid value
  /// (`:user-invalid`).
  default SELF userInvalid(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.USER_INVALID, mutator);
    return self();
  }

  /// Applies `mutator`'s styles when the user has changed a control to a valid value
  /// (`:user-valid`).
  default SELF userValid(UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.USER_VALID, mutator);
    return self();
  }

  /// Marks this element as the hover group `groupName` for [#groupHover]. The name is sanitized:
  /// characters outside `[A-Za-z0-9_-]` become `-`. Throws IllegalArgumentException if it is empty.
  default SELF group(String groupName) {
    return attr("data-group", Fnv1a64.sanitizeIdentifier(groupName));
  }

  /// Applies `mutator`'s styles while an ancestor marked with [#group(String)] for `groupName` is
  /// hovered. The ancestor is required. The name is sanitized as in [#group(String)], and an empty
  /// name throws IllegalArgumentException.
  default SELF groupHover(String groupName, UnaryOperator<StyleRefinement> mutator) {
    interactivity()
        .on(InteractiveState.GROUP_HOVER, Fnv1a64.sanitizeIdentifier(groupName), mutator);
    return self();
  }

  /// Applies `mutator`'s styles while `:has(relativeSelector)` matches this element, for example
  /// `"> img"`. The selector is inserted verbatim and not validated, so pass only trusted relative
  /// selectors.
  default SELF whenHas(String relativeSelector, UnaryOperator<StyleRefinement> mutator) {
    interactivity().on(InteractiveState.HAS, relativeSelector, mutator);
    return self();
  }

  /// Applies `mutator`'s styles when the nearest ancestor container named `containerName` is at
  /// least `minInlineSize` wide. Emits `@container <name> (inline-size >= <min>)`. The ancestor
  /// must be declared with `containerInlineSize(CssIdent)`.
  default SELF onContainer(
      CssIdent containerName, CssLength minInlineSize, UnaryOperator<StyleRefinement> mutator) {
    interactivity().onContainerMinWidth(containerName, minInlineSize, mutator);
    return self();
  }

  /// Plays `preset` as the element scrolls into view along the block axis, so scrolling back
  /// reverses it. Applies only under `prefers-reduced-motion: no-preference`.
  default SELF scrollReveal(ScrollReveal preset) {
    return scrollReveal(preset, TimelineAxis.BLOCK);
  }

  /// Plays `preset` as the element scrolls into view along `axis`, so scrolling back reverses it.
  /// Applies only under `prefers-reduced-motion: no-preference`.
  default SELF scrollReveal(ScrollReveal preset, TimelineAxis axis) {
    return scrollDriven(preset, axis, false);
  }

  /// Plays `preset` once, over 0.5s, the first time the element scrolls into view (CSS
  /// scroll-triggered; elements visible on load play immediately). Unlike
  /// [#scrollReveal(ScrollReveal)], it is time-based and does not reverse. Applies only under
  /// `prefers-reduced-motion: no-preference`.
  default SELF revealOnce(ScrollReveal preset) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity().on(InteractiveState.MOTION_SAFE, preset::revealOnce);
    return self();
  }

  /// Plays `preset` as the element scrolls out of view along the block axis, and keeps its end
  /// state. Applies only under `prefers-reduced-motion: no-preference`.
  default SELF scrollExit(ScrollReveal preset) {
    return scrollExit(preset, TimelineAxis.BLOCK);
  }

  /// Plays `preset` as the element scrolls out of view along `axis`, and keeps its end state.
  /// Applies only under `prefers-reduced-motion: no-preference`.
  default SELF scrollExit(ScrollReveal preset, TimelineAxis axis) {
    return scrollDriven(preset, axis, true);
  }

  private SELF scrollDriven(ScrollReveal preset, TimelineAxis axis, boolean exit) {
    interactivity().registerKeyframeRule(preset.keyframeRule());
    interactivity().on(InteractiveState.MOTION_SAFE, s -> preset.scrollDriven(s, axis, exit));
    return self();
  }

  /// Delays each sibling's animation by `stepMilliseconds` times its position among its siblings,
  /// so the first sibling has no delay. Pair it with a reveal preset. Applies only under
  /// `prefers-reduced-motion: no-preference`. Throws IllegalArgumentException if
  /// `stepMilliseconds` is negative.
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
