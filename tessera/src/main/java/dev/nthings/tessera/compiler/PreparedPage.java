package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A page that has been laid out and validated but not yet painted to HTML. Site compilation uses
/// it to check links across pages and to merge stylesheets before painting.
final class PreparedPage {
  private final PageDocument document;
  private final StyleSheet styles;
  private final List<ViewSnapshot.Node<String>> bodyNodes;
  private final List<ViewSnapshot.Node<String>> headNodes;
  private final Set<String> registeredDomIds;
  private final Map<String, Location> outgoingLinks;
  private final Map<TransitionKey, Location> transitionLocations;
  private final List<PageHeading> headings;
  private final List<PageScript> behaviors;

  PreparedPage(
      PageDocument document,
      StyleSheet styles,
      List<ViewSnapshot.Node<String>> bodyNodes,
      Set<String> registeredDomIds,
      Map<String, Location> outgoingLinks,
      Map<TransitionKey, Location> transitionLocations,
      List<ViewSnapshot.Node<String>> headNodes,
      List<PageHeading> headings,
      List<PageScript> behaviors) {
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

  List<ViewSnapshot.Node<String>> bodyNodes() {
    return bodyNodes;
  }

  List<ViewSnapshot.Node<String>> headNodes() {
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

  /// Page scripts registered on this page, in contribution order.
  List<PageScript> scripts() {
    return behaviors;
  }

  /// An inspection snapshot of this page's element tree.
  PageTree tree() {
    return PageTree.of(headNodes, bodyNodes);
  }

  /// This page's own CSS under `theme`, without any shared site stylesheet.
  String stylesheet(Theme theme) {
    return styles.render(theme);
  }

  /// Copies this page's CSS into `target`, a site-level stylesheet collector.
  void contributeStylesTo(StyleSheet target) {
    target.mergeFrom(styles);
  }
}
