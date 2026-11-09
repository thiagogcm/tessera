package dev.nthings.tessera.css;

/// How lines of text break, set with [Styled#textWrap(TextWrap)].
public enum TextWrap implements CssKeyword {
  WRAP,
  NOWRAP,
  /// Evens out line lengths; meant for headings and other short blocks.
  BALANCE,
  /// Avoids short last lines (orphans) in body text.
  PRETTY,
  STABLE
}
