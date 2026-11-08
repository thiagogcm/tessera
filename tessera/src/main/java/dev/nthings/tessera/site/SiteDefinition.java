package dev.nthings.tessera.site;

import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.OutputMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Multi-page static site specification; build one with [#builder()] or [#of(SiteRoute...)].
/// Throws IllegalArgumentException if it has no routes.
///
/// Defaults to [OutputMode#externalAsset(String)] (`"/assets"`), so pages share one
/// content-hashed stylesheet across cross-document view transitions, and to
/// [LinkSpeculation#PRERENDER].
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

  /// Returns a site with `routes` and default settings.
  public static SiteDefinition of(SiteRoute... routes) {
    return builder().routes(List.of(routes)).build();
  }

  /// Returns a site with `routes`, in iteration order, and default settings.
  public static SiteDefinition of(Collection<SiteRoute> routes) {
    return builder().routes(routes).build();
  }

  /// Returns an empty builder. Settings start at the [SiteDefinition] defaults.
  public static Builder builder() {
    return new Builder();
  }

  /// Routes and assets keep declaration order. Other settings start at the
  /// [SiteDefinition] defaults.
  public static final class Builder {

    private Tokens tokens = new Tokens();
    private OutputMode outputMode = OutputMode.externalAsset("/assets");
    private final List<SiteRoute> routes = new ArrayList<>();
    private @Nullable String baseUrl;
    private LinkSpeculation linkSpeculation = LinkSpeculation.PRERENDER;
    private final List<SiteAsset> assets = new ArrayList<>();

    private Builder() {}

    /// Sets the design tokens for every page. Defaults to `new Tokens()`.
    public Builder tokens(Tokens siteTokens) {
      this.tokens = siteTokens;
      return this;
    }

    /// Sets how the shared stylesheet and client behavior modules are packaged. Under
    /// [OutputMode#externalAsset(String)] the base path must start with `/`; a relative path makes
    /// [SiteCompiler#compile(SiteDefinition)] throw IllegalArgumentException.
    public Builder outputMode(OutputMode mode) {
      this.outputMode = mode;
      return this;
    }

    /// Adds a route. Routes keep the order they are added in, which sets the order of diagnostics,
    /// `sitemap.xml` entries and `search-index.json` entries.
    public Builder route(SiteRoute route) {
      routes.add(route);
      return this;
    }

    /// Adds `siteRoutes` in iteration order.
    public Builder routes(Collection<SiteRoute> siteRoutes) {
      routes.addAll(siteRoutes);
      return this;
    }

    /// Sets the base URL prepended to route paths in `sitemap.xml`, such as
    /// `https://example.com`. Without one, no sitemap is generated. A trailing `/` is removed.
    /// Throws IllegalArgumentException from [#build()] if the value is empty or only `/`.
    public Builder baseUrl(String url) {
      this.baseUrl = url;
      return this;
    }

    /// Sets the same-site speculation rules added to each page's `<head>`. Defaults to
    /// [LinkSpeculation#PRERENDER].
    public Builder linkSpeculation(LinkSpeculation speculation) {
      this.linkSpeculation = speculation;
      return this;
    }

    /// Adds a file published beside the pages.
    public Builder asset(SiteAsset asset) {
      assets.add(asset);
      return this;
    }

    /// Adds `siteAssets` in iteration order.
    public Builder assets(Collection<SiteAsset> siteAssets) {
      assets.addAll(siteAssets);
      return this;
    }

    /// Builds the site. Throws IllegalArgumentException if no route was added, or if the base URL
    /// is empty or only `/`.
    public SiteDefinition build() {
      return new SiteDefinition(tokens, outputMode, routes, baseUrl, linkSpeculation, assets);
    }
  }
}
