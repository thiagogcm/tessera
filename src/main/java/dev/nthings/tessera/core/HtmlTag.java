package dev.nthings.tessera.core;

/// Semantic HTML5 tags with HTML5 void-element classification.
public enum HtmlTag {
  DIV("div", false),
  SPAN("span", false),
  MAIN("main", false),
  HEADER("header", false),
  FOOTER("footer", false),
  NAV("nav", false),
  SECTION("section", false),
  ARTICLE("article", false),
  ASIDE("aside", false),
  HGROUP("hgroup", false),
  H1("h1", false),
  H2("h2", false),
  H3("h3", false),
  H4("h4", false),
  H5("h5", false),
  H6("h6", false),
  P("p", false),
  UL("ul", false),
  OL("ol", false),
  LI("li", false),
  DL("dl", false),
  DT("dt", false),
  DD("dd", false),
  BUTTON("button", false),
  A("a", false),
  DIALOG("dialog", false),
  FORM("form", false),
  LABEL("label", false),
  INPUT("input", true),
  SELECT("select", false),
  OPTION("option", false),
  OPTGROUP("optgroup", false),
  DATALIST("datalist", false),
  SELECTEDCONTENT("selectedcontent", false),
  TEXTAREA("textarea", false),
  FIELDSET("fieldset", false),
  LEGEND("legend", false),
  OUTPUT("output", false),
  TABLE("table", false),
  THEAD("thead", false),
  TBODY("tbody", false),
  TFOOT("tfoot", false),
  TR("tr", false),
  TH("th", false),
  TD("td", false),
  CAPTION("caption", false),
  FIGURE("figure", false),
  FIGCAPTION("figcaption", false),
  IMG("img", true),
  PICTURE("picture", false),
  SOURCE("source", true),
  TRACK("track", true),
  CODE("code", false),
  PRE("pre", false),
  KBD("kbd", false),
  TIME("time", false),
  MARK("mark", false),
  U("u", false),
  S("s", false),
  DEL("del", false),
  INS("ins", false),
  SUB("sub", false),
  SUP("sup", false),
  SMALL("small", false),
  STRONG("strong", false),
  EM("em", false),
  DETAILS("details", false),
  SUMMARY("summary", false),
  BR("br", true),
  HR("hr", true),
  META("meta", true),
  LINK("link", true);

  private final String tagName;
  private final boolean voidElement;

  HtmlTag(String tagName, boolean voidElement) {
    this.tagName = tagName;
    this.voidElement = voidElement;
  }

  public String tagName() {
    return tagName;
  }

  public boolean isVoid() {
    return voidElement;
  }
}
