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
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteNavigation;
import dev.nthings.tessera.site.SiteRoute;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentationAndCwvPrimsTest {

  @Test
  @DisplayName("Enforces LCP preload and CLS dimensions on image components")
  void enforcesLcpPreloadAndClsDimensions() {
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
    assertThat(preloadLink.attr("href")).isEqualTo("/assets/hero-banner.webp");
    assertThat(preloadLink.attr("fetchpriority")).isEqualTo("high");

    // 2. Hero <img> element has fetchpriority=high and no loading=lazy
    var heroImg = parsed.selectFirst("img[src='/assets/hero-banner.webp']");
    assertThat(heroImg.attr("fetchpriority")).isEqualTo("high");
    assertThat(heroImg.hasAttr("loading")).isFalse();

    // 3. Below-the-fold image defaults to lazy loading and async decoding
    var secondaryImg = parsed.selectFirst("img[src='/assets/thumbnail.webp']");
    assertThat(secondaryImg.attr("loading")).isEqualTo("lazy");
    assertThat(secondaryImg.attr("decoding")).isEqualTo("async");
    assertThat(secondaryImg.hasAttr("fetchpriority")).isFalse();
  }

  @Test
  @DisplayName("Rejects images missing both dimensions and aspect ratio in prepaint")
  void rejectsImageMissingDimensionsAndAspectRatio() {
    var invalidDoc =
        new PageDocument(
            "CLS Violation",
            main().child(img("/banner.jpg", "Banner without width/height or aspect-ratio")));

    assertThatThrownBy(() -> PageCompiler.compile(invalidDoc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("aspect-ratio");
  }

  @Test
  @DisplayName("Rejects multiple hero images on the same page")
  void rejectsDuplicateHeroImagesOnSamePage() {
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
  @DisplayName("Enforces per-page uniqueness for view transition keys")
  void enforcesPerPageTransitionKeyUniqueness() {
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
  @DisplayName("Enforces view transition keys appear across multiple distinct routes")
  void enforcesMultiRouteTransitionKeyAppearance() {
    var key = TransitionKey.of("page-title-morph", "morph-class");

    // Route 1 declares key, Route 2 does NOT -> failure
    var singleOccurrenceSite =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute("/about", new PageDocument("About", h1().text("About"))));

    assertThatThrownBy(() -> SiteCompiler.compile(singleOccurrenceSite))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("TransitionKey 'page-title-morph'")
        .hasMessageContaining("must appear on at least two distinct routes site-wide");

    // Both routes declare key -> success, generates class-specific timing rule
    var validSite =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute(
                "/about", new PageDocument("About", h1().text("About").transitionKey(key))));

    var compiledSite = SiteCompiler.compile(validSite);
    assertThat(compiledSite.sharedCssContent()).contains("::view-transition-group(*.morph-class)");
  }

  @Test
  @DisplayName("Callouts carry their tone and a default or custom title")
  void compilesCalloutAdmonitions() {
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

    var callouts = Jsoup.parse(PageCompiler.compile(doc).html()).select("aside[role=note]");

    // Each tone gets its modifier class and a default title unless one is given
    assertThat(callouts)
        .extracting(e -> e.selectFirst(".callout-title").text())
        .containsExactly("Note", "Tip", "Warning", "Hazard Ahead");
    assertThat(callouts.getFirst().hasClass("callout--note")).isTrue();
    assertThat(callouts.getLast().hasClass("callout--caution")).isTrue();
  }

  @Test
  @DisplayName("Code blocks are focusable and mark diff and highlighted lines")
  void compilesAccessibleCodeBlock() {
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
            .highlightLines(2, 3);

    var parsed =
        Jsoup.parse(PageCompiler.compile(new PageDocument("Code", main().child(block))).html());

    // Scrollable code must be keyboard focusable
    assertThat(parsed.selectFirst("pre.code-pre").attr("tabindex")).isEqualTo("0");

    // Diff markers win over highlighting; other highlighted lines are marked
    var lines = parsed.select("span.code-line");
    assertThat(lines).hasSize(6);
    assertThat(lines.get(1).hasClass("is-highlighted")).isTrue();
    assertThat(lines.get(2).classNames()).contains("is-add").doesNotContain("is-highlighted");
    assertThat(lines.get(3).hasClass("is-del")).isTrue();
  }

  @Test
  @DisplayName("Derives pagers, sitemap and search index from the site's routes")
  void compilesDocPagerSitemapAndSearchIndex() {
    var routes =
        List.of(
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
    var site = SiteDefinition.builder().baseUrl("https://tessera.dev/").routes(routes).build();

    var pager = SiteNavigation.docPager(site, "/guide");
    assertThat(pager.prev()).isEqualTo(new DocPager.PageRef("/", "Tessera Home"));
    assertThat(pager.next()).isEqualTo(new DocPager.PageRef("/faq", "FAQ"));
    assertThatThrownBy(() -> SiteNavigation.docPager(site, "/missing"))
        .isInstanceOf(IllegalArgumentException.class);

    var compiledSite = SiteCompiler.compile(site);
    assertThat(compiledSite.sitemapXml())
        .contains("<loc>https://tessera.dev/</loc>")
        .contains("<loc>https://tessera.dev/guide</loc>");

    String index = compiledSite.searchIndexJson().replaceAll("\\s", "");
    assertThat(index)
        .contains("\"path\":\"/guide\",\"title\":\"GettingStarted\"")
        .contains("{\"id\":\"config\",\"title\":\"Configuration\",\"level\":3}");

    assertThat(SiteCompiler.compile(SiteDefinition.of(routes)).sitemapXml()).isNull();
  }
}
