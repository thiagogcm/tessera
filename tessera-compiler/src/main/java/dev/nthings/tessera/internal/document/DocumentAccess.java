package dev.nthings.tessera.internal.document;

import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentTree;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.internal.foundation.Registration;
import java.util.function.Function;

/// Access to document builder data across packages in this module.
public final class DocumentAccess {
  private static final Registration<Function<DocumentElement, DocumentNode.Host>> HOSTS =
      new Registration<>("Document host accessor");
  private static final Registration<Function<DocumentTree, DocumentNode>> UNWRAP =
      new Registration<>("Document placement accessor");
  // Wrapping is the first use of a document placement, so it initializes DocumentPlacement.
  private static final Registration<Function<DocumentNode, DocumentTree>> WRAP =
      new Registration<>("Document placement accessor", () -> Documents.rawHtml(""));

  private DocumentAccess() {}

  public static void registerHost(Function<DocumentElement, DocumentNode.Host> accessor) {
    HOSTS.register(accessor);
  }

  public static DocumentNode.Host host(DocumentElement element) {
    return HOSTS.get().apply(element);
  }

  public static void registerPlacement(
      Function<DocumentTree, DocumentNode> unwrap, Function<DocumentNode, DocumentTree> wrap) {
    UNWRAP.register(unwrap);
    WRAP.register(wrap);
  }

  public static DocumentNode unwrap(DocumentTree placement) {
    return UNWRAP.get().apply(placement);
  }

  public static DocumentTree wrap(DocumentNode node) {
    return WRAP.get().apply(node);
  }
}
