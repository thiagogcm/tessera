package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.aside;
import static dev.nthings.tessera.core.Elements.details;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ol;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.summary;

import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.ViewScope;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Scroll-Spy Table of Contents and documentation layout primitives.
public final class Toc {

  private static final String CHEVRON_SVG =
      "<svg class=\"toc-chevron\" width=\"14\" height=\"14\" viewBox=\"0 0 24 24\" fill=\"none\""
          + " stroke=\"currentColor\" stroke-width=\"2.2\" stroke-linecap=\"round\""
          + " stroke-linejoin=\"round\" aria-hidden=\"true\"><polyline points=\"9 18 15 12 9"
          + " 6\"/></svg>";

  private Toc() {}

  /// Section that registers with [RenderContext] during layout expansion.
  public record DocSection(String id, String title, int level, List<Element> body)
      implements RenderOnce {

    public DocSection {
      ContentSection.checkHeadingLevel(level);
      body = List.copyOf(body);
    }

    @Override
    public Element render() {
      var entry = RenderContext.current().registerHeading(id, title, level);
      var heading = ContentSection.sectionHeading(level, entry.targetDomId(), title);

      var sectionEl =
          Elements.section()
              .attr("id", entry.targetDomId())
              .scrollMarginBlockStart(CssLength.rem(7))
              .flex()
              .flexCol()
              .gap(Tokens.SPACE_3)
              .child(heading);
      sectionEl.children(body);
      return sectionEl;
    }
  }

  public static DocSection section(String id, String title, IntoElement... children) {
    return createSection(id, title, 2, children);
  }

  public static DocSection subsection(String id, String title, IntoElement... children) {
    return createSection(id, title, 3, children);
  }

  private static DocSection createSection(
      String id, String title, int level, IntoElement... children) {
    var list = new ArrayList<Element>(children.length);
    for (var child : children) {
      list.add(child.intoElement());
    }
    return new DocSection(id, title, level, list);
  }

  /// TOC of every section registered on the page, resolved after layout so it may precede them.
  public static Element fromContext(String id, String heading) {
    return Elements.late(
        () ->
            new TableOfContents(
                ElementId.of(id), heading, RenderContext.current().documentState().headings()));
  }

  public static TableOfContents of(String id, String heading, List<DocumentHeading> explicitItems) {
    return new TableOfContents(ElementId.of(id), heading, explicitItems);
  }

  public static DocLayout docLayout(String tocId, String tocHeading, IntoElement... sections) {
    var list = new ArrayList<Element>(sections.length);
    for (var s : sections) {
      list.add(s.intoElement());
    }
    return new DocLayout(tocId, tocHeading, list);
  }

  /// Scroll-Spy Table of Contents view with collapsible disclosure.
  public static final class TableOfContents implements RenderView {

    private final ElementId id;
    private final String heading;
    private final List<DocumentHeading> items;

    public TableOfContents(ElementId id, String heading, List<DocumentHeading> items) {
      this.id = id;
      this.heading = heading;
      this.items = List.copyOf(items);
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.flex()
                      .flexCol()
                      .gap(Tokens.SPACE_2)
                      .p(CssLength.px(0))
                      .bgRaw("transparent")
                      .borderNone())
          .select(
              ":scope .toc-summary",
              s ->
                  s.flex()
                      .itemsCenter()
                      .justifyBetween()
                      .py(CssLength.rem(0.35))
                      .px(Tokens.SPACE_1)
                      .listStyleNone()
                      .cursorPointer()
                      .textColor(Tokens.TEXT_MUTED)
                      .fontSize(Tokens.FONT_SIZE_XS)
                      .fontWeight(700))
          .select(":scope .toc-summary:hover", s -> s.textColor(Tokens.TEXT_PRIMARY))
          .select(":scope .toc-chevron", s -> s.transition("transform 0.2s ease"))
          .select(":scope details:open .toc-chevron", s -> s.transform("rotate(90deg)"))
          .select(
              ":scope details::details-content",
              s -> s.transition("block-size 0.3s ease, content-visibility 0.3s allow-discrete"))
          .select(
              ":scope .toc-collapsible-wrapper",
              s ->
                  s.scrollTargetGroupAuto()
                      .borderInlineStartSubtle(Tokens.BORDER_SUBTLE)
                      .ps(Tokens.SPACE_1))
          .select(
              ":scope ol",
              s -> s.listStyleNone().flex().flexCol().gap(Tokens.SPACE_0_5).p(CssLength.px(0)))
          .select(
              ":scope a",
              s ->
                  s.block()
                      .py(CssLength.rem(0.35))
                      .px(Tokens.SPACE_3)
                      .textColor(Tokens.TEXT_MUTED)
                      .fontSize(Tokens.FONT_SIZE_SM)
                      .rounded(Tokens.RADIUS_SM)
                      .textDecorationNone()
                      .transition("color 0.15s ease, background 0.15s ease"))
          .select(
              ":scope a[data-level=\"3\"]", s -> s.ps(Tokens.SPACE_6).fontSize(Tokens.FONT_SIZE_XS))
          .select(":scope a:hover", s -> s.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
          .select(
              ":scope a:target-current",
              s ->
                  s.textColor(Tokens.ACCENT_PRIMARY)
                      .fontWeight(600)
                      .borderInlineStart("3px solid " + Tokens.ACCENT_PRIMARY.varReference())
                      .bgRaw(HighlightTone.ACCENT.toCssBackground()));
    }

    @Override
    public Element render() {
      var list = ol();
      for (var item : items) {
        list.child(
            li().child(
                    a("#" + item.targetDomId())
                        .attr("data-level", Integer.toString(item.level()))
                        .text(item.title())));
      }

      var summaryEl =
          summary()
              .attr("class", "toc-summary")
              .child(span().attr("class", "toc-title").text(heading))
              .child(rawHtml(CHEVRON_SVG));

      var detailsEl =
          details()
              .attr("open", "")
              .attr("class", "toc-collapsible-wrapper")
              .child(summaryEl)
              .child(list);

      return nav().attr("class", "toc-container").ariaLabel(heading).child(detailsEl);
    }
  }

  /// Two-column documentation layout with main prose and sticky Table of Contents rail.
  public record DocLayout(String tocId, String tocHeading, List<Element> sections)
      implements RenderView {

    public DocLayout {
      sections = List.copyOf(sections);
    }

    @Override
    public ElementId entityId() {
      return layoutId(tocId);
    }

    /// DOM id of section `sectionId` placed directly in a top-level `docLayout(tocId, ...)`, for
    /// building links to it from other pages.
    public static String sectionDomId(String tocId, String sectionId) {
      return ViewScope.ROOT.extend(layoutId(tocId)).extend(ElementId.of(sectionId)).domId();
    }

    private static ElementId layoutId(String tocId) {
      return ElementId.of(tocId + "-layout");
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.flex().flexCol().gap(Tokens.SPACE_8).containerInlineSize())
          .select(
              ":scope > .doc-grid", s -> s.grid().gridColumns("minmax(0, 1fr)").gap(Tokens.SPACE_6))
          .select(
              ":scope > .doc-grid > aside",
              s ->
                  s.gridRow("1")
                      .bg(Tokens.SURFACE_CARD)
                      .borderSubtle(Tokens.BORDER_SUBTLE)
                      .rounded(Tokens.RADIUS_LG)
                      .p(Tokens.SPACE_3))
          .select(
              ":scope > .doc-grid > .doc-prose-col",
              s ->
                  s.gridRow("2")
                      .flex()
                      .flexCol()
                      .gap(Tokens.SPACE_10)
                      .inlineSize(CssLength.pct(100))
                      .minInlineSize(CssLength.px(0))
                      .maxInlineSize(CssLength.ch(75))
                      .textColor(Tokens.TEXT_PRIMARY)
                      .lineHeight("1.75"))
          .select(
              ":scope .doc-prose-col p:not([class])",
              s ->
                  s.mb(CssLength.rem(1.125))
                      .fontSize(Tokens.FONT_SIZE_MD)
                      .textColor(Tokens.TEXT_PRIMARY)
                      .lineHeight("1.75"))
          .containerMinInlineSize(
              CssLength.rem(48),
              c ->
                  c.select(
                          ":scope > .doc-grid",
                          s ->
                              s.gridColumns("minmax(0, 1fr) 15rem")
                                  .itemsStart()
                                  .gap(Tokens.SPACE_10))
                      .select(
                          ":scope > .doc-grid > .doc-prose-col",
                          s ->
                              s.gridColumn("1")
                                  .gridRow("1")
                                  .inlineSize(CssLength.pct(100))
                                  .minInlineSize(CssLength.px(0))
                                  .maxInlineSize(CssLength.ch(75)))
                      .select(
                          ":scope > .doc-grid > aside",
                          s ->
                              s.gridColumn("2")
                                  .gridRow("1")
                                  .sticky()
                                  .insetBlockStart(CssLength.rem(6.75))
                                  .bgRaw("transparent")
                                  .borderNone()
                                  .p(CssLength.px(0))));
    }

    @Override
    public Element render() {
      var proseCol = div().attr("class", "doc-prose-col").flex().flexCol().gap(Tokens.SPACE_10);
      proseCol.children(sections);
      var sidebar = aside().attr("class", "doc-toc-rail").child(fromContext(tocId, tocHeading));
      return div().child(div().attr("class", "doc-grid").children(proseCol, sidebar));
    }
  }
}
