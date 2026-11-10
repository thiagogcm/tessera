package dev.nthings.tessera.core;

import java.util.List;
import java.util.function.Function;

/// What an expand callback can do beyond returning content: per-page state, head and end-of-body
/// contributions, page scripts, reading the document outline, and diagnostics. Expand callbacks,
/// late elements and document-end sections receive it from the compiler, or from a live backend
/// such as a browser mount. A mounted view has no document: there, per-page state and headings
/// last one render, and head contributions, document-end sections and page scripts are reported
/// as diagnostics. Don't keep it after the callback returns, and don't share mutable application
/// state through it: callbacks may run concurrently across pages. Outline headings are collected
/// from elements that carry them; this context does not register headings.
public interface DocumentContext {

  /// Per-page state for `key`, created on first use. Use it for facts that must hold once per page,
  /// such as whether a shared head element was already contributed.
  <T> T document(DocumentKey<T> key);

  /// Adds `element` to the page's `<head>`, after `<title>`, in contribution order. Nothing is
  /// deduplicated: N placements add N elements, so guard shared head content with
  /// [#document(DocumentKey)].
  void contributeHead(IntoElement element);

  /// Adds `section` to the end of `<body>`, after the page's late elements. Only the first
  /// registration per `owner` counts; later ones are ignored.
  void atDocumentEnd(
      DocumentKey<?> owner, Function<DocumentContext, ? extends IntoElement> section);

  /// Adds `script` to the page once. The first registration with a given id wins, and later ones
  /// with that id are ignored silently.
  void contributeScript(PageScript script);

  /// Headings collected so far on this page, in collection order. A late callback sees every
  /// heading collected from the body. The first heading for a target is kept. An unbound target
  /// reports `unbound-reference` when the document is linked.
  List<DocumentHeading> headings();

  /// Reports an authoring error found by a component, such as two options that cannot be combined.
  /// `rule` is a short kebab-case code, `message` says what is wrong and `fix` how to correct it.
  /// The diagnostic is located at the current component and fails compilation like the compiler's
  /// own rules.
  void report(String rule, String message, String fix);
}
