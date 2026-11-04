package dev.nthings.tessera.site;

import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.OutputMode;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Multi-page static site specification.
///
/// Defaults to [OutputMode#externalAsset(String)] (`"/assets"`) so all pages share a single
/// deduplicated, content-hashed CSS stylesheet across Cross-Document View Transitions, and to
/// [LinkSpeculation#PRERENDER] for same-site links.
public record SiteDefinition(
    Tokens tokens,
    OutputMode outputMode,
    List<SiteRoute> routes,
    @Nullable String baseUrl,
    LinkSpeculation linkSpeculation,
    List<SiteAsset> assets) {

  public SiteDefinition {
    if (routes.isEmpty()) {
      throw new IllegalArgumentException("SiteDefinition must contain at least one route");
    }
    routes = List.copyOf(routes);
    assets = List.copyOf(assets);
    if (baseUrl != null) {
      baseUrl = baseUrl.trim();
      if (baseUrl.endsWith("/")) {
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
      }
    }
  }

  public SiteDefinition(
      Tokens tokens, OutputMode outputMode, List<SiteRoute> routes, @Nullable String baseUrl) {
    this(tokens, outputMode, routes, baseUrl, LinkSpeculation.PRERENDER, List.of());
  }

  public SiteDefinition withAssets(List<SiteAsset> siteAssets) {
    return new SiteDefinition(tokens, outputMode, routes, baseUrl, linkSpeculation, siteAssets);
  }

  public SiteDefinition withLinkSpeculation(LinkSpeculation speculation) {
    return new SiteDefinition(tokens, outputMode, routes, baseUrl, speculation, assets);
  }

  public SiteDefinition(Tokens tokens, OutputMode outputMode, List<SiteRoute> routes) {
    this(tokens, outputMode, routes, null);
  }

  public SiteDefinition(List<SiteRoute> routes) {
    this(new Tokens(), OutputMode.externalAsset("/assets"), routes, null);
  }

  public SiteDefinition(SiteRoute... routes) {
    this(List.of(routes));
  }
}
