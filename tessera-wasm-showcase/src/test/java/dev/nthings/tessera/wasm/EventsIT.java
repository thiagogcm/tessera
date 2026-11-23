package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;

/// Native events dispatched to Java handlers on the events page.
@DisplayName("Events")
class EventsIT {

  @BothEngines
  @DisplayName("a capture-phase handler can stop a click before it reaches a child")
  void captureHandlersStopClicks(Session session) {
    session.open("/events/");
    session.button("Delete").click();
    session.assertList("Event log", "deleted");

    session.field("Read-only").check();
    session.button("Delete").click();
    session.assertList("Event log", "deleted", "blocked");
  }

  @BothEngines
  @DisplayName("preventing a click's default keeps a checkbox unchecked")
  void preventedDefaultsApply(Session session) {
    session.open("/events/");
    var terms = session.field("Accept terms");
    terms.click();
    session.assertList("Event log", "terms must be read first");
    assertThat(terms).not().isChecked();
  }

  @BothEngines
  @DisplayName("keyboard handlers receive the key and its modifiers")
  void keyboardHandlersReadKeys(Session session) {
    session.open("/events/");
    session.field("Shortcut").press("Shift+A");
    session.assertList("Event log", "Shift+Shift", "Shift+A");
  }

  @BothEngines
  @DisplayName("keyboard handlers can skip the keys of an input method composition")
  void keyboardHandlersSeeCompositions(Session session) {
    session.open("/events/");
    var shortcut = session.field("Shortcut");
    shortcut.dispatchEvent("keydown", Map.of("key", "Enter", "isComposing", true));
    shortcut.press("Enter");
    session.assertList("Event log", "Enter");
  }

  @Chromium
  @DisplayName("a one-shot listener fires once until a render binds it again")
  void oneShotListenersFireOnce(Session session) {
    session.open("/events/");
    session.button("Claim reward").click();
    var claimed = session.button("Reward claimed");
    claimed.click();
    claimed.click();
    session.assertList("Event log", "reward 1");

    session.button("Offer another reward").click();
    session.button("Claim reward").click();
    claimed.click();
    session.assertList("Event log", "reward 1", "reward 2");
  }
}
