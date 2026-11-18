package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ColorScheme;
import com.microsoft.playwright.options.ReducedMotion;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Forms")
class FormsIT {

  private static final String EMAIL_ERROR = "Enter a valid email address, e.g. ada@example.com.";

  @BrowserTest
  @DisplayName("a field explains an invalid value only after it is edited, and clears once fixed")
  void validationFollowsEditing(Session session) {
    var page = session.open("/forms/");
    var email = email(page);
    var error = session.text(EMAIL_ERROR);

    session.text("Work email").click();
    assertThat(email).isFocused();
    assertThat(error).isHidden();
    email.fill("invalid");
    email.press("Tab");

    assertThat(error).isVisible();
    assertThat(email).hasAccessibleErrorMessage(EMAIL_ERROR);
    assertThat(email).hasAccessibleDescription("We send build notifications here.");

    email.fill("ada@example.com");
    email.press("Tab");
    assertThat(error).isHidden();
  }

  @BrowserTest
  @DisplayName("in dark mode, an invalid field stays readable")
  void invalidFieldReadableInDarkMode(Session session) {
    session
        .page()
        .emulateMedia(
            new Page.EmulateMediaOptions()
                .setColorScheme(ColorScheme.DARK)
                .setReducedMotion(ReducedMotion.REDUCE));
    var page = session.open("/forms/");
    var email = email(page);
    email.fill("invalid");
    email.press("Tab");
    assertThat(session.text(EMAIL_ERROR)).isVisible();

    assertThat(Session.contrast(email)).as("text against the field").isGreaterThanOrEqualTo(4.5);
  }

  @BrowserTest
  @DisplayName("a switch toggles by click, label and Space")
  void switchToggles(Session session) {
    var page = session.open("/forms/");
    var digest = session.role(AriaRole.SWITCH, "Weekly digest");
    assertThat(digest).not().isChecked();

    digest.click();
    assertThat(digest).isChecked();
    session.text("Weekly digest").click();
    assertThat(digest).not().isChecked();
    digest.focus();
    page.keyboard().press("Space");
    assertThat(digest).isChecked();
  }

  @BrowserTest
  @DisplayName("choosing a deployment target from the keyboard shows the choice")
  void selectFromKeyboard(Session session) {
    var page = session.open("/forms/");
    var select = page.getByLabel("Deployment target", new Page.GetByLabelOptions().setExact(true));
    assertThat(select).hasValue("cloud-run");

    select.focus();
    var option =
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Kubernetes Engine"));
    if (session.engine() == Engine.CHROMIUM) {
      // The customizable select opens its picker; arrows move within it and Enter picks.
      page.keyboard().press("Enter");
      assertThat(option).isVisible();
      var box = Session.box(option);
      assertThat(box.y + box.height)
          .as("picker inside the window")
          .isLessThanOrEqualTo(Session.HEIGHT);
      page.keyboard().press("ArrowDown");
      page.keyboard().press("Enter");
      assertThat(option).isHidden();
      assertThat(select.locator("selectedcontent")).hasText("Kubernetes Engine");
    } else {
      page.keyboard().press("ArrowDown");
    }

    assertThat(select).hasValue("gke");
    assertThat(select).hasAccessibleName("Deployment target");
  }

  @BrowserTest
  @DisplayName("the release notes field grows as lines are typed")
  void textareaGrows(Session session) {
    var page = session.open("/forms/");
    var notes = page.getByLabel("Release notes", new Page.GetByLabelOptions().setExact(true));
    var empty = Session.box(notes).height;

    notes.click();
    page.keyboard().type("First line\nSecond line\nThird line\nFourth line\nFifth line");

    assertThat(Session.box(notes).height).isGreaterThan(empty);
    assertThat(Session.number(notes.evaluate("el => el.scrollHeight - el.clientHeight")))
        .as("no hidden lines")
        .isLessThanOrEqualTo(1);
  }

  @BrowserTest
  @DisplayName("clicking a plan card selects its radio, and arrows move the choice")
  void choiceCards(Session session) {
    var page = session.open("/layout/");
    var team =
        page.getByRole(
            AriaRole.RADIO, new Page.GetByRoleOptions().setName(Pattern.compile("^Team plan")));
    var organization =
        page.getByRole(
            AriaRole.RADIO,
            new Page.GetByRoleOptions().setName(Pattern.compile("^Organization plan")));
    assertThat(team).isChecked();

    session.text("Only one plan can be selected.").click();
    assertThat(organization).isChecked();
    assertThat(team).not().isChecked();

    page.keyboard().press("ArrowLeft");
    assertThat(team).isChecked();
    assertThat(team).isFocused();
  }

  @BrowserTest
  @DisplayName("a field tooltip appears just above its control")
  void fieldTooltipAboveControl(Session session) {
    var page = session.open("/forms/");
    var name = page.getByLabel("Full name", new Page.GetByLabelOptions().setExact(true));
    var tooltip =
        page.getByRole(AriaRole.TOOLTIP)
            .filter(new Locator.FilterOptions().setHasText("First and last name"));
    assertThat(tooltip).isHidden();

    name.hover();

    assertThat(tooltip).isVisible();
    var t = Session.box(tooltip);
    var f = Session.box(name);
    assertThat(t.y + t.height)
        .as("tooltip bottom against the control top")
        .isBetween(f.y - 8, f.y + 1);
    assertThat(t.x).isLessThan(f.x + f.width);
  }

  private static Locator email(Page page) {
    return page.getByLabel("Work email", new Page.GetByLabelOptions().setExact(true));
  }
}
