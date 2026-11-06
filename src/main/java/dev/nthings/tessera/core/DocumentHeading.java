package dev.nthings.tessera.core;

/// Navigation and search metadata pointing to an existing document element.
public record DocumentHeading(String targetDomId, String title, int level) {
  public DocumentHeading {
    if (level < 1 || level > 6) {
      throw new IllegalArgumentException("Heading level must be between 1 and 6");
    }
  }

  public static DocumentHeading h2(String targetDomId, String title) {
    return new DocumentHeading(targetDomId, title, 2);
  }

  public static DocumentHeading h3(String targetDomId, String title) {
    return new DocumentHeading(targetDomId, title, 3);
  }
}
