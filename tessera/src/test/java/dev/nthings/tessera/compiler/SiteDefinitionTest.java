package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SiteDefinition")
class SiteDefinitionTest {

  private final SiteDefinition site = SiteDefinition.of(documentationRoutes());

  @Test
  @DisplayName("finds a route by its normalized path")
  void findsRouteByNormalizedPath() {
    assertThat(site.route("guide/"))
        .hasValueSatisfying(route -> {
          assertThat(route.path()).isEqualTo("/guide");
          assertThat(route.document().title()).isEqualTo("Guide");
          assertThat(route.displayTitle()).isEqualTo("Getting Started");
        });
  }

  @Test
  @DisplayName("finds nothing at an unregistered path")
  void findsNothingAtUnregisteredPath() {
    assertThat(site.route("/missing")).isEmpty();
  }
}
