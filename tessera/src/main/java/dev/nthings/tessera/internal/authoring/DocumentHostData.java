package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.TagElement;
import java.util.ArrayList;
import java.util.List;

/// One portable host and the document children appended by its document builder.
public final class DocumentHostData {
  public final TagElement host;
  public final List<DocumentNode> children = new ArrayList<>();

  public DocumentHostData(TagElement host) {
    this.host = host;
  }
}
