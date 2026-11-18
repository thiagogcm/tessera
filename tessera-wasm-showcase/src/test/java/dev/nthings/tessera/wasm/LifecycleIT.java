package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// Effects, requests and roots that close, on the lifecycle page. The activity panel, a separate
/// root, lists what the panel and the session started and cleaned up.
@DisplayName("Lifecycle")
class LifecycleIT {

  @Chromium
  @DisplayName("an effect starts after commit, restarts when its key changes and stops on removal")
  void effectsFollowTheirPlacement(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/lifecycle/");
    session.button("Start auto-refresh").click();
    session.assertList("Activity", "auto-refresh 0 started");
    page.clock().runFor(3_000);
    assertThat(refreshed(session)).hasText("Refreshed 3 times");

    session.button("Restart auto-refresh").click();
    session.assertList(
        "Activity", "auto-refresh 0 started", "auto-refresh 0 stopped", "auto-refresh 1 started");
    session.button("Stop auto-refresh").click();
    session.assertList(
        "Activity",
        "auto-refresh 0 started",
        "auto-refresh 0 stopped",
        "auto-refresh 1 started",
        "auto-refresh 1 stopped");
    var stopped = refreshed(session).textContent();
    page.clock().runFor(5_000);
    assertThat(refreshed(session)).hasText(stopped);
  }

  @BothEngines
  @DisplayName("closing a panel stops its effects and aborts its request without a callback")
  void closingStopsWorkAndAbortsRequests(Session session) {
    var page = session.page();
    page.clock().install();
    var aborted = abortedRequests(page);
    session.open("/lifecycle/");
    session.button("Start auto-refresh").click();
    session.button("Load report").click();
    session.assertList("Activity", "auto-refresh 0 started", "report requested");

    session.button("Close").click();
    assertThat(panel(session)).hasCount(0);
    session.assertList(
        "Activity",
        "auto-refresh 0 started",
        "report requested",
        "auto-refresh 0 stopped",
        "panel closed");
    page.waitForCondition(() -> !aborted.isEmpty());
    page.clock().runFor(5_000);
    session.assertList(
        "Activity",
        "auto-refresh 0 started",
        "report requested",
        "auto-refresh 0 stopped",
        "panel closed");
  }

  @BothEngines
  @DisplayName("closing from inside a modal removes it, leaves the page interactive and can reopen")
  void closingFromAModal(Session session) {
    session.open("/lifecycle/");
    session.button("Open settings").click();
    var settings = session.role(AriaRole.DIALOG, "Panel settings");
    assertThat(settings).isVisible();

    session.button("Close panel").click();
    assertThat(panel(session)).hasCount(0);
    assertThat(settings).hasCount(0);
    session.button("Reopen panel").click();
    session.button("Start auto-refresh").click();
    session.assertList("Activity", "panel closed", "auto-refresh 0 started");
  }

  @Chromium
  @DisplayName("mounting a host that already holds a root is reported and leaves the root working")
  void mountingAnOccupiedHostIsReported(Session session) {
    session.open("/lifecycle/");
    assertThat(panel(session)).isVisible();
    session.button("Open panel again").click();
    session.awaitError("lifetime");
    session.button("Start auto-refresh").click();
    session.assertList("Activity", "auto-refresh 0 started");
    Assertions.assertThat(session.errors()).hasSize(1);
  }

  @Chromium
  @DisplayName("removing an effect aborts its request without a callback")
  void removingAnEffectAbortsItsRequest(Session session) {
    var page = session.page();
    var aborted = abortedRequests(page);
    session.open("/lifecycle/");
    page.waitForRequest("**/slow", () -> session.button("Load preview").click());
    session.button("Cancel preview").click();
    page.waitForCondition(() -> !aborted.isEmpty());
    session.button("Start auto-refresh").click();
    session.assertList("Activity", "auto-refresh 0 started");
  }

  @Chromium
  @DisplayName("a root that closes itself from a cleanup, an effect start or an after-render task")
  void rootsCloseThemselves(Session session) {
    record Case(String button, List<String> activity) {}
    var cases =
        List.of(
            new Case("Close from cleanup", List.of("guard cleaned up", "session closed")),
            new Case(
                "Close from effect start",
                List.of(
                    "closing effect started",
                    "closing effect cleaned up",
                    "guard cleaned up",
                    "session closed")),
            new Case(
                "Close after render",
                List.of("first after-render task ran", "guard cleaned up", "session closed")));
    for (var closing : cases) {
      session.open("/lifecycle/");
      session.button(closing.button()).click();
      assertThat(session.role(AriaRole.REGION, "Session")).hasCount(0);
      var activity = session.role(AriaRole.LIST, "Activity").getByRole(AriaRole.LISTITEM);
      assertThat(activity).hasCount(closing.activity().size());
      Assertions.assertThat(activity.allTextContents())
          .as(closing.button())
          .containsExactlyInAnyOrderElementsOf(closing.activity());
      // Other roots keep working.
      session.button("Start auto-refresh").click();
      assertThat(activity).hasCount(closing.activity().size() + 1);
    }
  }

  @Chromium
  @DisplayName("failing effect starts and cleanups are reported and the root keeps working")
  void effectFailuresAreReported(Session session) {
    session.open("/lifecycle/");
    session.button("Fail effect start").click();
    session.awaitError("Simulated effect start failure");

    session.button("Add failing cleanups").click();
    session.button("Remove failing cleanups").click();
    session.awaitError("Simulated cleanup failure");
    var activity = session.role(AriaRole.LIST, "Activity").getByRole(AriaRole.LISTITEM);
    assertThat(activity).hasCount(2);
    Assertions.assertThat(activity.allTextContents())
        .containsExactlyInAnyOrder("victim cleaned up", "survivor cleaned up");

    session.button("Sign out").click();
    assertThat(session.role(AriaRole.REGION, "Session")).hasCount(0);
    assertThat(activity).hasCount(5);
    Assertions.assertThat(activity.allTextContents())
        .containsExactlyInAnyOrder(
            "victim cleaned up",
            "survivor cleaned up",
            "failing effect cleaned up",
            "guard cleaned up",
            "session closed");
    Assertions.assertThat(session.errors()).hasSize(2);
  }

  private static Locator panel(Session session) {
    return session.role(AriaRole.REGION, "Panel");
  }

  private static Locator refreshed(Session session) {
    return panel(session).getByRole(AriaRole.STATUS);
  }

  /// Failures of requests to `/slow`, recorded as the page aborts them.
  private static List<String> abortedRequests(Page page) {
    var aborted = new ArrayList<String>();
    page.onRequestFailed(request -> {
      if (request.url().endsWith("/slow")) {
        aborted.add(request.url());
      }
    });
    return aborted;
  }
}
