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
  @DisplayName("Search palette renders top-layer dialog with declarative invoker commands")
  void searchPaletteDeclarativeDialogAndCommands() {
    var palette =
        SearchPalette.create("site-search")
            .placeholder("Search documentation...")
            .item("Guides", "Getting Started", "/getting-started", "Introduction to Tessera", "New")
            .item("Components", "Overlays", "/overlays", "Modals and Drawers");

    var triggerBtn = palette.triggerButton("open-search", "Quick Search");

    var doc = new PageDocument("Search Test", main().children(triggerBtn, palette.asDeferred(50)));

    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    // Trigger button has declarative invoker attributes and accesskey
    var button = parsed.selectFirst("button#open-search");
    assertThat(button).isNotNull();
    assertThat(button.attr("commandfor")).isEqualTo("site-search--dialog");
    assertThat(button.attr("command")).isEqualTo("show-modal");
    assertThat(button.attr("accesskey")).isEqualTo("k");
    assertThat(button.select("kbd")).hasSize(2);
    assertThat(button.select("kbd").get(0).text()).isEqualTo("Alt");
    assertThat(button.select("kbd").get(1).text()).isEqualTo("K");

    // Dialog element in top layer with closedby="any"
    var dialog = parsed.selectFirst("dialog#site-search--dialog");
    assertThat(dialog).isNotNull();
    assertThat(dialog.attr("closedby")).isEqualTo("any");
    assertThat(dialog.attr("aria-label")).isEqualTo("Quick open command palette");

    // Close button inside header
    var closeBtn = dialog.selectFirst(".palette-close");
    assertThat(closeBtn).isNotNull();
    assertThat(closeBtn.attr("commandfor")).isEqualTo("site-search--dialog");
    assertThat(closeBtn.attr("command")).isEqualTo("close");
  }

  @Test
  @DisplayName("Search palette provides native datalist autocomplete and grouped navigation links")
  void searchPaletteDatalistAndGroupedLinks() {
    var palette =
        SearchPalette.create("cmd-palette")
            .item("Routes", "Telemetry", "/telemetry", "Real-time metrics")
            .item("Routes", "Typography", "/typography", "Font hierarchy")
            .item("Actions", "Toggle Dark Mode", "/theme", "Switch themes", "Action");

    var doc = new PageDocument("Palette Test", main().child(palette));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    // Native search input with list attribute
    var input = parsed.selectFirst("dialog#cmd-palette--dialog input[type=search]");
    assertThat(input).isNotNull();
    assertThat(input.attr("list")).isEqualTo("cmd-palette--datalist");
    assertThat(input.attr("autocomplete")).isEqualTo("off");

    // Datalist with all options for zero-JS autocomplete
    var datalist = parsed.selectFirst("datalist#cmd-palette--datalist");
    assertThat(datalist).isNotNull();
    var options = datalist.select("option");
    assertThat(options).hasSize(3);
    assertThat(options.get(0).attr("value")).isEqualTo("/telemetry");
    assertThat(options.get(0).text()).isEqualTo("Telemetry (Routes)");
    assertThat(options.get(1).attr("value")).isEqualTo("/typography");
    assertThat(options.get(2).attr("value")).isEqualTo("/theme");

    // Grouped links
    var groups = parsed.select("dialog#cmd-palette--dialog .palette-group");
    assertThat(groups).hasSize(2);

    var itemLinks = parsed.select("dialog#cmd-palette--dialog a.palette-item");
    assertThat(itemLinks).hasSize(3);
    assertThat(itemLinks.get(0).attr("href")).isEqualTo("/telemetry");
    assertThat(itemLinks.get(0).selectFirst(".palette-item-title").text()).isEqualTo("Telemetry");
    assertThat(itemLinks.get(0).selectFirst(".palette-item-desc").text())
        .isEqualTo("Real-time metrics");
  }

  @Test
  @DisplayName(
      "SiteNavigation.searchPalette automatically crawls site routes into autocomplete entries")
  void searchPaletteCrawlsSiteRoutes() {
    var doc1 = new PageDocument("Home Page", div().id("home-root"));
    var doc2 = new PageDocument("Documentation", div().id("docs-root"));

    var site =
        new SiteDefinition(
            new SiteRoute("/", doc1, "Home"), new SiteRoute("/docs", doc2, "Documentation"));

    var palette = SiteNavigation.searchPalette(site);
    var compiled = PageCompiler.compile(new PageDocument("Site Search", main().child(palette)));
    var parsed = Jsoup.parse(compiled.html());

    var options = parsed.select("datalist option");
    assertThat(options).hasSize(2);
    assertThat(options.get(0).attr("value")).isEqualTo("/");
    assertThat(options.get(0).text()).isEqualTo("Home (Pages & Guides)");
    assertThat(options.get(1).attr("value")).isEqualTo("/docs");
    assertThat(options.get(1).text()).isEqualTo("Documentation (Pages & Guides)");
  }

  @Test
  @DisplayName("Accessible form inputs emit :user-invalid and :user-valid validation rules")
  void formInputControlsEmitValidationPseudoClasses() {
    var emailInput = Forms.email("user_email");
    var doc = new PageDocument("Forms Test", main().child(emailInput));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    var input = parsed.selectFirst("input[type=email]");
    assertThat(input).isNotNull();
    assertThat(input.attr("name")).isEqualTo("user_email");

    // CSS stylesheet contains :user-invalid and :user-valid rules
    var css = compiled.cssContent();
    assertThat(css).contains(":user-invalid");
    assertThat(css).contains(":user-valid");
  }

  @Test
  @DisplayName("Form switch control renders a role=switch checkbox with accessible label pairing")
  void formSwitchControlAndLabel() {
    var switchCtrl = Forms.switchInput("enable_notifications", true);
    var switchRow = Forms.switchField("Receive updates", "updates", false);

    var doc = new PageDocument("Switch Test", main().children(switchCtrl, switchRow));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    // Standalone switch
    var standalone =
        parsed.selectFirst("input[type=checkbox][role=switch][name=enable_notifications]");
    assertThat(standalone).isNotNull();
    assertThat(standalone.hasAttr("checked")).isTrue();

    // Row switch paired with label
    var label = parsed.selectFirst("label[for=switch-updates]");
    assertThat(label).isNotNull();
    assertThat(label.text()).isEqualTo("Receive updates");

    var rowSwitch = parsed.selectFirst("input#switch-updates");
    assertThat(rowSwitch).isNotNull();
    assertThat(rowSwitch.attr("role")).isEqualTo("switch");
    assertThat(rowSwitch.hasAttr("checked")).isFalse();
  }

  @Test
  @DisplayName("Custom base select renders appearance: base-select with button and selectedcontent")
  void formCustomBaseSelect() {
    var customSelect =
        Forms.selectControl("theme_mode")
            .id("select-theme")
            .option("light", "Light Mode")
            .option("dark", "Dark Mode", true)
            .option("system", "System Default");

    var doc = new PageDocument("Select Test", main().child(customSelect));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    var select = parsed.selectFirst("select[name=theme_mode][id$=--select-theme]");
    assertThat(select).isNotNull();
    assertThat(select.attr("name")).isEqualTo("theme_mode");

    // Output HTML contains button trigger and selectedcontent
    assertThat(compiled.html()).contains("<button type=\"button\" class=\"select-trigger\"");
    assertThat(compiled.html()).contains("<selectedcontent></selectedcontent>");

    // Options rendered with selected state
    var options = select.select("option");
    assertThat(options).hasSize(3);
    assertThat(options.get(1).attr("value")).isEqualTo("dark");
    assertThat(options.get(1).hasAttr("selected")).isTrue();

    // CSS stylesheet contains appearance: base-select rules
    var css = compiled.cssContent();
    assertThat(css).contains("appearance:base-select");
  }

  @Test
  @DisplayName("Form field wrapper wires accessible labels, hints, errors, and anchor tooltips")
  void formFieldWrapperWiresHintsErrorsAndTooltips() {
    var usernameInput = Forms.text("username");
    var field =
        Forms.field("Username", usernameInput)
            .id("user-field")
            .hint("Enter 3-20 characters without spaces.")
            .error("Username is already taken or invalid.")
            .anchorTooltip("Must be alphanumeric");

    var doc = new PageDocument("Field Test", main().child(field));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    // Label correctly wired to input ID
    var label = parsed.selectFirst("label[for=form-field-1--user-field]");
    assertThat(label).isNotNull();
    assertThat(label.text()).isEqualTo("Username");

    // Hint placed above input and linked via aria-describedby
    var hint = parsed.selectFirst("span#form-field-1--hint");
    assertThat(hint).isNotNull();
    assertThat(hint.text()).isEqualTo("Enter 3-20 characters without spaces.");

    var input = parsed.selectFirst("input#form-field-1--user-field");
    assertThat(input).isNotNull();
    assertThat(input.attr("aria-describedby")).isEqualTo("form-field-1--hint");
    assertThat(input.attr("aria-errormessage")).isEqualTo("form-field-1--error");

    // Error element placed after input
    var error = parsed.selectFirst("div#form-field-1--error");
    assertThat(error).isNotNull();
    assertThat(error.text()).contains("Username is already taken or invalid.");

    // Anchor tooltip rendered with ARIA role="tooltip"
    var tooltip = parsed.selectFirst(".field-anchor-tooltip");
    assertThat(tooltip).isNotNull();
    assertThat(tooltip.attr("role")).isEqualTo("tooltip");
    assertThat(tooltip.text()).isEqualTo("Must be alphanumeric");
  }
}
