package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.css.Tokens;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A page that has been laid out and validated but not yet painted to HTML. Site compilation uses
/// it to check links across pages and to merge stylesheets before painting.
final class PreparedPage {
  private final PageDocument document;
  private final StyleSheet styles;
  private final List<ResolvedNode> bodyNodes;
  private final List<ResolvedNode> headNodes;
  private final Set<String> registeredDomIds;
  private final Map<String, Location> outgoingLinks;
  private final Map<TransitionKey, Location> transitionLocations;
  private final List<PageHeading> headings;
  private final List<ClientBehavior> behaviors;

  PreparedPage(
      PageDocument document,
      StyleSheet styles,
      List<ResolvedNode> bodyNodes,
      Set<String> registeredDomIds,
      Map<String, Location> outgoingLinks,
      Map<TransitionKey, Location> transitionLocations,
      List<ResolvedNode> headNodes,
      List<PageHeading> headings,
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

  /// DOM ids declared on this page, for checking fragment links across pages.
  Set<String> registeredDomIds() {
    return registeredDomIds;
  }

  /// The first authoring location of each outgoing URL, for cross-route diagnostics.
  Map<String, Location> outgoingLinks() {
    return outgoingLinks;
  }

  /// The first authoring location of each transition key on this page.
  Map<TransitionKey, Location> transitionLocations() {
    return transitionLocations;
  }

  /// Navigation headings registered while the page was built, in registration order.
  List<PageHeading> headings() {
    return headings;
  }

  /// Client behaviors registered on this page, in contribution order.
  List<ClientBehavior> behaviors() {
    return behaviors;
  }

  /// An inspection snapshot of this page's element tree.
  PageTree tree() {
    return PageTree.of(headNodes, bodyNodes);
  }

  /// This page's own CSS under `tokens`, without any shared site stylesheet.
  String stylesheet(Tokens tokens) {
    return styles.render(tokens);
  }

  /// Copies this page's CSS into `target`, a site-level stylesheet collector.
  void contributeStylesTo(StyleSheet target) {
    target.mergeFrom(styles);
  }
}
