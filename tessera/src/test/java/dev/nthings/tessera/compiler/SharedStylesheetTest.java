package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;
import static dev.nthings.tessera.support.SiteFixtures.stylesheetPath;

import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Theme;
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

    assertThat(site.pages()).containsOnlyKeys("/", "/guide");
    assertThat(site.pages().keySet())
        .allSatisfy(
            route -> {
              var page = RenderedPage.of(site, route);
              assertThat(page.elements("head > link[rel=stylesheet]")).hasSize(1);
              assertThat(page.element("head > link[rel=stylesheet]"))
                  .hasAttribute("href", stylesheetPath(site));
              assertThat(page.elements("head > style")).isEmpty();
            });
  }

  @Nested
  @DisplayName("site theme tokens")
  class SiteTheme {

    private static CompiledSite site;

    @BeforeAll
    static void compileSiteWithBrandToken() {
      var brand = new DesignToken.Color("brand-hue", CssColor.oklch(60, 0.2, 300));
      site =
          Compiler.compile(
              SiteDefinition.builder()
                  .theme(Theme.defaults().with(brand, brand.defaultValue()))
                  .route(new SiteRoute("/", new PageDocument("Home", main().child(p().text("x")))))
                  .build());
    }

    @Test
    @DisplayName("site theme tokens are declared in the shared stylesheet")
    void declaresThemeTokens() {
      var stylesheet = site.files().get(stylesheetPath(site));

      assertThat(site.css()).contains("--brand-hue:oklch(60% 0.2 300)");
      assertThat(stylesheet.contentType()).isEqualTo("text/css; charset=utf-8");
      assertThat(stylesheet.textContent()).isEqualTo(site.css());
    }
  }
}
