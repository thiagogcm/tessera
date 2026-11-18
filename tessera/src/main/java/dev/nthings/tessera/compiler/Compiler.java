package dev.nthings.tessera.compiler;

import dev.nthings.tessera.css.Theme;

/// Compiles pages and sites. Every method throws [CompilationException] when it finds authoring
/// errors, before any HTML is produced; each reported error carries a stable rule code such as
/// `duplicate-id` or `image-alt`. Exceptions thrown by expand callbacks propagate unchanged.
public final class Compiler {
  private Compiler() {}

  /// Compiles `document` with [Theme#defaults()] and [OutputMode#inline()].
  public static CompiledPage compile(PageDocument document) {
    return compile(document, Theme.defaults(), OutputMode.inline());
  }

  /// Compiles `document` with `theme`'s token values, packaging CSS and page scripts as
  /// `outputMode` specifies. Publish every returned asset at its path and apply the response
  /// headers to the page when serving it.
  public static CompiledPage compile(PageDocument document, Theme theme, OutputMode outputMode) {
    return PageCompiler.compile(document, theme, outputMode);
  }

  /// Compiles every route of `site` into one [CompiledSite].
  ///
  /// Routes are first checked for duplicate paths (`duplicate-route`) and for registered or
  /// generated files that share a path with a route or another file (`file-path-collision`); these
  /// stop compilation before any page renders. Pages are then prepared concurrently on virtual
  /// threads, so expand callbacks must not change shared application state. Once every page
  /// compiles, links, fragments and transition keys are checked across pages, generated stylesheet
  /// and module paths are checked for collisions, the shared stylesheet is merged, and pages are
  /// emitted concurrently.
  ///
  /// Throws [CompilationException] with the route and file diagnostics, else with the page
  /// diagnostics in route order, else with the cross-route diagnostics.
  public static CompiledSite compile(SiteDefinition site) {
    return SiteCompiler.compile(site);
  }

  /// Lays out and validates `document` and returns its element tree instead of HTML. The theme does
  /// not affect the tree, so there is no theme overload.
  public static PageTree explain(PageDocument document) {
    return PageCompiler.prepare(document, new BundlePublication(OutputMode.inline())).tree();
  }
}
