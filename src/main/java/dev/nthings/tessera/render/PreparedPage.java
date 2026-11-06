package dev.nthings.tessera.render;

import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// Opaque, reusable result of layout and validation. Public metadata is snapshotted;
/// the linked tree and stylesheet collector remain owned by the compiler.
public final class PreparedPage {
  private final PageDocument document;
  private final StyleSheetCollector styles;
  private final List<ResolvedNode> bodyNodes;
  private final List<ResolvedNode> headNodes;
  private final Set<String> registeredDomIds;
  private final Set<String> outgoingHrefs;
  private final Set<TransitionKey> transitionKeys;
  private final List<DocumentHeading> headings;

  PreparedPage(
      PageDocument document,
      StyleSheetCollector styles,
      List<ResolvedNode> bodyNodes,
      Set<String> registeredDomIds,
      Set<String> outgoingHrefs,
      Set<TransitionKey> transitionKeys,
      List<ResolvedNode> headNodes,
      List<DocumentHeading> headings) {
    this.document = document;
    this.styles = new StyleSheetCollector();
    this.styles.mergeFrom(styles);
    this.bodyNodes = List.copyOf(bodyNodes);
    this.headNodes = List.copyOf(headNodes);
    this.registeredDomIds = Collections.unmodifiableSet(new LinkedHashSet<>(registeredDomIds));
    this.outgoingHrefs = Collections.unmodifiableSet(new LinkedHashSet<>(outgoingHrefs));
    this.transitionKeys = Collections.unmodifiableSet(new LinkedHashSet<>(transitionKeys));
    this.headings = List.copyOf(headings);
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

  public Set<String> outgoingHrefs() {
    return outgoingHrefs;
  }

  public Set<TransitionKey> transitionKeys() {
    return transitionKeys;
  }

  public List<DocumentHeading> headings() {
    return headings;
  }

  public String stylesheet(Tokens tokens) {
    return styles.toCssStylesheet(tokens);
  }

  /// Copies this page's CSS contribution into a site-owned collector without exposing its state.
  public void contributeStylesTo(StyleSheetCollector target) {
    target.mergeFrom(styles);
  }
}
