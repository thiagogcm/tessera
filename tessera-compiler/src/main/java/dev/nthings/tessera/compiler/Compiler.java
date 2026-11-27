package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.css.Theme;

/// Immutable compiler configuration pairing a [Theme] with an [OutputMode]. Start from [#forPage()]
/// (default theme, inline CSS and scripts) or [#forSite()] (default theme, external assets under
/// `/assets`), refine with [#withTheme(Theme)] and [#withOutputMode(OutputMode)], and compile pages
/// or sites.
///
/// A compiler accepts only a theme that passes [Theme#contrastDiagnostics()]: creating one with any
/// other theme, as [#withTheme(Theme)] does, throws [CompilationException] with the theme's
/// `theme-contrast` diagnostics, before any page is compiled.
///
/// [#compile(PageDocument)] and [#compile(SiteDefinition)] throw [CompilationException] on
/// authoring errors, carrying every collected diagnostic. Exceptions thrown by expand callbacks
/// propagate unchanged.
public record Compiler(Theme theme, OutputMode outputMode) {

  private static final Compiler FOR_PAGE = new Compiler(Theme.defaults(), OutputMode.inline());
  private static final Compiler FOR_SITE =
      new Compiler(Theme.defaults(), OutputMode.external(OutputMode.DEFAULT_EXTERNAL_BASE));

  /// Throws [CompilationException] if `theme` fails WCAG AA text contrast.
  public Compiler {
    var failures = theme.contrastDiagnostics();
    if (!failures.isEmpty()) {
      throw new CompilationException(failures);
    }
  }

  /// Compiler configured for a standalone page: [Theme#defaults()] and [OutputMode#inline()].
  public static Compiler forPage() {
    return FOR_PAGE;
  }

  /// Compiler configured for a multi-page site: [Theme#defaults()] and
  /// [OutputMode#external(String)] (`"/assets"`).
  public static Compiler forSite() {
    return FOR_SITE;
  }

  /// Returns a compiler with `theme` and this compiler's output mode. Throws [CompilationException]
  /// if `theme` fails WCAG AA text contrast.
  @CheckReturnValue
  public Compiler withTheme(Theme theme) {
    return new Compiler(theme, outputMode);
  }

  /// Returns a compiler with `outputMode` and this compiler's theme.
  @CheckReturnValue
  public Compiler withOutputMode(OutputMode outputMode) {
    return new Compiler(theme, outputMode);
  }

  /// Compiles `document` with this compiler's theme and output mode, throwing
  /// [CompilationException] on authoring errors. Publish every returned asset at its path and apply
  /// the response headers to the page when serving it.
  public CompiledPage compile(PageDocument document) {
    return PageCompiler.compile(document, theme, outputMode);
  }

  /// Compiles every route of `site` into one [CompiledSite] using this compiler's theme and output
  /// mode, throwing [CompilationException] on authoring errors.
  public CompiledSite compile(SiteDefinition site) {
    return SiteCompiler.compile(site, theme, outputMode);
  }

  /// Lays out and validates `document` and returns its element tree instead of HTML. The theme and
  /// output mode do not affect the element tree.
  public static PageTree explain(PageDocument document) {
    return PageCompiler.prepare(document, null, new BundlePublication(OutputMode.inline())).tree();
  }
}
