package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;
import java.net.URI;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/// An HTML document to compile with [Compiler]: its BCP 47 `lang` [Locale], its `<title>`, the
/// content of its `<body>`, and its page metadata. Create one with [#of(String, IntoElement)] or
/// [#of(String, DocumentContent)] (defaulting to [Locale#ENGLISH]) and refine it with the `with`
/// methods, each of which returns a new document.
///
/// - [#withLang(Locale)] sets `<html lang>`.
/// - [#withDescription(String)] emits `<meta name="description">` and feeds `search-index.json` and
///   [dev.nthings.tessera.navigation.SiteNavigation] search items.
/// - [#withCanonicalUrl(URI)] emits `<link rel="canonical">`. In a site with a base URL the
///   canonical URL defaults to the route's absolute URL.
///
/// The theme and output mode belong to [Compiler], not the document. Documents compare by identity.
public final class PageDocument {

  private final Locale lang;
  private final String title;
  private final DocumentContent bodyRoot;
  private final @Nullable String description;
  private final @Nullable URI canonicalUrl;

  private PageDocument(
      Locale lang,
      String title,
      DocumentContent bodyRoot,
      @Nullable String description,
      @Nullable URI canonicalUrl) {
    if (lang.toLanguageTag().equals("und")) {
      throw new IllegalArgumentException("Document language locale must specify a language");
    }
    this.lang = lang;
    this.title = title;
    this.bodyRoot = bodyRoot;
    this.description = description;
    this.canonicalUrl = canonicalUrl;
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
    if (description.isBlank()) {
      throw new IllegalArgumentException("Document description cannot be blank");
    }
    return new PageDocument(lang, title, bodyRoot, description, canonicalUrl);
  }

  /// Returns a copy of this document whose canonical URL is `url`. In a site compiled with a base
  /// URL this replaces the route's own absolute URL. Throws IllegalArgumentException unless `url`
  /// is an absolute `http` or `https` URI with a host and no fragment.
  @CheckReturnValue
  public PageDocument withCanonicalUrl(URI url) {
    var scheme = url.getScheme();
    if (scheme == null
        || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
        || url.getHost() == null
        || url.getRawFragment() != null) {
      throw new IllegalArgumentException(
          "Canonical URL must be an absolute http(s) URL without a fragment: " + url);
    }
    return new PageDocument(lang, title, bodyRoot, description, url);
  }

  /// The document language, emitted as `<html lang>`.
  public Locale lang() {
    return lang;
  }

  /// The document title, emitted as `<title>`.
  public String title() {
    return title;
  }

  /// The content of `<body>`.
  public DocumentContent bodyRoot() {
    return bodyRoot;
  }

  /// The page summary set with [#withDescription(String)], or `null`.
  public @Nullable String description() {
    return description;
  }

  /// The canonical URL set with [#withCanonicalUrl(URI)], or `null`.
  public @Nullable URI canonicalUrl() {
    return canonicalUrl;
  }

  @Override
  public String toString() {
    return "PageDocument[lang=" + lang.toLanguageTag() + ", title=" + title + "]";
  }
}
