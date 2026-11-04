package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.showcase.ShowcaseSite;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteRoute;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiteCompilerAndScopeStyleTest {

  @Test
  @DisplayName("Views with identical scoped CSS share one @scope block and one data-view key")
  void deduplicatesScopeStylesAndQualifiesViews() {
    List<Column<String>> columns =
        List.of(
            Column.<String>rowHeader("Key", s -> text(s)), Column.<String>text("Value", s -> s));
    var compiled =
        PageCompiler.compile(
            new PageDocument(
                "Scope Deduplication",
                main()
                    .children(
                        Card.create("one").title("First").bodyText("Body 1"),
                        Card.create("two").title("Second").bodyText("Body 2"),
                        new DataTable<>("a", "A", columns, List.of("row-a")),
                        new DataTable<>("b", "B", columns, List.of("row-b", "row-c")))));

    String css = compiled.cssContent();
    var doc = Jsoup.parse(compiled.html());
    for (var pair :
        List.of(List.of("card-one", "card-two"), List.of("datatable-a", "datatable-b"))) {
      String key = doc.selectFirst("[data-view-id=" + pair.get(0) + "]").attr("data-view");
      assertThat(doc.selectFirst("[data-view-id=" + pair.get(1) + "]").attr("data-view"))
          .isEqualTo(key);
      String scope = "@scope ([data-view=\"" + key + "\"])";
      assertThat(css.indexOf(scope)).isPositive().isEqualTo(css.lastIndexOf(scope));
    }
  }

  @Test
  @DisplayName("The showcase site compiles: every link, anchor, a11y and transition check passes")
  void compilesShowcaseSite() {
    var site = SiteCompiler.compile(ShowcaseSite.createDefinition());
    String css = site.sharedCssPath();
    assertThat(site.pagesByRoute().values())
        .allSatisfy(page -> assertThat(page.html()).contains("href=\"" + css + "\""));
  }

  @Test
  @DisplayName("Rejects broken cross-route links and missing anchors at compile time")
  void rejectsBrokenCrossRouteLinksAndAnchors() {
    var brokenRouteSite =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home",
                    main().child(a("/does-not-exist").px(CssLength.rem(1)).text("Broken link")))));

    assertThatThrownBy(() -> SiteCompiler.compile(brokenRouteSite))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("unregistered internal route or asset \"/does-not-exist\"");

    var brokenAnchorSite =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home", main().child(a("/docs#missing-section").text("Jump to section")))),
            new SiteRoute(
                "/docs", new PageDocument("Docs", main().id("intro").text("Introduction"))));

    assertThatThrownBy(() -> SiteCompiler.compile(brokenAnchorSite))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("missing target anchor #missing-section on route \"/docs\"");
  }
}
