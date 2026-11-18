package dev.nthings.tessera.compiler;

import java.util.List;
import java.util.Map;

/// Immutable output of one page: its HTML document, its stylesheet text, and the files the HTML
/// refers to. `css` is always present. External output returns its stylesheet and page scripts.
/// Wasm bundles always contribute separate assets. Publish every asset at its path and apply
/// `responseHeaders` to the HTML response.
public record CompiledPage(
    String html, String css, List<Asset> assets, Map<String, String> responseHeaders) {

  public CompiledPage {
    assets = List.copyOf(assets);
    responseHeaders = Map.copyOf(responseHeaders);
  }
}
