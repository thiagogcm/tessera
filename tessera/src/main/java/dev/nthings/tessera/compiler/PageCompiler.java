package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.view.ViewSnapshot;
import dev.nthings.tessera.view.ViewValidation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// The page phases: [#prepare] lays out and validates, [#emit] writes HTML. [Compiler] runs both
/// for one page; [SiteCompiler] validates every page before emitting any.
final class PageCompiler {
  // Children of these elements cannot be active div mounts in the parsed document.
  private static final Set<TagName> MOUNT_UNAVAILABLE =
      Set.of(
          HtmlTag.TEMPLATE,
          HtmlTag.TEXTAREA,
          HtmlTag.IFRAME,
          HtmlTag.STYLE,
          HtmlTag.SELECT,
          HtmlTag.OPTION,
          HtmlTag.OPTGROUP,
          HtmlTag.SELECTEDCONTENT);

  private PageCompiler() {}

  static CompiledPage compile(PageDocument document, Theme theme, OutputMode outputMode) {
    var prepared = prepare(document, new BundlePublication(outputMode));
    var css = prepared.stylesheet(theme);
    var resources = new HtmlWriter.Resources(css, outputMode, prepared.scripts());
    var assets = new ArrayList<>(resources.assets());
    assets.addAll(prepared.bundleAssets());
    return new CompiledPage(
        emit(prepared, resources, null), css, assets, prepared.responseHeaders(outputMode));
  }

  /// Lays out and validates `document` without emitting HTML. Throws [CompilationException] if the
  /// document has authoring errors.
  static PreparedPage prepare(PageDocument document, BundlePublication bundles) {
    var styles = new StyleSheet();
    var layout = DocumentPass.layout(document, styles, bundles);
    var state = layout.compilation();
    var bodyNodes = layout.bodyNodes();
    var headNodes = layout.headNodes();

    var links =
        ViewValidation.check(
                bodyNodes, headNodes, state.registeredDomIds, state.ambiguousDomIds, state::report)
            .checkDocument(bodyNodes, headNodes);
    links.forEach((url, location) -> state.outgoingLinks.put(url, state.location(location)));
    if (layout.contributions().bootstrap() != null) {
      validateMounts(bodyNodes, state, false);
      validateMounts(headNodes, state, true);
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
        layout.headings(),
        layout.contributions().scripts(),
        layout.contributions().bundleAssets(),
        layout.contributions().bootstrap());
  }

  private static void validateMounts(
      List<ViewSnapshot.Node<String>> nodes, CompilationState state, boolean forbidden) {
    for (var node : nodes) {
      switch (node) {
        case ViewSnapshot.Tag<String> tag -> {
          var mount = tag.attributes().containsKey("data-tessera-bundle");
          if (mount && forbidden) {
            state.report(
                tag.location(),
                "wasm-mount-placement",
                "Wasm mounts must be independent hosts in the body",
                "Place the mount outside the head, other mounts, and elements that cannot contain an active host.");
          }
          validateMounts(
              tag.children(), state, forbidden || mount || MOUNT_UNAVAILABLE.contains(tag.tag()));
        }
        case ViewSnapshot.Group<String> group -> validateMounts(group.children(), state, forbidden);
        default -> {}
      }
    }
  }

  /// Emits `prepared` with `resources`, the stylesheet and modules the page links or inlines: its
  /// own for a standalone page, or the shared ones of a site.
  static String emit(
      PreparedPage prepared, HtmlWriter.Resources resources, @Nullable String speculationRules) {
    return HtmlWriter.emit(
        prepared.document(),
        prepared.bodyNodes(),
        prepared.headNodes(),
        resources,
        prepared.scripts(),
        prepared.bundleBootstrap(),
        speculationRules);
  }
}
