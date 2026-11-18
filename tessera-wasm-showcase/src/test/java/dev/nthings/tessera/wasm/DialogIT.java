package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;

/// Live dialogs and drawers on the dialogs page: dismissal, focus return and authored state.
@DisplayName("Dialogs")
class DialogIT {

  @BothEngines
  @DisplayName("a bound modal refuses dismissal while the application keeps it open")
  void boundModalRefusesDismissal(Session session) {
    var page = session.page();
    session.open("/dialogs/");
    var editor = dialog(session, "Edit entry");
    var invoker = session.button("Edit entry");
    session.field("Unsaved changes").check();
    invoker.click();
    assertThat(editor).isVisible();

    page.keyboard().press("Escape");
    assertThat(session.text("Dismiss attempts: 1")).isVisible();
    assertThat(editor).isVisible();
    button(editor, "Close").click();
    assertThat(session.text("Dismiss attempts: 2")).isVisible();
    assertThat(editor).isVisible();

    page.keyboard().press("Control+s");
    assertThat(session.field("Unsaved changes")).not().isChecked();
    page.keyboard().press("Escape");
    assertThat(editor).isHidden();
    assertThat(invoker).isFocused();
  }

  @BothEngines
  @DisplayName("removing a modal's invoker while it is open sends focus to the fallback")
  void removedInvokerUsesTheFallback(Session session) {
    var page = session.page();
    session.open("/dialogs/");
    session.button("Edit entry").click();
    assertThat(dialog(session, "Edit entry")).isVisible();

    page.keyboard().press("Control+Delete");
    assertThat(session.button("Edit entry")).hasCount(0);
    page.keyboard().press("Escape");
    assertThat(dialog(session, "Edit entry")).isHidden();
    assertThat(entries(session)).isFocused();
  }

  @BothEngines
  @DisplayName("removing an open modal releases the page and moves focus to the fallback")
  void removedModalReleasesThePage(Session session) {
    var page = session.page();
    session.open("/dialogs/");
    session.button("Edit entry").click();
    assertThat(dialog(session, "Edit entry")).isVisible();

    page.keyboard().press("Control+d");
    assertThat(dialog(session, "Edit entry")).hasCount(0);
    assertThat(entries(session)).isFocused();
    session.field("Unsaved changes").check();
    assertThat(session.field("Unsaved changes")).isChecked();
  }

  @BothEngines
  @DisplayName("a modal a timer opens returns focus to the fallback when nothing had focus")
  void timerOpenedModalUsesTheFallback(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/dialogs/");
    // Dismissing the welcome tour leaves nothing focused.
    button(dialog(session, "Welcome tour"), "Close").press("Enter");
    assertThat(dialog(session, "Welcome tour")).isHidden();

    page.clock().fastForward(3_600_000);
    var warning = dialog(session, "Session expiring");
    assertThat(warning).isVisible();
    page.keyboard().press("Escape");
    assertThat(warning).isHidden();
    assertThat(entries(session)).isFocused();
  }

  @BothEngines
  @DisplayName("a modal whose opener is disabled when it closes returns focus to the fallback")
  void disabledOpenerUsesTheFallback(Session session) {
    session.open("/dialogs/");
    session.button("Save").click();
    var confirmation = dialog(session, "Save changes?");
    assertThat(confirmation).isVisible();

    button(confirmation, "Confirm").click();
    assertThat(confirmation).isHidden();
    assertThat(session.button("Saving")).isDisabled();
    assertThat(entries(session)).isFocused();
  }

  @BothEngines
  @DisplayName("a drawer shown through the host stays open and non-modal across updates")
  void hostShownDrawerSurvivesUpdates(Session session) {
    session.open("/dialogs/");
    var show = session.button("Show filters");
    show.click();
    var filters = dialog(session, "Filters");
    assertThat(filters).isVisible();

    // The rest of the page stays reachable by keyboard (a click outside would light-dismiss the
    // drawer), and the renders it causes keep the drawer open.
    toggleUnsaved(session);
    toggleUnsaved(session);
    assertThat(filters).isVisible();

    button(filters, "Close").click();
    assertThat(filters).isHidden();
    assertThat(show).isFocused();
  }

  @BothEngines
  @DisplayName("a dialog authored open shows on load and stays closed once dismissed")
  void authoredStateApplies(Session session) {
    session.open("/dialogs/");
    var tour = dialog(session, "Welcome tour");
    var help = dialog(session, "Help");
    assertThat(tour).isVisible();
    assertThat(help).isHidden();
    // Opened non-modally: the page stays reachable while the tour shows.
    toggleUnsaved(session);
    assertThat(tour).isVisible();

    button(tour, "Close").click();
    assertThat(tour).isHidden();
    toggleUnsaved(session);
    assertThat(tour).isHidden();

    session.button("Open help").click();
    assertThat(help).isVisible();
    session.page().keyboard().press("Escape");
    assertThat(help).isHidden();
  }

  /// Toggles the unsaved changes checkbox from the keyboard, which renders the page.
  private static void toggleUnsaved(Session session) {
    var unsaved = session.field("Unsaved changes");
    var checked = unsaved.isChecked();
    unsaved.focus();
    session.page().keyboard().press("Space");
    if (checked) {
      assertThat(unsaved).not().isChecked();
    } else {
      assertThat(unsaved).isChecked();
    }
  }

  private static Locator dialog(Session session, String name) {
    return session
        .page()
        .getByRole(
            AriaRole.DIALOG,
            new Page.GetByRoleOptions().setName(name).setExact(true).setIncludeHidden(true));
  }

  private static Locator button(Locator dialog, String name) {
    return dialog.getByRole(
        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private static Locator entries(Session session) {
    return session.role(AriaRole.HEADING, "Entries");
  }
}
