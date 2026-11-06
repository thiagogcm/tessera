package dev.nthings.tessera.site;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;

import dev.nthings.tessera.components.CardGrid;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.CompiledPage;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Site-wide shared stylesheet")
class SharedStylesheetTest {

  @Test
  @DisplayName("view keys are content-addressed, so pages with different @scope CSS never clash")
  void viewKeysAreContentAddressed() {
    var site =
        SiteCompiler.compile(
            SiteDefinition.of(
                List.of(
                    new SiteRoute("/", gridPage("A", CssLength.rem(18), "a")),
                    new SiteRoute("/b", gridPage("B", CssLength.rem(15), "b")))));

    var viewA = viewKey(site.pagesByRoute().get("/"));
    var viewB = viewKey(site.pagesByRoute().get("/b"));

    assertThat(viewA).isNotEqualTo(viewB);
    assertThat(site.sharedCssContent())
        .contains("@scope ([data-view=\"" + viewA + "\"])")
        .contains("@scope ([data-view=\"" + viewB + "\"])");
  }

  @Test
  @DisplayName("every page links the shared stylesheet from <head>")
  void everyPageLinksSharedStylesheet() {
    var site = SiteCompiler.compile(siteWithLogo());

    assertThat(site.pagesByRoute().values())
        .allSatisfy(
            page ->
                assertThat(RenderedPage.of(page).element("head > link[rel=stylesheet]"))
                    .hasAttribute("href", site.sharedCssPath()));
  }

  private static PageDocument gridPage(String title, CssLength minColumn, String text) {
    return new PageDocument(
        title, main().child(CardGrid.of("g", minColumn, CssLength.rem(1), p().text(text))));
  }

  private static String viewKey(CompiledPage page) {
    return RenderedPage.of(page).element("[data-view^=card-grid-]").attr("data-view");
  }

  @Nested
  @DisplayName("site tokens")
  class SiteTokens {

    private static CompiledSite site;

    @BeforeAll
    static void compileSiteWithBrandToken() {
      var brand = new DesignToken.Color("brand-hue", CssColor.oklch(60, 0.2, 300));
      RenderOnce tokenProbe =
          () ->
              p().text(
                      RenderContext.current().tokens().all().contains(brand) ? "brand" : "default");
      site =
          SiteCompiler.compile(
              SiteDefinition.builder()
                  .tokens(new Tokens().register(brand))
                  .route(new SiteRoute("/", new PageDocument("Home", main().child(tokenProbe))))
                  .build());
    }

    @Test
    @DisplayName("are visible to rendering")
    void areVisibleToRendering() {
      assertThat(RenderedPage.of(site.pagesByRoute().get("/")).element("main p")).hasText("brand");
    }

    @Test
    @DisplayName("are declared in the shared stylesheet")
    void areDeclaredInSharedStylesheet() {
      assertThat(site.sharedCssContent()).contains("--brand-hue:oklch(60% 0.2 300)");
    }
  }
}
