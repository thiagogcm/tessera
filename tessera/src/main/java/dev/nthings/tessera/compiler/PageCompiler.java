package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// The page phases: [#prepare] lays out and validates, [#paint] emits HTML. [Compiler] runs both
/// for one page; [SiteCompiler] validates every page before painting any.
final class PageCompiler {
  private PageCompiler() {}

  static CompiledPage compile(PageDocument document, Tokens tokens, OutputMode outputMode) {
    var prepared = prepare(document, List.of());
    var css = prepared.stylesheet(tokens);
    return new CompiledPage(
        paint(prepared, css, outputMode),
        css,
        HtmlWriter.assets(css, outputMode, prepared.behaviors()));
  }

  /// Lays out and validates `document` without emitting HTML. `siteHead` goes in
  /// `<head>` before the page's own head contributions, so a site can share it across pages.
  /// Throws [CompilationException] if the document has authoring errors.
  static PreparedPage prepare(PageDocument document, List<? extends IntoElement> siteHead) {
    var styles = new StyleSheetCollector();
    var state = new CompilationState(document.title());

    var layout = LayoutPass.layout(document, siteHead, styles, state);
    var bodyNodes = layout.bodyNodes();
    var headNodes = layout.headNodes();

    for (var node : bodyNodes) {
      PageValidator.validateNodeTree(node, state);
    }
    for (var head : headNodes) {
      PageValidator.validateNodeTree(head, state);
    }
    AccessibilityRules.check(bodyNodes, state);
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
        layout.headings(),
        layout.contributions().behaviors());
  }

  /// Paints `prepared` with `css`, the stylesheet the page links or inlines: its own stylesheet
  /// for a standalone page, or the merged stylesheet of a site.
  static String paint(PreparedPage prepared, String css, OutputMode outputMode) {
    return HtmlWriter.paint(
        prepared.document(),
        prepared.bodyNodes(),
        prepared.headNodes(),
        css,
        outputMode,
        prepared.behaviors());
  }
}
