package dev.nthings.tessera.compiler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Multi-page static site specification; build one with [#builder()] or [#of(SiteRoute...)]. Throws
/// IllegalArgumentException if it has no routes.
///
/// Compilation settings ([dev.nthings.tessera.css.Theme] and [OutputMode]) live on [Compiler].
/// Defaults to [LinkSpeculation#PRERENDER].
public record SiteDefinition(
    List<SiteRoute> routes,
    @Nullable URI baseUrl,
    LinkSpeculation linkSpeculation,
    List<Asset> assets) {

  public SiteDefinition {
    if (routes.isEmpty()) {
      throw new IllegalArgumentException("SiteDefinition must contain at least one route");
    }
    routes = List.copyOf(routes);
    assets = List.copyOf(assets);
    if (baseUrl != null) {
      baseUrl = normalizeBaseUrl(baseUrl);
    }
  }

  private static URI normalizeBaseUrl(URI uri) {
    var scheme = uri.getScheme();
    if (scheme == null
        || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
        || uri.getHost() == null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null) {
      throw new IllegalArgumentException(
          "Site baseUrl must be an absolute http(s) URL such as https://example.com: " + uri);
    }
    var text = uri.toASCIIString();
    if (text.endsWith("/")) {
      return URI.create(text.substring(0, text.length() - 1));
    }
    return uri;
  }

  /// The route published at `path`, normalized like a route path (so `guide/` finds `/guide`), or
  /// empty when no route has that path. Throws IllegalArgumentException if `path` is blank.
  public Optional<SiteRoute> route(String path) {
    var normalized = SiteRoute.normalizePath(path);
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

    private final List<SiteRoute> routes = new ArrayList<>();
    private @Nullable URI baseUrl;
    private LinkSpeculation linkSpeculation = LinkSpeculation.PRERENDER;
    private final List<Asset> assets = new ArrayList<>();

    private Builder() {}

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

    /// Sets the base URL prepended to each page's published URL in `sitemap.xml`, such as
    /// `URI.create("https://example.com")`. Without one, no sitemap is generated. A trailing `/` is
    /// removed. Throws IllegalArgumentException from [#build()] unless the value is an absolute
    /// `http` or `https` URI with a host and no query or fragment.
    public Builder baseUrl(URI url) {
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
    /// is not an absolute `http` or `https` URI.
    public SiteDefinition build() {
      return new SiteDefinition(routes, baseUrl, linkSpeculation, assets);
    }
  }
}
