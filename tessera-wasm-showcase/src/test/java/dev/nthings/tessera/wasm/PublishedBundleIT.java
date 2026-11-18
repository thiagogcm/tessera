package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import dev.nthings.tessera.compiler.Asset;
import dev.nthings.tessera.compiler.CompiledSite;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SitePreviewServer;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.document.WasmBundle;
import dev.nthings.tessera.document.WasmBundle.Kind;
import dev.nthings.tessera.document.WasmBundle.Resource;
import dev.nthings.tessera.wasm.example.PublishedWeb;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Published Web Image bundles")
class PublishedBundleIT {
  private static final String POLICY =
      "default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self'; object-src 'none'";
  private static CompiledSite nested;
  private static SitePreviewServer rootServer;
  private static SitePreviewServer nestedServer;
  private static Playwright playwright;
  private static Path build;

  @BeforeAll
  static void start() {
    build = Path.of(System.getProperty("wasm.build"));
    nested = PublishedWeb.compile(build, "/assets/./demo");
    rootServer = SitePreviewServer.start(PublishedWeb.compile(build, "/"), 0);
    nestedServer = SitePreviewServer.start(nested, 0);
    playwright = Playwright.create();
  }

  @AfterAll
  static void stop() {
    if (playwright != null) {
      playwright.close();
    }
    if (rootServer != null) {
      rootServer.close();
    }
    if (nestedServer != null) {
      nestedServer.close();
    }
  }

  @Test
  @DisplayName("the packaged distribution contains exactly the compiled pages and assets")
  void includesEveryPublishedFile() throws IOException {
    var output = build.resolve("published-web");
    for (var asset : nested.files().values()) {
      Assertions.assertThat(Files.readAllBytes(output.resolve(asset.path().substring(1))))
          .isEqualTo(asset.content());
    }
    Assertions.assertThat(output.resolve("index.html")).hasContent(nested.pages().get("/"));
    Assertions.assertThat(output.resolve("guide/index.html"))
        .hasContent(nested.pages().get("/guide"));
    try (var paths = Files.walk(output)) {
      var files = paths.filter(Files::isRegularFile).toList();
      Assertions.assertThat(files).hasSize(nested.files().size() + nested.pages().size());
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"chromium", "firefox"})
  @DisplayName(
      "published entries share resources and run controls and dynamic styles under the full policy")
  void runsUnderFullCsp(String engine) {
    try (var browser = launch(engine)) {
      verifyHome(browser, rootServer.baseUrl(), "/wasm/");
      verifyHome(browser, nestedServer.baseUrl(), "/assets/demo/wasm/");
      try (var page = browser.newPage()) {
        var failures = new ArrayList<String>();
        page.onPageError(failures::add);
        var response =
            Objects.requireNonNull(page.navigate(nestedServer.baseUrl() + "/guide/?fail-mount"));
        Assertions.assertThat(response.headers()).containsEntry("content-security-policy", POLICY);
        assertThat(page.locator("#counter button")).isVisible();
        assertThat(page.locator("#broken a")).hasText("Return to the static home");
        page.waitForCondition(() -> !failures.isEmpty());
        Assertions.assertThat(failures)
            .singleElement()
            .asString()
            .contains(
                "Render rejected in browser root 'broken'", "Simulated initial render failure");
      }
      try (var page = browser.newPage()) {
        var failures = new ArrayList<String>();
        page.onPageError(failures::add);
        page.addInitScript(
            """
                    const replace = Element.prototype.replaceChildren;
                    Element.prototype.replaceChildren = function (...children) {
                      if (this.id === 'app' && !window.initialFallback) {
                        window.initialFallback = this.querySelector('a');
                      }
                      return replace.apply(this, children);
                    };
            const original = Node.prototype.insertBefore;
            Node.prototype.insertBefore = function (node, before) {
              if (this.id === 'app' && this.hasAttribute('data-tessera-root')) {
                Node.prototype.insertBefore = original;
                throw new Error('Initial native installation failed');
              }
              return original.call(this, node, before);
            };
            """);
        page.navigate(nestedServer.baseUrl());
        assertThat(page.locator("#notes input")).isVisible();
        assertThat(page.locator("#app a")).hasText("Open the static guide");
        page.waitForCondition(() -> !failures.isEmpty());
        Assertions.assertThat(failures)
            .singleElement()
            .asString()
            .contains("Initial native installation failed");
        Assertions.assertThat(
                page.evaluate("document.querySelector('#app a') === window.initialFallback"))
            .isEqualTo(true);
      }
      try (var page = browser.newPage()) {
        page.addInitScript(
            """
            new MutationObserver((entries, observer) => {
              const unmount = Array.from(document.querySelectorAll('#app button'))
                .find(button => button.textContent.trim() === 'Unmount');
              if (unmount) {
                observer.disconnect();
                unmount.click();
                requestAnimationFrame(() => requestAnimationFrame(() => {
                  document.documentElement.setAttribute('data-test-unmounted', '');
                }));
              }
            }).observe(document, { childList: true, subtree: true });
            """);
        page.navigate(nestedServer.baseUrl());
        assertThat(page.locator("html[data-test-unmounted]")).hasCount(1);
        assertThat(page.locator("#notes input")).isVisible();
        assertThat(page.locator("#app")).isEmpty();
      }
    }
  }

  private static void verifyHome(Browser browser, String origin, String bundlePath) {
    try (var page = browser.newPage()) {
      var failures = new ArrayList<String>();
      var resources = new ArrayList<String>();
      var wasmTypes = new ArrayList<String>();
      page.onPageError(failures::add);
      page.onRequest(request -> {
        if (request.url().contains(bundlePath)) {
          resources.add(request.url());
        }
      });
      page.onResponse(response -> {
        if (response.url().endsWith(".wasm")) {
          wasmTypes.add(response.headers().get("content-type"));
        }
      });
      page.addInitScript(
          """
          window.policyViolations = [];
          document.addEventListener('securitypolicyviolation', event =>
            window.policyViolations.push(event.violatedDirective + ' ' + event.blockedURI));
          """);
      var response = Objects.requireNonNull(page.navigate(origin));
      Assertions.assertThat(response.headers()).containsEntry("content-security-policy", POLICY);
      assertThat(page.locator("#notes input")).isVisible();
      var input = page.locator("#app input[name=name]");
      assertThat(input).hasCSS("border-top-style", "solid");
      input.fill("Ada");
      page.getByText("Increment", new Page.GetByTextOptions().setExact(true)).click();
      assertThat(page.locator("#app [role=status]")).hasText("Count: 1");
      assertThat(input).hasValue("Ada");
      page.getByText("Second increment", new Page.GetByTextOptions().setExact(true)).click();
      assertThat(page.locator("#counter [data-second-count]"))
          .hasAttribute("data-second-count", "1");
      var meter = page.locator("[data-live-meter]");
      page.getByText("Animate live meter", new Page.GetByTextOptions().setExact(true)).click();
      assertThat(meter).hasCSS("inline-size", "120px");
      Assertions.assertThat(resources).hasSize(5).doesNotHaveDuplicates();
      Assertions.assertThat(wasmTypes).containsExactly("application/wasm", "application/wasm");
      Assertions.assertThat(page.evaluate("window.policyViolations")).isEqualTo(List.of());
      Assertions.assertThat(failures).isEmpty();
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"chromium", "firefox"})
  @DisplayName(
      "missing and modified bundle files preserve interactive fallback and isolate other entries")
  void keepsFallbackWhenBundleResourcesFail(String engine) {
    var files =
        nested.files().values().stream().filter(asset -> asset.path().endsWith(".wasm")).toList();
    var demoImage = files.getFirst();
    var secondImage = files.getLast();
    var demoLauncher =
        nested.files().values().stream()
            .filter(asset ->
                asset.path().startsWith("/assets/demo/wasm/") && asset.path().endsWith(".js"))
            .filter(asset ->
                asset.textContent().contains("Tessera requires a verified WebAssembly image."))
            .findFirst()
            .orElseThrow();

    try (var browser = launch(engine)) {
      verifyFailure(browser, demoImage, new Route.FulfillOptions().setStatus(404));
      verifyFailure(
          browser,
          demoImage,
          new Route.FulfillOptions()
              .setStatus(200)
              .setContentType(demoImage.contentType())
              .setBodyBytes(secondImage.content()));
      verifyFailure(
          browser,
          demoLauncher,
          new Route.FulfillOptions()
              .setStatus(200)
              .setContentType(demoLauncher.contentType())
              .setBodyBytes(
                  "globalThis.modifiedLauncher = true;".getBytes(StandardCharsets.UTF_8)));
    }
  }

  private static void verifyFailure(
      Browser browser, Asset failing, Route.FulfillOptions replacement) {
    try (var page = browser.newPage()) {
      page.route("**" + failing.path(), route -> route.fulfill(replacement));
      page.navigate(nestedServer.baseUrl());
      assertThat(page.locator("#app [data-tessera-load-error]")).containsText(failing.path());
      assertThat(page.locator("#app a")).hasText("Open the static guide");
      var fallback = page.locator("#notes input");
      fallback.fill("Editable while Java is unavailable");
      assertThat(fallback).hasValue("Editable while Java is unavailable");
      page.getByText("Second increment", new Page.GetByTextOptions().setExact(true)).click();
      assertThat(page.locator("#counter [data-second-count]"))
          .hasAttribute("data-second-count", "1");
      Assertions.assertThat(page.evaluate("globalThis.modifiedLauncher")).isNull();
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"chromium", "firefox"})
  @DisplayName("the verified supplementary payload is available to the launcher by logical name")
  void verifiesSupplementaryResources(String engine) {
    var source =
        """
        globalThis.GraalVM = {
          Config: class {},
          run(args, config) {
            const bytes = config.tesseraResources.get("extra/message.bin");
            document.getElementById(args[0].split("=")[0]).textContent =
              new TextDecoder().decode(bytes);
          }
        };
        """;
    var launcher =
        Resource.of(
            "launcher.js",
            Kind.LAUNCHER,
            source.getBytes(StandardCharsets.UTF_8),
            "main.wasm",
            "extra/message.bin");
    var bundle =
        new WasmBundle(
            "launcher.js",
            "main.wasm",
            List.of(
                launcher,
                Resource.of("main.wasm", Kind.WASM, new byte[] {0, 'a', 's', 'm', 1, 0, 0, 0}),
                Resource.of(
                    "extra/message.bin",
                    Kind.DATA,
                    "Verified chunk".getBytes(StandardCharsets.UTF_8))));
    var document =
        new PageDocument(
            "Chunks",
            Documents.wasmMount(
                "chunks", "view", bundle, Elements.p().text("Static chunk fallback")));
    var site = Compiler.compile(SiteDefinition.of(List.of(new SiteRoute("/", document))));
    var chunk =
        site.files().values().stream()
            .filter(file -> file.path().endsWith(".bin"))
            .findFirst()
            .orElseThrow();
    try (var server = SitePreviewServer.start(site, 0);
        var browser = launch(engine)) {
      try (var page = browser.newPage()) {
        page.navigate(server.baseUrl());
        assertThat(page.locator("#chunks")).hasText("Verified chunk");
      }
      try (var page = browser.newPage()) {
        page.route(
            "**" + chunk.path(),
            route ->
                route.fulfill(
                    new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType(chunk.contentType())
                        .setBody("Modified chunk")));
        page.navigate(server.baseUrl());
        assertThat(page.locator("#chunks [data-tessera-load-error]")).containsText(chunk.path());
        assertThat(page.getByText("Static chunk fallback")).isVisible();
      }
    }
  }

  private static Browser launch(String engine) {
    return (engine.equals("chromium") ? playwright.chromium() : playwright.firefox()).launch();
  }
}
