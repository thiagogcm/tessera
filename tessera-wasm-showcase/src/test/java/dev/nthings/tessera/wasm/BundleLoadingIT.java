package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;

/// Loading the published bundles through the shipped bootstrap: the full policy at root and nested
/// asset bases, and usable fallbacks when a resource, the browser, or an initial render fails.
@DisplayName("Bundle loading")
class BundleLoadingIT {
  private static final String POLICY =
      "default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self'; object-src 'none'";

  @BothEngines
  @DisplayName("both entries mount and work under the full policy at root and nested asset bases")
  void entriesRunUnderTheFullPolicy(Session session) {
    var fixture = session.fixture();
    verifyHome(session, fixture.rootServer().baseUrl());
    verifyHome(session, fixture.server().baseUrl());
  }

  private static void verifyHome(Session session, String origin) {
    var page = session.page();
    var resources = new ArrayList<String>();
    Consumer<com.microsoft.playwright.Request> record =
        request -> {
          if (request.url().contains("/wasm/")) {
            resources.add(request.url());
          }
        };
    page.onRequest(record);
    var response = Objects.requireNonNull(page.navigate(origin + "/"));
    Assertions.assertThat(response.headers()).containsEntry("content-security-policy", POLICY);

    var name = session.field("Name");
    name.fill("Ada");
    session.button("Increment").click();
    assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 1");
    assertThat(name).hasValue("Ada");
    session.button("Second increment").click();
    assertThat(session.text("Second count: 1")).isVisible();
    session.button("Animate progress").click();
    assertThat(session.role(AriaRole.PROGRESSBAR, "Upload progress"))
        .hasCSS("inline-size", "120px");

    // The bootstrap, and each entry's launcher and image, load once.
    Assertions.assertThat(resources).hasSize(5).doesNotHaveDuplicates();
    Assertions.assertThat(page.evaluate("typeof globalThis.GraalVM")).isEqualTo("undefined");
    page.offRequest(record);
  }

  @BothEngines
  @DisplayName("a failing resource keeps that entry's fallback usable while the other entry mounts")
  void failingResourcesKeepTheFallback(Session session) {
    var site = session.fixture().server();
    var home = Jsoup.parse(fixtureHome(session));
    var app = Objects.requireNonNull(home.getElementById("app"));
    var launcher = app.attr("data-tessera-entry");
    var image = imageUrl(app.attr("data-tessera-image"), app.attr("data-tessera-resources"));
    var otherImage =
        imageUrl(
            Objects.requireNonNull(home.getElementById("counter")).attr("data-tessera-image"),
            Objects.requireNonNull(home.getElementById("counter")).attr("data-tessera-resources"));
    var page = session.page();
    var otherImageBytes = page.request().get(site.baseUrl() + otherImage).body();
    var imageBytes = page.request().get(site.baseUrl() + image).body();
    session.expectError("Unable to load or verify");
    session.expectError("Failed to load resource");
    session.expectError("Failed to find a valid digest");
    session.expectError("integrity");
    session.expectError("None of the");
    session.expectError("Loading failed for the <script>");

    record Failure(String name, String url, Consumer<Route> handler) {}
    var failures =
        List.of(
            new Failure(
                "missing image",
                image,
                route -> route.fulfill(new Route.FulfillOptions().setStatus(404))),
            new Failure(
                "image with other content",
                image,
                route ->
                    route.fulfill(
                        new Route.FulfillOptions()
                            .setContentType("application/wasm")
                            .setBodyBytes(otherImageBytes))),
            new Failure(
                "image with a wrong media type",
                image,
                route ->
                    route.fulfill(
                        new Route.FulfillOptions()
                            .setContentType("application/octet-stream")
                            .setBodyBytes(imageBytes))),
            new Failure("image download aborted", image, Route::abort),
            new Failure(
                "missing launcher",
                launcher,
                route -> route.fulfill(new Route.FulfillOptions().setStatus(404))),
            new Failure(
                "modified launcher",
                launcher,
                route ->
                    route.fulfill(
                        new Route.FulfillOptions()
                            .setContentType("text/javascript")
                            .setBody("globalThis.modifiedLauncher = true;"))));
    for (var failure : failures) {
      page.route("**" + failure.url(), failure.handler());
      try {
        session.open("/");
        var host = page.locator("#app");
        assertThat(host.getByRole(AriaRole.STATUS)).isVisible();
        assertThat(host.getByRole(AriaRole.LINK)).hasText("Open the static guide");
        var notes = session.field("Fallback notes");
        notes.fill("Editable while Java is unavailable");
        assertThat(notes).hasValue("Editable while Java is unavailable");
        session.button("Second increment").click();
        assertThat(session.text("Second count: 1")).isVisible();
        Assertions.assertThat(page.evaluate("globalThis.modifiedLauncher")).isNull();
      } catch (AssertionError e) {
        throw new AssertionError(failure.name() + ": " + e.getMessage(), e);
      } finally {
        page.unrouteAll();
      }
    }
  }

  @BothEngines
  @DisplayName(
      "a browser without the required Wasm features keeps every fallback and loads nothing")
  void unsupportedBrowsersKeepTheFallback(Session session) {
    var page = session.page();
    var requested = new ArrayList<String>();
    page.onRequest(request -> requested.add(request.url()));
    page.addInitScript("WebAssembly.validate = () => false");
    session.expectError("WebAssembly GC");
    session.open("/");

    assertThat(page.locator("#app").getByRole(AriaRole.STATUS)).isVisible();
    assertThat(page.locator("#counter").getByRole(AriaRole.STATUS)).isVisible();
    assertThat(page.getByRole(AriaRole.LINK)).hasText("Open the static guide");
    assertThat(session.text("Counter is unavailable.")).isVisible();
    assertThat(session.field("Fallback notes")).isEditable();
    // Only the bootstrap itself was fetched: no launcher, image or other resource.
    Assertions.assertThat(requested).filteredOn(url -> url.contains("/wasm/")).hasSize(1);
  }

  @BothEngines
  @DisplayName("a host naming a view its entry lacks keeps its fallback while other hosts mount")
  void unknownViewsKeepTheFallback(Session session) {
    var page = session.page();
    page.route(
        session.url("/"),
        route -> {
          var response = route.fetch();
          route.fulfill(
              new Route.FulfillOptions()
                  .setResponse(response)
                  .setBody(
                      response
                          .text()
                          .replace(
                              "data-tessera-view=\"counter\"", "data-tessera-view=\"missing\"")));
        });
    session.open("/");
    session.awaitError("missing");

    session.button("Increment").click();
    assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 1");
    assertThat(session.text("Counter is unavailable.")).isVisible();
  }

  @BothEngines
  @DisplayName("a failed first render keeps that host's fallback while other hosts mount")
  void failedFirstRenderKeepsTheFallback(Session session) {
    session.open("/broken/");
    session.awaitError("Simulated initial render failure");

    assertThat(session.role(AriaRole.LINK, "Return to the static home")).isVisible();
    session.button("Second increment").click();
    assertThat(session.text("Second count: 1")).isVisible();
    Assertions.assertThat(session.errors()).hasSize(1);
  }

  private static String fixtureHome(Session session) {
    return Objects.requireNonNull(session.fixture().site().pages().get("/"));
  }

  /// The URL a bundle's resource manifest (`name url integrity` triples) gives the image `name`.
  private static String imageUrl(String name, String manifest) {
    var fields = manifest.split(" ");
    for (var index = 0; index < fields.length; index += 3) {
      if (fields[index].equals(name)) {
        return fields[index + 1];
      }
    }
    throw new IllegalArgumentException("No image " + name + " in " + manifest);
  }
}
