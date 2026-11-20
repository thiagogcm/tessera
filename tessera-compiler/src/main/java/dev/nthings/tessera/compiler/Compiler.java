package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.css.Theme;

/// Immutable compiler configuration pairing a [Theme] with an [OutputMode]. Start from [#forPage()]
/// (default theme, inline CSS and scripts) or [#forSite()] (default theme, external assets under
/// `/assets`), refine with [#withTheme(Theme)] and [#withOutputMode(OutputMode)], and compile pages
/// or sites.
///
/// [#compile(PageDocument)] and [#compile(SiteDefinition)] throw [CompilationException] on
/// authoring errors; [#tryCompile(PageDocument)] and [#tryCompile(SiteDefinition)] return a
/// [CompilationResult] instead. Exceptions thrown by expand callbacks propagate unchanged.
public record Compiler(Theme theme, OutputMode outputMode) {

  private static final Compiler FOR_PAGE = new Compiler(Theme.defaults(), OutputMode.inline());
  private static final Compiler FOR_SITE =
      new Compiler(Theme.defaults(), OutputMode.external(OutputMode.DEFAULT_EXTERNAL_BASE));

  /// Compiler configured for a standalone page: [Theme#defaults()] and [OutputMode#inline()].
  public static Compiler forPage() {
    return FOR_PAGE;
  }

  /// Compiler configured for a multi-page site: [Theme#defaults()] and
  /// [OutputMode#external(String)] (`"/assets"`).
  public static Compiler forSite() {
    return FOR_SITE;
  }

  /// Returns a compiler with `theme` and this compiler's output mode.
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

  /// Compiles `document` and returns [CompilationResult.Compiled] or [CompilationResult.Failed]
  /// without throwing on authoring errors.
  public CompilationResult<CompiledPage> tryCompile(PageDocument document) {
    try {
      return new CompilationResult.Compiled<>(compile(document));
    } catch (CompilationException failure) {
      return new CompilationResult.Failed<>(failure.diagnostics());
    }
  }

  /// Compiles every route of `site` into one [CompiledSite] using this compiler's theme and output
  /// mode, throwing [CompilationException] on authoring errors.
  public CompiledSite compile(SiteDefinition site) {
    return SiteCompiler.compile(site, theme, outputMode);
  }

  /// Compiles every route of `site` and returns [CompilationResult.Compiled] or
  /// [CompilationResult.Failed] without throwing on authoring errors.
  public CompilationResult<CompiledSite> tryCompile(SiteDefinition site) {
    try {
      return new CompilationResult.Compiled<>(compile(site));
    } catch (CompilationException failure) {
      return new CompilationResult.Failed<>(failure.diagnostics());
    }
  }

  /// Lays out and validates `document` and returns its element tree instead of HTML. The theme and
  /// output mode do not affect the element tree.
  public static PageTree explain(PageDocument document) {
    return PageCompiler.prepare(document, new BundlePublication(OutputMode.inline())).tree();
  }
}
