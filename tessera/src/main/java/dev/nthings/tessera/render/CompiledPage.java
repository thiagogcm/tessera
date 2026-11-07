package dev.nthings.tessera.render;

import java.util.List;
import org.jspecify.annotations.Nullable;

/// Immutable HTML, stylesheet, and generated asset output of page compilation.
public record CompiledPage(
    String html, @Nullable String externalCssPath, String cssContent, List<CompiledAsset> assets) {

  public CompiledPage {
    assets = List.copyOf(assets);
  }
}
