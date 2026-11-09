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
import java.util.function.Consumer;

/// Shared interactive, attribute, id-reference, container-query and scroll-animation capabilities
/// of element builders. [TagElement] implements it.
///
/// @param <SELF> concrete element type
public interface InteractiveElement<
        SELF extends InteractiveElement<SELF> & Styled<SELF> & FluentBuilder<SELF>>
    extends Styled<SELF>, Accessible<SELF>, FluentBuilder<SELF> {

  Interactivity interactivity();

  /// Sets an arbitrary attribute. `id` ([TagElement#id(ElementRef)]), `role` and `aria-*`
  /// ([Accessible]) have typed methods and are refused here with IllegalArgumentException.
  default SELF attr(String name, String value) {
    Attributes.requireGeneric(name);
    attributes().put(name, value);
    return self();
  }

  /// Sets a boolean attribute: present with an empty value when `enabled`, removed otherwise.
  /// Refuses `id`, `role` and `aria-*` as [#attr(String, String)] does.
  default SELF boolAttr(String name, boolean enabled) {
    Attributes.requireGeneric(name);
    if (enabled) {
      attributes().put(name, "");
    } else {
      attributes().remove(name);
    }
    return self();
  }

  /// Sets `attribute` to the DOM ids of `targets` once they are bound on the page. Throws
  /// IllegalArgumentException if no target is given, or for `id`, `role` and `aria-*`.
  default SELF idRef(String attribute, ElementRef... targets) {
    return idRef(attribute, new IdReference("", List.of(targets)));
  }

  private SELF idRef(String attribute, IdReference reference) {
    Attributes.requireGeneric(attribute);
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

  /// Sets `commandfor` and `command`, so activating this element runs `command` on the element
  /// bound to `target`, plus any payload attributes of a custom command. `show-modal`, `close` and
  /// `request-close` need a `<dialog>` target, and `toggle-popover`, `show-popover` and
  /// `hide-popover` need a target with a `popover` attribute; otherwise the compiler reports
  /// `incompatible-command-target`. Custom commands accept any target. Use `element.ref()` to
  /// target an element you hold.
  default SELF command(ElementRef target, Command command) {
    idRef("commandfor", target).attr("command", command.wireValue());
    command.payload().forEach(this::attr);
    return self();
  }

  /// Sets the `popover` attribute to `mode`.
  default SELF popover(PopoverMode mode) {
    return attr("popover", mode.wireValue());
  }

  /// Sets `closedby` on a `<dialog>`, choosing which user actions close it. See [ClosedBy].
  default SELF closedBy(ClosedBy closedBy) {
    return attr("closedby", closedBy.wireValue());
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

  /// Applies `styles` on hover, while the element is not disabled.
  default SELF hover(Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.HOVER, styles);
    return self();
  }

  /// Applies `styles` under `:focus-visible`, for keyboard focus.
  default SELF focusVisible(Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.FOCUS_VISIBLE, styles);
    return self();
  }

  /// Applies `styles` while the element is pressed, and not disabled.
  default SELF active(Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.ACTIVE, styles);
    return self();
  }

  /// Styles applied only under `prefers-reduced-motion: no-preference`.
  default SELF motionSafe(Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.MOTION_SAFE, styles);
    return self();
  }

  /// Applies `styles` while the element is `:checked`.
  default SELF checked(Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.CHECKED, styles);
    return self();
  }

  /// Applies `styles` when the user has changed a control to an invalid value
  /// (`:user-invalid`).
  default SELF userInvalid(Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.USER_INVALID, styles);
    return self();
  }

  /// Applies `styles` when the user has changed a control to a valid value
  /// (`:user-valid`).
  default SELF userValid(Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.USER_VALID, styles);
    return self();
  }

  /// Marks this element as the hover group `groupName` for [#groupHover]. The name is sanitized:
  /// characters outside `[A-Za-z0-9_-]` become `-`. Throws IllegalArgumentException if it is empty.
  default SELF group(String groupName) {
    return attr("data-group", Fnv1a64.sanitizeIdentifier(groupName));
  }

  /// Applies `styles` while an ancestor marked with [#group(String)] for `groupName` is
  /// hovered. The ancestor is required. The name is sanitized as in [#group(String)], and an empty
  /// name throws IllegalArgumentException.
  default SELF groupHover(String groupName, Consumer<StyleRefinement> styles) {
    interactivity().on(InteractiveState.GROUP_HOVER, Fnv1a64.sanitizeIdentifier(groupName), styles);
    return self();
  }

  /// Applies `styles` when the nearest ancestor container named `containerName` is at
  /// least `minInlineSize` wide. Emits `@container <name> (inline-size >= <min>)`. The ancestor
  /// must be declared with `containerInlineSize(CssIdent)`.
  default SELF onContainer(
      CssIdent containerName, CssLength minInlineSize, Consumer<StyleRefinement> styles) {
    interactivity().onContainerMinWidth(containerName, minInlineSize, styles);
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
    interactivity().scrollReveal(preset, axis);
    return self();
  }

  /// Plays `preset` once, over 0.5s, the first time the element scrolls into view (CSS
  /// scroll-triggered; elements visible on load play immediately). Unlike
  /// [#scrollReveal(ScrollReveal)], it is time-based and does not reverse. Applies only under
  /// `prefers-reduced-motion: no-preference`.
  default SELF revealOnce(ScrollReveal preset) {
    interactivity().revealOnce(preset);
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
    interactivity().scrollExit(preset, axis);
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
}
