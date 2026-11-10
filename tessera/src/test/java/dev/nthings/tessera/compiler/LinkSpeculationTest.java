package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;

import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LinkSpeculation")
class LinkSpeculationTest {

  private static final String PRERENDER_RULES =
      "{\"prerender\":[{\"where\":{\"href_matches\":\"/*\"},\"eagerness\":\"moderate\"}]}";
  private static final String PREFETCH_RULES =
      "{\"prefetch\":[{\"where\":{\"href_matches\":\"/*\"},\"eagerness\":\"moderate\"}]}";

  private final List<SiteRoute> routes =
      List.of(
          new SiteRoute("/", new PageDocument("Home", main().child(a("/b").text("B")))),
          new SiteRoute("/b", new PageDocument("B", main().child(a("/").text("Home")))));

  private static RenderedPage home(SiteDefinition site) {
    return RenderedPage.of(Compiler.compile(site), "/");
  }

  @Test
  @DisplayName("site pages declare same-site speculation rules by default")
  void declaresSpeculationRules() {
    var page = home(SiteDefinition.of(routes));
    var rules = page.element("head > script[type=speculationrules]");

    assertThat(page.elements("head > script[type=speculationrules]")).hasSize(1);
    assertThat(rules.data()).isEqualTo(PRERENDER_RULES);
    assertThat(page.html()).doesNotContain("\"prefetch\"");
  }

  @Test
  @DisplayName("PREFETCH declares prefetch rules instead of prerender")
  void prefetchDeclaresPrefetchRules() {
    var site =
        SiteDefinition.builder().routes(routes).linkSpeculation(LinkSpeculation.PREFETCH).build();
    var rules = home(site).element("head > script[type=speculationrules]");

    assertThat(rules.data()).isEqualTo(PREFETCH_RULES);
  }

  @Test
  @DisplayName("NONE omits the speculation rules")
  void noneOmitsSpeculationRules() {
    var site =
        SiteDefinition.builder().routes(routes).linkSpeculation(LinkSpeculation.NONE).build();
    var page = home(site);

    assertThat(page.elements("head > script[type=speculationrules]")).isEmpty();
    assertThat(page.html()).doesNotContain("speculationrules");
  }
}
