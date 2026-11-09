package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.css.Theme;
import java.util.List;

/// The page phases: [#prepare] lays out and validates, [#paint] emits HTML. [Compiler] runs both
/// for one page; [SiteCompiler] validates every page before painting any.
final class PageCompiler {
  private PageCompiler() {}

  static CompiledPage compile(PageDocument document, Theme theme, OutputMode outputMode) {
    var prepared = prepare(document, List.of());
    var css = prepared.stylesheet(theme);
    var resources = new HtmlWriter.Resources(css, outputMode, prepared.behaviors());
    return new CompiledPage(paint(prepared, resources), css, resources.assets());
  }

  /// Lays out and validates `document` without emitting HTML. `siteHead` goes in
  /// `<head>` before the page's own head contributions, so a site can share it across pages.
  /// Throws [CompilationException] if the document has authoring errors.
  static PreparedPage prepare(PageDocument document, List<? extends IntoElement> siteHead) {
    var styles = new StyleSheet();
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

  /// Paints `prepared` with `resources`, the stylesheet and modules the page links or inlines:
  /// its own for a standalone page, or the shared ones of a site.
  static String paint(PreparedPage prepared, HtmlWriter.Resources resources) {
    return HtmlWriter.paint(
        prepared.document(),
        prepared.bodyNodes(),
        prepared.headNodes(),
        resources,
        prepared.behaviors());
  }
}
