package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_CONTENT;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_PATH;
import static dev.nthings.tessera.support.SiteFixtures.homeWithLogoAndGuide;
import static dev.nthings.tessera.support.SiteFixtures.logo;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Diagnostic;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Site assets")
class SiteAssetsTest {

  @Test
  @DisplayName("an image referencing an unregistered asset is rejected")
  void rejectsUnregisteredImageAsset() {
    var site = SiteDefinition.of(homeWithLogoAndGuide());

    var failure = catchThrowableOfType(CompilationException.class, () -> Compiler.compile(site));

    assertThat(failure.diagnostics())
        .extracting(
            Diagnostic::rule,
            d -> d.location().route(),
            d -> d.location().page(),
            d -> d.location().tag(),
            Diagnostic::message)
        .containsExactly(
            tuple(
                "missing-route-or-asset",
                "/",
                "Home",
                "img",
                "Route \"/\" references unregistered internal route or asset \""
                    + LOGO_PATH
                    + "\""));
  }

  @Test
  @DisplayName("a registered asset is published with its bytes and content type")
  void publishesRegisteredAsset() {
    var files = Compiler.compile(siteWithLogo()).files();

    assertThat(files).containsKey(LOGO_PATH);
    assertThat(files.get(LOGO_PATH).contentType()).isEqualTo("image/svg+xml");
    assertThat(files.get(LOGO_PATH).textContent()).isEqualTo(LOGO_CONTENT);
  }

  @Test
  @DisplayName("an asset colliding with a generated file is rejected")
  void rejectsGeneratedFileCollision() {
    var site =
        SiteDefinition.builder()
            .routes(homeWithLogoAndGuide())
            .asset(logo())
            .asset(Asset.text("/search-index.json", "[]"))
            .build();

    assertThat(diagnostics(site))
        .extracting(
            Diagnostic::rule,
            Diagnostic::message,
            d -> d.location().route(),
            d -> d.location().page())
        .containsExactly(
            tuple(
                "file-path-collision",
                "Generated file \"/search-index.json\" has the same path as another file",
                null,
                null));
  }

  @Test
  @DisplayName("an asset colliding with a route is rejected")
  void rejectsAssetCollidingWithRoute() {
    var site =
        SiteDefinition.builder()
            .routes(homeWithLogoAndGuide())
            .asset(Asset.text("/guide", "x"))
            .build();

    assertThat(diagnostics(site))
        .extracting(
            Diagnostic::rule,
            Diagnostic::message,
            d -> d.location().route(),
            d -> d.location().page())
        .containsExactly(
            tuple(
                "file-path-collision",
                "File \"/guide\" has the same path as a route",
                "/guide",
                "Guide"));
  }

  @Test
  @DisplayName("two assets at one path are rejected")
  void rejectsDuplicateAssetPaths() {
    var site =
        SiteDefinition.builder().routes(homeWithLogoAndGuide()).asset(logo()).asset(logo()).build();

    assertThat(diagnostics(site))
        .extracting(
            Diagnostic::rule,
            Diagnostic::message,
            d -> d.location().route(),
            d -> d.location().page())
        .containsExactly(
            tuple(
                "file-path-collision",
                "File \"" + LOGO_PATH + "\" has the same path as another file",
                null,
                null));
  }

  @Test
  @DisplayName("two routes at one path are reported before any page renders")
  void rejectsDuplicateRoutes() {
    Component failing =
        context -> {
          throw new AssertionError("rendered");
        };
    var site =
        SiteDefinition.builder()
            .route(new SiteRoute("/a", new PageDocument("First", main().child(failing))))
            .route(new SiteRoute("a/", new PageDocument("Second", main().child(failing))))
            .build();

    var failure = catchThrowableOfType(CompilationException.class, () -> Compiler.compile(site));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule, d -> d.location().route(), d -> d.location().page())
        .containsExactly(tuple("duplicate-route", "/a", "Second"));
    assertThat(failure.getMessage())
        .contains("[duplicate-route] Route \"/a\", page \"Second\": Route path \"/a\"");
  }

  private static List<Diagnostic> diagnostics(SiteDefinition site) {
    return catchThrowableOfType(CompilationException.class, () -> Compiler.compile(site))
        .diagnostics();
  }
}
