package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
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

  /// Compiles `document` under the default tokens and returns its element tree instead of HTML.
  public static PageTree explain(PageDocument document) {
    return explain(document, new Tokens());
  }

  /// Compiles `document` under `tokens` and returns its element tree instead of HTML.
  public static PageTree explain(PageDocument document, Tokens tokens) {
    return prepare(document, tokens, List.of()).tree();
  }

  /// Runs layout and prepaint for `document` under `tokens`, with `siteHead` elements (shared by
  /// every page of a site) placed in `<head>` ahead of anything the page contributes.
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
