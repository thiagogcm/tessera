package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import java.util.List;
import java.util.function.Function;

/// Strongly typed, responsive data table component (`DataTable<T>`).
///
/// - Wraps the semantic `<table>` in a keyboard-scrollable `<div tabindex="0" role="region"
///   aria-labelledby="...">` container with `container-type: inline-size`.
/// - Emits sticky column headers (`thead`) and sticky row headers (`tbody th`).
/// - Transforms into a mobile-friendly stacked card layout inside narrow containers (`@container
///   (inline-size < breakpoint)`) by hiding `thead` and injecting `--label-N` CSS variables into
///   `td:nth-child(N)::before`.
///
/// @param <T> row model type
public record DataTable<T>(
    String id, String captionText, List<Column<T>> columns, List<T> rows, CssLength stackBreakpoint)
    implements RenderView {

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

    public TextAlign textAlign() {
      return textAlign;
    }

    public boolean tabularNums() {
      return tabularNums;
    }
  }

  public record Column<T>(
      String header,
      Function<T, ? extends IntoElement> cellExtractor,
      CellAlign align,
      boolean rowHeader) {

    public static <T> Column<T> rowHeader(
        String header, Function<T, ? extends IntoElement> extractor) {
      return new Column<>(header, extractor, CellAlign.START, true);
    }

    public static <T> Column<T> text(String header, Function<T, String> stringExtractor) {
      return new Column<>(
          header, item -> Elements.text(stringExtractor.apply(item)), CellAlign.START, false);
    }

    public static <T> Column<T> numeric(String header, Function<T, String> numericExtractor) {
      return new Column<>(
          header, item -> Elements.text(numericExtractor.apply(item)), CellAlign.NUMERIC, false);
    }

    public static <T> Column<T> of(
        String header, CellAlign align, Function<T, ? extends IntoElement> extractor) {
      return new Column<>(header, extractor, align, false);
    }
  }

  public DataTable(String id, String captionText, List<Column<T>> columns, List<T> rows) {
    this(id, captionText, columns, rows, CssLength.rem(36));
  }

  @Override
  public ElementId entityId() {
    return ElementId.of("datatable-" + id);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.overflowAuto()
                    .maxInlineSize(CssLength.pct(100))
                    .containerInlineSize()
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .rounded(Tokens.RADIUS_XL)
                    .bg(Tokens.SURFACE_CARD))
        .select(
            ":scope table",
            s -> {
              s.fullWidth().borderSeparateZero().textAlign(TextAlign.START);
              for (var i = 0; i < columns.size(); i++) {
                s.customProperty(labelProperty(i + 1), CssContent.text(columns.get(i).header()));
              }
              return s;
            })
        .select(
            ":scope caption",
            s ->
                s.px(Tokens.SPACE_4)
                    .py(Tokens.SPACE_3_5)
                    .fontWeight(600)
                    .textAlign(TextAlign.START)
                    .borderBlockEndSubtle(Tokens.BORDER_SUBTLE))
        .select(
            ":scope thead",
            s -> s.sticky().insetBlockStart(CssLength.px(0)).zIndex(2).bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope th, :scope td",
            s -> s.px(Tokens.SPACE_4).py(Tokens.SPACE_3).borderBlockEndSubtle(Tokens.BORDER_SUBTLE))
        .select(
            ":scope tbody th",
            s ->
                s.sticky()
                    .insetInlineStart(CssLength.px(0))
                    .zIndex(1)
                    .bg(Tokens.SURFACE_CARD)
                    .fontWeight(600))
        .select(":scope thead th", s -> s.textColor(Tokens.TEXT_MUTED));

    // Column alignment once per column instead of a utility class on every cell; START is
    // inherited from the table.
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

    scope.containerMaxInlineSize(
        stackBreakpoint,
        c -> {
          c.select(
                  ":scope table, :scope caption, :scope thead, :scope tbody, :scope tr, :scope th, :scope td",
                  s -> s.block())
              .select(":scope thead", s -> s.hidden())
              // Card mode: label and value read left to right regardless of column alignment.
              .select(":scope tr > :nth-child(n)", s -> s.textAlign(TextAlign.START))
              .select(
                  ":scope tr",
                  s ->
                      s.m(Tokens.SPACE_3)
                          .borderSubtle(Tokens.BORDER_SUBTLE)
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
    var caption = ElementRef.named("caption");

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

  private static CssIdent labelProperty(int column) {
    return CssIdent.custom("label-" + column);
  }
}
