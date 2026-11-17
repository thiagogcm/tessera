package dev.nthings.tessera.compiler;

/// How compiled CSS and page scripts are packaged: [#inline()] or [#external(String)].
public sealed interface OutputMode permits InlineOutput, ExternalOutput {

  /// Inline output: the stylesheet is embedded in a `<style>` element in `<head>`, and each page
  /// script in an inline `<script type="module">`. The page has no assets.
  static OutputMode inline() {
    return InlineOutput.INSTANCE;
  }

  /// External output: the page links `<assetBase>/styles-<hash>.css` from `<head>` and loads each
  /// page script from `<assetBase>/scripts/<sanitized id>-<hash>.js`. Hashes derive from the
  /// content, so a changed file is never served stale from a cache. The stylesheet and modules are
  /// returned as [Asset]s, which the caller must publish at their paths.
  ///
  /// `assetBase` must be an absolute URL path such as `/assets`; a trailing `/` is ignored. Throws
  /// IllegalArgumentException if it does not start with `/`, or contains `//` or `..`.
  static OutputMode external(String assetBase) {
    return new ExternalOutput(assetBase);
  }
}
