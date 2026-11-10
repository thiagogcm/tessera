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
  private final Expansion expansion;
  private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
  private final PriorityQueue<Deferred> deferred = new PriorityQueue<>();
  private long sequence;

  private DocumentPass(ExpansionState state) {
    expansion = new Expansion(state, this);
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
    var state = new ExpansionState(document.title());
    return new DocumentPass(state).document(document, siteHead, styles, state);
  }

  private Result document(
      PageDocument document,
      List<? extends IntoElement> siteHead,
      StyleSheet styles,
      ExpansionState state) {
    var contributions = new DocumentState();
    var root = PageContext.forDocument(styles, contributions, state);
    siteHead.forEach(root::contributeHead);
    var body = expansion.expand(document.bodyRoot().intoElement(), root);
    resolveLate();
    // Re-read each turn: a section may register further sections.
    List<Contribution<Function<DocumentContext, ? extends IntoElement>>> sections;
    for (var i = 0; i < (sections = contributions.documentEndSections()).size(); i++) {
      var section = sections.get(i);
      defer(() -> expand(root, section, "Document end", section.content()), Integer.MAX_VALUE);
    }
    // Indexed: laying out a head element may contribute further head elements.
    var heads = new ArrayList<ExpandedNode>();
    for (var i = 0; i < contributions.headElements().size(); i++) {
      var head = contributions.headElements().get(i);
      heads.add(expand(root, head, "Head", _ -> head.content()));
    }

    var bodies = new ArrayList<ExpandedNode>();
    bodies.add(body);
    for (Deferred next; (next = deferred.poll()) != null; ) {
      bodies.add(next.node());
    }
    var bodyNodes = ReferenceLinker.link(bodies, state);
    var headNodes = ReferenceLinker.link(heads, state);
    var headings = headings(contributions, state);
    var compilation = new CompilationState(state);
    PageValidator.index(bodyNodes, compilation);
    PageValidator.index(headNodes, compilation);
    return new Result(bodyNodes, headNodes, headings, contributions, compilation);
  }

  /// Resolves each registered heading's target; an unbound target is reported.
  private static List<PageHeading> headings(DocumentState contributions, ExpansionState state) {
    var headings = new ArrayList<PageHeading>();
    for (var contribution : contributions.headingContributions()) {
      var heading = contribution.content();
      var domId =
          state.boundId(
              heading.target(), "Heading \"" + heading.title() + "\"", contribution.placedBy());
      if (domId != null) {
        headings.add(new PageHeading(domId, heading.title(), heading.level()));
      }
    }
    return headings;
  }

  /// Expands a contribution in its registering component context, then its late elements.
  private ExpandedNode expand(
      PageContext root,
      Contribution<?> contribution,
      String phase,
      Function<DocumentContext, ? extends IntoElement> content) {
    var context = root.withPlacement(contribution.placedBy());
    var node = expansion.expand(context, phase, content);
    resolveLate();
    return node;
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
