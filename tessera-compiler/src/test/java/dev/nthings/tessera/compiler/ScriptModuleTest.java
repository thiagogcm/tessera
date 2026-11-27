package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;

import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.document.ScriptModule;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Json;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Vendored script modules")
class ScriptModuleTest {

  private static final ScriptModule CHART =
      ScriptModule.of("chart", "export function draw() { return 1; }");
  private static final ScriptModule DATES =
      ScriptModule.of("@acme/dates", "export const today = () => new Date();");

  private static DocumentComponent widget(PageScript script) {
    return context -> {
      context.contributeScript(script);
      return main().child(custom("test-chart"));
    };
  }

  private static PageScript chartScript() {
    return PageScript.of("test-chart", "import { draw } from \"chart\"; draw();")
        .withImports(CHART, DATES);
  }

  @Test
  @DisplayName("an imported module is published hashed and resolved by the page's import map")
  void publishesModulesAndImportMap() {
    var site =
        Compiler.forSite()
            .withOutputMode(OutputMode.external("/static"))
            .compile(
                SiteDefinition.of(
                    SiteRoute.of("/", PageDocument.of("Home", widget(chartScript()))),
                    SiteRoute.of("/plain", PageDocument.of("Plain", main(p("Plain"))))));

    var home = RenderedPage.of(site, "/");
    var map = home.element("script[type=importmap]");
    var imports = Json.parse(map.data()).get("imports");
    var chartPath = imports.get("chart").string();
    assertThat(chartPath).matches("/static/modules/chart-[a-z0-9]+\\.js");
    assertThat(site.assets().get(chartPath).textContent()).isEqualTo(CHART.source());
    assertThat(site.assets().get(chartPath).contentType()).startsWith("text/javascript");
    assertThat(site.assets().get(imports.get("@acme/dates").string()).textContent())
        .isEqualTo(DATES.source());
    assertThat(home.html().indexOf("type=\"importmap\""))
        .isLessThan(home.html().indexOf("type=\"module\""));
    assertThat(RenderedPage.of(site, "/plain").elements("script[type=importmap]")).isEmpty();
  }

  @Test
  @DisplayName("inline output publishes modules under the default asset base")
  void inlineOutputStillPublishesModuleFiles() {
    var compiled = Compiler.forPage().compile(PageDocument.of("Home", widget(chartScript())));
    var paths = compiled.assets().stream().map(Asset::path).toList();

    var map = Json.parse(RenderedPage.of(compiled).element("script[type=importmap]").data());
    assertThat(paths).contains(map.get("imports").get("chart").string());
    assertThat(paths).allMatch(path -> path.startsWith("/assets/modules/"));
  }

  @Test
  @DisplayName("a changed module source is published at a new path")
  void cacheBustsChangedModules() {
    var changed =
        PageScript.of("test-chart", "import { draw } from \"chart\";")
            .withImports(ScriptModule.of("chart", "export function draw() { return 2; }"));

    assertThat(importPath(changed)).isNotEqualTo(importPath(chartScript()));
  }

  private static String importPath(PageScript script) {
    var page = RenderedPage.of(PageDocument.of("Home", widget(script)));
    return Json.parse(page.element("script[type=importmap]").data())
        .get("imports")
        .get("chart")
        .string();
  }

  @Test
  @DisplayName("two sources under one module name are reported")
  void reportsConflictingModules() {
    var other =
        PageScript.of("other-chart", "import \"chart\";")
            .withImports(ScriptModule.of("chart", "export {};"));
    DocumentComponent both =
        context -> {
          context.contributeScript(chartScript());
          context.contributeScript(other);
          return main(p("x"));
        };

    assertThat(Diagnostics.rules(() -> Compiler.forPage().compile(PageDocument.of("Home", both))))
        .containsExactly("script-module-conflict");
    assertThat(
            Diagnostics.rules(() ->
                Compiler.forSite()
                    .compile(
                        SiteDefinition.of(
                            SiteRoute.of("/", PageDocument.of("A", widget(chartScript()))),
                            SiteRoute.of("/b", PageDocument.of("B", widget(other)))))))
        .containsExactly("script-module-conflict");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "./chart", "/chart", "../chart", "https://cdn.example/x.js", "a b"})
  @DisplayName("a module name must be a bare specifier")
  void rejectsNonBareNames(String name) {
    assertThatIllegalArgumentException().isThrownBy(() -> ScriptModule.of(name, ""));
  }
}
