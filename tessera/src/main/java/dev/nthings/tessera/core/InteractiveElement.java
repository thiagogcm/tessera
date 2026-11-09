package dev.nthings.tessera.core;

import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.List;

/// Shared attribute, id-reference, command and accessibility capabilities of element builders, on
/// top of [Styled] (including conditional styles via `on(...)`). [TagElement] implements it.
///
/// @param <SELF> concrete element type
public interface InteractiveElement<
        SELF extends InteractiveElement<SELF> & Styled<SELF> & FluentBuilder<SELF>>
    extends Styled<SELF>, Accessible<SELF>, FluentBuilder<SELF> {

  /// Sets an arbitrary attribute. `id` ([TagElement#id(ElementRef)]), `role` and `aria-*`
  /// ([Accessible]) have typed methods and are refused here with IllegalArgumentException.
  default SELF attr(String name, String value) {
    Attributes.requireGeneric(name);
    attributes().put(name, value);
    return self();
  }

  /// Sets a boolean attribute such as `disabled` or `open`: present with an empty value. Use
  /// `onlyIf(condition, el -> el.boolAttr(name))` to set it conditionally. Refuses `id`, `role` and
  /// `aria-*` as [#attr(String, String)] does.
  default SELF boolAttr(String name) {
    Attributes.requireGeneric(name);
    attributes().put(name, "");
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
}
