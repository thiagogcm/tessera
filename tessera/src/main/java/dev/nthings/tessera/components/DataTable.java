package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CustomProperty;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import java.util.List;
import java.util.function.Function;

/// Table of `rows` described by `columns`, with `captionText` as its caption. The table sits in a
/// focusable region named by the caption, which scrolls when the table is too wide. Create one with
/// `new DataTable<>(id, captionText, columns, rows)`. `T` is the row model type.
///
/// - Column headers stay at the top and row headers stay at the left while the region scrolls.
/// - Narrower than `stackBreakpoint` (default 36rem, measured on the region), each row becomes a
///   stacked card: the header row is hidden, and each cell that is not a row header is prefixed
///   with its column header.
/// - Ids it binds are prefixed `datatable-<id>`, for example `datatable-<id>--caption`.
public record DataTable<T>(
    String id, String captionText, List<Column<T>> columns, List<T> rows, CssLength stackBreakpoint)
    implements View {

  /// The scroll region; the stacked layout queries its width.
  private static final Container TABLE = Container.unnamed(Container.Type.INLINE_SIZE);

  /// Horizontal alignment of a column's cells. `NUMERIC` aligns to the end with tabular figures.
  public enum CellAlign {
    START(TextAlign.START, false),
    CENTER(TextAlign.CENTER, false),
    END(TextAlign.END, false),
    NUMERIC(TextAlign.END, true);

    private final TextAlign textAlign;
    private final boolean tabularNums;

    CellAlign(TextAlign textAlign, boolean tabularNums) {
      this.textAlign = textAlign;
      this.tabularNums = tabularNums;
    }

    /// Compiler-facing: the CSS text alignment for these cells.
    public TextAlign textAlign() {
      return textAlign;
    }

    /// Compiler-facing: whether these cells use tabular figures.
    public boolean tabularNums() {
      return tabularNums;
    }
  }

  /// One column: its `header` text, the `cellExtractor` that produces each row's cell, the `align`,
  /// and whether its cells are row headers (`th scope="row"`).
  public record Column<T>(
      String header,
      Function<T, ? extends IntoElement> cellExtractor,
      CellAlign align,
      boolean rowHeader) {

    /// Creates a start-aligned column whose cells are row headers.
    public static <T> Column<T> rowHeader(
        String header, Function<T, ? extends IntoElement> extractor) {
      return new Column<>(header, extractor, CellAlign.START, true);
    }

    /// Creates a start-aligned column of plain text cells.
    public static <T> Column<T> text(String header, Function<T, String> stringExtractor) {
      return new Column<>(
          header, item -> Elements.text(stringExtractor.apply(item)), CellAlign.START, false);
    }

    /// Creates a `NUMERIC` column of plain text cells.
    public static <T> Column<T> numeric(String header, Function<T, String> numericExtractor) {
      return new Column<>(
          header, item -> Elements.text(numericExtractor.apply(item)), CellAlign.NUMERIC, false);
    }

    /// Creates a column whose cells are the elements `extractor` returns, aligned by `align`.
    public static <T> Column<T> of(
        String header, CellAlign align, Function<T, ? extends IntoElement> extractor) {
      return new Column<>(header, extractor, align, false);
    }
  }

  /// Creates a table that stacks below the default 36rem breakpoint.
  public DataTable(String id, String captionText, List<Column<T>> columns, List<T> rows) {
    this(id, captionText, columns, rows, CssLength.rem(36));
  }

  @Override
  public String viewId() {
    return "datatable-" + id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.overflow(Overflow.AUTO)
                    .maxInlineSize(CssLength.pct(100))
                    .container(TABLE)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .rounded(Tokens.RADIUS_XL)
                    .bg(Tokens.SURFACE_CARD))
        .select(
            ":scope table",
            s -> {
              s.inlineSize(CssLength.pct(100)).borderSeparateZero().textAlign(TextAlign.START);
              for (var i = 0; i < columns.size(); i++) {
                s.customProperty(labelProperty(i + 1), CssContent.text(columns.get(i).header()));
              }
            })
        .select(
            ":scope caption",
            s ->
                s.px(Tokens.SPACE_4)
                    .py(Tokens.SPACE_3_5)
                    .fontWeight(600)
                    .textAlign(TextAlign.START)
                    .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE)))
        .select(
            ":scope thead",
            s -> s.sticky().insetBlockStart(CssLength.px(0)).zIndex(2).bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope th, :scope td",
            s ->
                s.px(Tokens.SPACE_4)
                    .py(Tokens.SPACE_3)
                    .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE)))
        .select(
            ":scope tbody th",
            s ->
                s.sticky()
                    .insetInlineStart(CssLength.px(0))
                    .zIndex(1)
                    .bg(Tokens.SURFACE_CARD)
                    .fontWeight(600))
        .select(":scope thead th", s -> s.textColor(Tokens.TEXT_MUTED));

    // Align per column, not per cell. START is inherited from the table.
    for (var i = 0; i < columns.size(); i++) {
      var align = columns.get(i).align();
      if (align != CellAlign.START) {
        scope.select(
            ":scope tr > :nth-child(" + (i + 1) + ")",
            s ->
                s.textAlign(align.textAlign())
                    .when(align.tabularNums(), StyleRefinement::tabularNums));
      }
    }

    scope.on(
        TABLE.below(stackBreakpoint),
        c -> {
          c.select(
                  ":scope table, :scope caption, :scope thead, :scope tbody, :scope tr, :scope th, :scope td",
                  s -> s.block())
              .select(":scope thead", s -> s.hidden())
              // Card mode: label and value read left to right whatever the alignment.
              .select(":scope tr > :nth-child(n)", s -> s.textAlign(TextAlign.START))
              .select(
                  ":scope tr",
                  s ->
                      s.m(Tokens.SPACE_3)
                          .border(Border.hairline(Tokens.BORDER_SUBTLE))
                          .rounded(Tokens.RADIUS_LG))
              .select(
                  ":scope tbody th",
                  s ->
                      s.sticky()
                          .insetBlockStart(CssLength.px(0))
                          .zIndex(2)
                          .bg(Tokens.SURFACE_SUBTLE));

          for (var i = 0; i < columns.size(); i++) {
            if (!columns.get(i).rowHeader()) {
              var colIndex = i + 1;
              c.select(
                  ":scope td:nth-child(" + colIndex + ")::before",
                  s ->
                      s.content(
                              CssContent.sequence(
                                  CssContent.property(labelProperty(colIndex)),
                                  CssContent.text(": ")))
                          .fontWeight(600)
                          .textColor(Tokens.TEXT_MUTED));
            }
          }
        });
  }

  @Override
  public Element render(RenderContext context) {
    var caption = ElementRef.scoped("caption");

    var headerRow = Elements.tr();
    for (var col : columns) {
      headerRow.child(Elements.th().attr("scope", "col").text(col.header()));
    }

    var tbody = Elements.tbody();
    for (var item : rows) {
      var tr = Elements.tr();
      for (var col : columns) {
        var cell = col.rowHeader() ? Elements.th().attr("scope", "row") : Elements.td();
        tr.child(cell.child(col.cellExtractor().apply(item)));
      }
      tbody.child(tr);
    }

    var table =
        Elements.table()
            .children(
                Elements.caption().id(caption).text(captionText),
                Elements.thead().child(headerRow),
                tbody);

    return Elements.div().tabIndex(0).role(Role.REGION).ariaLabelledBy(caption).child(table);
  }

  private static CustomProperty labelProperty(int column) {
    return CustomProperty.named("label-" + column);
  }
}
