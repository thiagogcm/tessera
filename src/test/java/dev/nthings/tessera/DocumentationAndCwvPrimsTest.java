package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.OutputMode;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteRoute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationAndCwvPrimsTest {

  @Test
  void imageComponentEnforcesLcpPreloadAndClsDimensions() {
    var doc =
        new PageDocument(
            "LCP Image Test",
            main()
                .children(
                    Image.hero("/assets/hero-banner.webp", "Hero Banner")
                        .dimensions(1280, 720)
                        .aspectRatio("16 / 9"),
                    Image.of("/assets/thumbnail.webp", "Secondary Thumbnail")
                        .dimensions(400, 300)));

    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    // 1. Head contains high-priority preload link for hero image
    var preloadLink = parsed.selectFirst("head > link[rel=preload][as=image]");
    assertThat(preloadLink).isNotNull();
    assertThat(preloadLink.attr("href")).isEqualTo("/assets/hero-banner.webp");
    assertThat(preloadLink.attr("fetchpriority")).isEqualTo("high");

    // 2. Hero <img> element has fetchpriority=high and no loading=lazy
    var heroImg = parsed.selectFirst("img[src='/assets/hero-banner.webp']");
    assertThat(heroImg).isNotNull();
    assertThat(heroImg.attr("fetchpriority")).isEqualTo("high");
    assertThat(heroImg.hasAttr("loading")).isFalse();
    assertThat(heroImg.attr("width")).isEqualTo("1280");
    assertThat(heroImg.attr("height")).isEqualTo("720");

    // 3. Below-the-fold image defaults to lazy loading and async decoding
    var secondaryImg = parsed.selectFirst("img[src='/assets/thumbnail.webp']");
    assertThat(secondaryImg).isNotNull();
    assertThat(secondaryImg.attr("loading")).isEqualTo("lazy");
    assertThat(secondaryImg.attr("decoding")).isEqualTo("async");
    assertThat(secondaryImg.hasAttr("fetchpriority")).isFalse();
  }

  @Test
  void imageMissingBothDimensionsAndAspectRatioFailsInPrepaint() {
    var invalidDoc =
        new PageDocument(
            "CLS Violation",
            main().child(img("/banner.jpg", "Banner without width/height or aspect-ratio")));

    assertThatThrownBy(() -> PageCompiler.compile(invalidDoc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining(
            "must declare both 'width' and 'height' attributes or a CSS 'aspect-ratio'");
  }

  @Test
  void duplicateHeroImagesOnSamePageThrowsIllegalStateException() {
    var duplicateHeroDoc =
        new PageDocument(
            "Multiple Heroes",
            main()
                .children(
                    Image.hero("/hero1.webp", "Hero 1").dimensions(800, 400),
                    Image.hero("/hero2.webp", "Hero 2").dimensions(800, 400)));

    assertThatThrownBy(() -> PageCompiler.compile(duplicateHeroDoc))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Only one hero image allowed per page");
  }

  @Test
  void transitionKeyEnforcesPerPageUniqueness() {
    var key = TransitionKey.of("hero-title");
    var doc =
        new PageDocument(
            "Duplicate Keys",
            main()
                .children(
                    h1().text("First Heading").transitionKey(key),
                    h1().text("Second Heading").transitionKey(key)));

    assertThatThrownBy(() -> PageCompiler.compile(doc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("Duplicate TransitionKey name 'hero-title'");
  }

  @Test
  void siteCompilerEnforcesTransitionKeyMultiRouteAppearance() {
    var key = TransitionKey.of("page-title-morph", "morph-class");

    // Route 1 declares key, Route 2 does NOT -> failure
    var singleOccurrenceSite =
        new SiteDefinition(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute("/about", new PageDocument("About", h1().text("About"))));

    assertThatThrownBy(() -> SiteCompiler.compile(singleOccurrenceSite))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("TransitionKey 'page-title-morph'")
        .hasMessageContaining("must appear on at least two distinct routes site-wide");

    // Both routes declare key -> success, generates class-specific timing rule
    var validSite =
        new SiteDefinition(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute(
                "/about", new PageDocument("About", h1().text("About").transitionKey(key))));

    var compiledSite = SiteCompiler.compile(validSite);
    assertThat(compiledSite.sharedCssContent())
        .contains(
            "::view-transition-group(*.morph-class){animation-duration:0.35s;animation-timing-function:cubic-bezier(0.2,0,0,1)}");
  }

  @Test
  void compilesCalloutAdmonitionsWithSemanticOklchTint() {
    var doc =
        new PageDocument(
            "Admonitions",
            main()
                .children(
                    Callout.note("Important note about JDK 25."),
                    Callout.tip("Enable Baseline 2026+ CSS features."),
                    Callout.warning("Do not use preview language flags."),
                    Callout.caution("Destructive operations require confirmation.")
                        .title("Hazard Ahead")));

    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    var callouts = parsed.select("aside[role=note]");
    assertThat(callouts).hasSize(4);

    var note = callouts.get(0);
    assertThat(note.attr("class")).contains("callout--note");
    assertThat(note.selectFirst(".callout-title").text()).isEqualTo("Note");
    assertThat(note.text()).contains("Important note about JDK 25.");

    var caution = callouts.get(3);
    assertThat(caution.attr("class")).contains("callout--caution");
    assertThat(caution.selectFirst(".callout-title").text()).isEqualTo("Hazard Ahead");

    assertThat(compiled.cssContent())
        .contains("@scope ([data-view=\"tessera-callout\"]) to ([data-view])")
        .contains("color-mix(in oklch, var(--status-info) 14%, var(--surface-card))")
        .contains("border-inline-start:4px solid var(--status-info)");
  }

  @Test
  void compilesAccessibleCodeBlockWithCounterLineNumbers() {
    var block =
        CodeBlock.ofLines(
                "public class Tessera {",
                "  public static void main(String[] args) {",
                "+   System.out.println(\"Hello 2026\");",
                "-   System.out.println(\"Hello legacy\");",
                "  }",
                "}")
            .filename("Tessera.java")
            .language("java")
            .diffAutoDetect(true)
            .highlightLines(2);

    var compiled = PageCompiler.compile(new PageDocument("Code", main().child(block)));
    var parsed = Jsoup.parse(compiled.html());

    // 1. Pre element must be keyboard focusable
    var pre = parsed.selectFirst("pre.code-pre");
    assertThat(pre).isNotNull();
    assertThat(pre.attr("tabindex")).isEqualTo("0");

    // 2. Header bar with filename and language
    assertThat(parsed.selectFirst(".code-filename").text()).isEqualTo("Tessera.java");
    assertThat(parsed.selectFirst(".code-lang").text()).isEqualTo("java");

    // 3. Diff and highlight line markers
    var lines = parsed.select("span.code-line");
    assertThat(lines).hasSize(6);
    assertThat(lines.get(1).hasClass("is-highlighted")).isTrue();
    assertThat(lines.get(2).hasClass("is-add")).isTrue();
    assertThat(lines.get(3).hasClass("is-del")).isTrue();

    // 4. CSS contains counter line numbering
    assertThat(compiled.cssContent())
        .contains("counter-reset:code-line")
        .contains("counter-increment:code-line")
        .contains("content:counter(code-line)");
  }

  @Test
  void compilesDocPagerAndGeneratesSitemapAndSearchIndex(@TempDir Path tempDir) throws Exception {
    var site =
        new SiteDefinition(
            new Tokens(),
            OutputMode.externalAsset("/assets"),
            List.of(
                new SiteRoute(
                    "/",
                    new PageDocument(
                        "Home",
                        main()
                            .children(
                                Toc.section(
                                    "features", "Core Features", p().text("Fast compiler.")),
                                DocPager.of(
                                    null, new DocPager.PageRef("/guide", "Getting Started")))),
                    "Tessera Home",
                    "Static HTML/CSS compiler",
                    1),
                new SiteRoute(
                    "/guide",
                    new PageDocument(
                        "Guide",
                        main()
                            .children(
                                Toc.section(
                                    "quickstart", "Quick Start", p().text("Setup commands.")),
                                Toc.subsection("config", "Configuration", p().text("Build flags.")),
                                DocPager.of(
                                    new DocPager.PageRef("/", "Tessera Home"),
                                    new DocPager.PageRef("/faq", "Frequently Asked Questions")))),
                    "Getting Started",
                    "Step by step guide",
                    2),
                new SiteRoute(
                    "/faq",
                    new PageDocument(
                        "FAQ",
                        main()
                            .children(
                                Toc.section("questions", "Common Questions", p().text("Q&A list.")),
                                DocPager.of(
                                    new DocPager.PageRef("/guide", "Getting Started"), null))),
                    "Frequently Asked Questions",
                    "Common questions answered",
                    3)),
            "https://tessera.dev");

    // Verify DocPager.fromSite auto-resolution
    var autoPager = DocPager.fromSite(site, "/guide");
    assertThat(autoPager.prev()).isNotNull();
    assertThat(autoPager.prev().href()).isEqualTo("/");
    assertThat(autoPager.prev().title()).isEqualTo("Tessera Home");
    assertThat(autoPager.next()).isNotNull();
    assertThat(autoPager.next().href()).isEqualTo("/faq");
    assertThat(autoPager.next().title()).isEqualTo("Frequently Asked Questions");

    var compiledSite = SiteCompiler.compile(site);

    // Verify sitemap.xml
    assertThat(compiledSite.sitemapXml()).isNotNull();
    assertThat(compiledSite.sitemapXml())
        .contains("<loc>https://tessera.dev/</loc>")
        .contains("<loc>https://tessera.dev/guide</loc>")
        .contains("<loc>https://tessera.dev/faq</loc>");

    // Verify search-index.json
    assertThat(compiledSite.searchIndexJson()).isNotNull();
    assertThat(compiledSite.searchIndexJson())
        .contains("\"path\": \"/\"")
        .contains("\"title\": \"Tessera Home\"")
        .contains("\"id\": \"features\"")
        .contains("\"path\": \"/guide\"")
        .contains("\"id\": \"quickstart\"")
        .contains("\"id\": \"config\"");

    // Verify writeToDirectory emits sitemap.xml and search-index.json
    SiteCompiler.writeToDirectory(compiledSite, tempDir);
    assertThat(Files.exists(tempDir.resolve("sitemap.xml"))).isTrue();
    assertThat(Files.exists(tempDir.resolve("search-index.json"))).isTrue();
    assertThat(Files.readString(tempDir.resolve("sitemap.xml")))
        .isEqualTo(compiledSite.sitemapXml());
    assertThat(Files.readString(tempDir.resolve("search-index.json")))
        .isEqualTo(compiledSite.searchIndexJson());
  }
}
