package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;

/// Several roots from two entries on the home page.
@DisplayName("Multiple roots")
class MultiRootIT {

  @BothEngines
  @DisplayName("roots of two entries stay independent and unmounting empties only its own host")
  void rootsStayIndependent(Session session) {
    session.open("/");
    var name = session.field("Name");
    var notes = session.field("Notes");
    name.fill("Ada");
    notes.fill("Independent notes");
    session.button("Second increment").click();
    session.button("Increment").click();
    assertThat(session.page().getByRole(AriaRole.STATUS)).hasText("Count: 1");
    assertThat(session.text("Second count: 1")).isVisible();
    assertThat(name).hasValue("Ada");
    assertThat(notes).hasValue("Independent notes");

    session.button("Unmount").click();
    assertThat(session.role(AriaRole.HEADING, "Java in the browser")).hasCount(0);
    assertThat(session.role(AriaRole.HEADING, "Published Web Images")).isVisible();
    assertThat(notes).hasValue("Independent notes");
    session.button("Second increment").click();
    assertThat(session.text("Second count: 2")).isVisible();
  }
}
