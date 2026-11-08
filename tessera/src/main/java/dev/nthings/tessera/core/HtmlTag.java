package dev.nthings.tessera.core;

import java.util.Locale;

/// Semantic HTML tags with void-element classification.
public enum HtmlTag implements TagName {
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
  VIDEO,
  AUDIO,
  IFRAME,
  CANVAS,
  TEMPLATE,
  SLOT,
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

  private final String tagName = name().toLowerCase(Locale.ROOT);

  @Override
  public String tagName() {
    return tagName;
  }

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

  @Override
  public boolean isVoid() {
    return switch (this) {
      case INPUT, IMG, SOURCE, TRACK, BR, HR, META, LINK -> true;
      default -> false;
    };
  }
}
