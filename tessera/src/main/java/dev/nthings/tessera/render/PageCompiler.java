package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Compiles a [PageDocument] to HTML. Entry points:
///
/// - [#compile(PageDocument)]: default tokens and inline CSS.
/// - [#compile(PageDocument, Tokens, OutputMode)]: custom tokens and output packaging. Under
///   [OutputMode.ExternalAsset] the caller must publish the stylesheet and each asset.
/// - [#explain(PageDocument)]: the element tree instead of HTML, for inspection.
/// - [#prepare] and [#paintPrepared]: the advanced phase split that `SiteCompiler` uses to
///   validate several pages before painting them with one shared stylesheet.
///
/// [#compile(PageDocument)], [#compile(PageDocument, Tokens, OutputMode)], [#explain(PageDocument)]
/// and [#prepare] throw [CompilationException] when authoring errors are found, before any HTML is
/// produced. Diagnostics carry a stable rule code, such as `duplicate-id` or `image-alt`.
public final class PageCompiler {
  private PageCompiler() {}

  /// Compiles `document` with the default [Tokens] and an inline `<style>`.
  ///
  /// Throws [CompilationException] if the document has authoring errors.
  public static CompiledPage compile(PageDocument document) {
    return compile(document, new Tokens(), OutputMode.inlineStyle());
  }

  /// Compiles `document` with `tokens`, packaging CSS and client behaviors as `outputMode`
  /// specifies.
  ///
  /// Under [OutputMode.ExternalAsset], the page links the stylesheet at
  /// [CompiledPage#externalCssPath()] and each module in [CompiledPage#assets()]. The caller must
  /// publish [CompiledPage#cssContent()] at that path and each asset at its path.
  ///
  /// Throws [CompilationException] if the document has authoring errors.
  public static CompiledPage compile(PageDocument document, Tokens tokens, OutputMode outputMode) {
    var prepared = prepare(document, tokens, List.of());
    var cssContent = prepared.stylesheet(tokens);
    return paintPrepared(prepared, cssContent, outputMode);
  }

  /// Compiles `document` under the default tokens and returns its element tree instead of HTML.
  ///
  /// Throws [CompilationException] if the document has authoring errors.
  public static PageTree explain(PageDocument document) {
    return explain(document, new Tokens());
  }

  /// Compiles `document` under `tokens` and returns its element tree instead of HTML.
  ///
  /// Throws [CompilationException] if the document has authoring errors.
  public static PageTree explain(PageDocument document, Tokens tokens) {
    return prepare(document, tokens, List.of()).tree();
  }

  /// Lays out and validates `document` under `tokens` without emitting HTML. Paint the result
  /// later with [#paintPrepared]. `siteHead` goes in `<head>` before the page's own head
  /// contributions, so a site can share it across pages.
  ///
  /// Throws [CompilationException] if the document has authoring errors.
  public static PreparedPage prepare(
      PageDocument document, Tokens tokens, List<? extends IntoElement> siteHead) {
    var styles = new StyleSheetCollector();
    var state = new CompilationState(document.title());

    var layout = LayoutPass.layout(document, tokens, siteHead, styles, state);
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
        layout.contributions().headings(),
        layout.contributions().behaviors());
  }

  /// Paints `prepared` as HTML. `cssContent` must be the stylesheet the page links or inlines:
  /// [PreparedPage#stylesheet(Tokens)] for a standalone page, or the merged stylesheet of a site.
  /// Under [OutputMode.ExternalAsset] the link path is derived from `cssContent`, so publish that
  /// text at [CompiledPage#externalCssPath()].
  public static CompiledPage paintPrepared(
      PreparedPage prepared, String cssContent, OutputMode outputMode) {
    var htmlBuffer = new StringBuilder(4096);
    var result =
        HtmlWriter.paint(
            prepared.document(),
            prepared.bodyNodes(),
            cssContent,
            outputMode,
            prepared.headNodes(),
            prepared.behaviors(),
            htmlBuffer);
    return new CompiledPage(
        htmlBuffer.toString(), result.externalCssPath(), cssContent, result.assets());
  }
}
