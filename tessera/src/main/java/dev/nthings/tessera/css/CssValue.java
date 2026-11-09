package dev.nthings.tessera.css;

/// A typed CSS value that serializes into a declaration.
///
/// Style methods accept only the value category their property takes, so a color cannot be
/// passed where a length belongs. Keyword sets are enums ([CssKeyword]); compounds are records.
public sealed interface CssValue
    permits CssLength,
        CssColor,
        CssNumber,
        CssDuration,
        Easing,
        Shadow,
        Border,
        Background,
        TrackList,
        GridPlacement,
        AspectRatio,
        Transform,
        Translation,
        CssFilter,
        FontStack,
        CssContent,
        CssIdent,
        Anchor,
        Counter,
        CustomProperty,
        PositionArea,
        PositionTry,
        ScrollSnap,
        GridTrack,
        FontStack.Entry,
        CssKeyword {

  /// The value as it appears after the property name in a declaration.
  String toCss();
}
