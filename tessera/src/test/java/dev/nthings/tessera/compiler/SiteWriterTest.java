package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_CONTENT;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_PATH;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;
import static dev.nthings.tessera.support.SiteFixtures.stylesheetPath;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("SiteWriter.writeToDirectory")
class SiteWriterTest {

  @TempDir private static Path out;

  private static CompiledSite site;

  @BeforeAll
  static void writeSite() {
    site = Compiler.compile(siteWithLogo());
    SiteWriter.writeToDirectory(site, out);
  }

  @Test
  @DisplayName("writes registered assets verbatim")
  void writesAssetsVerbatim() {
    assertThat(out.resolve(LOGO_PATH.substring(1))).hasContent(LOGO_CONTENT);
  }

  @Test
  @DisplayName("writes a route as a directory index")
  void writesRouteIndex() {
    assertThat(out.resolve("guide/index.html")).isRegularFile();
  }

  @Test
  @DisplayName("writes the shared stylesheet")
  void writesSharedStylesheet() {
    assertThat(out.resolve(stylesheetPath(site).substring(1))).isRegularFile();
  }
}
