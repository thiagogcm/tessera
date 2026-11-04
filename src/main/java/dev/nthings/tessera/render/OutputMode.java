package dev.nthings.tessera.render;

/// Configures how compiled CSS is packaged into the rendered document.
public sealed interface OutputMode permits OutputMode.InlineStyle, OutputMode.ExternalAsset {

  /// Embeds the compiled stylesheet directly inside a `<style>` tag in `<head>`.
  enum InlineStyle implements OutputMode {
    INSTANCE
  }

  /// Emits a content-hashed `<link rel="stylesheet" href="<basePath>/styles-<hash>.css">` tag in
  /// `<head>` and returns the external stylesheet payload in [PageCompiler.CompiledPage].
  record ExternalAsset(String assetBasePath) implements OutputMode {
    public ExternalAsset {
      if (assetBasePath.isBlank()) {
        throw new IllegalArgumentException("assetBasePath cannot be blank");
      }
    }
  }

  static OutputMode inlineStyle() {
    return InlineStyle.INSTANCE;
  }

  static OutputMode externalAsset(String assetBasePath) {
    return new ExternalAsset(assetBasePath);
  }
}
