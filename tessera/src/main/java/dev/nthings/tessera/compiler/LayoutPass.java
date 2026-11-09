package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.DocumentState;
import dev.nthings.tessera.core.DocumentState.Contribution;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Supplier;

/// Orders one document's expansion (body, late elements, end sections, head), then links it.
final class LayoutPass implements ViewExpansion.Placement {
  private final ViewExpansion expansion;
  private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
  private final PriorityQueue<Deferred> deferred = new PriorityQueue<>();
  private long sequence;

  private LayoutPass(CompilationState state) {
    expansion = new ViewExpansion(state, this);
  }

  private record Deferred(int priority, long sequence, ViewNode node)
      implements Comparable<Deferred> {
    @Override
    public int compareTo(Deferred other) {
      var order = Integer.compare(priority, other.priority);
      return order != 0 ? order : Long.compare(sequence, other.sequence);
    }
  }

  record Result(
      List<ResolvedNode> bodyNodes, List<ResolvedNode> headNodes, DocumentState contributions) {}

  static Result layout(
      PageDocument document,
      Tokens tokens,
      List<? extends IntoElement> siteHead,
      StyleSheetCollector styles,
      CompilationState state) {
    return new LayoutPass(state).document(document, tokens, siteHead, styles, state);
  }

  private Result document(
      PageDocument document,
      Tokens tokens,
      List<? extends IntoElement> siteHead,
      StyleSheetCollector styles,
      CompilationState state) {
    var contributions = new DocumentState();
    var root = RenderContext.forDocument(styles, tokens, contributions);
    siteHead.forEach(root::contributeHead);
    var body = expansion.expand(document.bodyRoot().intoElement(), root);
    resolveLate();
    // Re-read each turn: a section may register further sections.
    List<Contribution<Function<RenderContext, ? extends IntoElement>>> sections;
    for (var i = 0; i < (sections = contributions.documentEndSections()).size(); i++) {
      var section = sections.get(i);
      defer(() -> expand(root, section, "Document end", section.content()), Integer.MAX_VALUE);
    }
    // Indexed: laying out a head element may contribute further head elements.
    var heads = new ArrayList<ViewNode>();
    for (var i = 0; i < contributions.headElements().size(); i++) {
      var head = contributions.headElements().get(i);
      heads.add(expand(root, head, "Head", _ -> head.content()));
    }

    var bodyNodes = new ArrayList<ResolvedNode>();
    ReferenceLinker.link(body, state, bodyNodes);
    for (Deferred next; (next = deferred.poll()) != null; ) {
      ReferenceLinker.link(next.node(), state, bodyNodes);
    }
    var headNodes = new ArrayList<ResolvedNode>(heads.size());
    heads.forEach(head -> ReferenceLinker.link(head, state, headNodes));
    PageValidator.index(bodyNodes, state);
    PageValidator.index(headNodes, state);
    return new Result(bodyNodes, headNodes, contributions);
  }

  /// Expands a contribution in its registering component context, then its late elements.
  private ViewNode expand(
      RenderContext root,
      Contribution<?> contribution,
      String phase,
      Function<RenderContext, ? extends IntoElement> content) {
    var context = root.withComponentPath(contribution.componentPath(), contribution.origin());
    var node = expansion.expand(context, phase, content);
    resolveLate();
    return node;
  }

  @Override
  public ViewNode defer(Supplier<ViewNode> content, int priority) {
    var node = content.get();
    deferred.add(new Deferred(priority, sequence++, node));
    return ViewNode.Empty.INSTANCE;
  }

  @Override
  public ViewNode late(Supplier<ViewNode> content) {
    var slot = new ViewNode.Slot();
    pending.add(() -> slot.fill(content.get()));
    return slot;
  }

  private void resolveLate() {
    for (Runnable next; (next = pending.poll()) != null; ) {
      next.run();
    }
  }
}
