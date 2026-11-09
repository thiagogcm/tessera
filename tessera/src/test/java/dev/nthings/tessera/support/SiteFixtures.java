package dev.nthings.tessera.support;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.compiler.Asset;
import dev.nthings.tessera.compiler.CompiledSite;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.components.Toc;
import java.util.List;

/// Route sets built fresh per call, so tests cannot affect each other.
public final class SiteFixtures {

  public static final String LOGO_PATH = "/assets/logo.svg";
  public static final String LOGO_CONTENT = "<svg/>";

  private SiteFixtures() {}

  /// URL path of the shared stylesheet of a site compiled with external output.
  public static String stylesheetPath(CompiledSite site) {
    return site.files().keySet().stream()
        .filter(path -> path.endsWith(".css"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No stylesheet published"));
  }

  public static Asset logo() {
    return Asset.text(LOGO_PATH, LOGO_CONTENT);
  }

  /// Embeds unregistered [#LOGO_PATH], so compiling these routes alone fails; see
  /// [#siteWithLogo()].
  public static List<SiteRoute> homeWithLogoAndGuide() {
    var home =
        new PageDocument(
            "Home",
            main().children(img(LOGO_PATH, "Logo").dimensions(1, 1), a("/guide").text("Guide")));
    return List.of(
        new SiteRoute("/", home),
        new SiteRoute("/guide", new PageDocument("Guide", main().text("Guide"))));
  }

  /// [#homeWithLogoAndGuide()] with [#logo()] registered: a valid site.
  public static SiteDefinition siteWithLogo() {
    return SiteDefinition.builder().routes(homeWithLogoAndGuide()).asset(logo()).build();
  }

  /// Three documentation routes; the untitled FAQ falls back to its document title.
  public static List<SiteRoute> documentationRoutes() {
    return List.of(
        new SiteRoute(
            "/",
            new PageDocument("Home", main().child(Toc.section("features", "Features", p()))),
            "Tessera Home",
            "Static HTML/CSS compiler"),
        new SiteRoute(
            "/guide",
            new PageDocument(
                "Guide",
                main()
                    .children(
                        Toc.section("quickstart", "Quick Start", p()),
                        Toc.subsection("config", "Configuration", p()))),
            "Getting Started"),
        new SiteRoute("/faq", new PageDocument("FAQ", main().child(p().text("Q&A")))));
  }
}
