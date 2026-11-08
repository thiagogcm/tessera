package dev.nthings.tessera.core;

/// Navigation and search metadata for one heading on the page. `targetDomId` is the full, scoped
/// DOM id of the heading element: [RenderContext#registerHeading] returns a heading with it set,
/// and the element should be emitted with that id. Throws IllegalArgumentException if `level` is
/// outside 1 through 6.
public record DocumentHeading(String targetDomId, String title, int level) {
  public DocumentHeading {
    if (level < 1 || level > 6) {
      throw new IllegalArgumentException("Heading level must be between 1 and 6");
    }
  }

  /// Creates a level 2 heading, `<h2>`.
  public static DocumentHeading h2(String targetDomId, String title) {
    return new DocumentHeading(targetDomId, title, 2);
  }

  /// Creates a level 3 heading, `<h3>`.
  public static DocumentHeading h3(String targetDomId, String title) {
    return new DocumentHeading(targetDomId, title, 3);
  }
}
