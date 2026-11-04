package dev.nthings.tessera.site;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_CONTENT;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_PATH;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("SiteCompiler.writeToDirectory")
class SiteWriterTest {

  @TempDir private static Path out;

  private static SiteCompiler.CompiledSite site;

  @BeforeAll
  static void writeSite() {
    site = SiteCompiler.compile(siteWithLogo());
    SiteCompiler.writeToDirectory(site, out);
  }

  @Test
  @DisplayName("writes registered assets verbatim")
  void writesRegisteredAssetsVerbatim() {
    assertThat(out.resolve(LOGO_PATH.substring(1))).hasContent(LOGO_CONTENT);
  }

  @Test
  @DisplayName("writes each route as a directory index")
  void writesEachRouteAsDirectoryIndex() {
    assertThat(out.resolve("guide/index.html")).isRegularFile();
  }

  @Test
  @DisplayName("writes the shared stylesheet")
  void writesSharedStylesheet() {
    assertThat(out.resolve(site.sharedCssPath().substring(1))).isRegularFile();
  }
}
