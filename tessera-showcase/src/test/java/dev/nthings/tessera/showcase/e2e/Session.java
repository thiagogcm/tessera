package dev.nthings.tessera.showcase.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// One test's browser context and page on the published site.
///
/// The session records what a visitor would notice going wrong, whatever the test checks: an
/// uncaught script error, a `console.error`, a request that fails, or a same-origin response with
/// an error status. [BrowserSessions] fails the test when any were recorded.
final class Session {

  /// Failures a navigation causes on purpose: loads cancelled by leaving the page.
  private static final Set<String> CANCELLED =
      Set.of("net::ERR_ABORTED", "NS_BINDING_ABORTED", "NS_ERROR_ABORT");

  static final int DESKTOP_WIDTH = 1440;
  static final int PHONE_WIDTH = 390;
  static final int HEIGHT = 900;

  private final Engine engine;
  private final String baseUrl;
  private final BrowserContext context;
  private final Page page;
  private final List<String> problems = new ArrayList<>();

  Session(Engine engine, Browser browser, String baseUrl) {
    this.engine = engine;
    this.baseUrl = baseUrl;
    this.context =
        browser.newContext(new Browser.NewContextOptions().setViewportSize(DESKTOP_WIDTH, HEIGHT));
    context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
    context.onWebError(error -> problems.add("uncaught error: " + error.error()));
    context.onConsoleMessage(message -> {
      if (message.type().equals("error")) {
        problems.add("console.error: " + message.text() + " (" + message.location() + ")");
      }
    });
    context.onRequestFailed(this::recordFailure);
    context.onResponse(this::recordResponse);
    this.page = context.newPage();
  }

  Engine engine() {
    return engine;
  }

  Page page() {
    return page;
  }

  BrowserContext context() {
    return context;
  }

  /// The absolute URL of a site path such as `/forms/`.
  String url(String path) {
    return baseUrl + path;
  }

  /// Opens a site path and returns once its custom elements are defined.
  ///
  /// Module scripts run before `load`, so an element still undefined then means its behavior never
  /// loaded; the request guard reports why.
  Page open(String path) {
    var response = page.navigate(url(path));
    assertThat(response).as("response for %s", path).isNotNull();
    assertThat(response.status()).as("status of %s", path).isLessThan(400);
    page.waitForFunction("() => document.querySelectorAll(':not(:defined)').length === 0");
    return page;
  }

  /// Resizes the window to `width` by the default height.
  Session width(int width) {
    page.setViewportSize(width, HEIGHT);
    return this;
  }

  /// A button by exact accessible name.
  Locator button(String name) {
    return page.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  /// A link by exact accessible name.
  Locator link(String name) {
    return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  /// A dialog by exact accessible name.
  Locator dialog(String name) {
    return page.getByRole(
        AriaRole.DIALOG, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  /// A landmark or other role by exact accessible name.
  Locator role(AriaRole role, String name) {
    return page.getByRole(role, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  /// Text by exact content.
  Locator text(String text) {
    return page.getByText(text, new Page.GetByTextOptions().setExact(true));
  }

  /// Presses Tab from the start of the page until `target` has focus, as a keyboard user reaches
  /// it; fails after 60 presses.
  void tabTo(Locator target) {
    page.evaluate("document.activeElement?.blur()");
    for (var presses = 0; presses < 60; presses++) {
      page.keyboard().press("Tab");
      if (Boolean.TRUE.equals(target.evaluate("el => el === document.activeElement"))) {
        return;
      }
    }
    throw new AssertionError("Tab never reached " + target);
  }

  /// The element's box in viewport coordinates; fails if it is not rendered.
  static BoundingBox box(Locator element) {
    var box = element.boundingBox();
    assertThat(box).as("box of %s", element).isNotNull();
    return box;
  }

  /// Whether a pointer at the centre of `element` would reach it, rather than a sticky header or
  /// another layer painted on top.
  ///
  /// Waits up to two seconds for scrolling to settle before answering no.
  static boolean unobscured(Locator element) {
    try {
      element
          .page()
          .waitForFunction(
              """
              el => {
                const r = el.getBoundingClientRect();
                const x = r.left + r.width / 2, y = r.top + r.height / 2;
                if (x < 0 || y < 0 || x > innerWidth || y > innerHeight) return false;
                const hit = document.elementFromPoint(x, y);
                return hit !== null && (el === hit || el.contains(hit));
              }
              """,
              element.elementHandle(),
              new Page.WaitForFunctionOptions().setTimeout(2000));
      return true;
    } catch (TimeoutError e) {
      return false;
    }
  }

  /// The WCAG contrast ratio between the element's text color and its own background color.
  static double contrast(Locator element) {
    return number(
        element.evaluate(
            """
            el => {
              const srgb = (color) => {
                const context = document.createElement('canvas').getContext('2d');
                context.fillStyle = color;
                context.fillRect(0, 0, 1, 1);
                return Array.from(context.getImageData(0, 0, 1, 1).data.slice(0, 3));
              };
              const luminance = (rgb) => {
                const [r, g, b] = rgb.map((v) => {
                  v /= 255;
                  return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
                });
                return 0.2126 * r + 0.7152 * g + 0.0722 * b;
              };
              const style = getComputedStyle(el);
              const text = luminance(srgb(style.color));
              const background = luminance(srgb(style.backgroundColor));
              return (Math.max(text, background) + 0.05) / (Math.min(text, background) + 0.05);
            }
            """));
  }

  /// The right edge of the area fixed-position content can use, inside any scrollbar gutter.
  double contentRight() {
    return number(page.evaluate("document.documentElement.getBoundingClientRect().right"));
  }

  /// Whether the document is no wider than the window.
  boolean fitsViewport() {
    return (Boolean) page.evaluate("document.documentElement.scrollWidth <= innerWidth");
  }

  /// A number returned by `evaluate`.
  static double number(Object value) {
    return ((Number) value).doubleValue();
  }

  List<String> problems() {
    return problems;
  }

  /// Waits for one round trip to the page, so events it sent before the test ended are recorded.
  void settle(List<Throwable> errors) {
    try {
      page.evaluate("() => new Promise((resolve) => requestAnimationFrame(() => resolve()))");
    } catch (PlaywrightException e) {
      errors.add(e);
    }
  }

  /// Stops tracing, keeping the trace and a screenshot under `artifacts` only when kept.
  void finish(Path artifacts, boolean keep, List<Throwable> errors) {
    try {
      if (keep) {
        try {
          page.screenshot(
              new Page.ScreenshotOptions()
                  .setPath(artifacts.resolveSibling(artifacts.getFileName() + ".png"))
                  .setFullPage(true));
        } catch (RuntimeException e) {
          errors.add(e);
        }
        context
            .tracing()
            .stop(
                new Tracing.StopOptions()
                    .setPath(artifacts.resolveSibling(artifacts.getFileName() + ".zip")));
      } else {
        context.tracing().stop();
      }
    } finally {
      context.close();
    }
  }

  private void recordFailure(Request request) {
    var failure = request.failure();
    if (failure != null && !CANCELLED.contains(failure)) {
      problems.add("request failed: " + request.url() + " (" + failure + ")");
    }
  }

  private void recordResponse(Response response) {
    if (response.status() >= 400 && response.url().startsWith(baseUrl)) {
      problems.add("HTTP " + response.status() + ": " + response.url());
    }
  }
}
