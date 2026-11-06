package dev.nthings.tessera.css;

import java.util.List;
import java.util.stream.Collectors;

/// The tracks of `grid-template-columns` or `grid-template-rows`.
public record TrackList(List<GridTrack> tracks) implements CssValue {

  public TrackList {
    if (tracks.isEmpty()) {
      throw new IllegalArgumentException("A track list needs at least one track");
    }
    tracks = List.copyOf(tracks);
  }

  public static TrackList of(GridTrack... tracks) {
    return new TrackList(List.of(tracks));
  }

  /// `count` equal flexible tracks.
  public static TrackList equal(int count) {
    return of(GridTrack.repeat(count, GridTrack.fr(1)));
  }

  /// As many tracks as fit, each at least `minWidth` and never wider than the container.
  public static TrackList autoFit(CssLength minWidth) {
    var floor = CssLength.min(CssLength.pct(100), minWidth);
    return of(
        new GridTrack.AutoRepeat(
            GridTrack.AutoCount.AUTO_FIT,
            List.of(GridTrack.minmax(GridTrack.length(floor), GridTrack.fr(1)))));
  }

  /// Tracks inherited from the parent grid.
  public static TrackList subgrid() {
    return new TrackList(List.of(GridTrack.Keyword.SUBGRID));
  }

  @Override
  public String toCss() {
    return joined(tracks);
  }

  static String joined(List<GridTrack> tracks) {
    return tracks.stream().map(GridTrack::toCss).collect(Collectors.joining(" "));
  }
}
