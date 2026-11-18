package dev.nthings.tessera.document.components;

import static dev.nthings.tessera.core.Elements.aside;
import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridPlacement;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Two-column documentation layout: sections on the left and, on the right, a sticky
/// [TableOfContents] listing every heading on the page. In a container narrower than 48rem, the
/// table of contents stacks above the sections. The table of contents is collected from the
/// document, so the layout is document content. Create one with [#of(DocumentContent...)] or
/// [#of(IntoElement...)] and add more sections as children; they keep their page-level ids, so
/// links to them do not depend on the layout.
///
/// Without an [#id(String)], the compiler numbers instances `doc-layout-<n>`.
public final class DocLayout extends BuiltinDocumentScope<DocLayout> {

  private static final Container LAYOUT = Container.unnamed(Container.Type.INLINE_SIZE);

  private final List<DocumentContent> sections = new ArrayList<>();
  private String tocTitle = "On this page";

  private DocLayout() {}

  /// Creates an empty layout.
  public static DocLayout of() {
    return new DocLayout();
  }

  /// Creates a layout around portable `sections`.
  public static DocLayout of(IntoElement... sections) {
    var layout = new DocLayout();
    for (var section : sections) {
      layout.child(section);
    }
    return layout;
  }

  /// Creates a layout around document `sections`.
  public static DocLayout of(DocumentContent... sections) {
    return new DocLayout().children(List.of(sections));
  }

  @Override
  public DocLayout self() {
    return this;
  }

  /// Adds portable `section` after the existing sections.
  public DocLayout child(IntoElement section) {
    sections.add(DocumentContent.of(section));
    return this;
  }

  /// Adds document `section` after the existing sections.
  public DocLayout child(DocumentContent section) {
    sections.add(section);
    return this;
  }

  /// Adds each document section in iteration order.
  public DocLayout children(Collection<? extends DocumentContent> sections) {
    this.sections.addAll(sections);
    return this;
  }

  /// Sets the table of contents title. The default is `On this page`.
  public DocLayout tocTitle(String title) {
    this.tocTitle = title;
    return this;
  }

  @Override
  public String scopeName() {
    return "doc-layout";
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
  public DocumentElement expand(DocumentContext context) {
    var proseCol =
        DocumentElement.of(
                div().attr("class", "doc-prose-col").flex().flexCol().gap(Tokens.SPACE_10))
            .children(sections);
    var sidebar =
        DocumentElement.of(aside().attr("class", "doc-toc-rail"))
            .child(TableOfContents.of(tocTitle));
    return DocumentElement.of(div())
        .child(DocumentElement.of(div().attr("class", "doc-grid")).children(proseCol, sidebar));
  }
}
