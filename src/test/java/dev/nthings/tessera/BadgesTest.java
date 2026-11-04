package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.main;
import static org.assertj.core.api.Assertions.assertThat;

import dev.nthings.tessera.components.Badge;
import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.Pill;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BadgesTest {

  private static Document render(IntoElement... content) {
    return Jsoup.parse(
        PageCompiler.compile(new PageDocument("Test", main().children(content))).html());
  }

  @Test
  @DisplayName("Badge renders neutral tinted span by default")
  void badgeRendersDefault() {
    var badge = Badge.of("Status");
    var doc = render(badge);

    var el = doc.selectFirst("span.badge");
    assertThat(el).isNotNull();
    assertThat(el.text()).isEqualTo("Status");
    assertThat(el.hasClass("badge--neutral")).isTrue();
    assertThat(el.hasClass("badge--tinted")).isTrue();
    assertThat(el.hasClass("badge--md")).isTrue();
    assertThat(el.hasClass("is-pill")).isFalse();
    assertThat(el.select(".badge-dot")).isEmpty();
  }

  @Test
  @DisplayName("Badge supports tones, variants, sizes and pill styling")
  void badgeSupportsToneVariantSizeAndPill() {
    var badge = Badge.of("Critical").danger().solid().lg().pill();

    var doc = render(badge);

    var el = doc.selectFirst("span.badge");
    assertThat(el).isNotNull();
    assertThat(el.hasClass("badge--danger")).isTrue();
    assertThat(el.hasClass("badge--solid")).isTrue();
    assertThat(el.hasClass("badge--lg")).isTrue();
    assertThat(el.hasClass("is-pill")).isTrue();
  }

  @Test
  @DisplayName("Badge with dot renders decorative dot indicator")
  void badgeWithDotRendersIndicator() {
    var badge = Badge.dot("Online", Badge.Tone.SUCCESS);
    var doc = render(badge);

    var el = doc.selectFirst("span.badge");
    assertThat(el).isNotNull();
    assertThat(el.hasClass("badge--success")).isTrue();
    var dot = el.selectFirst(".badge-dot");
    assertThat(dot).isNotNull();
    assertThat(dot.attr("aria-hidden")).isEqualTo("true");
    assertThat(el.selectFirst(".badge-label").text()).isEqualTo("Online");
  }

  @Test
  @DisplayName("Badge with href renders accessible anchor link")
  void badgeWithHrefRendersAnchor() {
    var badge = Badge.accent("Docs").href("/docs");
    var doc = render(badge);

    var link = doc.selectFirst("a.badge");
    assertThat(link).isNotNull();
    assertThat(link.attr("href")).isEqualTo("/docs");
    assertThat(link.hasClass("badge--accent")).isTrue();
    assertThat(link.text()).isEqualTo("Docs");
  }

  @Test
  @DisplayName("Pill factory creates pill badges with all tones")
  void pillFactoryCreatesPills() {
    var pillAccent = Pill.accent("Pro");
    var pillSuccess = Pill.success("Active").dot();
    var pillWarning = Pill.warning("Warning").outline();

    var doc = render(pillAccent, pillSuccess, pillWarning);

    var pills = doc.select(".badge.is-pill");
    assertThat(pills).hasSize(3);
    assertThat(pills.get(0).hasClass("badge--accent")).isTrue();
    assertThat(pills.get(1).hasClass("badge--success")).isTrue();
    assertThat(pills.get(1).selectFirst(".badge-dot")).isNotNull();
    assertThat(pills.get(2).hasClass("badge--outline")).isTrue();
  }

  @Test
  @DisplayName("CodeBlock header renders the language as a pill badge")
  void codeBlockHeaderRendersLanguagePill() {
    var block =
        CodeBlock.ofLines("const greeting = 'hello';").filename("index.js").language("javascript");

    var doc = render(block);

    assertThat(doc.selectFirst(".code-header .code-filename").text()).isEqualTo("index.js");
    var langPill = doc.selectFirst(".code-header .badge.is-pill");
    assertThat(langPill).isNotNull();
    assertThat(langPill.text()).isEqualTo("javascript");
  }
}
