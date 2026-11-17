package dev.nthings.tessera.compiler;

import dev.nthings.tessera.css.Theme;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Multi-page static site specification; build one with [#builder()] or [#of(SiteRoute...)]. Throws
/// IllegalArgumentException if it has no routes.
///
/// Defaults to [OutputMode#external(String)] (`"/assets"`), so pages share one content-hashed
/// stylesheet across cross-document view transitions, and to [LinkSpeculation#PRERENDER].
public record SiteDefinition(
    Theme theme,
    OutputMode outputMode,
    List<SiteRoute> routes,
    @Nullable String baseUrl,
    LinkSpeculation linkSpeculation,
    List<Asset> assets) {

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

  /// The route published at `path`, normalized like a route path (so `guide/` finds `/guide`), or
  /// empty when no route has that path. Throws IllegalArgumentException if `path` is blank.
  public Optional<SiteRoute> route(String path) {
    var normalized = SiteRoute.normalizeRoutePath(path);
    return routes.stream().filter(route -> route.path().equals(normalized)).findFirst();
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

  /// Routes and assets keep declaration order. Other settings start at the [SiteDefinition]
  /// defaults.
  public static final class Builder {

    private Theme theme = Theme.defaults();
    private OutputMode outputMode = OutputMode.external("/assets");
    private final List<SiteRoute> routes = new ArrayList<>();
    private @Nullable String baseUrl;
    private LinkSpeculation linkSpeculation = LinkSpeculation.PRERENDER;
    private final List<Asset> assets = new ArrayList<>();

    private Builder() {}

    /// Sets the token values for every page. Defaults to [Theme#defaults()].
    public Builder theme(Theme siteTheme) {
      this.theme = siteTheme;
      return this;
    }

    /// Sets how the shared stylesheet and page scripts are packaged. Defaults to
    /// [OutputMode#external(String)] with `/assets`.
    public Builder outputMode(OutputMode mode) {
      this.outputMode = mode;
      return this;
    }

    /// Adds a route. Routes keep the order they are added in, which sets the order of diagnostics,
    /// `sitemap.xml` entries and `search-index.json` entries. Two routes with one path are reported
    /// as `duplicate-route` when the site is compiled.
    public Builder route(SiteRoute route) {
      routes.add(route);
      return this;
    }

    /// Adds `siteRoutes` in iteration order.
    public Builder routes(Collection<SiteRoute> siteRoutes) {
      routes.addAll(siteRoutes);
      return this;
    }

    /// Sets the base URL prepended to route paths in `sitemap.xml`, such as `https://example.com`.
    /// Without one, no sitemap is generated. A trailing `/` is removed. Throws
    /// IllegalArgumentException from [#build()] if the value is empty or only `/`.
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

    /// Adds a file published beside the pages. A file that shares its path with a route or another
    /// file is reported as `file-path-collision` when the site is compiled.
    public Builder asset(Asset asset) {
      assets.add(asset);
      return this;
    }

    /// Adds `siteAssets` in iteration order.
    public Builder assets(Collection<Asset> siteAssets) {
      assets.addAll(siteAssets);
      return this;
    }

    /// Builds the site. Throws IllegalArgumentException if no route was added, or if the base URL
    /// is empty or only `/`.
    public SiteDefinition build() {
      return new SiteDefinition(theme, outputMode, routes, baseUrl, linkSpeculation, assets);
    }
  }
}
