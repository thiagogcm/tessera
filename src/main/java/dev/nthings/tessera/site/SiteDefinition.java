package dev.nthings.tessera.site;

import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.OutputMode;
import java.util.List;

/**
 * Multi-page static site specification.
 *
 * <p>Defaults to {@link OutputMode#externalAsset(String)} ({@code "/assets"}) so all pages share a
 * single deduplicated, content-hashed CSS stylesheet across Cross-Document View Transitions.
 */
public record SiteDefinition(Tokens tokens, OutputMode outputMode, List<SiteRoute> routes) {

  public SiteDefinition {
    if (routes.isEmpty()) {
      throw new IllegalArgumentException("SiteDefinition must contain at least one route");
    }
    routes = List.copyOf(routes);
  }

  public SiteDefinition(List<SiteRoute> routes) {
    this(new Tokens(), OutputMode.externalAsset("/assets"), routes);
  }

  public SiteDefinition(SiteRoute... routes) {
    this(List.of(routes));
  }
}
