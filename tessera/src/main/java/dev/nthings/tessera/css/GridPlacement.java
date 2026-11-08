package dev.nthings.tessera.css;

/// A `grid-row` or `grid-column` placement: a line, a span, or a range.
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

  static GridPlacement at(int line) {
    return new Line(line);
  }

  static GridPlacement span(int tracks) {
    return new Span(tracks);
  }

  static GridPlacement between(int startLine, int endLine) {
    return new Range(new Line(startLine), new Line(endLine));
  }

  static GridPlacement from(int startLine, int tracks) {
    return new Range(new Line(startLine), new Span(tracks));
  }
}
