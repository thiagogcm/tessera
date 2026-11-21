package dev.nthings.tessera.css;

import java.util.List;
import java.util.stream.Collectors;

/// The tracks of `grid-template-columns` or `grid-template-rows`. Applied with
/// [Styled#gridColumns(TrackList)] or [Styled#gridRows(TrackList)].
public record TrackList(List<GridTrack> tracks) implements CssValue {

  public TrackList {
    if (tracks.isEmpty()) {
      throw new IllegalArgumentException("A track list needs at least one track");
    }
    tracks = List.copyOf(tracks);
  }

  /// Lists `tracks` in order. Throws IllegalArgumentException if no tracks are given.
  public static TrackList of(GridTrack... tracks) {
    return new TrackList(List.of(tracks));
  }

  /// `repeat(<count>, 1fr)`: `count` equal flexible tracks. Throws IllegalArgumentException if
  /// `count` is below 1.
  public static TrackList equal(int count) {
    return of(GridTrack.repeat(count, GridTrack.fr(1)));
  }

  /// `repeat(auto-fit, minmax(min(100%, <minWidth>), 1fr))`: as many tracks as fit, each at least
  /// `minWidth` and never wider than the container.
  public static TrackList autoFit(CssLength minWidth) {
    var floor = CssLength.min(CssLength.pct(100), minWidth);
    return of(
        new GridTracks.AutoFit(
            List.of(GridTrack.minmax(GridTrack.length(floor), GridTrack.fr(1)))));
  }

  /// `subgrid`: inherits the tracks of the parent grid.
  public static TrackList subgrid() {
    return new TrackList(List.of(GridTracks.Keyword.SUBGRID));
  }

  @Override
  public String toCss() {
    return joined(tracks);
  }

  static String joined(List<GridTrack> tracks) {
    return tracks.stream().map(GridTrack::toCss).collect(Collectors.joining(" "));
  }
}
