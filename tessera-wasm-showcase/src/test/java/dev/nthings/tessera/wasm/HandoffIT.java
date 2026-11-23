package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// The first commit replacing fallback content on the handoff page while a reader uses it: the Web
/// Image is held back until the reader has focused or scrolled the fallback.
@DisplayName("Fallback handoff")
class HandoffIT {

  @BothEngines
  @DisplayName("focus on a fallback field moves to the live field with the same id")
  void focusMovesToTheLiveField(Session session) {
    var images = hold(session);
    session.open("/handoff/");
    var note = session.field("Reader note");
    note.click();
    assertThat(note).isFocused();

    release(session, images);
    assertThat(session.role(AriaRole.COMPLEMENTARY, "Reading tools")).isAttached();
    assertThat(session.text("Loading the reading tools.")).hasCount(0);
    assertThat(note).isFocused();
  }

  @BothEngines
  @DisplayName("a reader scrolled into the fallback keeps their chapter at the same height")
  void readingPositionSurvives(Session session) {
    var images = hold(session);
    session.open("/handoff/");
    var chapter = session.role(AriaRole.HEADING, "Chapter 5");
    chapter.evaluate(
        "heading => window.scrollTo({top: window.scrollY"
            + " + heading.getBoundingClientRect().top - 100, behavior: 'instant'})");
    Assertions.assertThat(top(chapter)).isCloseTo(100, within(1.0));

    release(session, images);
    assertThat(session.role(AriaRole.COMPLEMENTARY, "Reading tools")).isAttached();
    Assertions.assertThat(top(chapter)).isCloseTo(100, within(2.0));
  }

  /// Holds every Web Image request until [#release].
  private static List<Route> hold(Session session) {
    var held = new ArrayList<Route>();
    session.page().route("**/*.wasm", held::add);
    return held;
  }

  private static void release(Session session, List<Route> held) {
    var page = session.page();
    page.waitForCondition(() -> !held.isEmpty());
    page.unrouteAll();
    held.forEach(Route::resume);
  }

  private static double top(Locator element) {
    return ((Number) element.evaluate("element => element.getBoundingClientRect().top"))
        .doubleValue();
  }
}
