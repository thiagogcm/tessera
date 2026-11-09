package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.aside;
import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridPlacement;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import java.util.ArrayList;
import java.util.List;

/// Two-column documentation layout: sections on the left and, on the right, a sticky
/// [TableOfContents] listing every heading on the page. In a container narrower than 48rem, the
/// table of contents stacks above the sections. Create one with [#of(IntoElement...)] and add
/// more sections as children; they keep their page-level ids, so links to them do not depend on
/// the layout.
///
/// Without an [#id(String)], the compiler numbers instances `doc-layout-<n>`.
public final class DocLayout extends BuiltinScope<DocLayout> implements ParentElement<DocLayout> {

  private static final Container LAYOUT = Container.unnamed(Container.Type.INLINE_SIZE);

  private final List<Element> sections = new ArrayList<>();
  private String tocTitle = "On this page";

  private DocLayout() {}

  /// Creates a layout around `sections`.
  public static DocLayout of(IntoElement... sections) {
    return new DocLayout().children(sections);
  }

  @Override
  public DocLayout self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    sections.add(child);
  }

  /// Sets the table of contents title. The default is `On this page`.
  public DocLayout tocTitle(String title) {
    this.tocTitle = title;
    return this;
  }

  @Override
  String scopeName() {
    return "doc-layout";
  }

  @Override
  void configureScope(ScopeStyle scope) {
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
                                        GridTrack.flexible(), GridTrack.length(CssLength.rem(15))))
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
  IntoElement expand(DocumentContext context) {
    var proseCol = div().attr("class", "doc-prose-col").flex().flexCol().gap(Tokens.SPACE_10);
    proseCol.children(sections);
    var sidebar = aside().attr("class", "doc-toc-rail").child(TableOfContents.of(tocTitle));
    return div().child(div().attr("class", "doc-grid").children(proseCol, sidebar));
  }
}
