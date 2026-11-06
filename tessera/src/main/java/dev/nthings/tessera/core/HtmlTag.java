package dev.nthings.tessera.core;

import java.util.Locale;

/// Semantic HTML5 tags with HTML5 void-element classification.
public enum HtmlTag {
  DIV,
  SPAN,
  MAIN,
  HEADER,
  SEARCH,
  FOOTER,
  NAV,
  SECTION,
  ARTICLE,
  ASIDE,
  HGROUP,
  H1,
  H2,
  H3,
  H4,
  H5,
  H6,
  P,
  UL,
  OL,
  LI,
  DL,
  DT,
  DD,
  BUTTON,
  A,
  DIALOG,
  FORM,
  LABEL,
  INPUT,
  SELECT,
  OPTION,
  OPTGROUP,
  DATALIST,
  SELECTEDCONTENT,
  TEXTAREA,
  FIELDSET,
  LEGEND,
  OUTPUT,
  TABLE,
  THEAD,
  TBODY,
  TFOOT,
  TR,
  TH,
  TD,
  CAPTION,
  FIGURE,
  FIGCAPTION,
  IMG,
  PICTURE,
  SOURCE,
  TRACK,
  CODE,
  PRE,
  KBD,
  TIME,
  MARK,
  U,
  S,
  DEL,
  INS,
  SUB,
  SUP,
  SMALL,
  STRONG,
  EM,
  DETAILS,
  SUMMARY,
  BR,
  HR,
  META,
  LINK,
  STYLE,
  SCRIPT;

  private final String tagName = name().toLowerCase(Locale.ROOT).replace('_', '-');

  public String tagName() {
    return tagName;
  }

  public boolean isVoid() {
    return switch (this) {
      case INPUT, IMG, SOURCE, TRACK, BR, HR, META, LINK -> true;
      default -> false;
    };
  }
}
