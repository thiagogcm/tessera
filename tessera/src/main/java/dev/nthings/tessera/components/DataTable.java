package dev.nthings.tessera.components;

import dev.nthings.tessera.components.TableGrid.Anchor;
import dev.nthings.tessera.components.TableGrid.Kind;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// A captioned [TableGrid] in a labelled, keyboard-scrollable region. Cells emit native table
/// markup with resolved spans and symbolic header references. Create one with [#of].
///
/// Scrolling is the default narrow layout. [#stackBelow] enables labelled cards for flat grids.
/// Sticky head groups and the first row-header column are opt-in. All generated ids are local to
/// the table's [#id(String)]; unnamed instances use `data-table-<n>`.
public final class DataTable extends BuiltinScope<DataTable> {

  private static final Container TABLE = Container.unnamed(Container.Type.INLINE_SIZE);
  private static final String CELLS =
      ":scope > table > :is(thead, tbody, tfoot) > tr > :is(th, td)";
  private static final String BODY_ROWS = ":scope > table > tbody > tr";
  private static final String STICKY_COLUMN =
      ":scope[data-sticky-column] > table > :is(tbody, tfoot) > tr > [data-row-header][data-column=\"0\"]";

  private final String caption;
  private final TableGrid grid;
  private @Nullable CssLength stackBelow;
  private boolean stickyHead;
  private boolean stickyFirstColumn;

  private DataTable(String caption, TableGrid grid) {
    this.caption = caption;
    this.grid = grid;
  }

  public static DataTable of(String caption, TableGrid grid) {
    return new DataTable(caption, grid);
  }

  /// Makes flat body rows into cards below `width`. Requires one head row of column headers, no
  /// spans, and only data or row-header cells in body groups. Other grids throw
  /// IllegalArgumentException. Rich headers supply their card label through
  /// [TableGrid#columnHeader(String, IntoElement)].
  public DataTable stackBelow(CssLength width) {
    if (!grid.supportsCards()) {
      throw new IllegalArgumentException(
          "Stacked cards require a flat grid with one column-header row");
    }
    stackBelow = width;
    return this;
  }

  /// Sticks the entire head group, including all its rows, to the scroll region's top edge.
  public DataTable stickyHead() {
    stickyHead = true;
    return this;
  }

  /// Sticks row headers starting in logical column zero to the scroll region's start edge. Other
  /// row-header columns scroll normally.
  public DataTable stickyFirstColumn() {
    stickyFirstColumn = true;
    return this;
  }

  @Override
  public DataTable self() {
    return this;
  }

  @Override
  String scopeName() {
    return "data-table";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s ->
            s.overflow(Overflow.AUTO)
                .minInlineSize(CssLength.px(0))
                .maxInlineSize(CssLength.pct(100))
                .container(TABLE)
                .border(Border.hairline(Tokens.BORDER_SUBTLE))
                .rounded(Tokens.RADIUS_XL)
                .bg(Tokens.SURFACE_CARD))
        .select(
            ":scope > table",
            s -> s.inlineSize(CssLength.pct(100)).borderSeparateZero().textAlign(TextAlign.START))
        .select(
            ":scope > table > caption",
            s ->
                s.px(Tokens.SPACE_4)
                    .py(Tokens.SPACE_3_5)
                    .fontWeight(600)
                    .textAlign(TextAlign.START)
                    .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE)))
        .select(
            CELLS,
            s ->
                s.px(Tokens.SPACE_4)
                    .py(Tokens.SPACE_3)
                    .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE)))
        .select(":scope > table > thead", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(":scope > table > thead > tr > th", s -> s.textColor(Tokens.TEXT_MUTED))
        .select(CELLS + "[data-align=center]", s -> s.textAlign(TextAlign.CENTER))
        .select(
            CELLS + "[data-align=end], " + CELLS + "[data-align=numeric]",
            s -> s.textAlign(TextAlign.END))
        .select(CELLS + "[data-align=numeric]", s -> s.tabularNums())
        .select(CELLS + " > .table-cell-label", s -> s.hidden())
        .select(
            ":scope[data-sticky-head] > table > thead",
            s -> s.sticky().insetBlockStart(CssLength.px(0)).zIndex(2))
        .select(
            STICKY_COLUMN,
            s -> s.sticky().insetInlineStart(CssLength.px(0)).zIndex(1).bg(Tokens.SURFACE_CARD));

    var breakpoint = stackBelow;
    if (breakpoint != null) {
      scope.on(
          TABLE.below(breakpoint),
          c ->
              c.select(
                      ":scope > table, :scope > table > caption, :scope > table > tbody, "
                          + BODY_ROWS,
                      s -> s.block())
                  .select(":scope > table > thead", s -> s.hidden())
                  .select(
                      BODY_ROWS,
                      s ->
                          s.m(Tokens.SPACE_3)
                              .border(Border.hairline(Tokens.BORDER_SUBTLE))
                              .rounded(Tokens.RADIUS_LG))
                  .select(CELLS + "[data-align]", s -> s.block().textAlign(TextAlign.START))
                  .select(STICKY_COLUMN, s -> s.relative())
                  .select(
                      CELLS + " > .table-cell-label",
                      s -> s.inlineBlock().fontWeight(600).textColor(Tokens.TEXT_MUTED)));
    }
  }

  @Override
  IntoElement expand(DocumentContext context) {
    var captionRef = ElementRef.scoped("caption");
    var references = new HashMap<Anchor, ElementRef>();
    var table = Elements.table().child(Elements.caption().id(captionRef).text(caption));
    for (var group : grid.groups()) {
      var section =
          switch (group.kind()) {
            case HEAD -> Elements.thead();
            case BODY -> Elements.tbody();
            case FOOT -> Elements.tfoot();
          };
      for (var row : group.rows()) {
        var tr = Elements.tr();
        for (var cell : row) {
          tr.child(renderCell(cell, references));
        }
        section.child(tr);
      }
      table.child(section);
    }
    return Elements.div()
        .tabIndex(0)
        .role(Role.REGION)
        .ariaLabelledBy(captionRef)
        .onlyIf(stickyHead, el -> el.boolAttr("data-sticky-head"))
        .onlyIf(stickyFirstColumn, el -> el.boolAttr("data-sticky-column"))
        .child(table);
  }

  private TagElement renderCell(Anchor anchor, Map<Anchor, ElementRef> references) {
    var cell = anchor.cell();
    var tag = cell.kind() == Kind.DATA ? Elements.td() : Elements.th();
    tag.attr("data-column", Integer.toString(anchor.column()));
    tag.attr(
        "data-align",
        switch (cell.align()) {
          case START -> "start";
          case CENTER -> "center";
          case END -> "end";
          case NUMERIC -> "numeric";
        });
    if (anchor.rowSpan() > 1) {
      tag.attr("rowspan", Integer.toString(anchor.rowSpan()));
    }
    if (anchor.columnSpan() > 1) {
      tag.attr("colspan", Integer.toString(anchor.columnSpan()));
    }
    if (cell.kind() != Kind.DATA) {
      tag.id(references.computeIfAbsent(anchor, DataTable::headerReference));
    }
    switch (cell.kind()) {
      case COLUMN_HEADER -> {
        if (anchor.columnSpan() == 1) {
          tag.attr("scope", "col");
        }
      }
      case ROW_HEADER -> {
        tag.boolAttr("data-row-header");
        if (anchor.rowSpan() == 1) {
          tag.attr("scope", "row");
        }
      }
      case SECTION_HEADER -> tag.attr("scope", "rowgroup");
      case DATA, CORNER_HEADER -> {}
    }
    var headers =
        grid.headers(anchor).stream()
            .map(header -> references.computeIfAbsent(header, DataTable::headerReference))
            .toArray(ElementRef[]::new);
    if (headers.length > 0) {
      tag.idRef("headers", headers);
    }
    if (stackBelow != null && cell.kind() == Kind.DATA) {
      var label = grid.groups().getFirst().rows().getFirst().get(anchor.column()).cell().label();
      tag.child(
          Elements.span().attr("class", "table-cell-label").ariaHidden(true).text(label + ": "));
    }
    return tag.child(cell.content());
  }

  private static ElementRef headerReference(Anchor cell) {
    return ElementRef.scoped("cell-" + cell.row() + "-" + cell.column());
  }
}
