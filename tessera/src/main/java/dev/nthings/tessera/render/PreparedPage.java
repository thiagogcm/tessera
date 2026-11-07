package dev.nthings.tessera.render;

import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Opaque, reusable result of layout and validation. Public metadata is snapshotted;
/// the linked tree and stylesheet collector remain owned by the compiler.
public final class PreparedPage {
  private final PageDocument document;
  private final StyleSheetCollector styles;
  private final List<ResolvedNode> bodyNodes;
  private final List<ResolvedNode> headNodes;
  private final Set<String> registeredDomIds;
  private final Map<String, CompilationLocation> outgoingLinks;
  private final Map<TransitionKey, CompilationLocation> transitionLocations;
  private final List<DocumentHeading> headings;
  private final List<ClientBehavior> behaviors;

  PreparedPage(
      PageDocument document,
      StyleSheetCollector styles,
      List<ResolvedNode> bodyNodes,
      Set<String> registeredDomIds,
      Map<String, CompilationLocation> outgoingLinks,
      Map<TransitionKey, CompilationLocation> transitionLocations,
      List<ResolvedNode> headNodes,
      List<DocumentHeading> headings,
      List<ClientBehavior> behaviors) {
    this.document = document;
    this.styles = new StyleSheetCollector();
    this.styles.mergeFrom(styles);
    this.bodyNodes = List.copyOf(bodyNodes);
    this.headNodes = List.copyOf(headNodes);
    this.registeredDomIds = Collections.unmodifiableSet(new LinkedHashSet<>(registeredDomIds));
    this.outgoingLinks = Collections.unmodifiableMap(new LinkedHashMap<>(outgoingLinks));
    this.transitionLocations =
        Collections.unmodifiableMap(new LinkedHashMap<>(transitionLocations));
    this.headings = List.copyOf(headings);
    this.behaviors = List.copyOf(behaviors);
  }

  PageDocument document() {
    return document;
  }

  List<ResolvedNode> bodyNodes() {
    return bodyNodes;
  }

  List<ResolvedNode> headNodes() {
    return headNodes;
  }

  public Set<String> registeredDomIds() {
    return registeredDomIds;
  }

  /// First authoring location of each outgoing URL, for cross-route diagnostics.
  public Map<String, CompilationLocation> outgoingLinks() {
    return outgoingLinks;
  }

  public Map<TransitionKey, CompilationLocation> transitionLocations() {
    return transitionLocations;
  }

  public List<DocumentHeading> headings() {
    return headings;
  }

  /// Client behaviors registered by components on this page, in contribution order.
  public List<ClientBehavior> behaviors() {
    return behaviors;
  }

  /// Snapshot of the linked element tree, for inspection.
  public PageTree tree() {
    return PageTree.of(headNodes, bodyNodes);
  }

  public String stylesheet(Tokens tokens) {
    return styles.toCssStylesheet(tokens);
  }

  /// Copies this page's CSS contribution into a site-owned collector without exposing its state.
  public void contributeStylesTo(StyleSheetCollector target) {
    target.mergeFrom(styles);
  }
}
