package dev.nthings.tessera.css;

import java.util.List;

/// One entry of a [TrackList].
public sealed interface GridTrack extends CssValue
    permits GridTrack.Fr,
        GridTrack.Size,
        GridTrack.Keyword,
        GridTrack.MinMax,
        GridTrack.Repeat,
        GridTrack.AutoRepeat {

  /// A flexible track, `<n>fr`.
  record Fr(double fraction) implements GridTrack {
    @Override
    public String toCss() {
      return CssLength.formatNumber(fraction) + "fr";
    }
  }

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
    public Repeat {
      if (count < 1) {
        throw new IllegalArgumentException("Repeat count must be positive: " + count);
      }
      tracks = List.copyOf(tracks);
    }

    @Override
    public String toCss() {
      return "repeat(" + count + ", " + TrackList.joined(tracks) + ")";
    }
  }

  /// How many tracks an [AutoRepeat] creates.
  enum AutoCount implements CssKeyword {
    AUTO_FIT,
    AUTO_FILL
  }

  /// `repeat(auto-fit | auto-fill, tracks)`.
  record AutoRepeat(AutoCount count, List<GridTrack> tracks) implements GridTrack {
    public AutoRepeat {
      tracks = List.copyOf(tracks);
    }

    @Override
    public String toCss() {
      return "repeat(" + count.toCss() + ", " + TrackList.joined(tracks) + ")";
    }
  }

  /// `minmax(0, 1fr)`: an equal share that may shrink below its content.
  static GridTrack flexible() {
    return minmax(length(CssLength.zero()), fr(1));
  }

  static GridTrack fr(double fraction) {
    return new Fr(fraction);
  }

  static GridTrack length(CssLength length) {
    return new Size(length);
  }

  static GridTrack minmax(GridTrack min, GridTrack max) {
    return new MinMax(min, max);
  }

  static GridTrack repeat(int count, GridTrack... tracks) {
    return new Repeat(count, List.of(tracks));
  }
}
