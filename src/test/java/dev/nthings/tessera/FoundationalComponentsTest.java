package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;
import static org.assertj.core.api.Assertions.assertThat;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.CardGrid;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.components.Footnotes;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.components.ScrollProgressBar;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.UnderlineStyle;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FoundationalComponentsTest {

  record ServiceRow(String service, String region) {}

  @Test
  @DisplayName("DataTable labels stacked cells from escaped headers and keeps row headers as <th>")
  void compilesTables() {
    var dataTable =
        new DataTable<>(
            "services",
            "Service telemetry",
            List.of(
                Column.rowHeader("Service", r -> text(r.service())),
                Column.text("Region \"AZ\"", ServiceRow::region)),
            List.of(new ServiceRow("auth-edge", "us-east-1"), new ServiceRow("ledger", "eu-1")));

    var compiled = PageCompiler.compile(new PageDocument("Tables", main().child(dataTable)));

    String css = compiled.cssContent();
    assertThat(css).doesNotContain("\"Region \"AZ\"\"");
    assertThat(css).contains("td:nth-child(2)::before").doesNotContain("td:nth-child(1)::before");

    Document doc = Jsoup.parse(compiled.html());
    var region = doc.selectFirst("div[role=region][tabindex=0]");
    assertThat(region.attr("aria-labelledby")).isEqualTo(region.selectFirst("caption").id());
    assertThat(region.select("tbody th[scope=row]"))
        .extracting(e -> e.text())
        .containsExactly("auth-edge", "ledger");
  }

  @Test
  @DisplayName("Cards keep the four-region structure for each interaction mode")
  void compilesCardsAndGrids() {
    var mediaCard =
        Card.create("architecture")
            .mediaImg("/assets/arch.webp", "Architecture diagram", "16 / 9")
            .title("Multi-Phase Compiler")
            .subtitle("Three-pass static pipeline")
            .bodyText("Separates layout expansion, validation, and paint.")
            .stretchedLink("/docs/compiler")
            .action(button().text("Bookmark"));

    var textOnlyCard =
        Card.create("tokens")
            .title("Reactive Tokens")
            .bodyText("Uses native light-dark().")
            .bodyText("Zero runtime theme scripts.");

    var choiceCard =
        Card.create("tier-pro")
            .title("Pro Tier")
            .bodyText("Isolated workers.")
            .choiceRadio("plan", "pro", true);

    var compiled =
        PageCompiler.compile(
            new PageDocument(
                "Cards",
                main()
                    .child(
                        CardGrid.of(
                            "feature-cards",
                            CssLength.rem(18),
                            CssLength.rem(1.25),
                            mediaCard,
                            textOnlyCard,
                            choiceCard))));

    Document doc = Jsoup.parse(compiled.html());

    // Media card: direct <img>, <hgroup> with the stretched link, single unwrapped <p>, <footer>
    var media = doc.selectFirst("[data-view-id=feature-cards--card-architecture]");
    assertThat(media.children())
        .extracting(e -> e.tagName())
        .containsExactly("img", "hgroup", "p", "footer");
    assertThat(media.selectFirst("hgroup h3 a.card-link[href=/docs/compiler]")).isNotNull();

    // Several body paragraphs are grouped into one element so they occupy one region
    var textOnly = doc.selectFirst("[data-view-id=feature-cards--card-tokens]");
    assertThat(textOnly.children()).extracting(e -> e.tagName()).containsExactly("h3", "div");

    // Choice cards are a <label> wrapping their own input
    var choice = doc.selectFirst("[data-view-id=feature-cards--card-tier-pro]");
    assertThat(choice.tagName()).isEqualTo("label");
    assertThat(choice.selectFirst("> input[type=radio][name=plan][checked]")).isNotNull();
  }

  @Test
  @DisplayName("Scroll-driven animations are gated behind prefers-reduced-motion")
  void compilesScrollDrivenAnimations() {
    var progress = new ScrollProgressBar("top-progress");
    var revealSection =
        Elements.section().scrollReveal(ScrollReveal.FADE_UP).child(p().text("Revealed on scroll"));

    var compiled =
        PageCompiler.compile(
            new PageDocument("Scroll Animations", main().children(progress, revealSection)));

    String css = compiled.cssContent();
    Document doc = Jsoup.parse(compiled.html());
    String revealRule =
        CssBlocks.classRule(
            css, CssBlocks.generatedClass(doc.selectFirst("section").className(), "i-"));
    assertThat(CssBlocks.block(revealRule, "@media (prefers-reduced-motion: no-preference){"))
        .contains("animation-timeline:view(block)");
    assertThat(css).contains("@keyframes tessera-fade-up{");

    // The animation shorthand resets animation-timeline, so it must be emitted first
    int animIdx = css.indexOf("animation:tessera-scroll-progress");
    int timelineIdx = css.indexOf("animation-timeline:scroll(root block)");
    assertThat(animIdx).isPositive().isLessThan(timelineIdx);

    assertThat(doc.getElementById("top-progress").attr("aria-hidden")).isEqualTo("true");
  }

  @Test
  @DisplayName("Inline formatting maps to semantic tags; footnotes number and link both ways")
  void compilesListsFormattingAndFootnotes() {
    var prose =
        p().children(
                Formatting.highlight("highlight", HighlightTone.ACCENT),
                Formatting.underline("spelling", UnderlineStyle.WAVY, Tokens.STATUS_DANGER),
                Formatting.strikethrough("old"),
                Formatting.deleted("removed"),
                Formatting.inserted("added"),
                Formatting.keybinding("Ctrl+K"),
                Footnotes.ref("fn-1", "First footnote."),
                text(" and "),
                Footnotes.ref("fn-2", "Second footnote."));

    var compiled = PageCompiler.compile(new PageDocument("Prose", main().child(prose)));
    Document doc = Jsoup.parse(compiled.html());

    assertThat(doc.selectFirst("main p").children())
        .extracting(e -> e.tagName())
        .startsWith("mark", "u", "s", "del", "ins", "kbd");

    var firstRefLink = doc.getElementById("fnref-fn-1").selectFirst("a[role=doc-noteref]");
    assertThat(firstRefLink.text()).isEqualTo("[1]");
    assertThat(firstRefLink.attr("href")).isEqualTo("#fn-fn-1");
    assertThat(firstRefLink.attr("interestfor")).isEqualTo(firstRefLink.nextElementSibling().id());
    assertThat(doc.getElementById("fnref-fn-2").selectFirst("a").text()).isEqualTo("[2]");

    // Without an explicit Footnotes.section, endnotes are appended after all content
    var endnotes = doc.body().children().last();
    assertThat(endnotes.attr("role")).isEqualTo("doc-endnotes");
    var noteItems = endnotes.select("ol > li[role=doc-footnote]");
    assertThat(noteItems).extracting(e -> e.id()).containsExactly("fn-fn-1", "fn-fn-2");
    assertThat(noteItems.getFirst().selectFirst("a[role=doc-backlink]").attr("href"))
        .isEqualTo("#fnref-fn-1");
  }
}
