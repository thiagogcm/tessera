package dev.nthings.tessera.render;

import dev.nthings.tessera.core.DocumentState;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

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
    // Indexed: a section may register further sections.
    for (var i = 0; i < contributions.documentEndSections().size(); i++) {
      var section = contributions.documentEndSections().get(i);
      var context = root.withComponentPath(section.componentPath(), section.origin());
      expansion.defer(
          Integer.MAX_VALUE,
          expansion.expand(section.content().apply(context).intoElement(), context));
      expansion.resolveLate();
    }
    // Indexed: laying out a head element may contribute further head elements.
    var heads = new ArrayList<ViewNode>();
    for (var i = 0; i < contributions.headElements().size(); i++) {
      var head = contributions.headElements().get(i);
      var context = root.withComponentPath(head.componentPath(), head.origin());
      heads.add(expansion.expand(head.content(), context));
      expansion.resolveLate();
    }

    var linker = new ReferenceLinker(expansion, state);
    var bodyNodes = new ArrayList<ResolvedNode>();
    linker.link(body, bodyNodes);
    for (ViewNode next; (next = expansion.nextDeferred()) != null; ) {
      linker.link(next, bodyNodes);
    }
    var headNodes = new ArrayList<ResolvedNode>(heads.size());
    heads.forEach(head -> linker.link(head, headNodes));
    return new Result(bodyNodes, headNodes, contributions);
  }
}
