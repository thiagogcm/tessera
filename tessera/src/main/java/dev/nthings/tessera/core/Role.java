package dev.nthings.tessera.core;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/// WAI-ARIA roles an element can declare. The compiler checks each declared role against its
/// requirements: accessible name, owned descendants, required states and supported `aria-*`.
public enum Role {
  // Landmarks
  BANNER(Naming.OPTIONAL),
  COMPLEMENTARY(Naming.OPTIONAL),
  CONTENTINFO(Naming.OPTIONAL),
  FORM(Naming.REQUIRED),
  MAIN(Naming.OPTIONAL),
  NAVIGATION(Naming.OPTIONAL),
  REGION(Naming.REQUIRED),
  SEARCH(Naming.OPTIONAL),

  // Widgets
  BUTTON(Naming.REQUIRED, true),
  CHECKBOX(Naming.REQUIRED, true),
  COMBOBOX(Naming.REQUIRED),
  LINK(Naming.REQUIRED, true),
  LISTBOX(Naming.REQUIRED),
  MENU(Naming.OPTIONAL),
  MENUBAR(Naming.OPTIONAL),
  MENUITEM(Naming.REQUIRED, true),
  MENUITEMCHECKBOX(Naming.REQUIRED, true),
  MENUITEMRADIO(Naming.REQUIRED, true),
  OPTION(Naming.REQUIRED, true),
  PROGRESSBAR(Naming.REQUIRED),
  RADIO(Naming.REQUIRED, true),
  SEARCHBOX(Naming.REQUIRED),
  SLIDER(Naming.REQUIRED),
  SPINBUTTON(Naming.REQUIRED),
  SWITCH(Naming.REQUIRED, true),
  TAB(Naming.REQUIRED, true),
  TABLIST(Naming.OPTIONAL),
  TABPANEL(Naming.OPTIONAL),
  TEXTBOX(Naming.REQUIRED),
  TREE(Naming.REQUIRED),
  TREEITEM(Naming.REQUIRED, true),

  // Document structure
  ARTICLE(Naming.OPTIONAL),
  GROUP(Naming.OPTIONAL),
  HEADING(Naming.REQUIRED, true),
  IMG(Naming.REQUIRED),
  LIST(Naming.OPTIONAL),
  LISTITEM(Naming.OPTIONAL),
  NONE(Naming.OPTIONAL),
  NOTE(Naming.OPTIONAL),
  PRESENTATION(Naming.OPTIONAL),
  RADIOGROUP(Naming.OPTIONAL),
  SEPARATOR(Naming.OPTIONAL),
  TABLE(Naming.OPTIONAL),
  TOOLTIP(Naming.OPTIONAL, true),

  // Live regions
  ALERT(Naming.OPTIONAL),
  STATUS(Naming.OPTIONAL),

  // Windows
  ALERTDIALOG(Naming.REQUIRED),
  DIALOG(Naming.REQUIRED),

  // Digital Publishing roles
  DOC_BACKLINK(Naming.REQUIRED, true),
  DOC_ENDNOTES(Naming.OPTIONAL),
  DOC_FOOTNOTE(Naming.OPTIONAL),
  DOC_NOTEREF(Naming.REQUIRED, true);

  /// Whether the element must be given an accessible name.
  public enum Naming {
    /// A name is optional.
    OPTIONAL,
    /// Required, from a label or, for some roles, from content.
    REQUIRED
  }

  private static final Set<String> GLOBAL_ARIA =
      Set.of(
          "aria-atomic",
          "aria-busy",
          "aria-controls",
          "aria-current",
          "aria-describedby",
          "aria-details",
          "aria-disabled",
          "aria-errormessage",
          "aria-flowto",
          "aria-haspopup",
          "aria-hidden",
          "aria-invalid",
          "aria-keyshortcuts",
          "aria-label",
          "aria-labelledby",
          "aria-live",
          "aria-owns",
          "aria-relevant",
          "aria-roledescription");

  /// Built once after every constant exists.
  private static final class Supported {
    static final List<Set<String>> BY_ORDINAL =
        Stream.of(values())
            .map(
                role -> {
                  var supported = new HashSet<>(GLOBAL_ARIA);
                  supported.addAll(role.specificAria());
                  return Set.copyOf(supported);
                })
            .toList();
  }

  private final String wireValue = name().toLowerCase(Locale.ROOT).replace('_', '-');
  private final Naming naming;
  private final boolean namedFromContent;

  Role(Naming naming) {
    this(naming, false);
  }

  Role(Naming naming, boolean namedFromContent) {
    this.naming = naming;
    this.namedFromContent = namedFromContent;
  }

  public String wireValue() {
    return wireValue;
  }

  public Naming naming() {
    return naming;
  }

  /// Whether visible content can serve as the accessible name.
  public boolean namedFromContent() {
    return namedFromContent;
  }

  /// Roles that an element with this role must own as a descendant; empty if none.
  public Set<Role> requiredChildren() {
    return switch (this) {
      case LIST -> EnumSet.of(LISTITEM);
      case TABLIST -> EnumSet.of(TAB);
      case LISTBOX -> EnumSet.of(OPTION);
      case MENU, MENUBAR -> EnumSet.of(MENUITEM, MENUITEMCHECKBOX, MENUITEMRADIO);
      case RADIOGROUP -> EnumSet.of(RADIO);
      case TREE -> EnumSet.of(TREEITEM);
      default -> Set.of();
    };
  }

  /// State attributes a custom element with this role must declare. Native elements that expose
  /// the state, such as `<input type="checkbox">`, are exempt.
  public Set<String> requiredAria() {
    return switch (this) {
      case CHECKBOX, SWITCH, RADIO, MENUITEMCHECKBOX, MENUITEMRADIO -> Set.of("aria-checked");
      case HEADING -> Set.of("aria-level");
      case SLIDER, SPINBUTTON -> Set.of("aria-valuenow");
      case TAB, OPTION -> Set.of("aria-selected");
      default -> Set.of();
    };
  }

  /// Supported `aria-*` attributes: the global set plus role-specific ones.
  public Set<String> supportedAria() {
    return Supported.BY_ORDINAL.get(ordinal());
  }

  private Set<String> specificAria() {
    return switch (this) {
      case CHECKBOX, SWITCH -> Set.of("aria-checked", "aria-readonly", "aria-required");
      case MENUITEMCHECKBOX, MENUITEMRADIO ->
          Set.of("aria-checked", "aria-expanded", "aria-posinset", "aria-setsize");
      case RADIO -> Set.of("aria-checked", "aria-posinset", "aria-setsize");
      case BUTTON -> Set.of("aria-expanded", "aria-pressed");
      case LINK, DOC_BACKLINK, DOC_NOTEREF -> Set.of("aria-expanded");
      case COMBOBOX ->
          Set.of(
              "aria-activedescendant",
              "aria-autocomplete",
              "aria-expanded",
              "aria-readonly",
              "aria-required");
      case LISTBOX ->
          Set.of(
              "aria-activedescendant",
              "aria-expanded",
              "aria-multiselectable",
              "aria-orientation",
              "aria-readonly",
              "aria-required");
      case OPTION -> Set.of("aria-checked", "aria-posinset", "aria-selected", "aria-setsize");
      case TAB -> Set.of("aria-expanded", "aria-posinset", "aria-selected", "aria-setsize");
      case TABLIST ->
          Set.of("aria-activedescendant", "aria-multiselectable", "aria-orientation", "aria-level");
      case MENU, MENUBAR -> Set.of("aria-activedescendant", "aria-expanded", "aria-orientation");
      case MENUITEM -> Set.of("aria-expanded", "aria-posinset", "aria-setsize");
      case SLIDER ->
          Set.of(
              "aria-orientation",
              "aria-readonly",
              "aria-valuemax",
              "aria-valuemin",
              "aria-valuenow",
              "aria-valuetext");
      case SPINBUTTON ->
          Set.of(
              "aria-activedescendant",
              "aria-readonly",
              "aria-required",
              "aria-valuemax",
              "aria-valuemin",
              "aria-valuenow",
              "aria-valuetext");
      case PROGRESSBAR ->
          Set.of("aria-valuemax", "aria-valuemin", "aria-valuenow", "aria-valuetext");
      case TEXTBOX, SEARCHBOX ->
          Set.of(
              "aria-activedescendant",
              "aria-autocomplete",
              "aria-multiline",
              "aria-placeholder",
              "aria-readonly",
              "aria-required");
      case HEADING -> Set.of("aria-level");
      case DIALOG, ALERTDIALOG -> Set.of("aria-modal");
      case TREE ->
          Set.of(
              "aria-activedescendant", "aria-multiselectable", "aria-orientation", "aria-required");
      case TREEITEM ->
          Set.of(
              "aria-checked",
              "aria-expanded",
              "aria-level",
              "aria-posinset",
              "aria-selected",
              "aria-setsize");
      case LISTITEM -> Set.of("aria-level", "aria-posinset", "aria-setsize");
      case ARTICLE -> Set.of("aria-posinset", "aria-setsize");
      case GROUP, RADIOGROUP -> Set.of("aria-activedescendant", "aria-readonly", "aria-required");
      case SEPARATOR ->
          Set.of(
              "aria-orientation",
              "aria-valuemax",
              "aria-valuemin",
              "aria-valuenow",
              "aria-valuetext");
      case TABLE -> Set.of("aria-colcount", "aria-rowcount");
      default -> Set.of();
    };
  }

  /// Role for a `role` attribute value, or `null` if not modeled.
  public static @Nullable Role fromWireValue(String value) {
    for (var role : values()) {
      if (role.wireValue.equals(value)) {
        return role;
      }
    }
    return null;
  }
}
