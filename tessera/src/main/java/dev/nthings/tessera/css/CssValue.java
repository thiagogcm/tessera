package dev.nthings.tessera.css;

/// A typed CSS value that serializes into a declaration.
///
/// Every style method accepts a value of the category its property takes, so the compiler rejects
/// a color where a length belongs and no declaration is built from raw text. Keyword sets are
/// enums ([CssKeyword]); compound values are records.
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
        PositionArea,
        PositionTry,
        ScrollSnap,
        GridTrack,
        FontStack.Entry,
        CssKeyword {

  /// The value as it appears after the property name in a declaration.
  String toCss();
}
