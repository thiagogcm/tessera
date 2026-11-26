package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SiteDefinition")
class SiteDefinitionTest {

  @Test
  @DisplayName("finds a route by its normalized path, and nothing at an unregistered one")
  void findsRouteByNormalizedPath() {
    var site = SiteDefinition.of(documentationRoutes());

    assertThat(site.route("guide/")).map(SiteRoute::displayTitle).hasValue("Getting Started");
    assertThat(site.route("/missing")).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "/",
        "example.com",
        "//example.com",
        "/docs",
        "https://example.com?q=1",
        "https://user:secret@example.com"
      })
  @DisplayName("a base URL must be an absolute origin")
  void rejectsBaseUrlWithoutOrigin(String baseUrl) {
    var builder =
        SiteDefinition.builder().routes(documentationRoutes()).baseUrl(URI.create(baseUrl));

    assertThatIllegalArgumentException().isThrownBy(builder::build);
  }
}
