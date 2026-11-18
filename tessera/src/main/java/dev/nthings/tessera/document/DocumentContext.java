package dev.nthings.tessera.document;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;
import java.util.List;
import java.util.function.Function;

/// The [ViewContext] of document callbacks: [DocumentComponent] and [DocumentScope] expansion,
/// [Documents#late(Function)] content and document-end sections. Only static compilation supplies
/// it, so the operations below are available only to content placed in a document. Expansion state
/// lasts one page compilation. Outline headings are collected from elements that carry them; this
/// context does not register headings.
public interface DocumentContext extends ViewContext {

  /// Adds `content` to the page's `<head>`, after `<title>`, in contribution order. Nothing is
  /// deduplicated: N placements add N elements, so guard shared head content with
  /// [#state(ViewKey)].
  void contributeHead(DocumentContent content);

  /// Adds portable `element` to the page's `<head>`, as [#contributeHead(DocumentContent)] does.
  default void contributeHead(IntoElement element) {
    contributeHead(DocumentContent.of(element));
  }

  /// Adds `section` to the end of `<body>`, after the page's late elements. Only the first
  /// registration per `owner` counts; later ones are ignored.
  void atDocumentEnd(
      ViewKey<?> owner, Function<DocumentContext, ? extends DocumentContent> section);

  /// Adds `script` to the page once. The first registration with a given id wins, and later ones
  /// with that id are ignored silently.
  void contributeScript(PageScript script);

  /// Headings collected so far on this page, in collection order. A late callback sees every
  /// heading collected from the body. The first heading for a target is kept. An unbound target
  /// reports `unbound-reference` when the document is linked.
  List<DocumentHeading> headings();
}
