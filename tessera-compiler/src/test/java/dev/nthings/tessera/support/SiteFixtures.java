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
import dev.nthings.tessera.components.Section;
import java.util.List;

/// Route sets built fresh per call, so tests cannot affect each other.
public final class SiteFixtures {

  public static final String LOGO_PATH = "/assets/logo.png";

  private SiteFixtures() {}

  /// Bytes that are not valid UTF-8, so any transcoding on the way out changes them.
  public static byte[] logoBytes() {
    return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xFF, (byte) 0xC3, 0x28};
  }

  public static Asset logo() {
    return Asset.of(LOGO_PATH, logoBytes());
  }

  /// URL path of the shared stylesheet of a site compiled with external output.
  public static String stylesheetPath(CompiledSite site) {
    return site.files().keySet().stream()
        .filter(path -> path.endsWith(".css"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No stylesheet published"));
  }

  /// A small documentation site: a home page showing [#logo()], a directory route, a nested route
  /// and an `.html` route, all linked from home.
  public static SiteDefinition.Builder docsSite() {
    var home =
        main()
            .children(
                img(LOGO_PATH, "Logo").dimensions(16, 16),
                a("/guide").text("Guide"),
                a("guide/install/").text("Install"),
                a("/notes.html").text("Notes"));
    return SiteDefinition.builder()
        .route(new SiteRoute("/", new PageDocument("Home", home)))
        .route(new SiteRoute("/guide", new PageDocument("Guide", main().text("Guide"))))
        .route(new SiteRoute("/guide/install", new PageDocument("Install", main().text("Install"))))
        .route(new SiteRoute("/notes.html", new PageDocument("Notes", main().text("Notes"))))
        .asset(logo());
  }

  /// Three documentation routes; the untitled FAQ falls back to its document title.
  public static List<SiteRoute> documentationRoutes() {
    return List.of(
        new SiteRoute(
            "/",
            new PageDocument("Home", main().child(Section.of("features", "Features").child(p()))),
            "Tessera Home",
            "Static HTML/CSS compiler"),
        new SiteRoute(
            "/guide",
            new PageDocument(
                "Guide",
                main()
                    .children(
                        Section.of("quickstart", "Quick Start").child(p()),
                        Section.of("config", "Configuration").level(3).child(p()))),
            "Getting Started"),
        new SiteRoute("/faq", new PageDocument("FAQ", main().child(p().text("Q&A")))));
  }
}
