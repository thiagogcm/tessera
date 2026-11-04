package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Lightweight semantic `<table>` builder for ad-hoc tabular data.
public final class SimpleTable implements RenderOnce {

  private @Nullable String caption;
  private final List<String> headers = new ArrayList<>();
  private final List<List<Element>> rows = new ArrayList<>();

  public static SimpleTable create() {
    return new SimpleTable();
  }

  public SimpleTable caption(String captionText) {
    this.caption = captionText;
    return this;
  }

  public SimpleTable headers(String... columnHeaders) {
    this.headers.addAll(List.of(columnHeaders));
    return this;
  }

  public SimpleTable row(String... cellTexts) {
    var cells = new ArrayList<Element>(cellTexts.length);
    for (var text : cellTexts) {
      cells.add(Elements.text(text));
    }
    this.rows.add(cells);
    return this;
  }

  public SimpleTable row(IntoElement... cells) {
    var rowCells = new ArrayList<Element>(cells.length);
    for (var cell : cells) {
      rowCells.add(cell.intoElement());
    }
    this.rows.add(rowCells);
    return this;
  }

  @Override
  public Element render() {
    TagElement table =
        Elements.table()
            .fullWidth()
            .maxInlineSize(CssLength.pct(100))
            .borderSubtle(Tokens.BORDER_SUBTLE)
            .rounded(CssLength.rem(0.5));

    if (caption != null) {
      table.child(
          Elements.caption()
              .py(CssLength.rem(0.5))
              .fontWeight(600)
              .textAlign("start")
              .text(caption));
    }

    if (!headers.isEmpty()) {
      var headRow = Elements.tr().bg(Tokens.SURFACE_SUBTLE);
      for (var h : headers) {
        headRow.child(
            Elements.th()
                .attr("scope", "col")
                .px(CssLength.rem(0.75))
                .py(CssLength.rem(0.5))
                .textAlign("start")
                .text(h));
      }
      table.child(Elements.thead().child(headRow));
    }

    var tbody = Elements.tbody();
    for (var row : rows) {
      var tr = Elements.tr();
      for (var cell : row) {
        tr.child(Elements.td().px(CssLength.rem(0.75)).py(CssLength.rem(0.5)).child(cell));
      }
      tbody.child(tr);
    }
    table.child(tbody);

    return table;
  }
}
