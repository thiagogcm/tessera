package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.List;
import java.util.function.Function;
import org.owasp.encoder.Encode;

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
    START("start", false),
    CENTER("center", false),
    END("end", false),
    NUMERIC("end", true);

    private final String textAlign;
    private final boolean tabularNums;

    CellAlign(String textAlign, boolean tabularNums) {
      this.textAlign = textAlign;
      this.tabularNums = tabularNums;
    }

    public String textAlign() {
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
    return ElementId.of("datatable-" + Fnv1a64.sanitizeIdentifier(id));
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
                    .rounded(CssLength.rem(0.75))
                    .bg(Tokens.SURFACE_CARD))
        .select(
            ":scope table",
            s -> {
              s.fullWidth().borderSeparateZero().textAlign("start");
              for (int i = 0; i < columns.size(); i++) {
                s.customProperty(
                    "--label-" + (i + 1),
                    "\"" + Encode.forCssString(columns.get(i).header()) + "\"");
              }
              return s;
            })
        .select(
            ":scope caption",
            s ->
                s.px(CssLength.rem(1))
                    .py(CssLength.rem(0.875))
                    .fontWeight(600)
                    .textAlign("start")
                    .borderBlockEndSubtle(Tokens.BORDER_SUBTLE))
        .select(
            ":scope thead",
            s -> s.sticky().insetBlockStart(CssLength.px(0)).zIndex(2).bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope th, :scope td",
            s ->
                s.px(CssLength.rem(1))
                    .py(CssLength.rem(0.75))
                    .borderBlockEndSubtle(Tokens.BORDER_SUBTLE))
        .select(
            ":scope tbody th",
            s ->
                s.sticky()
                    .insetInlineStart(CssLength.px(0))
                    .zIndex(1)
                    .bg(Tokens.SURFACE_CARD)
                    .fontWeight(600))
        .containerMaxInlineSize(
            stackBreakpoint,
            c -> {
              c.select(
                      ":scope table, :scope caption, :scope thead, :scope tbody, :scope tr, :scope th, :scope td",
                      s -> s.block())
                  .select(":scope thead", s -> s.hidden())
                  .select(
                      ":scope tr",
                      s ->
                          s.m(CssLength.rem(0.75))
                              .borderSubtle(Tokens.BORDER_SUBTLE)
                              .rounded(CssLength.rem(0.5)))
                  .select(
                      ":scope tbody th",
                      s ->
                          s.sticky()
                              .insetBlockStart(CssLength.px(0))
                              .zIndex(2)
                              .bg(Tokens.SURFACE_SUBTLE));

              for (int i = 0; i < columns.size(); i++) {
                if (!columns.get(i).rowHeader()) {
                  int colIndex = i + 1;
                  c.select(
                      ":scope td:nth-child(" + colIndex + ")::before",
                      s ->
                          s.cssContent("var(--label-" + colIndex + ") \": \"")
                              .fontWeight(600)
                              .textColor(Tokens.TEXT_MUTED));
                }
              }
            });
  }

  @Override
  public Element render() {
    String captionDomId =
        RenderContext.current().viewScope().extend(ElementId.of("caption")).domId();

    var headerRow = Elements.tr();
    for (var col : columns) {
      headerRow.child(
          Elements.th()
              .attr("scope", "col")
              .textAlign(col.align().textAlign())
              .textColor(Tokens.TEXT_MUTED)
              .text(col.header()));
    }

    var tbody = Elements.tbody();
    for (var item : rows) {
      var tr = Elements.tr();
      for (var col : columns) {
        TagElement cell = col.rowHeader() ? Elements.th().attr("scope", "row") : Elements.td();
        cell.textAlign(col.align().textAlign())
            .when(col.align().tabularNums(), TagElement::tabularNums)
            .child(col.cellExtractor().apply(item));
        tr.child(cell);
      }
      tbody.child(tr);
    }

    var table =
        Elements.table()
            .children(
                Elements.caption().id("caption").text(captionText),
                Elements.thead().child(headerRow),
                tbody);

    return Elements.div().tabIndex(0).role("region").ariaLabelledBy(captionDomId).child(table);
  }
}
