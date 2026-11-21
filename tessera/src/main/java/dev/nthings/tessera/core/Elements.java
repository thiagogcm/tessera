package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.PortableNode;

/// Static factories for portable element trees, usually imported statically. Most [HtmlTag] values
/// have a zero-arg factory returning a mutable [TagElement], plus overloads accepting child nodes
/// (`IntoElement...`) or text (`String`) for concise authoring. Tags without a dedicated factory,
/// such as `time`, are built with [#el(TagName)]. Also [#fragment()], [#custom], [#svg(SvgTag)],
/// and [#text] and [#empty], which return a plain [Element]. Content that only a static document
/// places comes from `Documents`.
public final class Elements {

  private Elements() {}

  private static TagElement with(TagElement tag, IntoElement first, IntoElement... rest) {
    return tag.child(first).children(rest);
  }

  /// Creates an element for any tag, including those without a factory here, such as
  /// `HtmlTag.TIME`. Text children are HTML-escaped. Scripts are document content: create them with
  /// `Documents.script`.
  public static TagElement el(TagName tag) {
    return new TagElement(tag);
  }

  /// Creates an autonomous custom element, such as `<my-widget>`. Throws IllegalArgumentException
  /// unless the name starts with a lowercase ASCII letter, contains a hyphen, and uses only
  /// lowercase ASCII letters, digits, `.`, `_` and `-`. Contribute the module that defines it from
  /// a `DocumentComponent` with `DocumentContext.contributeScript`.
  public static TagElement custom(String name) {
    return new TagElement(new CustomElement(name));
  }

  /// Creates a [FragmentElement], a transparent group that emits no wrapper markup.
  public static FragmentElement fragment() {
    return new FragmentElement();
  }

  /// Creates a [FragmentElement] containing `first` and `rest` in order.
  public static FragmentElement fragment(IntoElement first, IntoElement... rest) {
    return new FragmentElement().child(first).children(rest);
  }

  public static TagElement div() {
    return new TagElement(HtmlTag.DIV);
  }

  public static TagElement div(IntoElement first, IntoElement... rest) {
    return with(div(), first, rest);
  }

  public static TagElement span() {
    return new TagElement(HtmlTag.SPAN);
  }

  public static TagElement span(String text) {
    return span().text(text);
  }

  public static TagElement span(IntoElement first, IntoElement... rest) {
    return with(span(), first, rest);
  }

  public static TagElement main() {
    return new TagElement(HtmlTag.MAIN);
  }

  public static TagElement main(IntoElement first, IntoElement... rest) {
    return with(main(), first, rest);
  }

  public static TagElement header() {
    return new TagElement(HtmlTag.HEADER);
  }

  public static TagElement header(IntoElement first, IntoElement... rest) {
    return with(header(), first, rest);
  }

  /// `<search>` landmark grouping search or filtering controls.
  public static TagElement search() {
    return new TagElement(HtmlTag.SEARCH);
  }

  public static TagElement search(IntoElement first, IntoElement... rest) {
    return with(search(), first, rest);
  }

  public static TagElement footer() {
    return new TagElement(HtmlTag.FOOTER);
  }

  public static TagElement footer(IntoElement first, IntoElement... rest) {
    return with(footer(), first, rest);
  }

  public static TagElement nav() {
    return new TagElement(HtmlTag.NAV);
  }

  public static TagElement nav(IntoElement first, IntoElement... rest) {
    return with(nav(), first, rest);
  }

  public static TagElement section() {
    return new TagElement(HtmlTag.SECTION);
  }

  public static TagElement section(IntoElement first, IntoElement... rest) {
    return with(section(), first, rest);
  }

  public static TagElement article() {
    return new TagElement(HtmlTag.ARTICLE);
  }

  public static TagElement article(IntoElement first, IntoElement... rest) {
    return with(article(), first, rest);
  }

  public static TagElement aside() {
    return new TagElement(HtmlTag.ASIDE);
  }

  public static TagElement aside(IntoElement first, IntoElement... rest) {
    return with(aside(), first, rest);
  }

  public static TagElement hgroup() {
    return new TagElement(HtmlTag.HGROUP);
  }

  public static TagElement hgroup(IntoElement first, IntoElement... rest) {
    return with(hgroup(), first, rest);
  }

  public static TagElement h1() {
    return new TagElement(HtmlTag.H1);
  }

  public static TagElement h1(String text) {
    return h1().text(text);
  }

  public static TagElement h1(IntoElement first, IntoElement... rest) {
    return with(h1(), first, rest);
  }

  public static TagElement h2() {
    return new TagElement(HtmlTag.H2);
  }

  public static TagElement h2(String text) {
    return h2().text(text);
  }

  public static TagElement h2(IntoElement first, IntoElement... rest) {
    return with(h2(), first, rest);
  }

  public static TagElement h3() {
    return new TagElement(HtmlTag.H3);
  }

  public static TagElement h3(String text) {
    return h3().text(text);
  }

  public static TagElement h3(IntoElement first, IntoElement... rest) {
    return with(h3(), first, rest);
  }

  public static TagElement h4() {
    return new TagElement(HtmlTag.H4);
  }

  public static TagElement h4(String text) {
    return h4().text(text);
  }

  public static TagElement h4(IntoElement first, IntoElement... rest) {
    return with(h4(), first, rest);
  }

  public static TagElement h5() {
    return new TagElement(HtmlTag.H5);
  }

  public static TagElement h5(String text) {
    return h5().text(text);
  }

  public static TagElement h5(IntoElement first, IntoElement... rest) {
    return with(h5(), first, rest);
  }

  public static TagElement h6() {
    return new TagElement(HtmlTag.H6);
  }

  public static TagElement h6(String text) {
    return h6().text(text);
  }

  public static TagElement h6(IntoElement first, IntoElement... rest) {
    return with(h6(), first, rest);
  }

  public static TagElement p() {
    return new TagElement(HtmlTag.P);
  }

  public static TagElement p(String text) {
    return p().text(text);
  }

  public static TagElement p(IntoElement first, IntoElement... rest) {
    return with(p(), first, rest);
  }

  public static TagElement blockquote() {
    return new TagElement(HtmlTag.BLOCKQUOTE);
  }

  public static TagElement blockquote(String text) {
    return blockquote().text(text);
  }

  public static TagElement blockquote(IntoElement first, IntoElement... rest) {
    return with(blockquote(), first, rest);
  }

  public static TagElement ul() {
    return new TagElement(HtmlTag.UL);
  }

  public static TagElement ul(IntoElement first, IntoElement... rest) {
    return with(ul(), first, rest);
  }

  public static TagElement ol() {
    return new TagElement(HtmlTag.OL);
  }

  public static TagElement ol(IntoElement first, IntoElement... rest) {
    return with(ol(), first, rest);
  }

  public static TagElement li() {
    return new TagElement(HtmlTag.LI);
  }

  public static TagElement li(String text) {
    return li().text(text);
  }

  public static TagElement li(IntoElement first, IntoElement... rest) {
    return with(li(), first, rest);
  }

  public static TagElement dl() {
    return new TagElement(HtmlTag.DL);
  }

  public static TagElement dl(IntoElement first, IntoElement... rest) {
    return with(dl(), first, rest);
  }

  public static TagElement dt() {
    return new TagElement(HtmlTag.DT);
  }

  public static TagElement dt(String text) {
    return dt().text(text);
  }

  public static TagElement dt(IntoElement first, IntoElement... rest) {
    return with(dt(), first, rest);
  }

  public static TagElement dd() {
    return new TagElement(HtmlTag.DD);
  }

  public static TagElement dd(String text) {
    return dd().text(text);
  }

  public static TagElement dd(IntoElement first, IntoElement... rest) {
    return with(dd(), first, rest);
  }

  /// Creates a `<button type="button">`, which does not submit its form.
  public static TagElement button() {
    return new TagElement(HtmlTag.BUTTON).attr("type", "button");
  }

  /// Creates a `<button type="button">` with visible `text`.
  public static TagElement button(String text) {
    return button().text(text);
  }

  /// Creates a `<button type="button">` containing `first` and `rest`.
  public static TagElement button(IntoElement first, IntoElement... rest) {
    return with(button(), first, rest);
  }

  /// Creates a link to the URL `href`.
  public static TagElement a(String href) {
    return new TagElement(HtmlTag.A).attr("href", href);
  }

  /// Creates a link to the URL `href` with visible `text`.
  public static TagElement a(String href, String text) {
    return a(href).text(text);
  }

  /// Creates a link to the URL `href` containing `first` and `rest`.
  public static TagElement a(String href, IntoElement first, IntoElement... rest) {
    return with(a(href), first, rest);
  }

  /// Creates a same-page link to the element bound to `target`: `href="#<target dom id>"`. An
  /// unbound target reports `unbound-reference`.
  public static TagElement a(ElementRef target) {
    return new TagElement(HtmlTag.A).href(target);
  }

  /// Creates a same-page link to `target` with visible `text`.
  public static TagElement a(ElementRef target, String text) {
    return a(target).text(text);
  }

  /// Creates a same-page link to `target` containing `first` and `rest`.
  public static TagElement a(ElementRef target, IntoElement first, IntoElement... rest) {
    return with(a(target), first, rest);
  }

  public static TagElement dialog() {
    return new TagElement(HtmlTag.DIALOG);
  }

  public static TagElement dialog(IntoElement first, IntoElement... rest) {
    return with(dialog(), first, rest);
  }

  public static TagElement form() {
    return new TagElement(HtmlTag.FORM);
  }

  public static TagElement form(IntoElement first, IntoElement... rest) {
    return with(form(), first, rest);
  }

  public static TagElement label() {
    return new TagElement(HtmlTag.LABEL);
  }

  public static TagElement label(String text) {
    return label().text(text);
  }

  public static TagElement label(IntoElement first, IntoElement... rest) {
    return with(label(), first, rest);
  }

  /// Creates an `<input>` with the typed `type` attribute.
  public static TagElement input(InputType type) {
    return new TagElement(HtmlTag.INPUT).attr("type", type.wireValue());
  }

  public static TagElement select() {
    return new TagElement(HtmlTag.SELECT);
  }

  public static TagElement select(IntoElement first, IntoElement... rest) {
    return with(select(), first, rest);
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

  public static TagElement fieldset(IntoElement first, IntoElement... rest) {
    return with(fieldset(), first, rest);
  }

  public static TagElement legend() {
    return new TagElement(HtmlTag.LEGEND);
  }

  public static TagElement legend(String text) {
    return legend().text(text);
  }

  public static TagElement legend(IntoElement first, IntoElement... rest) {
    return with(legend(), first, rest);
  }

  public static TagElement output() {
    return new TagElement(HtmlTag.OUTPUT);
  }

  public static TagElement output(IntoElement first, IntoElement... rest) {
    return with(output(), first, rest);
  }

  /// Creates an `<img>` with `loading="lazy"` and `decoding="async"`. The compiler requires `alt`
  /// (`image-alt`); an empty `alt` also needs `role="presentation"` or `aria-hidden="true"`
  /// (`decorative-image`). It also requires `width` and `height`, a CSS aspect ratio, or
  /// [TagElement#unknownDimensions()] (`image-dimensions`). For a hero image that needs priority
  /// loading, wrap `components.Image` with `document.components.HeroImage`.
  public static TagElement img(String src, String alt) {
    return new TagElement(HtmlTag.IMG)
        .attr("src", src)
        .attr("alt", alt)
        .attr("loading", "lazy")
        .attr("decoding", "async");
  }

  public static TagElement video() {
    return new TagElement(HtmlTag.VIDEO);
  }

  public static TagElement audio() {
    return new TagElement(HtmlTag.AUDIO);
  }

  public static TagElement iframe() {
    return new TagElement(HtmlTag.IFRAME);
  }

  public static TagElement canvas() {
    return new TagElement(HtmlTag.CANVAS);
  }

  public static TagElement template() {
    return new TagElement(HtmlTag.TEMPLATE);
  }

  public static TagElement slot() {
    return new TagElement(HtmlTag.SLOT);
  }

  public static TagElement picture() {
    return new TagElement(HtmlTag.PICTURE);
  }

  public static TagElement source() {
    return new TagElement(HtmlTag.SOURCE);
  }

  public static TagElement track() {
    return new TagElement(HtmlTag.TRACK);
  }

  public static TagElement figure() {
    return new TagElement(HtmlTag.FIGURE);
  }

  public static TagElement figure(IntoElement first, IntoElement... rest) {
    return with(figure(), first, rest);
  }

  public static TagElement figcaption() {
    return new TagElement(HtmlTag.FIGCAPTION);
  }

  public static TagElement figcaption(String text) {
    return figcaption().text(text);
  }

  public static TagElement figcaption(IntoElement first, IntoElement... rest) {
    return with(figcaption(), first, rest);
  }

  public static TagElement table() {
    return new TagElement(HtmlTag.TABLE);
  }

  public static TagElement table(IntoElement first, IntoElement... rest) {
    return with(table(), first, rest);
  }

  public static TagElement caption() {
    return new TagElement(HtmlTag.CAPTION);
  }

  public static TagElement caption(String text) {
    return caption().text(text);
  }

  public static TagElement caption(IntoElement first, IntoElement... rest) {
    return with(caption(), first, rest);
  }

  public static TagElement thead() {
    return new TagElement(HtmlTag.THEAD);
  }

  public static TagElement thead(IntoElement first, IntoElement... rest) {
    return with(thead(), first, rest);
  }

  public static TagElement tbody() {
    return new TagElement(HtmlTag.TBODY);
  }

  public static TagElement tbody(IntoElement first, IntoElement... rest) {
    return with(tbody(), first, rest);
  }

  public static TagElement tfoot() {
    return new TagElement(HtmlTag.TFOOT);
  }

  public static TagElement tfoot(IntoElement first, IntoElement... rest) {
    return with(tfoot(), first, rest);
  }

  public static TagElement tr() {
    return new TagElement(HtmlTag.TR);
  }

  public static TagElement tr(IntoElement first, IntoElement... rest) {
    return with(tr(), first, rest);
  }

  public static TagElement th() {
    return new TagElement(HtmlTag.TH);
  }

  public static TagElement th(String text) {
    return th().text(text);
  }

  public static TagElement th(IntoElement first, IntoElement... rest) {
    return with(th(), first, rest);
  }

  public static TagElement td() {
    return new TagElement(HtmlTag.TD);
  }

  public static TagElement td(String text) {
    return td().text(text);
  }

  public static TagElement td(IntoElement first, IntoElement... rest) {
    return with(td(), first, rest);
  }

  public static TagElement code() {
    return new TagElement(HtmlTag.CODE);
  }

  public static TagElement code(String text) {
    return code().text(text);
  }

  public static TagElement code(IntoElement first, IntoElement... rest) {
    return with(code(), first, rest);
  }

  public static TagElement pre() {
    return new TagElement(HtmlTag.PRE);
  }

  public static TagElement pre(String text) {
    return pre().text(text);
  }

  public static TagElement pre(IntoElement first, IntoElement... rest) {
    return with(pre(), first, rest);
  }

  public static TagElement kbd() {
    return new TagElement(HtmlTag.KBD);
  }

  public static TagElement kbd(String text) {
    return kbd().text(text);
  }

  public static TagElement kbd(IntoElement first, IntoElement... rest) {
    return with(kbd(), first, rest);
  }

  public static TagElement mark() {
    return new TagElement(HtmlTag.MARK);
  }

  public static TagElement mark(String text) {
    return mark().text(text);
  }

  public static TagElement mark(IntoElement first, IntoElement... rest) {
    return with(mark(), first, rest);
  }

  public static TagElement u() {
    return new TagElement(HtmlTag.U);
  }

  public static TagElement u(String text) {
    return u().text(text);
  }

  public static TagElement u(IntoElement first, IntoElement... rest) {
    return with(u(), first, rest);
  }

  public static TagElement s() {
    return new TagElement(HtmlTag.S);
  }

  public static TagElement s(String text) {
    return s().text(text);
  }

  public static TagElement s(IntoElement first, IntoElement... rest) {
    return with(s(), first, rest);
  }

  public static TagElement del() {
    return new TagElement(HtmlTag.DEL);
  }

  public static TagElement del(String text) {
    return del().text(text);
  }

  public static TagElement del(IntoElement first, IntoElement... rest) {
    return with(del(), first, rest);
  }

  public static TagElement ins() {
    return new TagElement(HtmlTag.INS);
  }

  public static TagElement ins(String text) {
    return ins().text(text);
  }

  public static TagElement ins(IntoElement first, IntoElement... rest) {
    return with(ins(), first, rest);
  }

  public static TagElement sub() {
    return new TagElement(HtmlTag.SUB);
  }

  public static TagElement sub(String text) {
    return sub().text(text);
  }

  public static TagElement sub(IntoElement first, IntoElement... rest) {
    return with(sub(), first, rest);
  }

  public static TagElement sup() {
    return new TagElement(HtmlTag.SUP);
  }

  public static TagElement sup(String text) {
    return sup().text(text);
  }

  public static TagElement sup(IntoElement first, IntoElement... rest) {
    return with(sup(), first, rest);
  }

  public static TagElement strong() {
    return new TagElement(HtmlTag.STRONG);
  }

  public static TagElement strong(String text) {
    return strong().text(text);
  }

  public static TagElement strong(IntoElement first, IntoElement... rest) {
    return with(strong(), first, rest);
  }

  public static TagElement em() {
    return new TagElement(HtmlTag.EM);
  }

  public static TagElement em(String text) {
    return em().text(text);
  }

  public static TagElement em(IntoElement first, IntoElement... rest) {
    return with(em(), first, rest);
  }

  public static TagElement small() {
    return new TagElement(HtmlTag.SMALL);
  }

  public static TagElement small(String text) {
    return small().text(text);
  }

  public static TagElement small(IntoElement first, IntoElement... rest) {
    return with(small(), first, rest);
  }

  public static TagElement details() {
    return new TagElement(HtmlTag.DETAILS);
  }

  public static TagElement details(IntoElement first, IntoElement... rest) {
    return with(details(), first, rest);
  }

  public static TagElement summary() {
    return new TagElement(HtmlTag.SUMMARY);
  }

  public static TagElement summary(String text) {
    return summary().text(text);
  }

  public static TagElement summary(IntoElement first, IntoElement... rest) {
    return with(summary(), first, rest);
  }

  public static TagElement br() {
    return new TagElement(HtmlTag.BR);
  }

  public static TagElement hr() {
    return new TagElement(HtmlTag.HR);
  }

  /// Creates a text node. Its content is HTML-escaped.
  public static Element text(String content) {
    return new PortablePlacement(new PortableNode.Text(content));
  }

  /// Returns an element that emits no markup.
  public static Element empty() {
    return PortablePlacement.EMPTY;
  }

  /// Creates an element in the SVG namespace, such as `svg` or `path`. Static output writes it as
  /// inline SVG and a browser root creates it with its namespace, so bundled icons render the same
  /// in both. Set geometry with [TagElement#attr(String, String)], such as `viewBox` or `d`.
  public static TagElement svg(SvgTag tag) {
    return new TagElement(tag);
  }

  /// Creates an `<svg>` root with `viewBox="0 0 <width> <height>"` and `width` and `height` set,
  /// hidden from assistive technology. Name a meaningful graphic with
  /// [TagElement#ariaHidden(boolean)] `false` plus a [TagElement#role(Role)] and label.
  public static TagElement svgIcon(int width, int height) {
    return new TagElement(SvgTag.SVG)
        .attr("viewBox", "0 0 " + width + " " + height)
        .dimensions(width, height)
        .ariaHidden(true);
  }

  public static TagElement link() {
    return new TagElement(HtmlTag.LINK);
  }
}
