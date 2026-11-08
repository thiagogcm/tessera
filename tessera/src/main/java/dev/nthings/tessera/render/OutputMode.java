package dev.nthings.tessera.render;

/// How compiled CSS and client behavior modules are packaged into the rendered output. Create one
/// with [#inlineStyle()] or [#externalAsset(String)].
public sealed interface OutputMode permits OutputMode.InlineStyle, OutputMode.ExternalAsset {

  /// Inline output, from [#inlineStyle()].
  enum InlineStyle implements OutputMode {
    INSTANCE
  }

  /// External output, from [#externalAsset(String)].
  record ExternalAsset(String assetBasePath) implements OutputMode {
    public ExternalAsset {
      if (assetBasePath.isBlank()) {
        throw new IllegalArgumentException("assetBasePath cannot be blank");
      }
    }
  }

  /// Inline output: the stylesheet is embedded in a `<style>` element in `<head>`, and each client
  /// behavior module is embedded in an inline `<script type="module">`. The page has no external
  /// stylesheet path and no [CompiledPage#assets()].
  static OutputMode inlineStyle() {
    return InlineStyle.INSTANCE;
  }

  /// External output: the page links `<assetBasePath>/styles-<hash>.css` from `<head>` and loads
  /// each client behavior module from `<assetBasePath>/behaviors/<sanitized id>-<hash>.js`, where
  /// each hash is derived from the content. The modules are listed in [CompiledPage#assets()].
  /// The caller must publish them, along with the stylesheet at [CompiledPage#externalCssPath()].
  ///
  /// Hashed names mean a changed file is never served stale from a cache. A trailing `/` on
  /// `assetBasePath` is ignored. Throws IllegalArgumentException if `assetBasePath` is blank. For
  /// sites, the base path must start with `/`.
  static OutputMode externalAsset(String assetBasePath) {
    return new ExternalAsset(assetBasePath);
  }
}
