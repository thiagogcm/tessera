package dev.nthings.tessera.compiler;

import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Compiles pages and sites. Every method throws [CompilationException] when it finds authoring
/// errors, before any HTML is produced; each [Diagnostic] carries a stable rule code such as
/// `duplicate-id` or `image-alt`. Exceptions thrown by render callbacks propagate unchanged.
public final class Compiler {
  private Compiler() {}

  /// Compiles `document` with the default `Tokens` and [OutputMode#inline()].
  public static CompiledPage compile(PageDocument document) {
    return compile(document, new Tokens(), OutputMode.inline());
  }

  /// Compiles `document` with `tokens`, packaging CSS and client behaviors as `outputMode`
  /// specifies. Under [OutputMode#external(String)] the caller must publish every asset of the
  /// result at its path.
  public static CompiledPage compile(PageDocument document, Tokens tokens, OutputMode outputMode) {
    return PageCompiler.compile(document, tokens, outputMode);
  }

  /// Compiles every route of `site` into one [CompiledSite].
  ///
  /// Routes are first checked for duplicate paths (`duplicate-route`) and for registered or
  /// generated files that share a path with a route or another file (`file-path-collision`);
  /// these stop compilation before any page renders. Pages are then prepared concurrently on
  /// virtual threads, so render callbacks must not change shared application state. Once every
  /// page compiles, links, fragments and transition keys are checked across pages, generated
  /// stylesheet and module paths are checked for collisions, the shared stylesheet is merged, and
  /// pages are painted concurrently.
  ///
  /// Throws [CompilationException] with the route and file diagnostics, else with the page
  /// diagnostics in route order, else with the cross-route diagnostics.
  public static CompiledSite compile(SiteDefinition site) {
    return SiteCompiler.compile(site);
  }

  /// Compiles `document` under the default `Tokens` and returns its element tree instead of HTML.
  public static PageTree explain(PageDocument document) {
    return explain(document, new Tokens());
  }

  /// Compiles `document` under `tokens` and returns its element tree instead of HTML.
  public static PageTree explain(PageDocument document, Tokens tokens) {
    return PageCompiler.prepare(document, tokens, List.of()).tree();
  }
}
