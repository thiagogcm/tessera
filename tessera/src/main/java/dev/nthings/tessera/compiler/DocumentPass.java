package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.DocumentState.Contribution;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.StyleSheet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Supplier;

/// Orders one document's expansion (body, late elements, end sections, head), then links it.
final class DocumentPass implements Expansion.Placement {
  private final String pageTitle;
  private final Expansion expansion;
  private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
  private final PriorityQueue<Deferred> deferred = new PriorityQueue<>();
  private long sequence;

  private DocumentPass(String pageTitle, StyleSheet styles, DocumentState contributions) {
    this.pageTitle = pageTitle;
    this.expansion = new Expansion(this, styles, contributions);
  }

  private record Deferred(int priority, long sequence, ExpandedNode node)
      implements Comparable<Deferred> {
    @Override
    public int compareTo(Deferred other) {
      var order = Integer.compare(priority, other.priority);
      return order != 0 ? order : Long.compare(sequence, other.sequence);
    }
  }

  /// `headings` are the registered headings whose targets are bound, with their DOM ids.
  record Result(
      List<ResolvedNode> bodyNodes,
      List<ResolvedNode> headNodes,
      List<PageHeading> headings,
      DocumentState contributions,
      CompilationState compilation) {}

  static Result layout(
      PageDocument document, List<? extends IntoElement> siteHead, StyleSheet styles) {
    var contributions = new DocumentState();
    return new DocumentPass(document.title(), styles, contributions)
        .document(document, siteHead, contributions);
  }

  private Result document(
      PageDocument document, List<? extends IntoElement> siteHead, DocumentState contributions) {
    siteHead.forEach(head -> contributions.contributeHead(head.intoElement(), Location.ROOT));
    var body = expansion.expand(document.bodyRoot());
    resolveLate();
    // Re-read each turn: a section may register further sections.
    List<Contribution<Function<DocumentContext, ? extends IntoElement>>> sections;
    for (var i = 0; i < (sections = contributions.documentEndSections()).size(); i++) {
      var section = sections.get(i);
      defer(() -> expand(section, "Document end", section.content()), Integer.MAX_VALUE);
    }
    // Indexed: laying out a head element may contribute further head elements.
    var heads = new ArrayList<ExpandedNode>();
    for (var i = 0; i < contributions.headElements().size(); i++) {
      var head = contributions.headElements().get(i);
      heads.add(expand(head, "Head", _ -> head.content()));
    }

    var bodies = new ArrayList<ExpandedNode>();
    bodies.add(body);
    for (Deferred next; (next = deferred.poll()) != null; ) {
      bodies.add(next.node());
    }
    var bodyNodes = expansion.link(bodies);
    var headNodes = expansion.link(heads);
    var headings = headings(contributions, expansion);
    var compilation = expansion.compilation(pageTitle);
    return new Result(bodyNodes, headNodes, headings, contributions, compilation);
  }

  /// Resolves each registered heading's target; an unbound target is reported.
  private static List<PageHeading> headings(DocumentState contributions, Expansion expansion) {
    var headings = new ArrayList<PageHeading>();
    for (var contribution : contributions.headingContributions()) {
      var heading = contribution.content();
      var domId =
          expansion.boundId(
              heading.target(), "Heading \"" + heading.title() + "\"", contribution.placedBy());
      if (domId != null) {
        headings.add(new PageHeading(domId, heading.title(), heading.level()));
      }
    }
    return headings;
  }

  /// Expands a contribution in its registering component context, then its late elements.
  private ExpandedNode expand(
      Contribution<?> contribution,
      String phase,
      Function<DocumentContext, ? extends IntoElement> content) {
    var node = expansion.expand(contribution.placedBy(), phase, content);
    resolveLate();
    return node;
  }

  @Override
  public RuntimeException failureContext(Diagnostic diagnostic) {
    return new CompilationException(List.of(diagnostic.atPage(pageTitle)));
  }

  @Override
  public ExpandedNode defer(Supplier<ExpandedNode> content, int priority) {
    var node = content.get();
    deferred.add(new Deferred(priority, sequence++, node));
    return ExpandedNode.Empty.INSTANCE;
  }

  @Override
  public ExpandedNode late(Supplier<ExpandedNode> content) {
    var slot = new ExpandedNode.Slot();
    pending.add(() -> slot.fill(content.get()));
    return slot;
  }

  private void resolveLate() {
    for (Runnable next; (next = pending.poll()) != null; ) {
      next.run();
    }
  }
}
