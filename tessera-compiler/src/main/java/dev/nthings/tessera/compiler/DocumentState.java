package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.BundlePublication.File;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.document.DocumentNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// The document side of one page compilation: head contributions, client behaviors and end-of-body
/// sections, as the [DocumentPass] backend receives them. Expansion state and headings live in the
/// expansion's `ExpansionRegistry`, as in every backend.
final class DocumentState {

  private final List<Contribution<DocumentNode>> headElements = new ArrayList<>();
  private final Map<String, PageScript> behaviors = new LinkedHashMap<>();
  private final Map<String, Asset> bundleAssets = new LinkedHashMap<>();
  private @Nullable File bootstrap;
  private final Set<ViewKey<?>> documentEndOwners = new HashSet<>();
  private final List<Contribution<Function<DocumentContext, ? extends DocumentContent>>>
      documentEndSections = new ArrayList<>();

  /// Content placed at a page-level position, with the component context that placed it.
  record Contribution<T>(T content, Location placedBy) {}

  void contributeHead(DocumentNode content, Location placedBy) {
    headElements.add(new Contribution<>(content, placedBy));
  }

  void atDocumentEnd(
      ViewKey<?> owner,
      Function<DocumentContext, ? extends DocumentContent> section,
      Location placedBy) {
    if (documentEndOwners.add(owner)) {
      documentEndSections.add(new Contribution<>(section, placedBy));
    }
  }

  /// Registers `behavior` once per page; a later behavior with the same id is ignored.
  void contributeScript(PageScript behavior) {
    behaviors.putIfAbsent(behavior.id(), behavior);
  }

  void contributeBundle(BundlePublication.File script, BundlePublication.Entry entry) {
    bootstrap = script;
    bundleAssets.putIfAbsent(script.asset().path(), script.asset());
    entry.assets().forEach(asset -> bundleAssets.putIfAbsent(asset.path(), asset));
  }

  List<Asset> bundleAssets() {
    return List.copyOf(bundleAssets.values());
  }

  @Nullable File bootstrap() {
    return bootstrap;
  }

  List<PageScript> scripts() {
    return List.copyOf(behaviors.values());
  }

  /// Head contributions, in registration order; live, since laying one out may add more.
  List<Contribution<DocumentNode>> headElements() {
    return Collections.unmodifiableList(headElements);
  }

  /// End-of-page sections, in registration order; live, since laying one out may add more. Only the
  /// first section per owner key is kept.
  List<Contribution<Function<DocumentContext, ? extends DocumentContent>>> documentEndSections() {
    return Collections.unmodifiableList(documentEndSections);
  }
}
