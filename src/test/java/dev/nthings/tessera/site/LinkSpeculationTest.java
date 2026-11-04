package dev.nthings.tessera.site;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;

import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LinkSpeculation")
class LinkSpeculationTest {

  private final List<SiteRoute> routes =
      List.of(
          new SiteRoute("/", new PageDocument("Home", main().child(a("/b").text("B")))),
          new SiteRoute("/b", new PageDocument("B", main().child(a("/").text("Home")))));

  private static RenderedPage home(SiteDefinition site) {
    return RenderedPage.of(SiteCompiler.compile(site).pagesByRoute().get("/"));
  }

  @Test
  @DisplayName("site pages declare same-site speculation rules by default")
  void declaresSpeculationRulesByDefault() {
    var rules = home(SiteDefinition.of(routes)).element("head > script[type=speculationrules]");

    assertThat(rules.data()).contains("\"prerender\"").contains("\"eagerness\":\"moderate\"");
  }

  @Test
  @DisplayName("NONE omits the speculation rules")
  void noneOmitsSpeculationRules() {
    var site =
        SiteDefinition.builder().routes(routes).linkSpeculation(LinkSpeculation.NONE).build();

    assertThat(home(site).html()).doesNotContain("speculationrules");
  }
}
