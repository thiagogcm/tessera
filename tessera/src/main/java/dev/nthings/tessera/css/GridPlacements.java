package dev.nthings.tessera.css;

/// The [GridPlacement] implementations, built only through its static factories.
final class GridPlacements {

  private GridPlacements() {}

  /// A grid line: 1-based, or negative to count from the end.
  record Line(int index) implements GridPlacement {
    Line {
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
    Span {
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
}
