package dev.nthings.tessera.core;

import java.util.List;
import java.util.function.Function;

/// What a render callback can do beyond returning content: per-page state, head and end-of-body
/// contributions, client behaviors, heading registration and diagnostics. Render callbacks, late
/// elements and document-end sections receive it from the compiler. Don't keep it after the
/// callback returns, and don't share mutable application state through it: callbacks may run
/// concurrently across pages.
public interface RenderContext {

  /// Per-page state for `key`, created on first use. Use it for facts that must hold once per page,
  /// such as whether a shared head element was already contributed.
  <T> T document(DocumentKey<T> key);

  /// Adds `element` to the page's `<head>`, after `<title>`, in contribution order. Nothing is
  /// deduplicated: N placements add N elements, so guard shared head content with
  /// [#document(DocumentKey)].
  void contributeHead(IntoElement element);

  /// Adds `section` to the end of `<body>`, after the page's late elements. Only the first
  /// registration per `owner` counts; later ones are ignored.
  void atDocumentEnd(DocumentKey<?> owner, Function<RenderContext, ? extends IntoElement> section);

  /// Adds `behavior` to the page once. The first registration with a given id wins, and later ones
  /// with that id are ignored silently.
  void contributeBehavior(ClientBehavior behavior);

  /// Registers a navigation heading for the element bound to `target`, with `title` and `level`.
  /// Table-of-contents components and the site search index list it. Bind `target` on the heading
  /// element with [TagElement#id(ElementRef)]; a target never bound on the page reports
  /// `unbound-reference`. Registering one target twice keeps the first heading. Throws
  /// IllegalArgumentException if `level` is outside 1 through 6.
  void registerHeading(ElementRef target, String title, int level);

  /// Headings registered so far on this page, in registration order. Late elements see every
  /// heading in the body.
  List<DocumentHeading> headings();

  /// Reports an authoring error found by a component, such as two options that cannot be combined.
  /// `rule` is a short kebab-case code, `message` says what is wrong and `fix` how to correct it.
  /// The diagnostic is located at the current component and fails compilation like the compiler's
  /// own rules.
  void report(String rule, String message, String fix);
}
