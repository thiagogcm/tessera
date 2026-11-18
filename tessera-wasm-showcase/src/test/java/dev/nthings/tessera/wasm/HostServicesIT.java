package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// Browser services a root reaches through element references, on the services page.
@DisplayName("Host services")
class HostServicesIT {

  @BothEngines
  @DisplayName("text requests deliver their body and report HTTP failures by status")
  void textRequestsComplete(Session session) {
    session.open("/services/");
    var status = status(session);
    session.button("Load greeting").click();
    assertThat(status).hasText("Loaded: hello from tessera");
    session.expectError("404");
    session.button("Load missing page").click();
    assertThat(status).hasText("Failed with HTTP 404");
  }

  @BothEngines
  @DisplayName("a text selection made through a reference survives updates")
  void selectionsSurviveUpdates(Session session) {
    session.open("/services/");
    session.button("Select first word").click();
    session.button("Re-render services").click();
    session.button("Read selection").click();
    assertThat(status(session)).hasText("selection 0-5");
  }

  @BothEngines
  @DisplayName("a modal opened through the host stays open across updates and returns focus")
  void hostModalsReturnFocus(Session session) {
    session.open("/services/");
    var opener = session.button("Open services dialog");
    opener.click();
    var dialog = dialog(session, "Services dialog");
    assertThat(dialog).isVisible();
    var renders = dialog.getByText("Renders:").textContent();
    session.button("Re-render inside dialog").click();
    assertThat(dialog.getByText("Renders:")).not().hasText(renders);
    assertThat(dialog).isVisible();

    session.button("Close services dialog").click();
    assertThat(dialog).isHidden();
    assertThat(opener).isFocused();
  }

  @BothEngines
  @DisplayName("focus returns to the opener a render recreated while the modal was open")
  void focusReturnsToARecreatedOpener(Session session) {
    session.open("/services/");
    session.button("Open services dialog").click();
    session.button("Close and replace opener").click();
    assertThat(dialog(session, "Services dialog")).isHidden();
    assertThat(session.button("Open services dialog")).isFocused();
  }

  @BothEngines
  @DisplayName("an open popover and playing media survive updates and stop when the root closes")
  void popoverAndMediaFollowTheRoot(Session session) {
    var page = session.page();
    session.open("/services/");
    var status = status(session);
    session.button("Toggle menu").click();
    var menu = page.getByText("Menu", new Page.GetByTextOptions().setExact(true));
    assertThat(menu).isVisible();
    session.button("Play audio").click();
    assertThat(status).hasText("playing");
    page.waitForFunction("document.querySelector('audio').currentTime > 0");

    session.button("Re-render services").click();
    assertThat(menu).isVisible();
    session.button("Read media").click();
    assertThat(status).hasText("media paused=false advanced=true");

    var audio = page.locator("audio").elementHandle();
    session.button("Close services root").click();
    assertThat(session.button("Toggle menu")).hasCount(0);
    assertThat(menu).hasCount(0);
    page.waitForCondition(() -> Boolean.TRUE.equals(audio.evaluate("audio => audio.paused")));
  }

  @BothEngines
  @DisplayName("a timer opens a modal through a reference the application keeps")
  void timersOpenModalsThroughOwnedReferences(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/services/");
    session.button("Remind me in 5 seconds").click();
    page.clock().runFor(5_000);
    var reminder = dialog(session, "Confirm later");
    assertThat(reminder).isVisible();
    page.keyboard().press("Escape");
    assertThat(reminder).isHidden();

    // A reference from an earlier render no longer resolves: reported, and nothing opens.
    session.button("Remind me through a stale reference").click();
    page.clock().runFor(5_000);
    session.awaitError("not bound");
    assertThat(dialog(session, "Stale later")).isHidden();
    Assertions.assertThat(session.errors()).hasSize(1);
  }

  private static Locator status(Session session) {
    return session.page().getByRole(AriaRole.STATUS);
  }

  private static Locator dialog(Session session, String name) {
    return session
        .page()
        .getByRole(
            AriaRole.DIALOG,
            new Page.GetByRoleOptions().setName(name).setExact(true).setIncludeHidden(true));
  }
}
