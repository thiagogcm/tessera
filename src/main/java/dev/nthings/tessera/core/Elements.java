package dev.nthings.tessera.core;

import java.util.function.Supplier;

/// Static factory entry points for building semantic HTML5 elements and deferred overlays.
public final class Elements {

  private Elements() {}

  public static TagElement el(HtmlTag tag) {
    return new TagElement(tag);
  }

  public static TagElement div() {
    return new TagElement(HtmlTag.DIV);
  }

  public static TagElement span() {
    return new TagElement(HtmlTag.SPAN);
  }

  public static TagElement main() {
    return new TagElement(HtmlTag.MAIN);
  }

  public static TagElement header() {
    return new TagElement(HtmlTag.HEADER);
  }

  public static TagElement footer() {
    return new TagElement(HtmlTag.FOOTER);
  }

  public static TagElement nav() {
    return new TagElement(HtmlTag.NAV);
  }

  public static TagElement section() {
    return new TagElement(HtmlTag.SECTION);
  }

  public static TagElement article() {
    return new TagElement(HtmlTag.ARTICLE);
  }

  public static TagElement aside() {
    return new TagElement(HtmlTag.ASIDE);
  }

  public static TagElement hgroup() {
    return new TagElement(HtmlTag.HGROUP);
  }

  public static TagElement h1() {
    return new TagElement(HtmlTag.H1);
  }

  public static TagElement h2() {
    return new TagElement(HtmlTag.H2);
  }

  public static TagElement h3() {
    return new TagElement(HtmlTag.H3);
  }

  public static TagElement h4() {
    return new TagElement(HtmlTag.H4);
  }

  public static TagElement h5() {
    return new TagElement(HtmlTag.H5);
  }

  public static TagElement h6() {
    return new TagElement(HtmlTag.H6);
  }

  public static TagElement p() {
    return new TagElement(HtmlTag.P);
  }

  public static TagElement ul() {
    return new TagElement(HtmlTag.UL);
  }

  public static TagElement ol() {
    return new TagElement(HtmlTag.OL);
  }

  public static TagElement li() {
    return new TagElement(HtmlTag.LI);
  }

  public static TagElement dl() {
    return new TagElement(HtmlTag.DL);
  }

  public static TagElement dt() {
    return new TagElement(HtmlTag.DT);
  }

  public static TagElement dd() {
    return new TagElement(HtmlTag.DD);
  }

  public static TagElement button() {
    return new TagElement(HtmlTag.BUTTON).attr("type", "button");
  }

  public static TagElement submitButton() {
    return new TagElement(HtmlTag.BUTTON).attr("type", "submit");
  }

  public static TagElement a(String href) {
    return new TagElement(HtmlTag.A).attr("href", href);
  }

  public static TagElement dialog() {
    return new TagElement(HtmlTag.DIALOG);
  }

  public static TagElement form() {
    return new TagElement(HtmlTag.FORM);
  }

  public static TagElement label() {
    return new TagElement(HtmlTag.LABEL);
  }

  public static TagElement input(String type) {
    return new TagElement(HtmlTag.INPUT).attr("type", type);
  }

  public static TagElement select() {
    return new TagElement(HtmlTag.SELECT);
  }

  public static TagElement option() {
    return new TagElement(HtmlTag.OPTION);
  }

  public static TagElement option(String value, String text) {
    return new TagElement(HtmlTag.OPTION).attr("value", value).text(text);
  }

  public static TagElement optgroup(String label) {
    return new TagElement(HtmlTag.OPTGROUP).attr("label", label);
  }

  public static TagElement datalist() {
    return new TagElement(HtmlTag.DATALIST);
  }

  public static TagElement selectedcontent() {
    return new TagElement(HtmlTag.SELECTEDCONTENT);
  }

  public static TagElement textarea() {
    return new TagElement(HtmlTag.TEXTAREA);
  }

  public static TagElement fieldset() {
    return new TagElement(HtmlTag.FIELDSET);
  }

  public static TagElement legend() {
    return new TagElement(HtmlTag.LEGEND);
  }

  public static TagElement output() {
    return new TagElement(HtmlTag.OUTPUT);
  }

  public static TagElement img(String src, String alt) {
    return new TagElement(HtmlTag.IMG)
        .attr("src", src)
        .attr("alt", alt)
        .attr("loading", "lazy")
        .attr("decoding", "async");
  }

  public static TagElement figure() {
    return new TagElement(HtmlTag.FIGURE);
  }

  public static TagElement figcaption() {
    return new TagElement(HtmlTag.FIGCAPTION);
  }

  public static TagElement table() {
    return new TagElement(HtmlTag.TABLE);
  }

  public static TagElement caption() {
    return new TagElement(HtmlTag.CAPTION);
  }

  public static TagElement thead() {
    return new TagElement(HtmlTag.THEAD);
  }

  public static TagElement tbody() {
    return new TagElement(HtmlTag.TBODY);
  }

  public static TagElement tfoot() {
    return new TagElement(HtmlTag.TFOOT);
  }

  public static TagElement tr() {
    return new TagElement(HtmlTag.TR);
  }

  public static TagElement th() {
    return new TagElement(HtmlTag.TH);
  }

  public static TagElement td() {
    return new TagElement(HtmlTag.TD);
  }

  public static TagElement code() {
    return new TagElement(HtmlTag.CODE);
  }

  public static TagElement pre() {
    return new TagElement(HtmlTag.PRE);
  }

  public static TagElement kbd() {
    return new TagElement(HtmlTag.KBD);
  }

  public static TagElement mark() {
    return new TagElement(HtmlTag.MARK);
  }

  public static TagElement u() {
    return new TagElement(HtmlTag.U);
  }

  public static TagElement s() {
    return new TagElement(HtmlTag.S);
  }

  public static TagElement del() {
    return new TagElement(HtmlTag.DEL);
  }

  public static TagElement ins() {
    return new TagElement(HtmlTag.INS);
  }

  public static TagElement sub() {
    return new TagElement(HtmlTag.SUB);
  }

  public static TagElement sup() {
    return new TagElement(HtmlTag.SUP);
  }

  public static TagElement strong() {
    return new TagElement(HtmlTag.STRONG);
  }

  public static TagElement em() {
    return new TagElement(HtmlTag.EM);
  }

  public static TagElement small() {
    return new TagElement(HtmlTag.SMALL);
  }

  public static TagElement details() {
    return new TagElement(HtmlTag.DETAILS);
  }

  public static TagElement summary() {
    return new TagElement(HtmlTag.SUMMARY);
  }

  public static TagElement br() {
    return new TagElement(HtmlTag.BR);
  }

  public static TagElement hr() {
    return new TagElement(HtmlTag.HR);
  }

  public static TextElement text(String content) {
    return new TextElement(content);
  }

  public static RawHtmlElement rawHtml(String trustedHtml) {
    return new RawHtmlElement(trustedHtml);
  }

  public static EmptyElement empty() {
    return EmptyElement.INSTANCE;
  }

  public static DeferredElement deferred(IntoElement child) {
    return new DeferredElement(child.intoElement(), 0);
  }

  public static DeferredElement deferred(IntoElement child, int priority) {
    return new DeferredElement(child.intoElement(), priority);
  }

  /// Element resolved after the rest of the page's layout; see [LateElement].
  public static LateElement late(Supplier<? extends IntoElement> content) {
    return new LateElement(content);
  }

  public static TagElement link() {
    return new TagElement(HtmlTag.LINK);
  }
}
