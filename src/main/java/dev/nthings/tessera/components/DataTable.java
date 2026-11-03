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
import dev.nthings.tessera.css.Tokens;
import java.util.List;
import java.util.function.Function;
import org.owasp.encoder.Encode;

/**
 * Strongly typed, responsive data table component ({@code DataTable<T>}).
 *
 * <ul>
 *   <li>Wraps the semantic {@code <table>} in a keyboard-scrollable {@code <div tabindex="0"
 *       role="region" aria-labelledby="...">} container with {@code container-type: inline-size}.
 *   <li>Emits sticky column headers ({@code thead}) and sticky row headers ({@code tbody th}).
 *   <li>Transforms into a mobile-friendly stacked card layout inside narrow containers ({@code
 *       @container (inline-size < breakpoint)}) by hiding {@code thead} and injecting {@code
 *       --label-N} CSS variables into {@code td:nth-child(N)::before}.
 * </ul>
 *
 * @param <T> row model type
 */
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
  public String viewScopeRule() {
    var sb = new StringBuilder(1024);
    sb.append(
        ":scope{"
            + "overflow:auto;max-inline-size:100%;max-block-size:min(36rem,80dvh);"
            + "container-type:inline-size;border:1px solid var(--border-subtle);"
            + "border-radius:0.75rem;background:var(--surface-card)"
            + "}"
            + ":scope table{"
            + "inline-size:100%;border-collapse:separate;border-spacing:0;"
            + "text-align:start");

    for (int i = 0; i < columns.size(); i++) {
      sb.append(";--label-")
          .append(i + 1)
          .append(":\"")
          .append(Encode.forCssString(columns.get(i).header()))
          .append('"');
    }
    sb.append(
        "}"
            + ":scope caption{"
            + "padding:0.875rem 1rem;font-weight:600;text-align:start;"
            + "border-block-end:1px solid var(--border-subtle)"
            + "}"
            + ":scope thead{"
            + "position:sticky;inset-block-start:0;z-index:2;background:var(--surface-subtle)"
            + "}"
            + ":scope th,:scope td{"
            + "padding:0.75rem 1rem;border-block-end:1px solid var(--border-subtle)"
            + "}"
            + ":scope tbody th{"
            + "position:sticky;inset-inline-start:0;z-index:1;"
            + "background:var(--surface-card);font-weight:600"
            + "}"
            + "@container (inline-size < "
            + stackBreakpoint.toCss()
            + "){"
            + ":scope table,:scope thead,:scope tbody,:scope tr,:scope th,:scope td{display:block}"
            + ":scope thead{display:none}"
            + ":scope tr{margin:0.75rem;border:1px solid var(--border-subtle);border-radius:0.5rem}"
            + ":scope tbody th{position:sticky;inset-block-start:0;z-index:2;background:var(--surface-subtle)}");

    for (int i = 0; i < columns.size(); i++) {
      if (!columns.get(i).rowHeader()) {
        int colIndex = i + 1;
        sb.append(":scope td:nth-child(")
            .append(colIndex)
            .append(")::before{content:var(--label-")
            .append(colIndex)
            .append(") \": \";font-weight:600;color:var(--text-muted)}");
      }
    }
    sb.append('}');
    return sb.toString();
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
