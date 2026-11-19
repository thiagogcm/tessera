package dev.nthings.tessera.core;

/// WAI-ARIA roles an element can declare. The compiler checks each declared role against its
/// requirements: accessible name, owned descendants, required states and supported `aria-*`.
public enum Role implements WireToken {
  // Landmarks
  BANNER,
  COMPLEMENTARY,
  CONTENTINFO,
  FORM,
  MAIN,
  NAVIGATION,
  REGION,
  SEARCH,

  // Widgets
  BUTTON,
  CHECKBOX,
  COMBOBOX,
  LINK,
  LISTBOX,
  MENU,
  MENUBAR,
  MENUITEM,
  MENUITEMCHECKBOX,
  MENUITEMRADIO,
  OPTION,
  PROGRESSBAR,
  RADIO,
  SEARCHBOX,
  SLIDER,
  SPINBUTTON,
  SWITCH,
  TAB,
  TABLIST,
  TABPANEL,
  TEXTBOX,
  TREE,
  TREEITEM,

  // Document structure
  ARTICLE,
  GROUP,
  HEADING,
  IMG,
  LIST,
  LISTITEM,
  NONE,
  NOTE,
  PRESENTATION,
  RADIOGROUP,
  SEPARATOR,
  TABLE,
  TOOLTIP,

  // Live regions
  ALERT,
  STATUS,

  // Windows
  ALERTDIALOG,
  DIALOG,

  // Digital Publishing roles
  DOC_BACKLINK,
  DOC_ENDNOTES,
  DOC_FOOTNOTE,
  DOC_NOTEREF;

  /// The `role` attribute value, such as `doc-noteref`.
  @Override
  public String wireValue() {
    return WireToken.super.wireValue();
  }
}
