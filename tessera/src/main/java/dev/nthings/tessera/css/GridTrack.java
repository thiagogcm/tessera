package dev.nthings.tessera.css;

import java.util.List;

/// One entry of a [TrackList]. Build values with the static factories.
public sealed interface GridTrack extends CssValue
    permits GridTracks.Fr,
        GridTracks.Size,
        GridTracks.Keyword,
        GridTracks.MinMax,
        GridTracks.Repeat,
        GridTracks.AutoFit {

  /// `minmax(0, 1fr)`: an equal share that may shrink below its content.
  static GridTrack flexible() {
    return minmax(length(CssLength.zero()), fr(1));
  }

  /// A flexible track, `<fraction>fr`.
  static GridTrack fr(double fraction) {
    return new GridTracks.Fr(fraction);
  }

  /// A fixed track of `length`.
  static GridTrack length(CssLength length) {
    return new GridTracks.Size(length);
  }

  /// `auto`.
  static GridTrack auto() {
    return GridTracks.Keyword.AUTO;
  }

  /// `min-content`: the track's smallest content contribution.
  static GridTrack minContent() {
    return GridTracks.Keyword.MIN_CONTENT;
  }

  /// `max-content`: the track's largest content contribution.
  static GridTrack maxContent() {
    return GridTracks.Keyword.MAX_CONTENT;
  }

  /// `minmax(min, max)`.
  static GridTrack minmax(GridTrack min, GridTrack max) {
    return new GridTracks.MinMax(min, max);
  }

  /// `repeat(<count>, <tracks>)`. Throws IllegalArgumentException if `count` is below 1.
  static GridTrack repeat(int count, GridTrack... tracks) {
    return new GridTracks.Repeat(count, List.of(tracks));
  }
}
