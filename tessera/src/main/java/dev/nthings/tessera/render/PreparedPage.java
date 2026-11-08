package dev.nthings.tessera.render;

import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Reusable layout and validation result. Metadata is snapshotted; the linked tree and
/// stylesheet collector stay with the compiler.
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
    // Taken over as is: the compilation that produced it is finished and discarded.
    this.styles = styles;
    this.bodyNodes = bodyNodes;
    this.headNodes = headNodes;
    this.registeredDomIds = Collections.unmodifiableSet(registeredDomIds);
    this.outgoingLinks = Collections.unmodifiableMap(outgoingLinks);
    this.transitionLocations = Collections.unmodifiableMap(transitionLocations);
    this.headings = headings;
    this.behaviors = behaviors;
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

  /// Client behaviors registered on this page, in contribution order.
  public List<ClientBehavior> behaviors() {
    return behaviors;
  }

  public PageTree tree() {
    return PageTree.of(headNodes, bodyNodes);
  }

  public String stylesheet(Tokens tokens) {
    return styles.toCssStylesheet(tokens);
  }

  /// Copies this page's CSS into a site-owned collector without exposing its state.
  public void contributeStylesTo(StyleSheetCollector target) {
    target.mergeFrom(styles);
  }
}
