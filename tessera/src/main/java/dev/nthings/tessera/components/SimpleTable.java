package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Simple data table with an optional caption and a header row. Create one with [#of()], then
/// add headers and rows. Header cells get `scope="col"`. The table does not check that each row
/// has as many cells as there are headers.
public final class SimpleTable extends BuiltinComponent {

  private @Nullable String caption;
  private final List<String> headers = new ArrayList<>();
  private final List<List<Element>> rows = new ArrayList<>();

  private SimpleTable() {}

  /// Creates an empty table.
  public static SimpleTable of() {
    return new SimpleTable();
  }

  /// Sets the caption shown above the table, which also names it.
  public SimpleTable caption(String caption) {
    this.caption = caption;
    return this;
  }

  /// Adds column headers, in order.
  public SimpleTable headers(String... columnHeaders) {
    this.headers.addAll(List.of(columnHeaders));
    return this;
  }

  /// Adds a row of plain text cells.
  public SimpleTable row(String... cellTexts) {
    var cells = new ArrayList<Element>(cellTexts.length);
    for (var text : cellTexts) {
      cells.add(Elements.text(text));
    }
    this.rows.add(cells);
    return this;
  }

  /// Adds a row of cells, each of which may contain markup.
  public SimpleTable row(IntoElement... cells) {
    var rowCells = new ArrayList<Element>(cells.length);
    for (var cell : cells) {
      rowCells.add(cell.intoElement());
    }
    this.rows.add(rowCells);
    return this;
  }

  @Override
  IntoElement expand(DocumentContext context) {
    var table =
        Elements.table()
            .inlineSize(CssLength.pct(100))
            .maxInlineSize(CssLength.pct(100))
            .border(Border.hairline(Tokens.BORDER_SUBTLE))
            .rounded(Tokens.RADIUS_LG);

    if (caption != null) {
      table.child(
          Elements.caption()
              .py(Tokens.SPACE_2)
              .fontWeight(600)
              .textAlign(TextAlign.START)
              .text(caption));
    }

    if (!headers.isEmpty()) {
      var headRow = Elements.tr().bg(Tokens.SURFACE_SUBTLE);
      for (var h : headers) {
        headRow.child(
            Elements.th()
                .attr("scope", "col")
                .px(Tokens.SPACE_3)
                .py(Tokens.SPACE_2)
                .textAlign(TextAlign.START)
                .text(h));
      }
      table.child(Elements.thead().child(headRow));
    }

    var tbody = Elements.tbody();
    for (var row : rows) {
      var tr = Elements.tr();
      for (var cell : row) {
        tr.child(Elements.td().px(Tokens.SPACE_3).py(Tokens.SPACE_2).child(cell));
      }
      tbody.child(tr);
    }
    table.child(tbody);

    return table;
  }
}
