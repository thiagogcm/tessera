package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static org.assertj.core.api.Assertions.assertThat;

import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteNavigation;
import dev.nthings.tessera.site.SiteRoute;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormsAndSearchPaletteTest {

  @Test
  @DisplayName("Search palette opens and closes its dialog through invoker commands")
  void searchPaletteDeclarativeDialogAndCommands() {
    var palette =
        SearchPalette.create("site-search")
            .item("Guides", "Getting Started", "/getting-started", "Introduction", "New");
    var doc =
        new PageDocument(
            "Search Test",
            main()
                .children(palette.triggerButton("open-search", "Search"), palette.asDeferred(50)));
    var parsed = Jsoup.parse(PageCompiler.compile(doc).html());

    var dialog = parsed.selectFirst("dialog[closedby=any]");
    var open = parsed.getElementById("open-search");
    assertThat(open.attr("commandfor")).isEqualTo(dialog.id());
    assertThat(open.attr("command")).isEqualTo("show-modal");
    var close = dialog.selectFirst(".palette-close");
    assertThat(close.attr("commandfor")).isEqualTo(dialog.id());
    assertThat(close.attr("command")).isEqualTo("close");
  }

  @Test
  @DisplayName("Search palette autocompletes from a datalist and groups result links")
  void searchPaletteDatalistAndGroupedLinks() {
    var palette =
        SearchPalette.create("cmd-palette")
            .item("Routes", "Telemetry", "/telemetry", "Real-time metrics")
            .item("Routes", "Typography", "/typography", "Font hierarchy")
            .item("Actions", "Toggle Dark Mode", "/theme", "Switch themes", "Action");
    var parsed =
        Jsoup.parse(
            PageCompiler.compile(new PageDocument("Palette", main().child(palette))).html());

    var input = parsed.selectFirst("dialog input[type=search]");
    var datalist = parsed.getElementById(input.attr("list"));
    assertThat(datalist.tagName()).isEqualTo("datalist");
    assertThat(datalist.select("option"))
        .extracting(o -> o.attr("value"))
        .containsExactly("/telemetry", "/typography", "/theme");

    var groups = parsed.select("dialog .palette-group");
    assertThat(groups).hasSize(2);
    assertThat(groups.getFirst().select("a.palette-item"))
        .extracting(a -> a.attr("href"))
        .containsExactly("/telemetry", "/typography");
  }

  @Test
  @DisplayName("SiteNavigation.searchPalette indexes every route, preferring the route title")
  void searchPaletteCrawlsSiteRoutes() {
    var site =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home Page", div()), "Home"),
            new SiteRoute("/docs", new PageDocument("Documentation", div())));

    var palette = SiteNavigation.searchPalette(site);
    var parsed =
        Jsoup.parse(PageCompiler.compile(new PageDocument("Site", main().child(palette))).html());

    var options = parsed.select("datalist option");
    assertThat(options).extracting(o -> o.attr("value")).containsExactly("/", "/docs");
    assertThat(options.getFirst().text()).startsWith("Home ");
    assertThat(options.getLast().text()).startsWith("Documentation ");
  }

  @Test
  @DisplayName("Switches are labelled role=switch checkboxes reflecting their initial state")
  void formSwitchControlAndLabel() {
    var doc =
        new PageDocument(
            "Switch Test",
            main()
                .children(
                    Forms.switchInput("enable_notifications", true),
                    Forms.switchField("Receive updates", "updates", false)));
    var parsed = Jsoup.parse(PageCompiler.compile(doc).html());

    assertThat(
            parsed.selectFirst("input[role=switch][name=enable_notifications]").hasAttr("checked"))
        .isTrue();
    var rowSwitch = parsed.selectFirst("input[role=switch][name=updates]");
    assertThat(rowSwitch.hasAttr("checked")).isFalse();
    assertThat(parsed.selectFirst("label").attr("for")).isEqualTo(rowSwitch.id());
  }

  @Test
  @DisplayName("Customizable select keeps native options and adds a selectedcontent trigger")
  void formCustomBaseSelect() {
    var customSelect =
        Forms.selectControl("theme_mode")
            .option("light", "Light Mode")
            .option("dark", "Dark Mode", true)
            .option("system", "System Default");
    var compiled = PageCompiler.compile(new PageDocument("Select", main().child(customSelect)));
    var select = Jsoup.parse(compiled.html()).selectFirst("select[name=theme_mode]");

    // Jsoup's legacy parser hoists <button> out of <select>, so match the emitted markup
    assertThat(compiled.html())
        .containsPattern(
            "<select[^>]*name=\"theme_mode\"[^>]*><button[^>]*class=\"select-trigger\"[^>]*>"
                + "<selectedcontent></selectedcontent></button>");
    assertThat(select.select("option[selected]"))
        .extracting(o -> o.attr("value"))
        .containsExactly("dark");
    assertThat(compiled.cssContent()).contains("appearance:base-select");
  }

  @Test
  @DisplayName("Fields wire label, hint, error and an anchored tooltip to their control")
  void formFieldWrapperWiresHintsErrorsAndTooltips() {
    var field =
        Forms.field("Username", Forms.text("username"))
            .hint("3-20 characters.")
            .error("Username is taken.")
            .anchorTooltip("Alphanumeric only");
    var compiled = PageCompiler.compile(new PageDocument("Field", main().child(field)));
    var parsed = Jsoup.parse(compiled.html());

    var input = parsed.selectFirst("input[name=username]");
    assertThat(parsed.selectFirst("label").attr("for")).isEqualTo(input.id());
    assertThat(input.attr("aria-describedby")).isEqualTo(parsed.selectFirst(".field-hint").id());

    // The error follows the input directly: its styling is `:user-invalid + .field-error`
    var error = input.nextElementSibling();
    assertThat(error.hasClass("field-error")).isTrue();
    assertThat(input.attr("aria-errormessage")).isEqualTo(error.id());

    // The tooltip is anchor-positioned against the input's anchor name
    String css = compiled.cssContent();
    var tooltip = parsed.selectFirst(".field-anchor-tooltip[role=tooltip]");
    String anchor = "--anchor-" + input.id();
    assertThat(CssBlocks.classRule(css, CssBlocks.generatedClass(input.className(), "u-")))
        .contains("anchor-name:" + anchor);
    assertThat(CssBlocks.classRule(css, CssBlocks.generatedClass(tooltip.className(), "u-")))
        .contains("position-anchor:" + anchor);
  }
}
