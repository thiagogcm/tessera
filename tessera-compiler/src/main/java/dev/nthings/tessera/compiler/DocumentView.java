package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.OutlineHeading;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.PageAddress;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.document.DocumentNode;
import dev.nthings.tessera.internal.view.ExpansionContext;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/// The [DocumentContext] of a document callback: the portable operations of its expansion context
/// plus the document operations of its [DocumentPass]. Portable callbacks nested in the result
/// receive the portable context, not this one.
final class DocumentView implements DocumentContext {

  private final ExpansionContext context;
  private final DocumentPass backend;

  DocumentView(ExpansionContext context, DocumentPass backend) {
    this.context = context;
    this.backend = backend;
  }

  @Override
  public <T> T state(ViewKey<T> key) {
    return context.state(key);
  }

  @Override
  public void report(String rule, String message, String fix) {
    context.report(rule, message, fix);
  }

  @Override
  public void contributeHead(DocumentContent content) {
    backend.contributeHead(DocumentNode.capture(content), context.location());
  }

  @Override
  public void atDocumentEnd(
      ViewKey<?> owner, Function<DocumentContext, ? extends DocumentContent> section) {
    backend.atDocumentEnd(owner, section, context.location());
  }

  @Override
  public void contributeScript(PageScript script) {
    backend.contributeScript(script);
  }

  @Override
  public Optional<PageAddress> address() {
    return Optional.ofNullable(backend.address());
  }

  @Override
  public List<OutlineHeading> headings() {
    return context.expansion().registry().headings();
  }
}
