package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// An HTML document to compile with [Compiler]: its BCP 47 `lang` [Locale], its `<title>`, the
/// content of its `<body>`, and its page metadata. Create one with [#of(String, IntoElement)] or
/// [#of(String, DocumentContent)] (defaulting to [Locale#ENGLISH]) and refine it with the `with`
/// methods, each of which returns a new document.
///
/// - [#withLang(Locale)] sets `<html lang>`.
/// - [#withDescription(String)] emits `<meta name="description">` and feeds `search-index.json` and
///   [SiteNavigation] search items.
/// - [#withCanonicalUrl(URI)] emits `<link rel="canonical">`. In a site with a base URL the
///   canonical URL defaults to the route's absolute URL.
///
/// The theme and output mode belong to [Compiler], not the document.
///
/// @param description a short summary of the page, or `null`; blank values are rejected
/// @param canonicalUrl the canonical URL of the page, or `null`; it must be an absolute `http` or
///   `https` URI with a host and no user info or fragment
public record PageDocument(
    Locale lang,
    String title,
    DocumentContent bodyRoot,
    @Nullable String description,
    @Nullable URI canonicalUrl) {

  public PageDocument {
    if (lang.toLanguageTag().equals("und")) {
      throw new IllegalArgumentException("Document language locale must specify a language");
    }
    if (description != null && description.isBlank()) {
      throw new IllegalArgumentException("Document description cannot be blank");
    }
    if (canonicalUrl != null && !isCanonicalUrl(canonicalUrl)) {
      throw new IllegalArgumentException(
          "Canonical URL must be an absolute http(s) URL without user info or a fragment: "
              + canonicalUrl);
    }
  }

  private static boolean isCanonicalUrl(URI url) {
    var scheme = url.getScheme();
    return scheme != null
        && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
        && url.getHost() != null
        && url.getRawUserInfo() == null
        && url.getRawFragment() == null;
  }

  /// Creates an English document whose body is portable `bodyRoot`.
  public static PageDocument of(String title, IntoElement bodyRoot) {
    return of(title, DocumentContent.of(bodyRoot));
  }

  /// Creates an English document whose body is document `bodyRoot`.
  public static PageDocument of(String title, DocumentContent bodyRoot) {
    return new PageDocument(Locale.ENGLISH, title, bodyRoot, null, null);
  }

  /// Returns a copy of this document with `lang`. Throws IllegalArgumentException if `lang` has no
  /// language.
  @CheckReturnValue
  public PageDocument withLang(Locale lang) {
    return new PageDocument(lang, title, bodyRoot, description, canonicalUrl);
  }

  /// Returns a copy of this document with `description`, a short summary of the page for search
  /// engines, link previews, and site search. Throws IllegalArgumentException if `description` is
  /// blank.
  @CheckReturnValue
  public PageDocument withDescription(String description) {
    return new PageDocument(
        lang, title, bodyRoot, Objects.requireNonNull(description), canonicalUrl);
  }

  /// Returns a copy of this document whose canonical URL is `url`. In a site compiled with a base
  /// URL this replaces the route's own absolute URL. Throws IllegalArgumentException unless `url`
  /// is an absolute `http` or `https` URI with a host and no user info or fragment.
  @CheckReturnValue
  public PageDocument withCanonicalUrl(URI url) {
    return new PageDocument(lang, title, bodyRoot, description, Objects.requireNonNull(url));
  }
}
