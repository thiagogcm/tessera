package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;
import static dev.nthings.tessera.support.SiteFixtures.stylesheetPath;

import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Site-wide shared stylesheet")
class SharedStylesheetTest {

  @Test
  @DisplayName("every page links the shared stylesheet from <head>")
  void everyPageLinksSharedStylesheet() {
    var site = Compiler.compile(siteWithLogo());

    assertThat(site.pages().keySet())
        .allSatisfy(
            route ->
                assertThat(RenderedPage.of(site, route).element("head > link[rel=stylesheet]"))
                    .hasAttribute("href", stylesheetPath(site)));
  }

  @Nested
  @DisplayName("site tokens")
  class SiteTokens {

    private static CompiledSite site;

    @BeforeAll
    static void compileSiteWithBrandToken() {
      var brand = new DesignToken.Color("brand-hue", CssColor.oklch(60, 0.2, 300));
      RenderOnce tokenProbe =
          context -> p().text(context.tokens().all().contains(brand) ? "brand" : "default");
      site =
          Compiler.compile(
              SiteDefinition.builder()
                  .tokens(new Tokens().register(brand))
                  .route(new SiteRoute("/", new PageDocument("Home", main().child(tokenProbe))))
                  .build());
    }

    @Test
    @DisplayName("are visible to rendering")
    void areVisibleToRendering() {
      assertThat(RenderedPage.of(site, "/").element("main p")).hasText("brand");
    }

    @Test
    @DisplayName("are declared in the shared stylesheet")
    void areDeclaredInSharedStylesheet() {
      assertThat(site.css()).contains("--brand-hue:oklch(60% 0.2 300)");
    }
  }
}
