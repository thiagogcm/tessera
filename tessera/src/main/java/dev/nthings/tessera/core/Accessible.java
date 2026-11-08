package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssLength;
import java.util.List;
import java.util.Map;

/// Typed WAI-ARIA authoring: role, accessible name and states on an element builder.
///
/// Each `aria-*` attribute has a method taking the type it accepts. The generic attribute method,
/// [InteractiveElement#attr], refuses `role` and `aria-*`, so use these methods for them. The
/// compiler checks the result against the [Role] requirements.
public interface Accessible<SELF extends Accessible<SELF>> {

  /// Returns this builder, typed as `SELF`.
  SELF self();

  /// Returns the live attribute map of the element that will be emitted. A wrapper builder must
  /// return the map of the element it delegates to, not a copy, so typed methods take effect.
  /// Entries are emitted as written, except that a bound [ElementRef] replaces `id` and generated
  /// style classes are appended to `class`. A literal `id` is not scoped to its view scope.
  Map<String, String> attributes();

  /// Attributes whose values are DOM ids of [ElementRef] targets, resolved by the compiler. Returns
  /// the live map, under the same delegation rule as [#attributes()].
  Map<String, IdReference> idReferences();

  /// Value of `aria-current`, marking the item that is current within a set.
  enum Current implements WireToken {
    PAGE,
    STEP,
    LOCATION,
    DATE,
    TIME,
    TRUE
  }

  /// The value of a tri-state `aria-checked`.
  enum Checked implements WireToken {
    TRUE,
    FALSE,
    MIXED
  }

  /// How assertively assistive technology announces changes (`aria-live`).
  enum Live implements WireToken {
    OFF,
    POLITE,
    ASSERTIVE
  }

  /// The kind of popup an element opens (`aria-haspopup`).
  enum Popup implements WireToken {
    MENU,
    LISTBOX,
    TREE,
    GRID,
    DIALOG
  }

  /// Sets `role`. The compiler checks the required name, children and attributes of the role.
  default SELF role(Role role) {
    attributes().put("role", role.wireValue());
    return self();
  }

  /// Sets `aria-label`, the accessible name as text.
  default SELF ariaLabel(String label) {
    return set("aria-label", label);
  }

  /// Sets `aria-labelledby` to the bound `labels`, which name the element. Throws
  /// IllegalArgumentException with no targets.
  default SELF ariaLabelledBy(ElementRef... labels) {
    return reference("aria-labelledby", labels);
  }

  /// Sets `aria-describedby` to the bound `descriptions`. Throws IllegalArgumentException with no
  /// targets.
  default SELF ariaDescribedBy(ElementRef... descriptions) {
    return reference("aria-describedby", descriptions);
  }

  default SELF ariaErrorMessage(ElementRef message) {
    return reference("aria-errormessage", message);
  }

  /// Sets `aria-controls` to the bound `controlled` elements, e.g. a tab's panel. Throws
  /// IllegalArgumentException with no targets.
  default SELF ariaControls(ElementRef... controlled) {
    return reference("aria-controls", controlled);
  }

  default SELF ariaHidden(boolean hidden) {
    return flag("aria-hidden", hidden);
  }

  default SELF ariaCurrent(Current current) {
    return set("aria-current", current.wireValue());
  }

  default SELF ariaExpanded(boolean expanded) {
    return flag("aria-expanded", expanded);
  }

  default SELF ariaChecked(Checked checked) {
    return set("aria-checked", checked.wireValue());
  }

  default SELF ariaSelected(boolean selected) {
    return flag("aria-selected", selected);
  }

  default SELF ariaPressed(boolean pressed) {
    return flag("aria-pressed", pressed);
  }

  default SELF ariaBusy(boolean busy) {
    return flag("aria-busy", busy);
  }

  default SELF ariaDisabled(boolean disabled) {
    return flag("aria-disabled", disabled);
  }

  default SELF ariaInvalid(boolean invalid) {
    return flag("aria-invalid", invalid);
  }

  default SELF ariaRequired(boolean required) {
    return flag("aria-required", required);
  }

  default SELF ariaModal(boolean modal) {
    return flag("aria-modal", modal);
  }

  default SELF ariaLive(Live live) {
    return set("aria-live", live.wireValue());
  }

  default SELF ariaHasPopup(Popup popup) {
    return set("aria-haspopup", popup.wireValue());
  }

  /// Sets `aria-level`, the level of a `role="heading"`. Throws IllegalArgumentException outside
  /// 1 through 6.
  default SELF ariaLevel(int level) {
    if (level < 1 || level > 6) {
      throw new IllegalArgumentException("aria-level must be 1 to 6: " + level);
    }
    return set("aria-level", Integer.toString(level));
  }

  /// Sets `aria-keyshortcuts`, the keys that activate the element, e.g. `Alt+K`.
  default SELF ariaKeyShortcuts(String shortcuts) {
    return set("aria-keyshortcuts", shortcuts);
  }

  default SELF ariaValueNow(double value) {
    return set("aria-valuenow", format(value));
  }

  default SELF ariaValueMin(double value) {
    return set("aria-valuemin", format(value));
  }

  default SELF ariaValueMax(double value) {
    return set("aria-valuemax", format(value));
  }

  private SELF set(String attribute, String value) {
    attributes().put(attribute, value);
    return self();
  }

  private SELF flag(String attribute, boolean value) {
    return set(attribute, Boolean.toString(value));
  }

  private SELF reference(String attribute, ElementRef... targets) {
    // Reserve the slot so resolved ids keep declaration order.
    attributes().putIfAbsent(attribute, "");
    idReferences().put(attribute, new IdReference("", List.of(targets)));
    return self();
  }

  private static String format(double value) {
    return CssLength.formatNumber(value);
  }
}
