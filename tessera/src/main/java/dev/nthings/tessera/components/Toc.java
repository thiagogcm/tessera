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

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.GridPlacement;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.TrackList;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Transition;
import java.util.Arrays;
import java.util.List;

/// Table of contents and section layout for documents. [#section] and [#subsection] create
/// headings (`<h2>` and `<h3>`) that the page registers. [#fromContext] lists every registered
/// heading on the page, and it may appear before the sections it lists. Use
/// [#docLayout] instead when the table of contents should sit beside the sections in a
/// two-column layout.
public final class Toc {

  private static final String CHEVRON_SVG =
      "<svg class=\"toc-chevron\" width=\"14\" height=\"14\" viewBox=\"0 0 24 24\" fill=\"none\""
          + " stroke=\"currentColor\" stroke-width=\"2.2\" stroke-linecap=\"round\""
          + " stroke-linejoin=\"round\" aria-hidden=\"true\"><polyline points=\"9 18 15 12 9"
          + " 6\"/></svg>";

  private Toc() {}

  /// Section whose heading is registered with the page, so [Toc#fromContext(String, String)]
  /// lists it. `id` is the section's page-level DOM id ([ElementRef#page(String)]), so
  /// `/guide/#installation` links to it from anywhere. `level` is the heading level, from 2 to 4.
  /// Throws IllegalArgumentException if `level` is outside that range.
  public record DocSection(String id, String title, int level, List<Element> body)
      implements Component {

    public DocSection {
      ContentSection.checkHeadingLevel(level);
    }

    @Override
    public Element render(RenderContext context) {
      var target = ElementRef.page(id);
      context.registerHeading(target, title, level);
      var heading = ContentSection.sectionHeading(level, title);

      var sectionEl =
          Elements.section()
              .id(target)
              .scrollMarginBlockStart(CssLength.rem(7))
              .flex()
              .flexCol()
              .gap(Tokens.SPACE_3)
              .child(heading);
      sectionEl.children(body);
      return sectionEl;
    }
  }

  /// Creates a level-2 section. `id` is the section's page-level DOM id, which [#fromContext]
  /// links to; two sections with one id on a page report `duplicate-id`.
  public static DocSection section(String id, String title, IntoElement... children) {
    return createSection(id, title, 2, children);
  }

  /// Creates a level-3 subsection, listed indented under the section before it.
  public static DocSection subsection(String id, String title, IntoElement... children) {
    return createSection(id, title, 3, children);
  }

  private static DocSection createSection(
      String id, String title, int level, IntoElement... children) {
    return new DocSection(id, title, level, elements(children));
  }

  /// Creates a table of contents that lists every heading registered on the page, including
  /// [ContentSection] headings. It is resolved after layout, so it may precede the sections.
  /// `heading` titles the `<nav>`.
  public static Element fromContext(String id, String heading) {
    return Elements.late(context -> new TableOfContents(id, heading, context.headings()));
  }

  /// Creates a table of contents with the given headings, in order, instead of the headings
  /// registered on the page.
  public static TableOfContents of(String id, String heading, List<DocumentHeading> explicitItems) {
    return new TableOfContents(id, heading, explicitItems);
  }

  /// Creates a two-column layout: `sections` on the left, and on the right a sticky table of
  /// contents listing every heading on the page. In a container narrower than 48rem, the table
  /// of contents stacks above the sections. `tocId` names the layout and its table of contents. The
  /// sections keep their page-level ids, so links to them do not depend on the layout.
  public static DocLayout docLayout(String tocId, String tocHeading, IntoElement... sections) {
    return new DocLayout(tocId, tocHeading, elements(sections));
  }

  private static List<Element> elements(IntoElement... children) {
    return Arrays.stream(children).map(IntoElement::intoElement).toList();
  }

  /// Collapsible `<nav>` of links to headings, with the link for the section in view
  /// highlighted. Create one with [Toc#fromContext(String, String)] or
  /// [Toc#of(String, String, List)].
  public static final class TableOfContents implements View {

    private final String id;
    private final String heading;
    private final List<DocumentHeading> items;

    /// Creates a table of contents `id` titled `heading` that links to `items` in order.
    public TableOfContents(String id, String heading, List<DocumentHeading> items) {
      this.id = id;
      this.heading = heading;
      this.items = List.copyOf(items);
    }

    @Override
    public String viewId() {
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
                      .bg(CssColor.transparent())
                      .border(Border.none()))
          .select(
              ":scope .toc-summary",
              s ->
                  s.flex()
                      .itemsCenter()
                      .justifyBetween()
                      .py(CssLength.rem(0.35))
                      .px(Tokens.SPACE_1)
                      .listStyleNone()
                      .cursor(Cursor.POINTER)
                      .textColor(Tokens.TEXT_MUTED)
                      .fontSize(Tokens.FONT_SIZE_XS)
                      .fontWeight(700))
          .select(":scope .toc-summary:hover", s -> s.textColor(Tokens.TEXT_PRIMARY))
          .select(
              ":scope .toc-chevron",
              s ->
                  s.transition(
                      Transition.of(CssDuration.ms(200), Easing.ease(), Animatable.TRANSFORM)))
          .select(":scope details:open .toc-chevron", s -> s.transform(Transform.rotate(90)))
          .select(
              ":scope details::details-content",
              s ->
                  s.transition(
                      Transition.of(
                              CssDuration.ms(300),
                              Easing.ease(),
                              Animatable.BLOCK_SIZE,
                              Animatable.CONTENT_VISIBILITY)
                          .allowingDiscrete()))
          .select(
              ":scope .toc-collapsible-wrapper",
              s ->
                  s.scrollTargetGroupAuto()
                      .borderInlineStart(Border.hairline(Tokens.BORDER_SUBTLE))
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
                      .transition(
                          Transition.controlFeedback()
                              .properties(Animatable.COLOR, Animatable.BACKGROUND)))
          .select(
              ":scope a[data-level=\"3\"]", s -> s.ps(Tokens.SPACE_6).fontSize(Tokens.FONT_SIZE_XS))
          .select(":scope a:hover", s -> s.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
          .select(
              ":scope a:target-current",
              s ->
                  s.textColor(Tokens.ACCENT_TEXT)
                      .fontWeight(600)
                      .borderInlineStart(Border.solid(CssLength.px(3), Tokens.ACCENT_PRIMARY))
                      .bg(Tone.ACCENT.color().mixTransparent(24)));
    }

    @Override
    public Element render(RenderContext context) {
      var list = ol();
      for (var item : items) {
        list.child(
            li().child(
                    a(item.target())
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

  /// Two-column layout of prose sections and a sticky table of contents. Create one with
  /// [Toc#docLayout(String, String, IntoElement...)].
  public record DocLayout(String tocId, String tocHeading, List<Element> sections) implements View {

    private static final Container LAYOUT = Container.unnamed(Container.Type.INLINE_SIZE);

    @Override
    public String viewId() {
      return tocId + "-layout";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.flex().flexCol().gap(Tokens.SPACE_8).container(LAYOUT))
          .select(
              ":scope > .doc-grid",
              s -> s.grid().gridColumns(TrackList.of(GridTrack.flexible())).gap(Tokens.SPACE_6))
          .select(
              ":scope > .doc-grid > aside",
              s ->
                  s.gridRow(GridPlacement.at(1))
                      .bg(Tokens.SURFACE_CARD)
                      .border(Border.hairline(Tokens.BORDER_SUBTLE))
                      .rounded(Tokens.RADIUS_LG)
                      .p(Tokens.SPACE_3))
          .select(
              ":scope > .doc-grid > .doc-prose-col",
              s ->
                  s.gridRow(GridPlacement.at(2))
                      .flex()
                      .flexCol()
                      .gap(Tokens.SPACE_10)
                      .inlineSize(CssLength.pct(100))
                      .minInlineSize(CssLength.px(0))
                      .maxInlineSize(CssLength.ch(75))
                      .textColor(Tokens.TEXT_PRIMARY)
                      .lineHeight(1.75))
          .select(
              ":scope .doc-prose-col p:not([class])",
              s ->
                  s.mb(CssLength.rem(1.125))
                      .fontSize(Tokens.FONT_SIZE_MD)
                      .textColor(Tokens.TEXT_PRIMARY)
                      .lineHeight(1.75))
          .on(
              LAYOUT.atLeast(CssLength.rem(48)),
              c ->
                  c.select(
                          ":scope > .doc-grid",
                          s ->
                              s.gridColumns(
                                      TrackList.of(
                                          GridTrack.flexible(),
                                          GridTrack.length(CssLength.rem(15))))
                                  .itemsStart()
                                  .gap(Tokens.SPACE_10))
                      .select(
                          ":scope > .doc-grid > .doc-prose-col",
                          s ->
                              s.gridColumn(GridPlacement.at(1))
                                  .gridRow(GridPlacement.at(1))
                                  .inlineSize(CssLength.pct(100))
                                  .minInlineSize(CssLength.px(0))
                                  .maxInlineSize(CssLength.ch(75)))
                      .select(
                          ":scope > .doc-grid > aside",
                          s ->
                              s.gridColumn(GridPlacement.at(2))
                                  .gridRow(GridPlacement.at(1))
                                  .sticky()
                                  .insetBlockStart(CssLength.rem(6.75))
                                  .bg(CssColor.transparent())
                                  .border(Border.none())
                                  .p(CssLength.px(0))));
    }

    @Override
    public Element render(RenderContext context) {
      var proseCol = div().attr("class", "doc-prose-col").flex().flexCol().gap(Tokens.SPACE_10);
      proseCol.children(sections);
      var sidebar = aside().attr("class", "doc-toc-rail").child(fromContext(tocId, tocHeading));
      return div().child(div().attr("class", "doc-grid").children(proseCol, sidebar));
    }
  }
}
