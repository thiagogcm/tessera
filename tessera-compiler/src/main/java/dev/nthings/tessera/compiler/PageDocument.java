package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;
import java.util.Locale;

/// An HTML document to compile with [Compiler]: its BCP 47 `lang` [Locale], its `<title>`, and the
/// content of its `<body>`. Create one with [#of(String, IntoElement)] or [#of(String,
/// DocumentContent)] (defaulting to [Locale#ENGLISH]) and refine with [#withLang(Locale)].
///
/// The theme and output mode belong to [Compiler], not the document.
public record PageDocument(Locale lang, String title, DocumentContent bodyRoot) {

  public PageDocument {
    if (lang.toLanguageTag().equals("und")) {
      throw new IllegalArgumentException("Document language locale must specify a language");
    }
  }

  /// Creates an English document whose body is portable `bodyRoot`.
  public static PageDocument of(String title, IntoElement bodyRoot) {
    return new PageDocument(Locale.ENGLISH, title, DocumentContent.of(bodyRoot));
  }

  /// Creates an English document whose body is document `bodyRoot`.
  public static PageDocument of(String title, DocumentContent bodyRoot) {
    return new PageDocument(Locale.ENGLISH, title, bodyRoot);
  }

  /// Returns a copy of this document with `lang`.
  @CheckReturnValue
  public PageDocument withLang(Locale lang) {
    return new PageDocument(lang, title, bodyRoot);
  }
}
