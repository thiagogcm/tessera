package dev.nthings.tessera.render;

import java.util.List;
import org.jspecify.annotations.Nullable;

/// Immutable output of one page: its HTML document, its stylesheet text, and the files it refers
/// to. `cssContent` is always present, whether or not it is inlined. `externalCssPath` is null
/// under [OutputMode#inlineStyle()]. `assets` lists the client behavior modules under
/// [OutputMode#externalAsset(String)] and is empty under inline output.
public record CompiledPage(
    String html, @Nullable String externalCssPath, String cssContent, List<CompiledAsset> assets) {

  public CompiledPage {
    assets = List.copyOf(assets);
  }
}
