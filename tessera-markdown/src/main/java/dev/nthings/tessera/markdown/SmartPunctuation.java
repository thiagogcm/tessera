package dev.nthings.tessera.markdown;

/// Typographic punctuation for one text run: curly quotes, en and em dashes, and ellipses.
final class SmartPunctuation {

  private SmartPunctuation() {}

  /// `afterText` says the run continues inline content, so a leading quote closes.
  static String apply(String text, boolean afterText) {
    var out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (text.startsWith("---", i)) {
        out.append('—');
        i += 2;
      } else if (text.startsWith("--", i)) {
        out.append('–');
        i += 1;
      } else if (text.startsWith("...", i)) {
        out.append('…');
        i += 2;
      } else if (c == '"') {
        out.append(opens(text, i, afterText) ? '“' : '”');
      } else if (c == '\'') {
        out.append(opens(text, i, afterText) ? '‘' : '’');
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  /// A quote opens when it starts the run or follows whitespace or an opening bracket, and
  /// something other than whitespace follows it. Everything else, apostrophes included, closes.
  private static boolean opens(String text, int at, boolean afterText) {
    boolean afterBoundary =
        at == 0
            ? !afterText
            : (Character.isWhitespace(text.charAt(at - 1))
                || "([{—–".indexOf(text.charAt(at - 1)) >= 0);
    boolean beforeText = at + 1 < text.length() && !Character.isWhitespace(text.charAt(at + 1));
    return afterBoundary && beforeText;
  }
}
