package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;

/// Live tabs on the tabs page: browser-owned mailboxes and app-bound settings.
@DisplayName("Tabs")
class TabsIT {

  @BothEngines
  @DisplayName("arrow, Home and End keys select tabs, and selection and focus survive tab changes")
  void automaticTabsFollowTheKeyboard(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/tabs/");
    var mailboxes = session.role(AriaRole.TABLIST, "Mailboxes");
    var drafts = tab(mailboxes, "Drafts");
    var sent = tab(mailboxes, "Sent");
    assertSelected(drafts);
    drafts.focus();

    page.keyboard().press("ArrowRight");
    assertThat(sent).isFocused();
    assertSelected(sent);
    assertThat(session.text("Sent messages")).isVisible();
    page.keyboard().press("Home");
    assertThat(tab(mailboxes, "Inbox")).isFocused();
    page.keyboard().press("End");
    assertThat(sent).isFocused();
    page.keyboard().press("ArrowLeft");
    assertThat(drafts).isFocused();
    assertSelected(drafts);

    // The sync archives the inbox and adds a spam folder while the user stays on Drafts.
    page.clock().runFor(60_000);
    assertThat(tab(mailboxes, "Spam")).isVisible();
    assertThat(tab(mailboxes, "Inbox")).hasCount(0);
    assertThat(drafts).isFocused();
    assertSelected(drafts);

    page.keyboard().press("Delete");
    assertThat(drafts).hasCount(0);
    assertThat(sent).isFocused();
    assertSelected(sent);
    assertThat(session.text("Sent messages")).isVisible();
  }

  @BothEngines
  @DisplayName("bound manual tabs switch only when the application accepts the request")
  void boundTabsWaitForTheApplication(Session session) {
    var page = session.page();
    session.open("/tabs/");
    var settings = session.role(AriaRole.TABLIST, "Settings");
    var account = tab(settings, "Account");
    var billing = tab(settings, "Billing");
    assertSelected(account);

    account.focus();
    page.keyboard().press("ArrowRight");
    assertThat(billing).isFocused();
    assertSelected(account);
    page.keyboard().press("Enter");
    assertThat(session.text("Requested: billing")).isVisible();
    assertSelected(account);
    assertThat(billing).isFocused();

    session.field("Allow switching").check();
    billing.focus();
    page.keyboard().press("Space");
    assertSelected(billing);
    assertThat(session.text("Billing settings")).isVisible();

    session.button("Restore a removed section").click();
    session.awaitError("tab-selection");
    assertSelected(billing);
    session.button("Reset settings").click();
    assertSelected(account);
  }

  private static Locator tab(Locator tablist, String name) {
    return tablist.getByRole(
        AriaRole.TAB, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private static void assertSelected(Locator tab) {
    assertThat(tab).hasAttribute("aria-selected", "true");
  }
}
