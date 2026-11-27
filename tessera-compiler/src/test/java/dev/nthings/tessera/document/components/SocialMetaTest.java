package dev.nthings.tessera.document.components;

import static dev.nthings.tessera.core.Elements.main;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.support.RenderedPage;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SocialMeta")
class SocialMetaTest {

  private static SiteDefinition site(SocialMeta meta) {
    var body = Documents.fragment(DocumentContent.of(main().text("Post")), meta);
    return SiteDefinition.of(SiteRoute.of("/blog/hello", PageDocument.of("Hello", body)));
  }

  /// Each `property` or `name` meta in the head, mapped to its content.
  private static Map<String, String> tags(RenderedPage page) {
    var tags = new LinkedHashMap<String, String>();
    for (var meta : page.dom().head().select("meta[property], meta[name^=twitter]")) {
      tags.put(
          meta.hasAttr("property") ? meta.attr("property") : meta.attr("name"),
          meta.attr("content"));
    }
    return tags;
  }

  @Test
  @DisplayName("an article with an image gets every tag, with absolute URLs")
  void articleWithImage() {
    var compiled =
        Compiler.forSite()
            .compile(
                site(SocialMeta.of("Hello")
                        .description("First post")
                        .image("/og.png")
                        .siteName("nthings.dev")
                        .article())
                    .withBaseUrl(URI.create("https://nthings.dev")));

    assertThat(tags(RenderedPage.of(compiled, "/blog/hello")))
        .containsExactly(
            Map.entry("og:title", "Hello"),
            Map.entry("og:type", "article"),
            Map.entry("og:description", "First post"),
            Map.entry("og:site_name", "nthings.dev"),
            Map.entry("og:url", "https://nthings.dev/blog/hello/"),
            Map.entry("og:image", "https://nthings.dev/og.png"),
            Map.entry("twitter:card", "summary_large_image"),
            Map.entry("twitter:title", "Hello"),
            Map.entry("twitter:description", "First post"),
            Map.entry("twitter:image", "https://nthings.dev/og.png"));
  }

  @Test
  @DisplayName("a page without an image is a website summary card, without og:url off-site")
  void pageWithoutImage() {
    var compiled = Compiler.forSite().compile(site(SocialMeta.of("Hello")));

    assertThat(tags(RenderedPage.of(compiled, "/blog/hello")))
        .containsExactly(
            Map.entry("og:title", "Hello"),
            Map.entry("og:type", "website"),
            Map.entry("twitter:card", "summary"),
            Map.entry("twitter:title", "Hello"));
  }

  @Test
  @DisplayName("a relative image without a base URL is reported")
  void relativeImageNeedsBaseUrl() {
    var site = site(SocialMeta.of("Hello").image("/og.png"));

    assertThatThrownBy(() -> Compiler.forSite().compile(site))
        .isInstanceOfSatisfying(
            CompilationException.class,
            failure ->
                assertThat(failure.diagnostics())
                    .extracting(diagnostic -> diagnostic.rule())
                    .containsExactly("social-meta-url"));
  }
}
