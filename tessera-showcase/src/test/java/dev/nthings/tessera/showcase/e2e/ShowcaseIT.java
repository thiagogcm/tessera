package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.OutputMode;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.showcase.Catalog;
import dev.nthings.tessera.showcase.ShowcaseSite;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteRoute;
import dev.nthings.tessera.site.SiteWriter;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Showcase browser behavior")
class ShowcaseIT {
  @TempDir private static Path publishedSite;
  private static HttpServer server;
  private static Playwright playwright;
  private static Browser browser;
  private static String baseUrl;
  private BrowserContext context;
  private Page page;

  @RegisterExtension
  final TestExecutionExceptionHandler screenshotOnFailure =
      (test, failure) -> {
        page.screenshot(
            new Page.ScreenshotOptions()
                .setPath(artifactPath(test.getDisplayName(), ".png"))
                .setFullPage(true));
        throw failure;
      };

  @BeforeAll
  static void publishAndLaunch() {
    var compiled = SiteCompiler.compile(ShowcaseSite.createDefinition());
    SiteWriter.writeToDirectory(compiled, publishedSite);
    // This published fixture proves one theme can change multiple independently authored
    // components.
    var themed =
        SiteDefinition.builder()
            .outputMode(OutputMode.inlineStyle())
            .tokens(
                new Tokens()
                    .register(Tokens.DENSITY.withValue("0.5"))
                    .register(Tokens.FONT_BODY.withValue("monospace"))
                    .register(Tokens.FONT_SIZE_MD.withValue("20px"))
                    .register(Tokens.RADIUS_XL.withValue("18px"))
                    .register(Tokens.RADIUS_LG.withValue("12px")))
            .route(
                new SiteRoute(
                    "/",
                    new PageDocument(
                        "Custom theme",
                        Elements.main()
                            .children(
                                Card.create("custom").title("Themed card").bodyText("Body"),
                                Forms.field("Custom field", Forms.text("custom"))))))
            .build();
    SiteWriter.writeToDirectory(SiteCompiler.compile(themed), publishedSite.resolve("theme"));
    var linked =
        SiteDefinition.builder()
            .outputMode(OutputMode.inlineStyle())
            .route(
                new SiteRoute(
                    "/links/start",
                    new PageDocument(
                        "Start",
                        Elements.main()
                            .child(
                                Elements.a("../caf%C3%A9?mode=full#caf%C3%A9%2Bintro")
                                    .text("Read the café guide")))))
            .route(
                new SiteRoute(
                    "/links/café",
                    new PageDocument(
                        "Café guide",
                        Elements.main()
                            .child(
                                Elements.h1().attr("id", "café+intro").text("Café introduction")))))
            .build();
    SiteWriter.writeToDirectory(SiteCompiler.compile(linked), publishedSite.resolve("urls"));
    var firstSearch =
        SearchPalette.create("first-search")
            .item("Guides", "Café guide", "#cafe")
            .item("Guides", "Tea notes", "#tea");
    var secondSearch =
        SearchPalette.create("second-search")
            .item("Guides", "Layout guide", "#layout")
            .item("Guides", "Forms guide", "#forms");
    var searchPage =
        SiteDefinition.builder()
            .outputMode(OutputMode.inlineStyle())
            .route(
                new SiteRoute(
                    "/",
                    new PageDocument(
                        "Search examples",
                        Elements.main()
                            .children(
                                firstSearch.triggerButton("first-trigger", "Find articles"),
                                secondSearch.triggerButton("second-trigger", "Find guides"),
                                Elements.h2().id("cafe").text("Café guide"),
                                Elements.h2().id("tea").text("Tea notes"),
                                Elements.h2().id("layout").text("Layout guide"),
                                Elements.h2().id("forms").text("Forms guide"),
                                firstSearch.asDeferred(10),
                                secondSearch.asDeferred(20)))))
            .build();
    SiteWriter.writeToDirectory(SiteCompiler.compile(searchPage), publishedSite.resolve("search"));
    server =
        SimpleFileServer.createFileServer(
            new InetSocketAddress("127.0.0.1", 0),
            publishedSite,
            SimpleFileServer.OutputLevel.NONE);
    server.start();
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    playwright = Playwright.create();
    browser = playwright.chromium().launch();
  }

  @AfterAll
  static void stop() {
    if (playwright != null) {
      playwright.close();
    }
    if (server != null) {
      server.stop(0);
    }
  }

  @BeforeEach
  void openContext() {
    context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
    context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
    page = context.newPage();
  }

  @AfterEach
  void closeContext(TestInfo test) {
    context
        .tracing()
        .stop(new Tracing.StopOptions().setPath(artifactPath(test.getDisplayName(), ".zip")));
    context.close();
  }

  private static Path artifactPath(String test, String extension) {
    return Path.of("target", "playwright", test.replaceAll("[^a-zA-Z0-9-]", "_") + extension);
  }

  private static Stream<Arguments> routesAndViewports() {
    return ShowcaseSite.createDefinition().routes().stream()
        .filter(route -> !route.path().startsWith("/stories/"))
        .flatMap(
            route ->
                Stream.of(390, 1440)
                    .map(
                        width ->
                            Arguments.of(
                                route.path(),
                                route.document().title(),
                                route.displayTitle(),
                                width)));
  }

  @ParameterizedTest(name = "{0} at {3}px")
  @MethodSource("routesAndViewports")
  @DisplayName("published pages render with styles and images")
  void publishedPagesRenderWithStylesAndImages(
      String route, String title, String heading, int width) {
    var failures = new ArrayList<String>();
    page.onResponse(
        response -> {
          if (response.status() >= 400) {
            failures.add(response.status() + " " + response.url());
          }
        });
    page.onRequestFailed(request -> failures.add(request.url() + ": " + request.failure()));
    page.setViewportSize(width, 900);
    var response = page.navigate(baseUrl + route);

    Assertions.assertThat(response.status()).isEqualTo(200);
    assertThat(page).hasTitle(title);
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
        .hasText(heading);
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
        .isVisible();
    assertThat(page.locator("body")).hasCSS("font-family", "system-ui, sans-serif");
    for (var image : page.locator("img").all()) {
      image.scrollIntoViewIfNeeded();
      assertThat(image).hasJSProperty("complete", true);
      Assertions.assertThat(image.evaluate("image => image.naturalWidth > 0")).isEqualTo(true);
    }
    Assertions.assertThat(
            page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"))
        .as("page fits the viewport")
        .isEqualTo(true);
    Assertions.assertThat(failures).isEmpty();
  }

  @Test
  @DisplayName("modal opens dismisses and restores focus")
  void modalOpensDismissesAndRestoresFocus() {
    page.navigate(caseUrl("modal", "closed"));
    var trigger =
        page.getByRole(
            AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal").setExact(true));
    var dialog =
        page.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Publish release?"));

    trigger.click();
    assertThat(dialog).isVisible();
    assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Got it")))
        .isFocused();
    page.keyboard().press("Escape");
    assertThat(dialog).isHidden();
    assertThat(trigger).isFocused();
    trigger.click();
    dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Got it")).click();
    assertThat(dialog).isHidden();
  }

  @Test
  @DisplayName("popover toggles and light dismisses")
  void popoverTogglesAndLightDismisses() {
    page.navigate(caseUrl("popover", "toggle"));
    var trigger =
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Toggle popover"));
    var popover =
        page.getByText(
            "Click outside or press Escape to close.", new Page.GetByTextOptions().setExact(true));
    trigger.click();
    assertThat(popover).isVisible();
    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)).click();
    assertThat(popover).isHidden();
    trigger.click();
    assertThat(popover).isVisible();
    trigger.click();
    assertThat(popover).isHidden();
  }

  @ParameterizedTest
  @CsvSource({"Open start drawer, Filters", "Open end drawer, Details"})
  @DisplayName("drawers open with an accessible name and dismiss")
  void drawersOpenWithAnAccessibleNameAndDismiss(String button, String title) {
    page.navigate(baseUrl + "/overlays");
    var trigger = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(button));
    var drawer =
        page.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName(title).setExact(true));
    trigger.click();
    assertThat(drawer).isVisible();
    page.keyboard().press("Escape");
    assertThat(drawer).isHidden();
    assertThat(trigger).isFocused();
  }

  @Test
  @DisplayName("labels focus fields and native validation tracks input")
  void labelsFocusFieldsAndNativeValidationTracksInput() {
    page.navigate(baseUrl + "/forms");
    var email = page.getByLabel("Work email", new Page.GetByLabelOptions().setExact(true));
    page.getByText("Work email", new Page.GetByTextOptions().setExact(true)).click();
    assertThat(email).isFocused();
    email.fill("invalid");
    email.press("Tab");
    var error =
        page.getByText(
            "Enter a valid email address, e.g. ada@example.com.",
            new Page.GetByTextOptions().setExact(true));
    assertThat(error).isVisible();
    email.fill("ada@example.com");
    email.press("Tab");
    assertThat(error).isHidden();
  }

  @Test
  @DisplayName("switching tabs changes visible content")
  void switchingTabsChangesVisibleContent() {
    page.navigate(baseUrl + "/navigation");
    var javaPanel =
        page.getByText(
            "Components are plain Java records and builders.",
            new Page.GetByTextOptions().setExact(true));
    var htmlPanel =
        page.getByText(
            "Output is semantic, validated HTML.", new Page.GetByTextOptions().setExact(true));
    assertThat(javaPanel).isVisible();
    assertThat(htmlPanel).isHidden();
    page.getByText("HTML", new Page.GetByTextOptions().setExact(true)).click();
    assertThat(htmlPanel).isVisible();
    assertThat(javaPanel).isHidden();
  }

  @Test
  @DisplayName("exclusive accordion closes previous item")
  void exclusiveAccordionClosesPreviousItem() {
    page.navigate(baseUrl + "/navigation");
    var first =
        page.getByText(
            "No. Disclosure is native HTML.", new Page.GetByTextOptions().setExact(true));
    var second =
        page.getByText(
            "Use Accordion.multiOpen(id) for that.", new Page.GetByTextOptions().setExact(true));
    assertThat(first).isVisible();
    assertThat(second).isHidden();
    page.getByText("Can several items be open?", new Page.GetByTextOptions().setExact(true))
        .click();
    assertThat(second).isVisible();
    assertThat(first).isHidden();
  }

  @Test
  @DisplayName("navigation and table of contents reach published targets")
  void navigationAndTableOfContentsReachPublishedTargets() {
    page.navigate(baseUrl + "/");
    page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Primary navigation"))
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Forms").setExact(true))
        .click();
    assertThat(page).hasURL(baseUrl + "/forms/");
    page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("On this page"))
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Switch").setExact(true))
        .click();
    assertThat(page).hasURL(baseUrl + "/forms/#toc-layout--switches");
    assertThat(
            page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Switch").setExact(true)))
        .isInViewport();
    var toggle =
        page.getByRole(AriaRole.SWITCH, new Page.GetByRoleOptions().setName("Weekly digest"));
    toggle.check();
    assertThat(toggle).isChecked();
  }

  @Test
  @DisplayName("search filters visible links and navigates by keyboard")
  void searchFiltersVisibleLinksAndNavigatesByKeyboard() {
    page.navigate(baseUrl + "/");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search").setExact(true))
        .click();
    var dialog =
        page.getByRole(
            AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Quick open command palette"));
    var query = dialog.getByRole(AriaRole.SEARCHBOX);
    assertThat(query).isFocused();
    query.fill("modal");
    var result =
        dialog.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Modal dialog"));
    assertThat(dialog.locator("a.palette-item:visible")).hasCount(1);
    query.press("ArrowDown");
    assertThat(result).isFocused();
    result.press("Enter");
    assertThat(page).hasURL(baseUrl + "/overlays/#toc-layout--modal");
    assertThat(
            page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal").setExact(true)))
        .isInViewport();
  }

  @Test
  @DisplayName("search handles empty results clearing and enter from the query")
  void searchHandlesEmptyResultsClearingAndEnterFromTheQuery() {
    page.navigate(baseUrl + "/");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search").setExact(true))
        .click();
    var dialog = page.getByRole(AriaRole.DIALOG);
    var query = dialog.getByRole(AriaRole.SEARCHBOX);
    query.fill("not-a-real-component");
    assertThat(dialog.getByRole(AriaRole.STATUS)).isVisible();
    assertThat(dialog.locator("a.palette-item:visible")).hasCount(0);
    query.fill("");
    assertThat(dialog.getByRole(AriaRole.STATUS)).isHidden();
    query.fill("auto growing textarea");
    assertThat(dialog.locator("a.palette-item:visible")).hasCount(1);
    query.press("Enter");
    assertThat(page).hasURL(baseUrl + "/forms/#toc-layout--textarea");
  }

  @Test
  @DisplayName("palettes include their runtime automatically and keep searches independent")
  void palettesIncludeTheirRuntimeAutomaticallyAndKeepSearchesIndependent() {
    var errors = new ArrayList<String>();
    page.onPageError(errors::add);
    page.navigate(baseUrl + "/search/");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Find articles")).click();
    var dialog = page.getByRole(AriaRole.DIALOG);
    var query = dialog.getByRole(AriaRole.SEARCHBOX);
    query.fill("cafe");
    assertThat(dialog.getByRole(AriaRole.LINK)).hasCount(1);
    assertThat(
            dialog.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Café guide")))
        .isVisible();
    query.press("Escape");
    assertThat(dialog).isHidden();
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Find guides")).click();
    assertThat(query).hasValue("");
    assertThat(dialog.getByRole(AriaRole.LINK)).hasCount(2);
    query.fill("guide");
    query.press("ArrowUp");
    var forms =
        dialog.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Forms guide"));
    assertThat(forms).isFocused();
    forms.press("Enter");
    assertThat(page).hasURL(baseUrl + "/search/#forms");
    assertThat(dialog).isHidden();
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Forms guide")))
        .isInViewport();
    Assertions.assertThat(errors).isEmpty();
  }

  @Test
  @DisplayName("tokens customize typography density and radii across components")
  void tokensCustomizeTypographyDensityAndRadiiAcrossComponents() {
    page.navigate(baseUrl + "/theme/");
    assertThat(page.locator("body")).hasCSS("font-family", "monospace");
    assertThat(page.locator("body")).hasCSS("font-size", "20px");
    var card = page.getByRole(AriaRole.ARTICLE);
    assertThat(card).hasCSS("border-radius", "18px");
    assertThat(card).hasCSS("padding-inline-start", "10px");
    var field = page.getByLabel("Custom field", new Page.GetByLabelOptions().setExact(true));
    assertThat(field).hasCSS("font-family", "monospace");
    assertThat(field).hasCSS("padding-inline-start", "7px");
    assertThat(field).hasCSS("border-radius", "12px");
  }

  @Test
  @DisplayName("relative links preserve queries and encoded fragments on the published site")
  void relativeLinksPreserveQueriesAndEncodedFragmentsOnThePublishedSite() {
    page.navigate(baseUrl + "/urls/links/start/");
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Read the café guide"))
        .click();
    assertThat(page).hasURL(baseUrl + "/urls/links/caf%C3%A9/?mode=full#caf%C3%A9%2Bintro");
    assertThat(
            page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Café introduction")))
        .isVisible();
  }

  @ParameterizedTest
  @ValueSource(ints = {390, 1440})
  @DisplayName("component cases fit the viewport")
  void componentCasesFitTheViewport(int width) {
    var failures = new ArrayList<String>();
    page.onResponse(
        response -> {
          if (response.status() >= 400) {
            failures.add(response.status() + " " + response.url());
          }
        });
    page.onRequestFailed(request -> failures.add(request.url() + ": " + request.failure()));
    page.setViewportSize(width, 900);
    for (var route : ShowcaseSite.createDefinition().routes()) {
      // Color scheme cannot change layout, so the dark cases add navigations but no coverage.
      if (!route.path().startsWith("/stories/") || route.path().contains("/dark/")) {
        continue;
      }
      var response = page.navigate(baseUrl + route.path());
      Assertions.assertThat(response.status()).as(route.path()).isEqualTo(200);
      assertThat(page).hasTitle(route.document().title());
      assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
          .hasText(route.displayTitle());
      Assertions.assertThat(fitsViewport()).as(route.path() + " fits the viewport").isEqualTo(true);
    }
    Assertions.assertThat(failures).isEmpty();
  }

  @Test
  @DisplayName("a named case matches the setup used on its documentation page")
  void namedCaseMatchesTheDocumentedSetup() {
    page.navigate(caseUrl("modal", "closed"));
    assertClosedPublishDialog();
    page.navigate(baseUrl + "/overlays/");
    assertClosedPublishDialog();

    page.navigate(caseUrl("popover", "toggle"));
    assertThat(popoverCopy()).hasCount(1);
    page.navigate(baseUrl + "/overlays/");
    assertThat(popoverCopy()).hasCount(1);
  }

  @Test
  @DisplayName("opening another case starts fresh and reopening restores the declared state")
  void casesStartFreshAfterNavigation() {
    page.navigate(caseUrl("modal", "closed"));
    namedButton("Open modal").click();
    assertThat(
            page.getByRole(
                AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Publish release?")))
        .isVisible();

    page.navigate(caseUrl("switches", "on"));
    var toggle =
        page.getByRole(AriaRole.SWITCH, new Page.GetByRoleOptions().setName("Weekly digest"));
    assertThat(toggle).isChecked();
    toggle.uncheck();
    assertThat(toggle).not().isChecked();

    page.navigate(caseUrl("modal", "closed"));
    assertThat(
            page.getByRole(
                AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Publish release?")))
        .isHidden();

    page.navigate(caseUrl("switches", "on"));
    assertThat(
            page.getByRole(AriaRole.SWITCH, new Page.GetByRoleOptions().setName("Weekly digest")))
        .isChecked();
  }

  @Test
  @DisplayName("case variants expose disabled busy compact and open states")
  void caseVariantsExposeTheirDeclaredState() {
    page.navigate(caseUrl("actions", "disabled"));
    assertThat(namedButton("Delete")).isDisabled();

    page.navigate(caseUrl("actions", "busy"));
    assertThat(namedButton("Publishing")).hasAttribute("aria-busy", "true");

    page.navigate(caseUrl("actions", "icon"));
    assertThat(namedButton("Refresh")).isVisible();

    page.navigate(caseUrl("actions", "compact"));
    var compact = fontSize("Edit");
    page.navigate(caseUrl("actions", "primary"));
    Assertions.assertThat(compact).isLessThan(fontSize("Save"));

    page.navigate(caseUrl("modal", "open"));
    assertThat(
            page.getByRole(
                AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Publish release?")))
        .isVisible();

    page.navigate(caseUrl("switches", "off"));
    assertThat(
            page.getByRole(AriaRole.SWITCH, new Page.GetByRoleOptions().setName("Weekly digest")))
        .not()
        .isChecked();
  }

  @Test
  @DisplayName("appearance and density come from the case url")
  void appearanceAndDensityComeFromTheCaseUrl() {
    page.navigate(
        baseUrl
            + Catalog.casePath(
                "actions", "primary", Catalog.Appearance.DARK, Catalog.Density.COMPACT));
    Assertions.assertThat(colorScheme()).isEqualTo("dark");
    Assertions.assertThat(densityValue()).isEqualTo("0.8");
    var compact = paddingInline("Save");

    page.navigate(
        baseUrl
            + Catalog.casePath(
                "actions", "primary", Catalog.Appearance.LIGHT, Catalog.Density.REGULAR));
    Assertions.assertThat(colorScheme()).isEqualTo("light");
    Assertions.assertThat(densityValue()).isEqualTo("1");
    Assertions.assertThat(compact).isLessThan(paddingInline("Save"));
  }

  @ParameterizedTest
  @ValueSource(ints = {390, 1440})
  @DisplayName("overflowed actions stay reachable without widening the document")
  void overflowedActionsStayReachable(int width) {
    page.setViewportSize(width, 900);
    page.navigate(caseUrl("overflow", "row"));
    namedButton("Save").focus();
    assertThat(namedButton("Save")).isFocused();
    for (var step = 0; step < 4; step++) {
      page.keyboard().press("Tab");
    }
    assertThat(namedButton("Archive")).isFocused();
    Assertions.assertThat(fitsViewport()).as("document fits at " + width).isEqualTo(true);
    if (width == 390) {
      Assertions.assertThat(
              page.evaluate(
                  "() => { const el = document.querySelector('.story-overflow');"
                      + " return el.scrollWidth > el.clientWidth; }"))
          .as("the row scrolls inside its region")
          .isEqualTo(true);
    }
  }

  private static String caseUrl(String story, String variant) {
    return baseUrl + Catalog.casePath(story, variant);
  }

  private Locator namedButton(String name) {
    return page.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  private void assertClosedPublishDialog() {
    assertThat(namedButton("Open modal")).isVisible();
    var dialog =
        page.locator("dialog").filter(new Locator.FilterOptions().setHasText("Publish release?"));
    assertThat(dialog).hasCount(1);
    Assertions.assertThat(dialog.textContent()).contains("Got it");
  }

  private Locator popoverCopy() {
    return page.getByText(
        "Click outside or press Escape to close.", new Page.GetByTextOptions().setExact(true));
  }

  private String colorScheme() {
    return (String) page.evaluate("getComputedStyle(document.documentElement).colorScheme");
  }

  private String densityValue() {
    return (String)
        page.evaluate(
            "getComputedStyle(document.documentElement).getPropertyValue('--density').trim()");
  }

  private double paddingInline(String name) {
    return number(
        namedButton(name).evaluate("el => parseFloat(getComputedStyle(el).paddingInlineStart)"));
  }

  private double fontSize(String name) {
    return number(namedButton(name).evaluate("el => parseFloat(getComputedStyle(el).fontSize)"));
  }

  private static double number(Object value) {
    return ((Number) value).doubleValue();
  }

  private boolean fitsViewport() {
    return (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth");
  }
}
