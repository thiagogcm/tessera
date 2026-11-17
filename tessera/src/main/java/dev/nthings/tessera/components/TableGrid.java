package dev.nthings.tessera.components;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/// A rectangular table described by OTSL cells and continuation slots. Each row has one entry per
/// logical column: [Merge#LEFT] extends a cell horizontally, [Merge#UP] vertically, and
/// [Merge#BOTH] fills the interior of a cell spanning both axes. An empty cell is [#empty()].
///
/// Build rows with [#row], group them with [#head], [#body] and [#foot], then call [#of].
/// Construction resolves merges and rejects ragged rows, invalid continuations and nonrectangular
/// cells. Cells never span row groups. The resolved topology is immutable; authored element content
/// follows Tessera's usual mutable builder contract.
public final class TableGrid {

  public sealed interface Entry permits Cell, Merge {}

  public enum Merge implements Entry {
    LEFT,
    UP,
    BOTH
  }

  public enum Kind {
    DATA,
    COLUMN_HEADER,
    ROW_HEADER,
    CORNER_HEADER,
    SECTION_HEADER
  }

  public enum Align {
    START,
    CENTER,
    END,
    NUMERIC
  }

  public enum SectionKind {
    HEAD,
    BODY,
    FOOT
  }

  /// Content and semantics of one anchor. `label` supplies plain text for card-mode column labels;
  /// it is explicit when a column header contains rich markup.
  @CheckReturnValue
  public record Cell(Kind kind, Element content, String label, Align align) implements Entry {
    public Cell align(Align alignment) {
      return new Cell(kind, content, label, alignment);
    }
  }

  public record Row(List<Entry> entries) {
    public Row {
      entries = List.copyOf(entries);
    }
  }

  public record Section(SectionKind kind, List<Row> rows) {
    public Section {
      rows = List.copyOf(rows);
    }
  }

  /// One resolved cell, with zero-based global row and column coordinates. Continuation slots have
  /// no anchor of their own.
  public record Anchor(Cell cell, int row, int column, int rowSpan, int columnSpan, int group) {}

  /// Resolved rows contain only anchors starting in that row, in column order. A row entirely
  /// covered by cells from above has an empty anchor list.
  public record Group(SectionKind kind, List<List<Anchor>> rows) {
    public Group {
      rows = rows.stream().map(List::copyOf).toList();
    }
  }

  private final int columns;
  private final List<Group> groups;
  private final List<Anchor> cells;
  private final Map<ColumnKey, List<Anchor>> columnHeaders = new HashMap<>();
  private final Map<Integer, List<Anchor>> rowHeaders = new HashMap<>();
  private final Map<Integer, List<Anchor>> sectionHeaders = new HashMap<>();

  private TableGrid(int columns, List<Group> groups) {
    this.columns = columns;
    this.groups = List.copyOf(groups);
    cells = groups.stream().flatMap(g -> g.rows().stream()).flatMap(List::stream).toList();
    for (var anchor : cells) {
      switch (anchor.cell().kind()) {
        case COLUMN_HEADER -> {
          for (var c = anchor.column(); c < anchor.column() + anchor.columnSpan(); c++) {
            columnHeaders
                .computeIfAbsent(new ColumnKey(anchor.group(), c), _ -> new ArrayList<>())
                .add(anchor);
          }
        }
        case ROW_HEADER -> {
          for (var r = anchor.row(); r < anchor.row() + anchor.rowSpan(); r++) {
            rowHeaders.computeIfAbsent(r, _ -> new ArrayList<>()).add(anchor);
          }
        }
        case SECTION_HEADER ->
            sectionHeaders.computeIfAbsent(anchor.group(), _ -> new ArrayList<>()).add(anchor);
        case DATA, CORNER_HEADER -> {}
      }
    }
  }

  public static Cell data(String text) {
    return data(Elements.text(text));
  }

  public static Cell data(IntoElement content) {
    return cell(Kind.DATA, "", content);
  }

  public static Cell empty() {
    return data(Elements.empty());
  }

  public static Cell columnHeader(String label) {
    return columnHeader(label, Elements.text(label));
  }

  public static Cell columnHeader(String label, IntoElement content) {
    return cell(Kind.COLUMN_HEADER, label, content);
  }

  public static Cell rowHeader(String text) {
    return rowHeader(Elements.text(text));
  }

  public static Cell rowHeader(IntoElement content) {
    return cell(Kind.ROW_HEADER, "", content);
  }

  /// Header at the intersection of row and column headings; it does not label data cells.
  public static Cell cornerHeader(String text) {
    return cell(Kind.CORNER_HEADER, "", Elements.text(text));
  }

  /// Labels the cells in its body group. Use a separate [#body] for each section.
  public static Cell sectionHeader(String text) {
    return cell(Kind.SECTION_HEADER, "", Elements.text(text));
  }

  private static Cell cell(Kind kind, String label, IntoElement content) {
    return new Cell(kind, content.intoElement(), label, Align.START);
  }

  public static Row row(Entry... entries) {
    return new Row(List.of(entries));
  }

  public static Section head(Row... rows) {
    return new Section(SectionKind.HEAD, List.of(rows));
  }

  public static Section body(Row... rows) {
    return new Section(SectionKind.BODY, List.of(rows));
  }

  public static Section foot(Row... rows) {
    return new Section(SectionKind.FOOT, List.of(rows));
  }

  /// Resolves and validates the grid. Head and foot groups occur at most once, in that order around
  /// any body groups. Groups must contain rows, and rows must contain entries.
  public static TableGrid of(Section... sections) {
    var groups = new ArrayList<Group>();
    var columns = 0;
    var rowOffset = 0;
    var previous = -1;
    for (var section : sections) {
      var order = section.kind().ordinal();
      if (order < previous || (order == previous && section.kind() != SectionKind.BODY)) {
        throw new IllegalArgumentException("Table groups must be head, bodies, then foot");
      }
      previous = order;
      if (section.rows().isEmpty()) {
        throw new IllegalArgumentException("Table groups must contain rows");
      }
      if (columns == 0) {
        columns = section.rows().getFirst().entries().size();
      }
      if (columns == 0) {
        throw new IllegalArgumentException("Table rows must contain entries");
      }
      groups.add(resolve(section, columns, rowOffset, groups.size()));
      rowOffset += section.rows().size();
    }
    if (groups.isEmpty()) {
      throw new IllegalArgumentException("A table grid must contain a row group");
    }
    return new TableGrid(columns, groups);
  }

  public int columns() {
    return columns;
  }

  public List<Group> groups() {
    return groups;
  }

  public List<Anchor> cells() {
    return cells;
  }

  /// Headers associated with `target`, in reading order. Column headers label overlapping columns
  /// below them, row headers label overlapping rows to their right, and section headers label their
  /// body group. Column headers in a head group apply to subsequent groups.
  public List<Anchor> headers(Anchor target) {
    var candidates = new HashSet<Anchor>();
    for (var c = target.column(); c < target.column() + target.columnSpan(); c++) {
      candidates.addAll(columnHeaders.getOrDefault(new ColumnKey(target.group(), c), List.of()));
      if (target.group() > 0 && groups.getFirst().kind() == SectionKind.HEAD) {
        candidates.addAll(columnHeaders.getOrDefault(new ColumnKey(0, c), List.of()));
      }
    }
    for (var r = target.row(); r < target.row() + target.rowSpan(); r++) {
      candidates.addAll(rowHeaders.getOrDefault(r, List.of()));
    }
    candidates.addAll(sectionHeaders.getOrDefault(target.group(), List.of()));
    return candidates.stream()
        .filter(header -> header != target && labels(header, target))
        .sorted(Comparator.comparingInt(Anchor::row).thenComparingInt(Anchor::column))
        .toList();
  }

  private record ColumnKey(int group, int column) {}

  private boolean labels(Anchor header, Anchor target) {
    return switch (header.cell().kind()) {
      case COLUMN_HEADER ->
          (groups.get(header.group()).kind() == SectionKind.HEAD
                  || header.group() == target.group())
              && header.row() + header.rowSpan() <= target.row()
              && overlaps(
                  header.column(), header.columnSpan(), target.column(), target.columnSpan());
      case ROW_HEADER ->
          header.group() == target.group()
              && header.column() + header.columnSpan() <= target.column()
              && overlaps(header.row(), header.rowSpan(), target.row(), target.rowSpan());
      case SECTION_HEADER ->
          header.group() == target.group()
              && (target.cell().kind() == Kind.DATA || target.cell().kind() == Kind.ROW_HEADER);
      case DATA, CORNER_HEADER -> false;
    };
  }

  private static boolean overlaps(int start, int span, int otherStart, int otherSpan) {
    return start < otherStart + otherSpan && otherStart < start + span;
  }

  /// Whether rows can become independent cards with one plain-text label per data column.
  public boolean supportsCards() {
    if (groups.getFirst().kind() != SectionKind.HEAD || groups.getFirst().rows().size() != 1) {
      return false;
    }
    var headers = groups.getFirst().rows().getFirst();
    return cells.stream().allMatch(c -> c.rowSpan() == 1 && c.columnSpan() == 1)
        && headers.stream().allMatch(c -> c.cell().kind() == Kind.COLUMN_HEADER)
        && groups.stream().skip(1).allMatch(g -> g.kind() == SectionKind.BODY)
        && cells.stream()
            .filter(c -> c.group() > 0)
            .allMatch(c -> c.cell().kind() == Kind.DATA || c.cell().kind() == Kind.ROW_HEADER);
  }

  private static Group resolve(Section section, int columns, int offset, int group) {
    var owners = new int[section.rows().size()][columns];
    var cells = new ArrayList<Extent>();
    for (var r = 0; r < owners.length; r++) {
      var entries = section.rows().get(r).entries();
      if (entries.size() != columns) {
        throw new IllegalArgumentException(
            "Table row " + (offset + r) + " must have " + columns + " slots");
      }
      for (var c = 0; c < columns; c++) {
        switch (entries.get(c)) {
          case Cell cell -> {
            if (cell.kind() == Kind.SECTION_HEADER && section.kind() != SectionKind.BODY) {
              throw new IllegalArgumentException("Section headers belong in body groups");
            }
            owners[r][c] = cells.size();
            cells.add(new Extent(cell, r, c));
          }
          case Merge merge -> {
            var owner = mergeOwner(merge, owners, cells, r, c, offset);
            var extent = cells.get(owner);
            owners[r][c] = owner;
            extent.lastRow = Math.max(extent.lastRow, r);
            extent.lastColumn = Math.max(extent.lastColumn, c);
          }
        }
      }
    }
    var rows = new ArrayList<List<Anchor>>();
    for (var r = 0; r < owners.length; r++) {
      rows.add(new ArrayList<>());
    }
    for (var i = 0; i < cells.size(); i++) {
      var extent = cells.get(i);
      for (var r = extent.row; r <= extent.lastRow; r++) {
        for (var c = extent.column; c <= extent.lastColumn; c++) {
          if (owners[r][c] != i) {
            throw new IllegalArgumentException(
                "Nonrectangular table cell at row "
                    + (offset + extent.row)
                    + ", column "
                    + extent.column);
          }
        }
      }
      rows.get(extent.row)
          .add(
              new Anchor(
                  extent.cell,
                  offset + extent.row,
                  extent.column,
                  extent.lastRow - extent.row + 1,
                  extent.lastColumn - extent.column + 1,
                  group));
    }
    return new Group(section.kind(), rows);
  }

  private static int mergeOwner(
      Merge merge, int[][] owners, List<Extent> cells, int r, int c, int offset) {
    return switch (merge) {
      case LEFT -> {
        if (c == 0 || cells.get(owners[r][c - 1]).row != r) {
          throw invalidMerge(Merge.LEFT, offset + r, c);
        }
        yield owners[r][c - 1];
      }
      case UP -> {
        if (r == 0 || cells.get(owners[r - 1][c]).column != c) {
          throw invalidMerge(Merge.UP, offset + r, c);
        }
        yield owners[r - 1][c];
      }
      case BOTH -> {
        if (r == 0 || c == 0 || owners[r][c - 1] != owners[r - 1][c]) {
          throw invalidMerge(Merge.BOTH, offset + r, c);
        }
        var owner = owners[r][c - 1];
        var extent = cells.get(owner);
        if (extent.row == r || extent.column == c) {
          throw invalidMerge(Merge.BOTH, offset + r, c);
        }
        yield owner;
      }
    };
  }

  private static final class Extent {
    final Cell cell;
    final int row;
    final int column;
    int lastRow;
    int lastColumn;

    Extent(Cell cell, int row, int column) {
      this.cell = cell;
      this.row = row;
      this.column = column;
      lastRow = row;
      lastColumn = column;
    }
  }

  private static IllegalArgumentException invalidMerge(Merge merge, int row, int column) {
    return new IllegalArgumentException(
        "Invalid " + merge + " merge at row " + row + ", column " + column);
  }
}
