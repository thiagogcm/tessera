package dev.nthings.tessera.css;

/// A `grid-row` or `grid-column` placement: a line, a span, or a range. Applied with
/// [LayoutStyled#gridColumn(GridPlacement)] or [LayoutStyled#gridRow(GridPlacement)].
public sealed interface GridPlacement extends CssValue
    permits GridPlacement.Line, GridPlacement.Span, GridPlacement.Range {

  /// A grid line: 1-based, or negative to count from the end.
  record Line(int index) implements GridPlacement {
    public Line {
      if (index == 0) {
        throw new IllegalArgumentException("Grid lines are numbered from 1 or -1");
      }
    }

    @Override
    public String toCss() {
      return Integer.toString(index);
    }
  }

  /// `span <n>`.
  record Span(int tracks) implements GridPlacement {
    public Span {
      if (tracks < 1) {
        throw new IllegalArgumentException("A span covers at least one track: " + tracks);
      }
    }

    @Override
    public String toCss() {
      return "span " + tracks;
    }
  }

  /// `start / end`.
  record Range(GridPlacement start, GridPlacement end) implements GridPlacement {
    @Override
    public String toCss() {
      return start.toCss() + " / " + end.toCss();
    }
  }

  /// A single grid line, as `<line>`. Throws IllegalArgumentException if `line` is 0.
  static GridPlacement at(int line) {
    return new Line(line);
  }

  /// `span <tracks>`. Throws IllegalArgumentException if `tracks` is below 1.
  static GridPlacement span(int tracks) {
    return new Span(tracks);
  }

  /// Runs from line `startLine` to line `endLine`, as `startLine / endLine`. Throws
  /// IllegalArgumentException if either line is 0.
  static GridPlacement between(int startLine, int endLine) {
    return new Range(new Line(startLine), new Line(endLine));
  }

  /// Starts at line `startLine` and covers `tracks` tracks, as `startLine / span tracks`. Throws
  /// IllegalArgumentException if `startLine` is 0 or `tracks` is below 1.
  static GridPlacement from(int startLine, int tracks) {
    return new Range(new Line(startLine), new Span(tracks));
  }
}
