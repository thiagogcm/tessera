package dev.nthings.tessera.render;

import dev.nthings.tessera.core.DocumentState;
import dev.nthings.tessera.core.DocumentState.Contribution;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/// Orders one document's expansion: body, late elements, end-of-document sections and head
/// contributions, then links the expanded nodes for validation and painting.
final class LayoutPass {
  private LayoutPass() {}

  record Result(
      List<ResolvedNode> bodyNodes, List<ResolvedNode> headNodes, DocumentState contributions) {}

  static Result layout(
      PageDocument document,
      Tokens tokens,
      List<? extends IntoElement> siteHead,
      StyleSheetCollector styles,
      CompilationState state) {
    var expansion = new ViewExpansion(state);
    var contributions = new DocumentState();
    var root = RenderContext.forDocument(styles, tokens, contributions);
    siteHead.forEach(root::contributeHead);
    var body = expansion.expand(document.bodyRoot().intoElement(), root);
    expansion.resolveLate();
    // Re-read each turn: a section may register further sections.
    List<Contribution<Function<RenderContext, ? extends IntoElement>>> sections;
    for (var i = 0; i < (sections = contributions.documentEndSections()).size(); i++) {
      var section = sections.get(i);
      expansion.defer(
          Integer.MAX_VALUE, expand(expansion, root, section, "Document end", section.content()));
    }
    // Indexed: laying out a head element may contribute further head elements.
    var heads = new ArrayList<ViewNode>();
    for (var i = 0; i < contributions.headElements().size(); i++) {
      var head = contributions.headElements().get(i);
      heads.add(expand(expansion, root, head, "Head", _ -> head.content()));
    }

    var bodyNodes = new ArrayList<ResolvedNode>();
    ReferenceLinker.link(body, state, bodyNodes);
    for (ViewNode next; (next = expansion.nextDeferred()) != null; ) {
      ReferenceLinker.link(next, state, bodyNodes);
    }
    var headNodes = new ArrayList<ResolvedNode>(heads.size());
    heads.forEach(head -> ReferenceLinker.link(head, state, headNodes));
    return new Result(bodyNodes, headNodes, contributions);
  }

  /// Expands a contribution in the component context it was registered from, then its late
  /// elements.
  private static ViewNode expand(
      ViewExpansion expansion,
      RenderContext root,
      Contribution<?> contribution,
      String phase,
      Function<RenderContext, ? extends IntoElement> content) {
    var context = root.withComponentPath(contribution.componentPath(), contribution.origin());
    var node = expansion.expand(context, phase, content);
    expansion.resolveLate();
    return node;
  }
}
