package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.view.ViewValidation;
import java.util.List;

/// The page phases: [#prepare] lays out and validates, [#emit] writes HTML. [Compiler] runs both
/// for one page; [SiteCompiler] validates every page before emitting any.
final class PageCompiler {
  private PageCompiler() {}

  static CompiledPage compile(PageDocument document, Theme theme, OutputMode outputMode) {
    var prepared = prepare(document, List.of());
    var css = prepared.stylesheet(theme);
    var resources = new HtmlWriter.Resources(css, outputMode, prepared.scripts());
    return new CompiledPage(emit(prepared, resources), css, resources.assets());
  }

  /// Lays out and validates `document` without emitting HTML. `siteHead` goes in `<head>` before
  /// the page's own head contributions, so a site can share it across pages. Throws
  /// [CompilationException] if the document has authoring errors.
  static PreparedPage prepare(PageDocument document, List<? extends IntoElement> siteHead) {
    var styles = new StyleSheet();
    var layout = DocumentPass.layout(document, siteHead, styles);
    var state = layout.compilation();
    var bodyNodes = layout.bodyNodes();
    var headNodes = layout.headNodes();

    var links =
        ViewValidation.check(
                bodyNodes, headNodes, state.registeredDomIds, state.ambiguousDomIds, state::report)
            .checkDocument(bodyNodes, headNodes);
    links.forEach((url, location) -> state.outgoingLinks.put(url, state.location(location)));
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
        layout.contributions().scripts());
  }

  /// Emits `prepared` with `resources`, the stylesheet and modules the page links or inlines: its
  /// own for a standalone page, or the shared ones of a site.
  static String emit(PreparedPage prepared, HtmlWriter.Resources resources) {
    return HtmlWriter.emit(
        prepared.document(),
        prepared.bodyNodes(),
        prepared.headNodes(),
        resources,
        prepared.scripts());
  }
}
