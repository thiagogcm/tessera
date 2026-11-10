package dev.nthings.tessera.view;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/// Renders one turn of a mounted view, such as a browser root in `tessera-wasm`.
/// Expansion, linking, styles and semantic checks are shared with the compiler.
/// Document placement, whole-page checks and HTML emission belong to the compiler.
///
/// A mounted view has no document, so document-only features are reported as diagnostics: head
/// contributions (`native-head-contribution`), document-end sections (`native-document-end`),
/// client behaviors (`native-page-script`, including the interest-tooltip fallback),
/// deferred and late elements (`native-document-placement`) and raw HTML (`native-raw-html`).
/// `DocumentContext#document` values and headings last one render.
public final class ViewRenderer {

  private ViewRenderer() {}

  /// Renders what `content` returns into a linked snapshot with its embedded style rules. Every
  /// generated and scoped DOM id is prefixed with `scope` (sanitized like a view id), so roots on
  /// one page keep distinct ids; pass an empty `scope` for page-level ids. Throws
  /// [ViewException] listing every diagnostic if the render has authoring errors; a failing render
  /// callback propagates its own exception with a suppressed [ViewException] naming the component.
  public static ViewSnapshot render(
      Function<DocumentContext, ? extends IntoElement> content, String scope) {
    var backend = new LiveBackend();
    var expansion =
        new ViewExpansion(
            backend,
            backend.styles,
            scope.isEmpty()
                ? ViewScope.ROOT
                : ViewScope.ROOT.extend(Fnv1a64.sanitizeLocalId(scope), false));
    var nodes = expansion.link(expansion.expand(Location.ROOT, "Expand", content));
    ViewValidation.check(
        nodes, List.of(), expansion.ids(), expansion.ambiguousIds(), expansion.reporter());
    if (!expansion.diagnostics().isEmpty()) {
      throw new ViewException(expansion.diagnostics());
    }
    return new ViewSnapshot(
        nodes, backend.styles.embeddedRules(), expansion.references(), expansion.ids());
  }

  /// The mounted-view policy: no document, effects and live styles.
  private static final class LiveBackend implements ViewExpansion.Backend {
    private final StyleSheet styles = new StyleSheet();

    @Override
    public ViewNode defer(
        Supplier<ViewNode> content,
        int priority,
        Location placedBy,
        ViewExpansion.Reporter reporter) {
      return unplaced(placedBy, "Deferred", reporter);
    }

    @Override
    public ViewNode late(
        Supplier<ViewNode> content, Location placedBy, ViewExpansion.Reporter reporter) {
      return unplaced(placedBy, "Late", reporter);
    }

    @Override
    public ViewNode raw(String html, Location placedBy, ViewExpansion.Reporter reporter) {
      return reporter.reject(
          placedBy,
          "native-raw-html",
          "Mounted views do not accept raw HTML",
          "Render text or elements instead of trusted raw HTML.");
    }

    @Override
    public ViewNode effect(ViewSnapshot.Effect effect, ViewExpansion.Reporter reporter) {
      return effect;
    }

    @Override
    public void liveDeclarations(
        Declarations live, Map<String, String> properties, Declarations into) {
      styles.registerLive(live, properties, into);
    }

    @Override
    public void contributeHead(
        Element element, Location placedBy, ViewExpansion.Reporter reporter) {
      reporter.report(
          placedBy,
          "native-head-contribution",
          "Head contributions require a document backend",
          "Render this component in a static page, or move the head content into the host page.");
    }

    @Override
    public void atDocumentEnd(
        DocumentKey<?> owner,
        Function<DocumentContext, ? extends IntoElement> section,
        Location placedBy,
        ViewExpansion.Reporter reporter) {
      reporter.report(
          placedBy,
          "native-document-end",
          "Document-end sections require a document backend",
          "Render the section as ordinary content of the mounted view.");
    }

    @Override
    public void contributeScript(
        PageScript behavior, Location placedBy, ViewExpansion.Reporter reporter) {
      reporter.report(
          placedBy,
          "native-page-script",
          "Page script '" + behavior.id() + "' requires a document backend",
          "Handle the interaction with Java event handlers in a browser mount.");
    }

    private static ViewNode unplaced(
        Location placedBy, String kind, ViewExpansion.Reporter reporter) {
      return reporter.reject(
          placedBy,
          "native-document-placement",
          kind + " elements require document placement",
          "Render the content in place from current application state.");
    }
  }
}
