package dev.nthings.tessera.css;

/// A `grid-row` or `grid-column` placement: a line, a span, or a range. Applied with
/// [Styled#gridColumn(GridPlacement)] or [Styled#gridRow(GridPlacement)]. Build values with the
/// static factories.
public sealed interface GridPlacement extends CssValue
    permits GridPlacements.Line, GridPlacements.Span, GridPlacements.Range {

  /// A single grid line, as `<line>`: 1-based, or negative to count from the end. Throws
  /// IllegalArgumentException if `line` is 0.
  static GridPlacement at(int line) {
    return new GridPlacements.Line(line);
  }

  /// `span <tracks>`. Throws IllegalArgumentException if `tracks` is below 1.
  static GridPlacement span(int tracks) {
    return new GridPlacements.Span(tracks);
  }

  /// Runs from line `startLine` to line `endLine`, as `startLine / endLine`. Throws
  /// IllegalArgumentException if either line is 0.
  static GridPlacement between(int startLine, int endLine) {
    return new GridPlacements.Range(at(startLine), at(endLine));
  }

  /// Starts at line `startLine` and covers `tracks` tracks, as `startLine / span tracks`. Throws
  /// IllegalArgumentException if `startLine` is 0 or `tracks` is below 1.
  static GridPlacement from(int startLine, int tracks) {
    return new GridPlacements.Range(at(startLine), span(tracks));
  }
}
