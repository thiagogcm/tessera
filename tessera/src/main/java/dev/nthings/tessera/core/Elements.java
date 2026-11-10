package dev.nthings.tessera.core;

import java.util.function.Function;

/// Static factories for element trees, usually imported statically. Most [HtmlTag] values have a
/// factory returning a mutable [TagElement]; tags without one, such as `time`, are built with
/// [#el(TagName)]. Also [#fragment()], and [#text], [#custom], [#rawHtml], [#empty], [#deferred],
/// [#late] and [#effect], which return a plain [Element].
public final class Elements {

  private Elements() {}

  /// Creates an element for any tag, including those without a factory here, such as
  /// `HtmlTag.TIME`. Text children are HTML-escaped, so `script` and `style` content needs
  /// [#rawHtml].
  public static TagElement el(TagName tag) {
    return new TagElement(tag);
  }

  /// Creates an autonomous custom element, such as `<my-widget>`. Throws IllegalArgumentException
  /// unless the name is lowercase and contains a hyphen. Pair it with a [PageScript] registered
  /// through [DocumentContext#contributeScript].
  public static TagElement custom(String name) {
    return new TagElement(new CustomElement(name));
  }

  /// Creates a [FragmentElement], a transparent group that emits no wrapper markup.
  public static FragmentElement fragment() {
    return new FragmentElement();
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

  /// `<search>` landmark grouping search or filtering controls.
  public static TagElement search() {
    return new TagElement(HtmlTag.SEARCH);
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

  /// Creates a `<button type="button">`, which does not submit its form.
  public static TagElement button() {
    return new TagElement(HtmlTag.BUTTON).attr("type", "button");
  }

  /// Creates a link to the URL `href`.
  public static TagElement a(String href) {
    return new TagElement(HtmlTag.A).attr("href", href);
  }

  /// Creates a same-page link to the element bound to `target`: `href="#<target dom id>"`. An
  /// unbound target reports `unbound-reference`.
  public static TagElement a(ElementRef target) {
    return new TagElement(HtmlTag.A).href(target);
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

  /// Creates an `<img>` with `loading="lazy"` and `decoding="async"`. The compiler requires `alt`
  /// (`image-alt`); an empty `alt` also needs `role="presentation"` or `aria-hidden="true"`
  /// (`decorative-image`). It also requires `width` and `height`, or a CSS aspect ratio
  /// (`image-dimensions`). For a hero image that needs priority loading, use `components.Image`.
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

  /// Creates a text node. Its content is HTML-escaped.
  public static Element text(String content) {
    return new TextElement(content);
  }

  /// Creates a node for pre-sanitized HTML or inline SVG, emitted verbatim without escaping,
  /// parsing or validation. Ids, links and ARIA inside it are not checked by `duplicate-id`,
  /// `missing-fragment`, site link validation or the accessibility rules, so never pass
  /// user-controlled text.
  public static Element rawHtml(String trustedHtml) {
    return new RawHtmlElement(trustedHtml);
  }

  /// Returns an element that emits no markup.
  public static Element empty() {
    return EmptyElement.INSTANCE;
  }

  /// Moves `child` to the end of `<body>` at priority 0, for content that cannot sit in its
  /// parent's content model, such as a dialog or popover inside a parent that does not allow it.
  /// The child keeps its id scope and DOM ids.
  public static Element deferred(IntoElement child) {
    return new DeferredElement(child.intoElement(), 0);
  }

  /// Moves `child` to the end of `<body>` with `priority`. Lower priorities are placed first, and
  /// equal priorities keep placement order. The child keeps its id scope and DOM ids.
  public static Element deferred(IntoElement child, int priority) {
    return new DeferredElement(child.intoElement(), priority);
  }

  /// Creates an element resolved after the rest of the page body is expanded, in the context where
  /// it appears. Use it to read what other elements register, such as [DocumentContext#headings()].
  /// Late elements resolve first in, first out, before document-end sections and head
  /// contributions.
  public static Element late(Function<DocumentContext, ? extends IntoElement> content) {
    return new LateElement(content);
  }

  /// Places `effect` at this position of a live tree, keyed by `key` like
  /// [IntoElement#key(String)]. A browser mount starts it after the first commit that contains it
  /// and keeps it running while later renders place an effect with the same `key` here; a
  /// different key disposes it and starts the new one, so the key names the effect's
  /// dependencies. Removing it, or closing the root, disposes it. The effect emits no markup. The
  /// key shares its parent's sibling namespace with keyed children, so it must differ from every
  /// other effect and keyed sibling of that parent (`duplicate-key`). Static compilation reports
  /// `native-effect`, since a document has no lifetime to bind it to. Throws
  /// IllegalArgumentException if `key` is blank.
  public static Element effect(String key, Effect effect) {
    return new EffectElement(effect).key(key);
  }

  public static TagElement link() {
    return new TagElement(HtmlTag.LINK);
  }
}
