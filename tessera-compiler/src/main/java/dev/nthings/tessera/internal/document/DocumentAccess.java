package dev.nthings.tessera.internal.document;

import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentTree;
import dev.nthings.tessera.document.Documents;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Access to document builder data across packages in this module.
public final class DocumentAccess {
  private static final AtomicReference<@Nullable Function<DocumentElement, DocumentNode.Host>>
      HOSTS = new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<DocumentTree, DocumentNode>> UNWRAP =
      new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<DocumentNode, DocumentTree>> WRAP =
      new AtomicReference<>();

  private DocumentAccess() {}

  public static void registerHost(Function<DocumentElement, DocumentNode.Host> accessor) {
    if (!HOSTS.compareAndSet(null, accessor)) {
      throw new IllegalStateException("Document host accessor already registered");
    }
  }

  public static DocumentNode.Host host(DocumentElement element) {
    return Objects.requireNonNull(HOSTS.get()).apply(element);
  }

  public static void registerPlacement(
      Function<DocumentTree, DocumentNode> unwrap, Function<DocumentNode, DocumentTree> wrap) {
    if (!UNWRAP.compareAndSet(null, unwrap) || !WRAP.compareAndSet(null, wrap)) {
      throw new IllegalStateException("Document placement accessor already registered");
    }
  }

  public static DocumentNode unwrap(DocumentTree placement) {
    return Objects.requireNonNull(UNWRAP.get()).apply(placement);
  }

  public static DocumentTree wrap(DocumentNode node) {
    return Holder.WRAP.apply(node);
  }

  private static final class Holder {
    static final Function<DocumentNode, DocumentTree> WRAP = load();

    private static Function<DocumentNode, DocumentTree> load() {
      // Trigger DocumentPlacement class initialization on first wrap.
      var seed = Documents.rawHtml("");
      return Objects.requireNonNull(
          DocumentAccess.WRAP.get(),
          () -> "No document placement accessor registered for " + seed.getClass().getSimpleName());
    }
  }
}
