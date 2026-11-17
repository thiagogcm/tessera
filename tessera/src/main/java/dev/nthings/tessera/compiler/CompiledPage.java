package dev.nthings.tessera.compiler;

import java.util.List;

/// Immutable output of one page: its HTML document, its stylesheet text, and the files the HTML
/// refers to. `css` is always present, whether or not it is inlined. Under [OutputMode#inline()]
/// `assets` is empty; under [OutputMode#external(String)] it holds the stylesheet first, then each
/// page script. Publish every asset at its path.
public record CompiledPage(String html, String css, List<Asset> assets) {

  public CompiledPage {
    assets = List.copyOf(assets);
  }
}
