package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;

/// Input method composition in bound text controls and keyed rows. The profile form accepts text
/// only once a composition ends, as `ControlState.composing()` advises.
@DisplayName("IME composition")
class ImeIT {

  @BothEngines
  @DisplayName("provisional text survives a render and the committed text reaches Java")
  void committedTextReachesJava(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/forms/");
    var preview = session.role(AriaRole.STATUS, "Preview");
    var text = session.field("Controlled text");
    text.fill("");
    assertThat(preview).containsText("Text: ;");
    var ime = new Ime(session);

    ime.compose(text, "かな");
    assertThat(text).hasValue("かな");
    page.clock().runFor(60_000);
    assertThat(session.text("Autosaved 1 times")).isVisible();
    assertThat(text).hasValue("かな");
    assertThat(text).isFocused();
    assertThat(preview).containsText("Text: ;");

    ime.commit(text, "確定");
    assertThat(preview).containsText("Text: 確定;");
    assertThat(text).hasValue("確定");
    page.clock().runFor(60_000);
    assertThat(session.text("Autosaved 2 times")).isVisible();
    assertThat(text).hasValue("確定");
  }

  @BothEngines
  @DisplayName("a locked form keeps provisional text and reverts the committed text")
  void rejectedCompositionReverts(Session session) {
    session.open("/forms/");
    var text = session.field("Controlled text");
    assertThat(text).hasValue("Initial text");
    session.field("Lock form").check();
    text.click();
    session.page().keyboard().press("End");
    var ime = new Ime(session);

    ime.compose(text, "かな");
    assertThat(text).hasValue("Initial textかな");
    ime.commit(text, "確定");
    assertThat(text).hasValue("Initial text");
  }

  @Chromium
  @DisplayName("a composition in a keyed row survives the live feed inserting a row above it")
  void compositionSurvivesKeyedMoves(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/lists/");
    session.field("Live feed").check();
    var entry = session.field("Entry b");
    entry.fill("Draft ");
    var ime = new Ime(session);

    ime.compose(entry, "かな");
    page.clock().runFor(60_000);
    assertThat(session.field("Entry item-4")).isVisible();
    assertThat(entry).isFocused();
    assertThat(entry).hasValue("Draft かな");
    ime.commit(entry, "確定");
    assertThat(entry).hasValue("Draft 確定");
    assertThat(entry).isFocused();
  }
}
