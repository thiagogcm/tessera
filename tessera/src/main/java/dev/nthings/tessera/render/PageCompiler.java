package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Coordinates layout, reference linking, validation, and HTML emission.
public final class PageCompiler {
  private PageCompiler() {}

  /// Compiles `document` with the default [Tokens] and an inline `<style>`.
  public static CompiledPage compile(PageDocument document) {
    return compile(document, new Tokens(), OutputMode.inlineStyle());
  }

  public static CompiledPage compile(PageDocument document, Tokens tokens, OutputMode outputMode) {
    var prepared = prepare(document, tokens, List.of());
    var cssContent = prepared.stylesheet(tokens);
    return paintPrepared(prepared, cssContent, outputMode);
  }

  /// Runs layout and prepaint for `document` under `tokens`, with `siteHead` elements (shared by
  /// every page of a site) placed in `<head>` ahead of anything the page contributes.
  public static PreparedPage prepare(
      PageDocument document, Tokens tokens, List<? extends IntoElement> siteHead) {
    var styles = new StyleSheetCollector();
    var state = new CompilationState(document.title());

    var p1 = LayoutPass.layout(document, tokens, siteHead, styles, state);

    // Phase 2: prepaint
    var bodyNodes = prepaint(p1.root(), state);
    var headNodes = new ArrayList<ResolvedNode>(p1.headNodes().size());
    for (var head : p1.headNodes()) {
      var linked = ReferenceLinker.link(head, state);
      headNodes.add(linked);
    }

    for (var node : bodyNodes) {
      PageValidator.validateNodeTree(node, state);
    }
    for (var head : headNodes) {
      PageValidator.validateNodeTree(head, state);
    }
    if (!state.diagnostics.isEmpty()) {
      throw new CompilationException(state.diagnostics);
    }

    return new PreparedPage(
        document,
        styles,
        bodyNodes,
        state.registeredDomIds,
        state.outgoingLinks,
        state.transitionLocations,
        headNodes,
        p1.state().headings());
  }

  public static CompiledPage paintPrepared(
      PreparedPage prepared, String cssContent, OutputMode outputMode) {
    var htmlBuffer = new StringBuilder(4096);
    var externalCssPath =
        HtmlWriter.paint(
            prepared.document(),
            prepared.bodyNodes(),
            cssContent,
            outputMode,
            prepared.headNodes(),
            htmlBuffer);
    return new CompiledPage(htmlBuffer.toString(), externalCssPath, cssContent);
  }

  private static List<ResolvedNode> prepaint(ResolvedNode root, CompilationState state) {
    var bodyNodes = new ArrayList<ResolvedNode>();
    bodyNodes.add(ReferenceLinker.link(root, state));
    for (CompilationState.ResolvedDeferred deferred;
        (deferred = state.deferredQueue.poll()) != null; ) {
      bodyNodes.add(ReferenceLinker.link(deferred.node(), state));
    }
    return bodyNodes;
  }
}
