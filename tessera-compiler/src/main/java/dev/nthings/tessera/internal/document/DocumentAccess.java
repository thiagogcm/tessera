package dev.nthings.tessera.internal.document;

import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentTree;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.internal.foundation.Registration;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Access to document builder data across packages in this module.
public final class DocumentAccess {
  private static final Registration<Function<DocumentElement, DocumentNode.Host>> HOSTS =
      new Registration<>("Document host accessor");
  private static final Registration<Function<DocumentTree, DocumentNode>> UNWRAP =
      new Registration<>("Document placement accessor");
  // Wrapping is the first use of a document placement, so it initializes DocumentPlacement.
  private static final Registration<Function<DocumentNode, DocumentTree>> WRAP =
      new Registration<>("Document placement accessor", () -> Documents.rawHtml(""));

  private static volatile Function<DocumentContent, @Nullable DocumentNode> builtins =
      content -> null;

  private DocumentAccess() {}

  /// Registers the lowering of built-in document components, which keep `expand` package-private.
  public static void registerBuiltin(Function<DocumentContent, @Nullable DocumentNode> lowering) {
    builtins = lowering;
  }

  /// Lowers a built-in document component, or returns null for any other content.
  public static @Nullable DocumentNode builtin(DocumentContent content) {
    return builtins.apply(content);
  }

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
