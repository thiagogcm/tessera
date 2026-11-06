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
import dev.nthings.tessera.showcase.ShowcaseSite;
import dev.nthings.tessera.site.SiteCompiler;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

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
  void modalOpensDismissesAndRestoresFocus() {
    page.navigate(baseUrl + "/overlays");
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
  void popoverTogglesAndLightDismisses() {
    page.navigate(baseUrl + "/overlays");
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
}
