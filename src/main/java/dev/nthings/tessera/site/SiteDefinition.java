package dev.nthings.tessera.site;

import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.OutputMode;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Multi-page static site specification.
 *
 * <p>Defaults to {@link OutputMode#externalAsset(String)} ({@code "/assets"}) so all pages share a
 * single deduplicated, content-hashed CSS stylesheet across Cross-Document View Transitions.
 */
public record SiteDefinition(
    Tokens tokens, OutputMode outputMode, List<SiteRoute> routes, @Nullable String baseUrl) {

  public SiteDefinition {
    if (routes.isEmpty()) {
      throw new IllegalArgumentException("SiteDefinition must contain at least one route");
    }
    routes = List.copyOf(routes);
    if (baseUrl != null) {
      baseUrl = baseUrl.trim();
      if (baseUrl.endsWith("/")) {
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
      }
    }
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
