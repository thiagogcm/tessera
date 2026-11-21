package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.foundation.Decimals;
import java.util.List;

/// The [GridTrack] implementations, built only through its static factories and [TrackList].
final class GridTracks {

  private GridTracks() {}

  /// A flexible track, `<n>fr`.
  record Fr(double fraction) implements GridTrack {
    @Override
    public String toCss() {
      return Decimals.format(fraction) + "fr";
    }
  }

  /// A fixed track of a length.
  record Size(CssLength length) implements GridTrack {
    @Override
    public String toCss() {
      return length.toCss();
    }
  }

  /// Intrinsic sizing keywords, or `subgrid` (inherits the parent's tracks).
  enum Keyword implements GridTrack, CssKeyword {
    AUTO,
    MIN_CONTENT,
    MAX_CONTENT,
    SUBGRID
  }

  /// `minmax(min, max)`.
  record MinMax(GridTrack min, GridTrack max) implements GridTrack {
    @Override
    public String toCss() {
      return "minmax(" + min.toCss() + ", " + max.toCss() + ")";
    }
  }

  /// `repeat(count, tracks)`.
  record Repeat(int count, List<GridTrack> tracks) implements GridTrack {
    Repeat {
      if (count < 1) {
        throw new IllegalArgumentException("Repeat count must be positive: " + count);
      }
    }

    @Override
    public String toCss() {
      return "repeat(" + count + ", " + TrackList.joined(tracks) + ")";
    }
  }

  /// `repeat(auto-fit, tracks)`: as many repetitions as fit.
  record AutoFit(List<GridTrack> tracks) implements GridTrack {
    @Override
    public String toCss() {
      return "repeat(auto-fit, " + TrackList.joined(tracks) + ")";
    }
  }
}
