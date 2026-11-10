package dev.nthings.tessera.view;

import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.ElementRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Backend-facing: the per-page state every backend keeps the same way, for one page or one live
/// render: the [dev.nthings.tessera.core.DocumentContext#document(DocumentKey)] values and the
/// navigation headings, at most one per target.
public final class DocumentRegistry {

  /// A registered heading with the component context that registered it.
  public record Heading(DocumentHeading heading, Location placedBy) {}

  private final Map<DocumentKey<?>, Object> values = new HashMap<>();
  private final List<Heading> headings = new ArrayList<>();
  private final Map<ElementRef, Boolean> headingTargets = new IdentityHashMap<>();

  /// Creates an empty registry.
  public DocumentRegistry() {}

  /// The state for `key`, created on first use.
  public <T> T value(DocumentKey<T> key) {
    return key.getOrCreate(values);
  }

  /// Registers `heading` for its target. A second heading for the same target is ignored.
  public void registerHeading(DocumentHeading heading, Location placedBy) {
    if (headingTargets.putIfAbsent(heading.target(), Boolean.TRUE) == null) {
      headings.add(new Heading(heading, placedBy));
    }
  }

  /// The registered headings with their registration context, in registration order.
  public List<Heading> placedHeadings() {
    return List.copyOf(headings);
  }

  /// The registered headings, in registration order.
  public List<DocumentHeading> headings() {
    return headings.stream().map(Heading::heading).toList();
  }
}
