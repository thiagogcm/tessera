package dev.nthings.tessera.site;

import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.OutputMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Multi-page static site specification; build one with [#builder()] or [#of(SiteRoute...)].
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
      baseUrl = baseUrl.strip();
      if (baseUrl.endsWith("/")) {
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
      }
      if (baseUrl.isEmpty()) {
        throw new IllegalArgumentException("Site baseUrl must be an absolute origin");
      }
    }
  }

  /// Site with default settings serving `routes`.
  public static SiteDefinition of(SiteRoute... routes) {
    return builder().routes(List.of(routes)).build();
  }

  /// Site with default settings serving `routes`.
  public static SiteDefinition of(Collection<SiteRoute> routes) {
    return builder().routes(routes).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /// Accumulates routes and assets in declaration order; every other setting starts at the
  /// defaults documented on [SiteDefinition].
  public static final class Builder {

    private Tokens tokens = new Tokens();
    private OutputMode outputMode = OutputMode.externalAsset("/assets");
    private final List<SiteRoute> routes = new ArrayList<>();
    private @Nullable String baseUrl;
    private LinkSpeculation linkSpeculation = LinkSpeculation.PRERENDER;
    private final List<SiteAsset> assets = new ArrayList<>();

    private Builder() {}

    public Builder tokens(Tokens siteTokens) {
      this.tokens = siteTokens;
      return this;
    }

    public Builder outputMode(OutputMode mode) {
      this.outputMode = mode;
      return this;
    }

    public Builder route(SiteRoute route) {
      routes.add(route);
      return this;
    }

    public Builder routes(Collection<SiteRoute> siteRoutes) {
      routes.addAll(siteRoutes);
      return this;
    }

    /// Absolute origin (e.g. `https://example.com`) used for `sitemap.xml`; without it no
    /// sitemap is generated.
    public Builder baseUrl(String url) {
      this.baseUrl = url;
      return this;
    }

    public Builder linkSpeculation(LinkSpeculation speculation) {
      this.linkSpeculation = speculation;
      return this;
    }

    public Builder asset(SiteAsset asset) {
      assets.add(asset);
      return this;
    }

    public Builder assets(Collection<SiteAsset> siteAssets) {
      assets.addAll(siteAssets);
      return this;
    }

    public SiteDefinition build() {
      return new SiteDefinition(tokens, outputMode, routes, baseUrl, linkSpeculation, assets);
    }
  }
}
