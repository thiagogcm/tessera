package dev.nthings.tessera.render;

/** Configures how compiled CSS is packaged into the rendered document. */
public sealed interface OutputMode permits OutputMode.InlineStyle, OutputMode.ExternalAsset {

  /** Embeds the compiled stylesheet directly inside a {@code <style>} tag in {@code <head>}. */
  enum InlineStyle implements OutputMode {
    INSTANCE
  }

  /**
   * Emits a content-hashed {@code <link rel="stylesheet" href="<basePath>/styles-<hash>.css">} tag
   * in {@code <head>} and returns the external stylesheet payload in {@link
   * PageCompiler.CompiledPage}.
   */
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
