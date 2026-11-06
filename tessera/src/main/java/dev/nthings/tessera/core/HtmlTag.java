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

  /// The heading level of `<h1>` through `<h6>`, or 0 for any other tag.
  public int headingLevel() {
    return switch (this) {
      case H1 -> 1;
      case H2 -> 2;
      case H3 -> 3;
      case H4 -> 4;
      case H5 -> 5;
      case H6 -> 6;
      default -> 0;
    };
  }

  /// The heading tag of `level`, 1 through 6.
  public static HtmlTag heading(int level) {
    return switch (level) {
      case 1 -> H1;
      case 2 -> H2;
      case 3 -> H3;
      case 4 -> H4;
      case 5 -> H5;
      case 6 -> H6;
      default -> throw new IllegalArgumentException("Heading level must be 1 to 6: " + level);
    };
  }

  public boolean isVoid() {
    return switch (this) {
      case INPUT, IMG, SOURCE, TRACK, BR, HR, META, LINK -> true;
      default -> false;
    };
  }
}
