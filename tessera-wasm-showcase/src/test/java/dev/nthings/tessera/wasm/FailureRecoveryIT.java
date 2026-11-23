package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// Invalid renders, runaway rendering, failing handlers and page id collisions on the failures
/// page: each is reported, leaves what the user sees in place, and the view recovers.
@DisplayName("Failure recovery")
class FailureRecoveryIT {

  @Chromium
  @DisplayName("an invalid render keeps the rows and the draft, and the next valid render applies")
  void invalidRendersKeepTheView(Session session) {
    session.open("/failures/");
    var draft = session.field("Draft");
    draft.fill("My draft");
    var rows = session.role(AriaRole.LIST, "Rows").getByRole(AriaRole.LISTITEM);
    var expected = new String[] {"Alpha", "Beta", "Gamma"};
    assertThat(rows).hasText(expected);

    session.button("Duplicate a row").click();
    session.awaitError("duplicate-key");
    session.button("Remove duplicate").click();
    session.button("Fail next render").click();
    session.awaitError("Simulated render failure");
    session.button("Reject render with effect").click();
    session.awaitError("audit");
    assertThat(rows).hasText(expected);
    assertThat(draft).hasValue("My draft");

    var renders = renders(session).textContent();
    session.button("Refresh").click();
    assertThat(renders(session)).not().hasText(renders);
    assertThat(rows).hasText(expected);
    assertThat(draft).hasValue("My draft");
    assertThat(session.text("Effects started: 0")).isVisible();
    Assertions.assertThat(session.errors()).hasSize(3);
  }

  @Chromium
  @DisplayName("repeated invalidations before a render coalesce into one render")
  void invalidationsCoalesce(Session session) {
    session.open("/failures/");
    var before = count(renders(session));
    session.button("Refresh five times").click();
    assertThat(renders(session)).hasText("Renders: " + (before + 1));
    session.button("Refresh").click();
    assertThat(renders(session)).hasText("Renders: " + (before + 2));
  }

  @Chromium
  @DisplayName("runaway rendering stops with one report and resumes on the next change")
  void runawayRenderingStops(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/failures/");
    session.button("Start runaway").click();
    session.awaitError("Runaway invalidation");
    var halted = renders(session).textContent();
    page.clock().runFor(1_000);
    assertThat(renders(session)).hasText(halted);

    session.button("Stop runaway").click();
    assertThat(renders(session)).not().hasText(halted);
    Assertions.assertThat(session.errors()).hasSize(1);
  }

  @Chromium
  @DisplayName("a failing handler is reported and the view stays interactive")
  void handlerFailuresAreReported(Session session) {
    session.open("/failures/");
    session.button("Throw from handler").click();
    session.awaitError("Simulated handler failure");
    var before = count(renders(session));
    session.button("Refresh").click();
    assertThat(renders(session)).hasText("Renders: " + (before + 1));
  }

  @Chromium
  @DisplayName("a change that fails part-way still renders what it changed")
  void failedUpdatesStillRender(Session session) {
    session.open("/failures/");
    session.button("Fail during update").click();
    session.awaitError("Simulated update failure");
    assertThat(session.text("Changes before a failure: 1")).isVisible();
  }

  @BothEngines
  @DisplayName("an id the page uses rejects the render even when fallback content uses it first")
  void fallbackIdsDoNotHidePageIds(Session session) {
    var page = session.page();
    // A host page written by hand can repeat an id of its own inside the fallback content.
    page.route(
        session.url("/failures/shadowed/"),
        route -> {
          var response = route.fetch();
          route.fulfill(
              new Route.FulfillOptions()
                  .setResponse(response)
                  .setBody(
                      response
                          .text()
                          .replace(
                              "<p>The claim is loading.</p>",
                              "<p id=\"page-footer\">The claim is loading.</p>")));
        });
    session.open("/failures/shadowed/");
    session.awaitError("cross-root-id");

    assertThat(session.text("The claim is loading.")).isVisible();
    assertThat(session.text("Claimed footer")).hasCount(0);
    assertThat(session.text("A footer owned by the page")).isVisible();
  }

  @Chromium
  @DisplayName("ids used elsewhere on the page reject the render")
  void pageIdCollisionsRejectRenders(Session session) {
    session.open("/failures/");
    var updates = session.page().getByText("Updates:");
    assertThat(updates).hasText("Updates: 0");
    var reported = 0;
    for (var claim : new String[] {"Claim second entry id", "Claim page id", "Claim host id"}) {
      session.button(claim).click();
      session.awaitErrors(++reported, "cross-root-id");
      assertThat(session.text("Claimed")).hasCount(0);
      session.button("Release ids").click();
    }
    session.button("Update").click();
    assertThat(updates).hasText("Updates: 1");
    assertThat(session.text("A note owned by the page")).isVisible();
    Assertions.assertThat(session.errors()).hasSize(reported);
  }

  private static Locator renders(Session session) {
    return session.role(AriaRole.REGION, "Render checks").getByRole(AriaRole.STATUS);
  }

  private static int count(Locator status) {
    var text = status.textContent();
    return Integer.parseInt(text.substring(text.indexOf(':') + 2));
  }
}
