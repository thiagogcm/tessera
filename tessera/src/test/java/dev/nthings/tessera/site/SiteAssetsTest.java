package dev.nthings.tessera.site;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_PATH;
import static dev.nthings.tessera.support.SiteFixtures.homeWithLogoAndGuide;
import static dev.nthings.tessera.support.SiteFixtures.logo;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;

import dev.nthings.tessera.render.CompilationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Site assets")
class SiteAssetsTest {

  @Test
  @DisplayName("an image referencing an unregistered asset is rejected")
  void rejectsImageReferencingUnregisteredAsset() {
    var site = SiteDefinition.of(homeWithLogoAndGuide());

    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> SiteCompiler.compile(site))
        .withMessageContaining(LOGO_PATH);
  }

  @Test
  @DisplayName("a registered asset is published among the site's files")
  void publishesRegisteredAsset() {
    assertThat(SiteCompiler.compile(siteWithLogo()).files()).containsKey(LOGO_PATH);
  }

  @Test
  @DisplayName("an asset colliding with a generated file is rejected")
  void rejectsAssetCollidingWithGeneratedFile() {
    var site =
        SiteDefinition.builder()
            .routes(homeWithLogoAndGuide())
            .asset(logo())
            .asset(SiteAsset.text("/search-index.json", "[]"))
            .build();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> SiteCompiler.compile(site))
        .withMessageContaining("/search-index.json");
  }

  @Test
  @DisplayName("an asset colliding with a route is rejected")
  void rejectsAssetCollidingWithRoute() {
    var site =
        SiteDefinition.builder()
            .routes(homeWithLogoAndGuide())
            .asset(SiteAsset.text("/guide", "x"))
            .build();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> SiteCompiler.compile(site))
        .withMessageContaining("collides with route");
  }
}
