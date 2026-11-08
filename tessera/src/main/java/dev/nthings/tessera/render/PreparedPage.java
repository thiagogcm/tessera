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

/// A page that has been laid out and validated but not yet painted to HTML. Obtain one from
/// [PageCompiler#prepare], then paint it with [PageCompiler#paintPrepared]. `SiteCompiler` uses
/// it to check links across pages and to merge stylesheets before painting. The page's element
/// tree stays private; read it through [#tree()].
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

  /// Compiler-facing: DOM ids declared on this page, for checking fragment links across pages.
  public Set<String> registeredDomIds() {
    return registeredDomIds;
  }

  /// Compiler-facing: the first authoring location of each outgoing URL, for cross-route
  /// diagnostics.
  public Map<String, CompilationLocation> outgoingLinks() {
    return outgoingLinks;
  }

  /// Compiler-facing: the first authoring location of each transition key on this page.
  public Map<TransitionKey, CompilationLocation> transitionLocations() {
    return transitionLocations;
  }

  /// Navigation headings registered while the page was built, in registration order.
  public List<DocumentHeading> headings() {
    return headings;
  }

  /// Client behaviors registered on this page, in contribution order.
  public List<ClientBehavior> behaviors() {
    return behaviors;
  }

  /// An inspection snapshot of this page's element tree, as a [PageTree].
  public PageTree tree() {
    return PageTree.of(headNodes, bodyNodes);
  }

  /// Returns this page's own CSS under `tokens`, without any shared site stylesheet.
  public String stylesheet(Tokens tokens) {
    return styles.toCssStylesheet(tokens);
  }

  /// Compiler-facing: copies this page's CSS into `target`, a site-level stylesheet collector.
  public void contributeStylesTo(StyleSheetCollector target) {
    target.mergeFrom(styles);
  }
}
