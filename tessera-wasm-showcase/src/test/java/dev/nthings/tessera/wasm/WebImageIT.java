package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.google.gson.JsonParser;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.example.ControlledFormDemo;
import dev.nthings.tessera.wasm.example.KeyedListDemo;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Web Image")
class WebImageIT {
  private static final Map<String, Browser> BROWSERS = new HashMap<>();
  private static HttpServer server;

  /// Never released: `/slow` responds only after its timeout, long after a test aborts it.
  private static final CountDownLatch SLOW = new CountDownLatch(1);

  private static Playwright playwright;

  @BothEngines
  @DisplayName("shared Java components render and handle native clicks")
  void sharedJavaComponentsRenderAndHandleNativeClicks(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector("#app main", new Page.WaitForSelectorOptions().setTimeout(30_000));

          var name = label(page, "Name");
          assertThat(name).isVisible();
          assertThat(label(page, "Notes")).isVisible();
          var notesId = label(page, "Notes").getAttribute("id");
          Assertions.assertThat(notesId).isNotEqualTo(name.getAttribute("id"));
          assertThat(name).hasCSS("border-top-style", "solid");
          name.fill("Ada");
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 0");
          assertThat(page.getByRole(AriaRole.STATUS)).hasCSS("font-weight", "600");
          assertThat(page.locator("#app input[aria-describedby]")).hasCount(1);
          var increment = button(page, "Increment");
          assertThat(increment).hasCSS("cursor", "pointer");

          var requests = new ArrayList<String>();
          page.onRequest(request -> requests.add(request.url()));
          increment.click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 1");
          assertThat(name).hasValue("Ada");
          assertThat(page.getByRole(AriaRole.STATUS)).hasCSS("font-weight", "400");
          assertThat(page.locator("#app input[aria-describedby]")).hasCount(0);
          assertThat(page.locator("#app input[aria-errormessage]")).hasCount(1);
          increment.click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 2");
          assertThat(name).hasValue("Ada");
          assertThat(page.getByRole(AriaRole.STATUS)).hasCSS("font-weight", "600");
          assertThat(page.locator("#app input[aria-describedby]")).hasCount(1);
          assertThat(page.locator("#app input[aria-errormessage]")).hasCount(0);
          Assertions.assertThat(requests).isEmpty();

          page.getByRole(
                  AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Unmount").setExact(true))
              .click();
          assertThat(page.locator("#app")).isEmpty();
          assertThat(label(page, "Notes")).isVisible();
          assertThat(label(page, "Notes")).hasAttribute("id", notesId);
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("unsupported Wasm features retain fallback without loading the launcher")
  void unsupportedWasmFeaturesRetainFallbackWithoutLoadingTheLauncher(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.addInitScript("WebAssembly.validate = () => false");
          var requests = new ArrayList<String>();
          page.onRequest(request -> requests.add(request.url()));
          page.navigate(url);

          assertFallback(page, "WebAssembly GC", "Update to a current Chromium or Firefox");
          Assertions.assertThat(requests).noneMatch(request -> request.endsWith("tessera-demo.js"));
        });
  }

  @BothEngines
  @DisplayName("a missing launcher retains fallback and identifies the missing asset")
  void missingLauncherRetainsFallbackAndIdentifiesTheMissingAsset(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.route(
              "**/tessera-demo.js",
              route -> route.fulfill(new Route.FulfillOptions().setStatus(404)));
          page.navigate(url);

          assertFallback(page, "Unable to load tessera-demo.js", "deployed and reachable");
        });
  }

  @BothEngines
  @DisplayName("a missing image retains fallback and identifies the runtime load failure")
  void missingImageRetainsFallbackAndIdentifiesTheRuntimeLoadFailure(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.route(
              "**/tessera-demo.js.wasm",
              route -> route.fulfill(new Route.FulfillOptions().setStatus(404)));
          page.navigate(url);

          assertFallback(page, "runtime/image initialization failed", "tessera-demo.js.wasm");
        });
  }

  private static void assertFallback(Page page, String reason, String action) {
    assertThat(page.getByRole(AriaRole.ALERT)).containsText(reason);
    assertThat(page.getByRole(AriaRole.ALERT)).containsText(action);
    assertThat(page.locator("#app")).hasText("The Java browser application has not started.");
    assertThat(page.locator("#app button")).hasCount(0);
  }

  @BothEngines
  @DisplayName(
      "keyed fragment rows retain nodes and input state through insertion reordering and removal")
  void keyedFragmentRowsRetainNodesAndInputStateThroughInsertionReorderingAndRemoval(
      String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          var beta = page.getByLabel("Entry b");
          assertThat(beta).isVisible();
          beta.fill("Edited Beta");
          beta.evaluate(
              "input => { input.setSelectionRange(2, 7); window.beta = input; window.betaId = input.id; window.betaHint = input.getAttribute('aria-describedby'); window.betaLabel = document.querySelector(`label[for='${input.id}']`); window.betaButton = Array.from(document.querySelectorAll('button')).find(node => node.textContent === 'Choose b'); }");

          button(page, "Prepend entry").dispatchEvent("click");
          assertOrder(page, List.of("item-4", "a", "b", "c"));
          button(page, "Reverse entries").dispatchEvent("click");
          assertOrder(page, List.of("c", "b", "a", "item-4"));
          button(page, "Toggle details").dispatchEvent("click");
          assertThat(
                  page.getByText("Additional details", new Page.GetByTextOptions().setExact(true)))
              .isVisible();
          Assertions.assertThat(
                  page.evaluate(
                      "beta === document.querySelector('[data-entry=b]') && betaLabel === document.querySelector(`label[for='${beta.id}']`) && betaButton === Array.from(document.querySelectorAll('button')).find(node => node.textContent === 'Choose b') && beta.id === betaId && beta.getAttribute('aria-describedby') === betaHint"))
              .isEqualTo(true);
          assertThat(beta).hasValue("Edited Beta");
          assertThat(beta).isFocused();
          Assertions.assertThat(
                  beta.evaluate("input => [input.selectionStart, input.selectionEnd]"))
              .isEqualTo(List.of(2, 7));
          Assertions.assertThat(
                  page.locator("[data-entries]")
                      .evaluate(
                          "section => Array.from(section.children).map(node => node.tagName)"))
              .isEqualTo(
                  List.of(
                      "DIV", "ASIDE", "LABEL", "INPUT", "SPAN", "BUTTON", "LABEL", "INPUT", "SPAN",
                      "BUTTON", "LABEL", "INPUT", "SPAN", "BUTTON", "LABEL", "INPUT", "SPAN",
                      "BUTTON", "P", "P"));

          button(page, "Remove first entry").click();
          assertOrder(page, List.of("b", "a", "item-4"));
          button(page, "Remove first entry").click();
          assertOrder(page, List.of("a", "item-4"));
          Assertions.assertThat(
                  page.evaluate(
                      "!beta.isConnected && !betaLabel.isConnected && !betaButton.isConnected"))
              .isEqualTo(true);
          clickDetachedBeta(page);
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 0; choices: 0; last: none");
        });
  }

  @BothEngines
  @DisplayName("prepending a keyed row inserts it in place without moving the retained rows")
  void prependingAKeyedRowInsertsItInPlaceWithoutMovingTheRetainedRows(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          assertOrder(page, List.of("a", "b", "c"));
          page.locator("[data-entries]")
              .evaluate(
                  "section => { window.detachedRows = 0; new MutationObserver(records => { for (const record of records) window.detachedRows += record.removedNodes.length; }).observe(section, {childList: true}); }");

          button(page, "Prepend entry").dispatchEvent("click");
          assertOrder(page, List.of("item-4", "a", "b", "c"));
          Assertions.assertThat(page.evaluate("window.detachedRows")).isEqualTo(0);

          button(page, "Reverse entries").dispatchEvent("click");
          assertOrder(page, List.of("c", "b", "a", "item-4"));
          Assertions.assertThat(page.evaluate("window.detachedRows")).isNotEqualTo(0);
        });
  }

  @BothEngines
  @DisplayName(
      "retained elements use current callbacks and incompatible component types are replaced")
  void retainedElementsUseCurrentCallbacksAndIncompatibleComponentTypesAreReplaced(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          assertThat(page.getByLabel("Entry b")).isVisible();
          page.getByLabel("Entry b").fill("Keep while compatible");
          page.evaluate(
              "window.alpha = document.querySelector('[data-entry=a]'); window.beta = document.querySelector('[data-entry=b]'); window.betaButton = Array.from(document.querySelectorAll('button')).find(node => node.textContent === 'Choose b');");
          button(page, "Refresh entries").click();
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 1; choices: 0; last: none");
          button(page, "Choose b").click();
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 1; choices: 1; last: b@1");
          button(page, "Refresh entries").click();
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 2; choices: 1; last: b@1");
          button(page, "Choose b").click();
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 2; choices: 2; last: b@2");
          Assertions.assertThat(
                  page.evaluate(
                      "beta === document.querySelector('[data-entry=b]') && betaButton === Array.from(document.querySelectorAll('button')).find(node => node.textContent === 'Choose b')"))
              .isEqualTo(true);
          assertThat(page.getByLabel("Entry b")).hasValue("Keep while compatible");

          button(page, "Replace entry type").click();
          assertThat(page.getByLabel("Entry b")).hasAttribute("data-variant", "replacement");
          Assertions.assertThat(
                  page.evaluate(
                      "beta !== document.querySelector('[data-entry=b]') && !beta.isConnected && alpha === document.querySelector('[data-entry=a]')"))
              .isEqualTo(true);
          assertThat(page.getByLabel("Entry b")).hasValue("");
          clickDetachedBeta(page);
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 2; choices: 2; last: b@2");
          button(page, "Choose b").click();
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 2; choices: 3; last: b@2");
        });
  }

  @BothEngines
  @DisplayName(
      "duplicate keys reject the candidate without changing the committed view and allow recovery")
  void duplicateKeysRejectTheCandidateWithoutChangingTheCommittedViewAndAllowRecovery(
      String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          assertThat(page.getByLabel("Entry b")).isVisible();
          var before = page.locator("#list").innerHTML();
          page.evaluate("window.beta = document.querySelector('[data-entry=b]')");
          button(page, "Duplicate keys").click();
          page.waitForCondition(() -> !failures.isEmpty());

          Assertions.assertThat(failures)
              .anySatisfy(
                  error -> Assertions.assertThat(error).contains("Duplicate sibling key 'a'"));
          Assertions.assertThat(page.locator("#list").innerHTML()).isEqualTo(before);
          Assertions.assertThat(page.evaluate("beta === document.querySelector('[data-entry=b]')"))
              .isEqualTo(true);
          button(page, "Recover entries").click();
          button(page, "Refresh entries").click();
          assertThat(page.locator("[data-list-state]"))
              .hasText("Revision: 1; choices: 0; last: none");
          assertOrder(page, List.of("a", "b", "c"));
          assertThat(label(page, "Notes")).isVisible();
        });
  }

  @BothEngines
  @DisplayName(
      "static and live keyed content, including a bundled SVG icon, have matching semantic DOM"
          + " and computed styles")
  void staticAndLiveKeyedContentHaveMatchingSemanticDomAndComputedStyles(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          assertThat(page.getByLabel("Entry b")).isVisible();
          button(page, "Toggle details").click();
          assertThat(
                  page.getByText("Additional details", new Page.GetByTextOptions().setExact(true)))
              .isVisible();
          try (var staticPage = page.context().newPage()) {
            staticPage.navigate(url + "/static");
            Assertions.assertThat(page.locator("[data-entries]").evaluate(SEMANTICS))
                .isEqualTo(staticPage.locator("[data-entries]").evaluate(SEMANTICS));
            Assertions.assertThat(page.locator("[data-entries]").evaluate(STYLES))
                .isEqualTo(staticPage.locator("[data-entries]").evaluate(STYLES));
            // The bundled callout icon is structured SVG, created in its namespace by both.
            for (var output : List.of(page, staticPage)) {
              var icon = output.locator("[data-entries] svg circle");
              Assertions.assertThat(icon.evaluate("node => node.namespaceURI"))
                  .isEqualTo("http://www.w3.org/2000/svg");
              Assertions.assertThat(icon.evaluate("node => node.getBBox().width > 0"))
                  .isEqualTo(true);
            }
          }
        });
  }

  @BothEngines
  @DisplayName("portable form defaults match static output and survive live edits until reset")
  void portableDefaultsMatchStaticAndSurviveLiveEdits(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url + "/control-defaults");
          var form = page.locator("[data-initial-form]");
          assertThat(form).isVisible();
          var staticState = controlDefaults(form);
          Assertions.assertThat(staticState)
              .isEqualTo(
                  List.of(
                      List.of("a\r\nb\rc d", "a\r\nb\rc d"),
                      List.of("Portable <value>", "Portable <value>"),
                      List.of("\nPortable & notes", "\nPortable & notes"),
                      List.of(true, true),
                      List.of(true, true),
                      List.of(false, false),
                      List.of(List.of("b"), List.of("b")),
                      List.of(List.of("a", "c"), List.of("a", "c"))));

          page.navigate(url);
          assertThat(form).isVisible();
          Assertions.assertThat(controlDefaults(form)).isEqualTo(staticState);

          var text = form.getByLabel("Default text");
          var notes = form.getByLabel("Default notes");
          var check = form.getByLabel("Default check");
          var radio = form.getByLabel("Default radio two");
          var choice =
              form.getByLabel("Default choice", new Locator.GetByLabelOptions().setExact(true));
          var choices =
              form.getByLabel("Default choices", new Locator.GetByLabelOptions().setExact(true));
          var request = page.locator("[data-requested-default-change]");
          text.fill("Browser edit");
          assertThat(request).hasText("value:Browser edit");
          notes.fill("Edited notes");
          check.uncheck();
          assertThat(request).hasText("checked:false");
          radio.check();
          choice.selectOption("a");
          choices.selectOption(new String[] {"b"});
          assertThat(request).hasText("selected:b");

          form.evaluate(
              """
              form => {
                window.changedDefaults = 0;
                new MutationObserver(records => window.changedDefaults += records.length)
                    .observe(form, {subtree: true, attributes: true,
                                    attributeFilter: ['value', 'checked', 'selected']});
              }
              """);
          text.focus();
          text.evaluate("node => node.setSelectionRange(1, 4, 'backward')");
          button(page, "Change initial defaults").dispatchEvent("click");
          assertThat(page.locator("[data-defaults-version]"))
              .hasAttribute("data-defaults-version", "0:true:false");
          button(page, "Reverse options").dispatchEvent("click");
          assertThat(page.locator("[data-defaults-version]"))
              .hasAttribute("data-defaults-version", "0:true:true");
          assertThat(text).hasValue("Browser edit");
          assertThat(text).isFocused();
          Assertions.assertThat(
                  text.evaluate(
                      "node => [node.selectionStart, node.selectionEnd, node.selectionDirection]"))
              .isEqualTo(List.of(1, 4, "backward"));
          assertThat(notes).hasValue("Edited notes");
          assertThat(check).not().isChecked();
          assertThat(radio).isChecked();
          assertThat(choice).hasValue("a");
          assertSelection(choices, List.of("b"));

          Assertions.assertThat(page.evaluate("window.changedDefaults")).isEqualTo(0);

          button(page, "Reset browser defaults").click();
          assertThat(text).hasValue("Portable <value>");
          assertThat(notes).hasValue("\nPortable & notes");
          assertThat(check).isChecked();
          assertThat(radio).not().isChecked();
          assertThat(choice).hasValue("b");
          assertSelection(choices, List.of("c", "a"));

          button(page, "Replace initial form").click();
          assertThat(page.locator("[data-defaults-version]"))
              .hasAttribute("data-defaults-version", "1:true:true");
          assertThat(text).hasValue("Replacement");
          assertThat(notes).hasValue("Replacement notes");
          assertThat(check).not().isChecked();
          assertThat(radio).isChecked();
          assertThat(choice).hasValue("c");
          assertSelection(choices, List.of("b"));
          Assertions.assertThat(failures).isEmpty();
        });
  }

  private static Object controlDefaults(Locator form) {
    return form.evaluate(
        """
        form => Array.from(form.elements).filter(control => control.localName !== 'button')
            .map(control => {
              if (control.localName === 'select') {
                const options = Array.from(control.options);
                return [options.filter(o => o.selected).map(o => o.value),
                        options.filter(o => o.defaultSelected).map(o => o.value)];
              }
              return control.type === 'checkbox' || control.type === 'radio'
                  ? [control.checked, control.defaultChecked]
                  : [control.value, control.defaultValue];
            })
        """);
  }

  @BothEngines
  @DisplayName("connected radio groups apply bindings and hand off controls before native mutation")
  void preservesValuesAcrossRadioInsertionAndControlHandoff(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var first = page.getByLabel("First bound radio");
          var second = page.getByLabel("Second bound radio");
          var text = page.getByLabel("Legacy bound text");
          var check = page.getByLabel("Legacy bound check");
          var notes = page.getByLabel("Legacy notes");
          assertThat(first).isChecked();
          assertThat(second).not().isChecked();
          assertThat(text).hasValue("Legacy start");
          assertThat(check).isChecked();
          text.focus();
          text.evaluate("node => node.setSelectionRange(1, 4, 'backward')");

          notes.focus();
          notes.evaluate("node => node.setSelectionRange(2, 6, 'backward')");
          button(page, "Hand off controls").dispatchEvent("click");
          assertThat(text).hasAttribute("value", "Legacy changed");
          assertThat(text).hasValue("Legacy start");
          assertThat(notes).isFocused();
          assertThat(notes).hasValue("Original notes");
          Assertions.assertThat(
                  notes.evaluate(
                      "node => [node.selectionStart, node.selectionEnd, node.selectionDirection]"))
              .isEqualTo(List.of(2, 6, "backward"));
          Assertions.assertThat(
                  text.evaluate(
                      "node => [node.selectionStart, node.selectionEnd, node.selectionDirection]"))
              .isEqualTo(List.of(1, 4, "backward"));
          assertThat(check).not().hasAttribute("checked", "");
          assertThat(check).isChecked();
          assertThat(first).isChecked();
          assertThat(second).not().isChecked();
          assertThat(page.getByLabel("New bound radio")).not().isChecked();

          button(page, "Reset native controlled form").click();
          assertThat(text).hasValue("Legacy changed");
          assertThat(notes).hasValue("Original notes");
          assertThat(check).not().isChecked();
          assertThat(first).not().isChecked();
          assertThat(second).isChecked();
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("controlled form events round trip to Java and resets update live properties")
  void controlledFormEventsRoundTripToJavaAndResetsUpdateLiveProperties(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          var text = label(page, "Controlled text");
          var notes = page.getByLabel("Controlled notes");
          var check = label(page, "Controlled check");
          var choice = label(page, "Controlled choice");
          var choices = label(page, "Controlled choices");
          var browser = page.getByLabel("Browser text");
          assertThat(text).hasValue("Initial text");
          assertThat(notes).hasValue("Initial notes");
          assertThat(check).isChecked();
          assertThat(choice).hasValue("b");
          assertSelection(choices, List.of("a", "c"));
          browser.fill("Browser edit");
          text.fill("Java text");
          assertThat(page.locator("[data-form-event]"))
              .containsText("text:Java text; checked=false; selected=; composing=false");
          notes.fill("Java notes");
          assertThat(page.locator("[data-form-event]")).containsText("notes:Java notes");
          check.uncheck();
          assertThat(page.locator("[data-form-event]")).containsText("check:on; checked=false");
          choice.selectOption("a");
          assertThat(page.locator("[data-form-event]"))
              .containsText("choice:a; checked=false; selected=a");
          choices.selectOption(new String[] {"b", "c"});
          assertThat(page.locator("[data-form-event]"))
              .containsText("choices:b; checked=false; selected=b,c");
          button(page, "Reverse options").click();
          settleFrames(page);
          assertThat(choice).hasValue("a");
          assertSelection(choices, List.of("c", "b"));
          text.evaluate("node => window.formText = node");
          var captured = page.locator("[data-form-event]").textContent();
          button(page, "Reset form").click();
          assertThat(text).hasValue("");
          assertThat(notes).hasValue("");
          assertThat(check).not().isChecked();
          assertThat(choice).hasValue("c");
          assertSelection(choices, List.of());
          assertThat(browser).hasValue("Browser edit");
          assertThat(text).hasAttribute("value", "Attribute text");
          assertThat(check).hasAttribute("checked", "");
          Assertions.assertThat(text.evaluate("node => node === formText")).isEqualTo(true);
          assertThat(page.locator("[data-form-event]")).hasText(captured);

          button(page, "Reset native controlled form").click();
          assertThat(text).hasValue("Attribute text");
          assertThat(notes).hasValue("Default notes");
          assertThat(check).isChecked();
          assertThat(choice).hasValue("a");
          assertSelection(choices, List.of("b"));
          button(page, "Refresh form").click();
          assertThat(text).hasValue("");
          assertThat(notes).hasValue("");
          assertThat(check).not().isChecked();
          assertThat(choice).hasValue("c");
          assertSelection(choices, List.of());
        });
  }

  @BothEngines
  @DisplayName(
      "unchanged controlled text preserves caret selection and focus while rejected edits are restored")
  void unchangedControlledTextPreservesCaretSelectionAndFocusWhileRejectedEditsAreRestored(
      String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          var text = label(page, "Controlled text");
          assertThat(text).hasValue("Initial text");
          for (var label : List.of("Controlled text", "Controlled notes")) {
            var control = label(page, label);
            control.fill("abcdef");
            settleFrames(page);
            control.evaluate("node => node.setSelectionRange(2, 4, 'backward')");
            button(page, "Refresh form").dispatchEvent("click");
            settleFrames(page);
            assertThat(control).isFocused();
            Assertions.assertThat(
                    control.evaluate(
                        "node => [node.selectionStart, node.selectionEnd, node.selectionDirection]"))
                .isEqualTo(List.of(2, 4, "backward"));
            control.press("Z");
            assertThat(page.locator("[data-form-event]")).containsText(":abZef;");
            assertThat(control).hasValue("abZef");
            Assertions.assertThat(
                    control.evaluate("node => [node.selectionStart, node.selectionEnd]"))
                .isEqualTo(List.of(3, 3));
          }
          button(page, "Toggle edit acceptance").click();
          settleFrames(page);
          text.fill("Rejected");
          assertThat(page.locator("[data-form-event]")).containsText("text:Rejected;");
          assertThat(text).hasValue("abZef");
          var check = label(page, "Controlled check");
          check.click();
          assertThat(page.locator("[data-form-event]")).containsText("checked=false");
          assertThat(check).isChecked();
          var choices = label(page, "Controlled choices");
          choices.selectOption(new String[] {});
          assertThat(page.locator("[data-form-event]")).containsText("selected=;");
          assertSelection(choices, List.of("a", "c"));
        });
  }

  @BothEngines
  @DisplayName("uncontrolled transitions preserve browser state until Java takes control again")
  void uncontrolledTransitionsPreserveBrowserStateUntilJavaTakesControlAgain(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          var text = label(page, "Controlled text");
          assertThat(text).hasValue("Initial text");
          text.evaluate("node => window.formText = node");
          button(page, "Toggle controlled form").click();
          button(page, "Toggle edit acceptance").click();
          settleFrames(page);
          text.fill("Browser owns text");
          var notes = page.getByLabel("Controlled notes");
          notes.fill("Browser owns notes");
          var check = label(page, "Controlled check");
          check.uncheck();
          var choice = label(page, "Controlled choice");
          choice.selectOption("a");
          var choices = label(page, "Controlled choices");
          choices.selectOption(new String[] {"b"});
          button(page, "Reset form").click();
          assertThat(page.locator("[data-form-revision]")).hasAttribute("data-form-revision", "1");
          assertThat(text).hasValue("Browser owns text");
          assertThat(notes).hasValue("Browser owns notes");
          assertThat(check).not().isChecked();
          assertThat(choice).hasValue("a");
          assertSelection(choices, List.of("b"));
          button(page, "Toggle controlled form").click();
          assertThat(text).hasValue("");
          assertThat(notes).hasValue("");
          assertThat(choice).hasValue("c");
          assertSelection(choices, List.of());
          Assertions.assertThat(text.evaluate("node => node === formText")).isEqualTo(true);
        });
  }

  @BothEngines
  @DisplayName("an unkeyed field control survives its hint and error appearing and disappearing")
  void anUnkeyedFieldControlSurvivesItsHintAndErrorAppearingAndDisappearing(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          var text = label(page, "Browser text");
          text.fill("Typed by the user");
          text.evaluate("node => window.browserText = node");
          button(page, "Toggle field feedback").click();
          assertThat(page.getByText("Shown before the control")).isAttached();
          assertThat(page.getByText("Shown after the control")).isAttached();
          assertThat(text).hasValue("Typed by the user");
          Assertions.assertThat(text.evaluate("node => node === browserText")).isEqualTo(true);
          button(page, "Toggle field feedback").click();
          assertThat(page.getByText("Shown before the control")).hasCount(0);
          assertThat(page.getByText("Shown after the control")).hasCount(0);
          assertThat(text).hasValue("Typed by the user");
          Assertions.assertThat(text.evaluate("node => node === browserText")).isEqualTo(true);
        });
  }

  @BothEngines
  @DisplayName("composition defers controlled writes and finishes with fresh Java state")
  void compositionDefersControlledWritesAndFinishesWithFreshJavaState(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          assertThat(page.getByLabel("Controlled notes")).hasValue("Initial notes");
          for (var label : List.of("Controlled text", "Controlled notes")) {
            var control = label(page, label);
            control.focus();
            control.evaluate(
                """
                node => {
                  node.dispatchEvent(new CompositionEvent('compositionstart', {bubbles: true}));
                  node.value = 'かな';
                  node.dispatchEvent(new InputEvent('input', {bubbles: true, isComposing: true, data: 'かな'}));
                }
                """);
            assertThat(page.locator("[data-form-event]")).containsText("composing=true");
            button(page, "Reset form").dispatchEvent("click");
            settleFrames(page);
            assertThat(control).hasValue("かな");
            assertThat(control).isFocused();
            control.evaluate(
                """
                node => {
                  node.dispatchEvent(new CompositionEvent('compositionend', {bubbles: true, data: '確定'}));
                  node.value = '確定';
                  node.dispatchEvent(new InputEvent('input', {bubbles: true, isComposing: false, data: '確定'}));
                }
                """);
            assertThat(page.locator("[data-form-event]")).containsText(":確定;");
            assertThat(page.locator("[data-form-event]")).containsText("composing=false");
            settleFrames(page);
            assertThat(control).hasValue("確定");
            button(page, "Toggle edit acceptance").dispatchEvent("click");
            settleFrames(page);
            control.evaluate(
                "node => { node.dispatchEvent(new CompositionEvent('compositionstart')); node.value = '未確定'; }");
            button(page, "Reset form").dispatchEvent("click");
            settleFrames(page);
            assertThat(control).hasValue("未確定");
            control.evaluate("node => node.dispatchEvent(new CompositionEvent('compositionend'))");
            settleFrames(page);
            assertThat(control).hasValue("");
            button(page, "Toggle edit acceptance").dispatchEvent("click");
            settleFrames(page);
          }
        });
  }

  @BothEngines
  @DisplayName("removed controls release application and composition listeners")
  void removedControlsReleaseApplicationAndCompositionListeners(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var text = label(page, "Controlled text");
          assertThat(text).hasValue("Initial text");
          text.evaluate("node => window.removedControl = node");
          button(page, "Remove form controls").click();
          assertThat(text).hasCount(0);
          var section = page.locator("[data-form-turn]");
          var turn = section.getAttribute("data-form-turn");
          var captured = page.locator("[data-form-event]").textContent();
          page.evaluate(
              """
              () => {
                for (const type of ['compositionstart', 'input', 'change', 'compositionend']) {
                  removedControl.dispatchEvent(new Event(type));
                }
              }
              """);
          settleFrames(page);
          assertThat(section).hasAttribute("data-form-turn", turn);
          assertThat(page.locator("[data-form-event]")).hasText(captured);
          button(page, "Unmount form").click();
          assertThat(page.locator("#controls > *")).hasCount(0);
          page.evaluate("removedControl.dispatchEvent(new Event('compositionend'))");
          settleFrames(page);
          assertThat(page.locator("#controls > *")).hasCount(0);
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @Test
  @DisplayName("browser generated composition survives Java resets until native commit")
  void browserGeneratedCompositionSurvivesJavaResetsUntilNativeCommit() {
    withPage(
        "chromium",
        (page, url, _) -> {
          page.navigate(url);
          var text = label(page, "Controlled text");
          assertThat(text).hasValue("Initial text");
          text.fill("");
          settleFrames(page);
          text.evaluate(
              """
              node => {
                window.imeEvents = [];
                for (const type of ['compositionstart', 'compositionupdate', 'compositionend']) {
                  node.addEventListener(type, event => imeEvents.push({type, trusted: event.isTrusted}));
                }
              }
              """);
          var session = page.context().newCDPSession(page);
          try {
            session.send(
                "Input.imeSetComposition",
                JsonParser.parseString(
                        """
                        {"text":"かな","selectionStart":2,"selectionEnd":2}
                        """)
                    .getAsJsonObject());
            assertThat(page.locator("[data-form-event]")).containsText("composing=true");
            button(page, "Reset form").dispatchEvent("click");
            settleFrames(page);
            assertThat(text).hasValue("かな");
            assertThat(text).isFocused();
            session.send(
                "Input.insertText",
                JsonParser.parseString(
                        """
                        {"text":"確定"}
                        """)
                    .getAsJsonObject());
            settleFrames(page);
            assertThat(text).hasValue("確定");
            button(page, "Refresh form").dispatchEvent("click");
            settleFrames(page);
            assertThat(text).hasValue("確定");
            Assertions.assertThat(page.evaluate("imeEvents.map(event => event.type)"))
                .isEqualTo(
                    List.of(
                        "compositionstart",
                        "compositionupdate",
                        "compositionupdate",
                        "compositionend"));
          } finally {
            session.detach();
          }
        });
  }

  @BothEngines
  @DisplayName("repeated invalidations coalesce into one committed render")
  void repeatedInvalidationsCoalesceIntoOneCommittedRender(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          var renders = page.locator("[data-schedule-renders]");
          assertThat(renders).isVisible();
          var before = renderCount(renders, "data-schedule-renders");
          button(page, "Burst invalidations").click();
          page.waitForFunction(
              "before => Number(document.querySelector('[data-schedule-renders]').dataset.scheduleRenders) > before",
              before);
          settleFrames(page);
          Assertions.assertThat(renderCount(renders, "data-schedule-renders") - before)
              .isEqualTo(1);
        });
  }

  @BothEngines
  @DisplayName("render chains yield paint between budgeted batches")
  void renderChainsYieldPaintBetweenBudgetedBatches(String engine) {
    withPage(
        engine,
        (page, url, _) -> {
          page.navigate(url);
          var renders = page.locator("[data-schedule-renders]");
          var before = renderCount(renders, "data-schedule-renders");
          startFrameCounter(page);
          button(page, "Start render chain").click();
          page.waitForFunction(
              "() => document.querySelector('[data-schedule-remaining]').dataset.scheduleRemaining === '0'");
          settleFrames(page);
          Assertions.assertThat(renderCount(renders, "data-schedule-renders") - before)
              .isEqualTo(5);
          Assertions.assertThat(frameCount(page)).isGreaterThanOrEqualTo(2);
        });
  }

  @BothEngines
  @DisplayName("runaway self invalidation is reported once and the root can recover")
  void runawaySelfInvalidationIsReportedOnceAndTheRootCanRecover(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var renders = page.locator("[data-schedule-renders]");
          var before = renderCount(renders, "data-schedule-renders");
          startFrameCounter(page);
          button(page, "Start runaway").click();
          page.waitForCondition(() -> !failures.isEmpty());
          Assertions.assertThat(failures)
              .anySatisfy(error -> Assertions.assertThat(error).contains("Runaway invalidation"));
          var halted = renderCount(renders, "data-schedule-renders");
          Assertions.assertThat(halted - before).isEqualTo(8);
          settleFrames(page);
          settleFrames(page);
          Assertions.assertThat(renderCount(renders, "data-schedule-renders")).isEqualTo(halted);
          Assertions.assertThat(frameCount(page)).isGreaterThanOrEqualTo(1);
          Assertions.assertThat(failures).hasSize(1);
          button(page, "Stop runaway").click();
          page.waitForFunction(
              "halted => Number(document.querySelector('[data-schedule-renders]').dataset.scheduleRenders) > halted",
              halted);
          Assertions.assertThat(failures).hasSize(1);
        });
  }

  @BothEngines
  @DisplayName(
      "rejected candidates preserve the committed tree, keep no expansion state, start no effect"
          + " and allow recovery")
  void rejectedCandidatesPreserveTheCommittedTreeKeepNoStateStartNoEffectAndAllowRecovery(
      String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var renders = page.locator("[data-schedule-renders]");
          assertThat(renders).isVisible();
          var tree = page.locator("#schedule");
          var before = tree.innerHTML();
          var beforeRenders = renderCount(renders, "data-schedule-renders");
          button(page, "Reject next render").click();
          page.waitForCondition(() -> !failures.isEmpty());
          Assertions.assertThat(failures)
              .anySatisfy(error -> Assertions.assertThat(error).contains("Duplicate sibling key"));
          Assertions.assertThat(tree.innerHTML()).isEqualTo(before);
          Assertions.assertThat(renders.getAttribute("data-schedule-renders"))
              .isEqualTo(Integer.toString(beforeRenders));
          button(page, "Refresh schedule").click();
          page.waitForFunction(
              "before => Number(document.querySelector('[data-schedule-renders]').dataset.scheduleRenders) > before",
              beforeRenders);
          assertThat(page.locator("[data-schedule-tally]"))
              .hasAttribute("data-schedule-tally", "1");
          assertThat(renders).hasAttribute("data-schedule-rejected-effects", "0");
          Assertions.assertThat(failures).hasSize(1);
        });
  }

  @BothEngines
  @DisplayName("stale completions are ignored after disposal while current completions apply")
  void staleCompletionsAreIgnoredAfterDisposalWhileCurrentCompletionsApply(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          button(page, "Complete current update").click();
          assertThat(page.locator("[data-schedule-status]")).containsText("current applied");
          button(page, "Close schedule root").click();
          assertThat(page.locator("#schedule")).isEmpty();
          button(page, "Complete stale update").click();
          assertThat(page.locator("[data-control-status]")).hasText("stale ignored");
          assertThat(page.locator("#schedule")).isEmpty();
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("render callback failures keep the committed tree, name the component and recover")
  void renderCallbackFailuresKeepTheCommittedTreeNameTheComponentAndRecover(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var renders = page.locator("[data-schedule-renders]");
          assertThat(renders).isVisible();
          var tree = page.locator("#schedule");
          var before = tree.innerHTML();
          var beforeRenders = renderCount(renders, "data-schedule-renders");
          button(page, "Fail next render").click();
          assertReported(
              page,
              failures,
              "Render rejected in browser root 'schedule'",
              "Simulated render failure",
              "FailingPanel");
          Assertions.assertThat(tree.innerHTML()).isEqualTo(before);
          button(page, "Refresh schedule").click();
          page.waitForFunction(
              "before => Number(document.querySelector('[data-schedule-renders]').dataset.scheduleRenders) > before + 1",
              beforeRenders);
          Assertions.assertThat(failures).hasSize(1);
        });
  }

  @BothEngines
  @DisplayName("Java handler failures are reported and leave the root interactive")
  void javaHandlerFailuresAreReportedAndLeaveTheRootInteractive(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          button(page, "Throw from handler").click();
          assertReported(
              page,
              failures,
              "'click' handler failed in browser root 'schedule-control'",
              "Simulated handler failure");
          button(page, "Complete current update").click();
          assertThat(page.locator("[data-control-status]")).hasText("current applied");
          Assertions.assertThat(failures).hasSize(1);
        });
  }

  @BothEngines
  @DisplayName("a failed initial render keeps fallback content while later roots mount")
  void failedInitialRenderKeepsFallbackContentWhileLaterRootsMount(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url + "/?fail-mount");
          page.waitForSelector("#app main", new Page.WaitForSelectorOptions().setTimeout(30_000));

          assertThat(page.locator("#broken"))
              .hasText("Fallback content for a root that fails to render.");
          assertThat(label(page, "Notes")).isVisible();
          Assertions.assertThat(failures)
              .singleElement()
              .satisfies(error ->
                  Assertions.assertThat(error)
                      .contains("Render rejected in browser root 'broken'")
                      .contains("BrokenPanel")
                      .contains("Simulated initial render failure"));
        });
  }

  @BothEngines
  @DisplayName("components share one retained constructed sheet without restyling the host page")
  void componentsShareOneRetainedConstructedSheetWithoutRestylingTheHostPage(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector("#app main", new Page.WaitForSelectorOptions().setTimeout(30_000));
          Assertions.assertThat(page.evaluate("document.querySelectorAll('style').length"))
              .isEqualTo(0);
          // One shared sheet per compiled entry: this page runs two.
          page.waitForSelector(
              "#second section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          Assertions.assertThat(page.evaluate("document.adoptedStyleSheets.length")).isEqualTo(2);
          Assertions.assertThat(page.evaluate("getComputedStyle(document.body).marginTop"))
              .isEqualTo("8px");
          assertThat(page.locator("#error")).isHidden();

          var increment = button(page, "Increment");
          increment.click();
          increment.click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 2");
          var rules = ruleCount(page);
          increment.click();
          increment.click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 4");
          Assertions.assertThat(ruleCount(page)).isEqualTo(rules);

          page.evaluate(
              """
              () => new MutationObserver(() => {
                const badge = document.querySelector('#app [data-scope^="tessera-badge"]');
                if (badge && window.badgeDisplay === undefined) {
                  window.badgeDisplay = getComputedStyle(badge).display;
                }
              }).observe(document.getElementById('app'), {childList: true, subtree: true})
              """);
          button(page, "Show badge").click();
          var badge = page.locator("#app [data-scope^=tessera-badge]");
          assertThat(badge).hasText("Saved");
          // The scoped inline-flex root is blockified to flex inside the flex container.
          assertThat(badge).hasCSS("display", "flex");
          assertThat(badge).hasCSS("white-space", "nowrap");
          Assertions.assertThat(page.evaluate("window.badgeDisplay")).isEqualTo("flex");
          var withBadge = ruleCount(page);
          Assertions.assertThat(withBadge).isGreaterThan(rules);

          button(page, "Unmount").click();
          assertThat(page.locator("#app")).isEmpty();
          Assertions.assertThat(ruleCount(page)).isEqualTo(withBadge);
          assertThat(label(page, "Notes")).hasCSS("border-top-style", "solid");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("values that change on every render keep the shared sheet bounded")
  void valuesThatChangeOnEveryRenderKeepTheSharedSheetBounded(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector("#app main", new Page.WaitForSelectorOptions().setTimeout(30_000));
          var baseline = ruleCount(page);
          page.evaluate(
              """
              async () => {
                const advance = [...document.querySelectorAll('#app button')]
                  .find(button => button.textContent.trim() === 'Advance meter');
                for (let step = 0; step < 600; step++) {
                  advance.click();
                  await new Promise(resolve => setTimeout(resolve, 0));
                }
              }
              """);
          assertThat(page.locator("[data-meter]")).hasCSS("inline-size", "600px");
          Assertions.assertThat(ruleCount(page)).isBetween(baseline + 500, baseline + 513);
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("a live value animated for many frames keeps a constant rule count under CSP")
  void liveValueAnimatedForManyFramesKeepsAConstantRuleCountUnderCsp(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.addInitScript(
              """
              window.policyViolations = [];
              document.addEventListener('securitypolicyviolation', event =>
                policyViolations.push(event.violatedDirective + ' ' + event.blockedURI));
              """);
          page.navigate(url + "/csp/");
          page.waitForSelector(
              "#second section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          assertThat(page.getByRole(AriaRole.STATUS)).hasCSS("font-weight", "600");
          assertThat(button(page, "Increment")).hasCSS("cursor", "pointer");
          assertThat(label(page, "Name")).hasCSS("border-top-style", "solid");
          var meter = page.locator("[data-live-meter]");
          assertThat(meter).hasCSS("inline-size", "0px");
          var baseline = ruleCount(page);

          button(page, "Animate live meter").click();
          assertThat(meter)
              .hasCSS(
                  "inline-size", "120px", new LocatorAssertions.HasCSSOptions().setTimeout(15_000));
          Assertions.assertThat(ruleCount(page)).isEqualTo(baseline);
          Assertions.assertThat(
                  meter.evaluate("node => node.style.getPropertyValue('--tl-inline-size')"))
              .isEqualTo("120px");
          Assertions.assertThat(page.evaluate("policyViolations.length")).isEqualTo(0);
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("effects start after commit, restart when their key changes and stop on removal")
  void effectsStartAfterCommitRestartWhenTheirKeyChangesAndStopOnRemoval(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var log = page.locator("[data-lifetime-log]");
          button(page, "Start timer").click();
          page.waitForFunction(
              "Number(document.querySelector('[data-lifetime-ticks]').dataset.lifetimeTicks) >= 3");
          assertThat(log).hasAttribute("data-lifetime-log", "start 0");

          button(page, "Restart timer").click();
          assertThat(log).hasAttribute("data-lifetime-log", "start 0,stop 0,start 1");
          button(page, "Stop timer").click();
          assertThat(log).hasAttribute("data-lifetime-log", "start 0,stop 0,start 1,stop 1");
          var stopped = page.locator("[data-lifetime-ticks]").getAttribute("data-lifetime-ticks");
          page.waitForTimeout(200);
          assertThat(page.locator("[data-lifetime-ticks]"))
              .hasAttribute("data-lifetime-ticks", stopped);
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName(
      "closing a root disposes effects and its lifetime, aborts requests and is idempotent")
  void closingRootDisposesEffectsAndLifetimeAbortsRequestsAndIsIdempotent(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var control = page.locator("[data-lifetime-log]");
          button(page, "Start timer").click();
          page.waitForFunction(
              "Number(document.querySelector('[data-lifetime-ticks]').dataset.lifetimeTicks) >= 2");
          var aborted = new ArrayList<String>();
          page.onRequestFailed(request -> {
            if (request.url().endsWith("/slow")) {
              aborted.add(request.failure());
            }
          });
          button(page, "Start request").click();
          assertThat(control).hasAttribute("data-lifetime-request", "pending");

          var close = button(page, "Close lifetime root");
          close.click();
          close.click();
          assertThat(page.locator("#lifetime")).isEmpty();
          assertThat(control).hasAttribute("data-lifetime-log", "start 0,stop 0,root disposed");
          page.waitForCondition(() -> !aborted.isEmpty());
          // A disposed request reports nothing: its completion would target a closed root.
          assertThat(control).hasAttribute("data-lifetime-request", "pending");
          page.waitForTimeout(200);
          assertThat(control).hasAttribute("data-lifetime-log", "start 0,stop 0,root disposed");

          button(page, "Increment").click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 1");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("closing from a modal closes it and leaves no callbacks on detached nodes")
  void closingFromModalClosesItAndLeavesNoCallbacksOnDetachedNodes(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector(
              "#lifetime section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          page.evaluate(
              "window.detachedStart = [...document.querySelectorAll('#lifetime button')]"
                  + ".find(button => button.textContent.trim() === 'Start timer')");
          button(page, "Open lifetime dialog").click();
          assertThat(
                  page.getByRole(
                      AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Lifetime dialog")))
              .isVisible();
          Assertions.assertThat(page.evaluate("document.querySelectorAll(':modal').length"))
              .isEqualTo(1);

          button(page, "Close from dialog").click();
          assertThat(page.locator("#lifetime")).isEmpty();
          Assertions.assertThat(page.evaluate("document.querySelectorAll(':modal').length"))
              .isEqualTo(0);
          button(page, "Increment").click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 1");

          var log = page.locator("[data-lifetime-log]");
          assertThat(log).hasAttribute("data-lifetime-log", "root disposed");
          page.evaluate("detachedStart.click()");
          settleFrames(page);
          page.waitForTimeout(100);
          assertThat(log).hasAttribute("data-lifetime-log", "root disposed");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("capture and bubble listeners run in phase order and capture can stop propagation")
  void captureAndBubbleListenersRunInPhaseOrder(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var log = page.locator("[data-events-log]");
          button(page, "Phase target").click();
          assertThat(log).hasAttribute("data-events-log", "capture,target 0,bubble");
          page.locator("[data-phases]").dispatchEvent("click capture");
          assertThat(log).hasAttribute("data-events-log", "capture,target 0,bubble,custom");

          button(page, "Stop in capture").click();
          button(page, "Clear events").click();
          assertThat(log).hasAttribute("data-events-log", "");
          button(page, "Phase target").click();
          assertThat(log).hasAttribute("data-events-log", "capture");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("default prevention is synchronous and passive listeners cannot prevent defaults")
  void defaultPreventionIsSynchronousAndPassiveListenersCannotPreventDefaults(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var log = page.locator("[data-events-log]");
          var prevented =
              page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Prevented"));
          var passive =
              page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Passive"));

          prevented.click();
          assertThat(log).hasAttribute("data-events-log", "prevented prevented=true");
          assertThat(prevented).not().isChecked();
          passive.click();
          assertThat(log)
              .hasAttribute("data-events-log", "prevented prevented=true,passive prevented=false");
          assertThat(passive).isChecked();
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("a once listener stays spent across renders until a render removes it")
  void onceListenerStaysSpentAcrossRendersUntilARenderRemovesIt(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var log = page.locator("[data-events-log]");
          var once = page.locator("[data-once]");
          for (var click = 0; click < 3; click++) {
            once.click();
            settleFrames(page);
          }
          assertThat(log).hasAttribute("data-events-log", "once 1");

          button(page, "Re-arm once").click();
          settleFrames(page);
          page.waitForTimeout(50);
          once.click();
          assertThat(log).hasAttribute("data-events-log", "once 1,once 2");
          once.click();
          settleFrames(page);
          assertThat(log).hasAttribute("data-events-log", "once 1,once 2");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("typed keyboard data is copied and a mismatched event type is reported")
  void typedKeyboardDataIsCopiedAndMismatchedEventTypeIsReported(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var keys = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Keys"));
          keys.press("Shift+A");
          assertThat(page.locator("[data-events-log]"))
              .hasAttribute(
                  "data-events-log", java.util.regex.Pattern.compile(".*key A shift=true.*"));

          button(page, "Keyboard data on click").click();
          assertReported(
              page,
              failures,
              "'click' handler failed in browser root 'events'",
              "Keyboard data requires a keyboard event, not click");
        });
  }

  @BothEngines
  @DisplayName("a real click opens a modal synchronously within user activation and restores focus")
  void realClickOpensModalSynchronouslyWithinUserActivationAndRestoresFocus(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var opener = button(page, "Open events dialog");
          opener.click();
          var dialog =
              page.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Events dialog"));
          assertThat(dialog).isVisible();
          Assertions.assertThat(page.evaluate("document.activeElement.textContent.trim()"))
              .isEqualTo("Dismiss");
          var activation =
              engine.equals("chromium")
                  ? List.of("activation true")
                  : List.of("activation true", "activation unsupported");
          Assertions.assertThat(activation)
              .contains(page.locator("[data-events-log]").getAttribute("data-events-log"));

          page.keyboard().press("Escape");
          assertThat(dialog).isHidden();
          assertThat(opener).isFocused();
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("text requests complete with their body and report HTTP failures by status")
  void textRequestsCompleteWithTheirBodyAndReportHttpFailuresByStatus(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var control = page.locator("[data-lifetime-log]");
          button(page, "Fetch text").click();
          assertThat(control).hasAttribute("data-lifetime-request", "text hello from tessera");
          button(page, "Fetch missing").click();
          assertThat(control).hasAttribute("data-lifetime-request", "http 404");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("a text selection made through a reference survives updates")
  void textSelectionMadeThroughAReferenceSurvivesUpdates(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var services = page.locator("[data-services-renders]");
          button(page, "Select first word").click();
          var renders = renderCount(services, "data-services-renders");
          button(page, "Re-render services").click();
          page.waitForFunction(
              "before => Number(document.querySelector('[data-services-renders]').dataset.servicesRenders) > before",
              renders);
          button(page, "Read selection").click();
          assertThat(services).hasAttribute("data-services-status", "selection 0-5");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("a modal stays open across updates and returns focus to its opener")
  void modalStaysOpenAcrossUpdatesAndReturnsFocusToItsOpener(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var opener = button(page, "Open services dialog");
          opener.click();
          var dialog =
              page.getByRole(
                  AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Services dialog"));
          assertThat(dialog).isVisible();
          var before = page.locator("[data-dialog-renders]").textContent();
          button(page, "Re-render inside dialog").click();
          assertThat(page.locator("[data-dialog-renders]")).not().hasText(before);
          Assertions.assertThat(page.evaluate("document.querySelectorAll(':modal').length"))
              .isEqualTo(1);

          button(page, "Close services dialog").click();
          assertThat(dialog).isHidden();
          assertThat(opener).isFocused();
          var other = button(page, "Increment");
          other.focus();
          page.evaluate("restoreSavedFocus()");
          assertThat(other).isFocused();

          opener.click();
          button(page, "Close services root").dispatchEvent("click");
          assertThat(page.locator("#services")).isEmpty();
          other.focus();
          page.evaluate("restoreSavedFocus()");
          assertThat(other).isFocused();
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("focus falls back to the opener a render re-creates while the modal is open")
  void focusFallsBackToTheOpenerARenderRecreatesWhileTheModalIsOpen(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var opener = button(page, "Open services dialog");
          assertThat(opener).isVisible();
          page.evaluate("window.oldOpener = document.querySelector('[data-generation=\"0\"]')");
          opener.click();
          button(page, "Close and replace opener").click();
          assertThat(page.locator("[data-generation='1']")).isFocused();
          Assertions.assertThat(page.evaluate("oldOpener.isConnected")).isEqualTo(false);
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("media and popover state survive updates and closing the root stops them")
  void mediaAndPopoverStateSurviveUpdatesAndClosingTheRootStopsThem(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var services = page.locator("[data-services-renders]");
          button(page, "Play audio").click();
          assertThat(services).hasAttribute("data-services-status", "playing");
          page.waitForFunction("document.querySelector('#services audio').currentTime > 0");
          button(page, "Re-render services").click();
          button(page, "Read media").click();
          assertThat(services)
              .hasAttribute("data-services-status", "media paused=false advanced=true");
          button(page, "Pause audio").click();
          button(page, "Read media").click();
          assertThat(services)
              .hasAttribute("data-services-status", "media paused=true advanced=true");

          button(page, "Play audio").click();
          assertThat(services).hasAttribute("data-services-status", "playing");
          button(page, "Toggle menu").click();
          Assertions.assertThat(page.evaluate("document.querySelectorAll(':popover-open').length"))
              .isEqualTo(1);
          page.evaluate("window.audio = document.querySelector('#services audio')");
          button(page, "Close services root").click();
          assertThat(page.locator("#services")).isEmpty();
          // Removal pauses media at the browser's next stable state.
          page.waitForFunction("audio.paused && !audio.isConnected");
          Assertions.assertThat(page.evaluate("document.querySelectorAll(':popover-open').length"))
              .isEqualTo(0);
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("a timer opens a rebuilt modal through an application-owned reference")
  void timerOpensARebuiltModalThroughAnApplicationOwnedReference(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var services = page.locator("[data-services-renders]");
          var renders = renderCount(services, "data-services-renders");
          button(page, "Open overlay later").click();
          var dialog =
              page.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Confirm later"));
          assertThat(dialog).isVisible();
          Assertions.assertThat(renderCount(services, "data-services-renders"))
              .isGreaterThan(renders);
          page.keyboard().press("Escape");
          assertThat(dialog).isHidden();

          button(page, "Open stale overlay later").click();
          assertReported(
              page,
              failures,
              "Scheduled task failed in browser root 'services'",
              "References resolve against the latest committed render");
        });
  }

  @BothEngines
  @DisplayName("an operation on a reference the committed tree does not bind is reported")
  void operationOnAReferenceTheCommittedTreeDoesNotBindIsReported(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          button(page, "Focus unbound reference").click();
          assertReported(
              page,
              failures,
              "'click' handler failed in browser root 'services'",
              "ElementRef.scoped(missing) is not bound in the committed tree");
        });
  }

  @BothEngines
  @DisplayName("declared entries load once, mount their hosts and remain independently interactive")
  void declaredEntriesLoadOnceAndRemainIndependentlyInteractive(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          var requests = new ArrayList<String>();
          page.onRequest(request -> requests.add(request.url()));
          page.navigate(url);
          page.waitForSelector(
              "#second section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          page.waitForSelector("#collisions section");

          for (var asset :
              List.of(
                  "tessera-demo.js",
                  "tessera-demo.js.wasm",
                  "tessera-second.js",
                  "tessera-second.js.wasm")) {
            Assertions.assertThat(requests)
                .filteredOn(request -> request.endsWith("/" + asset))
                .hasSize(1);
          }
          Assertions.assertThat(
                  page.evaluate("document.querySelectorAll('[data-tessera-root]').length"))
              .isEqualTo(page.evaluate("document.querySelectorAll('[data-tessera-entry]').length"));
          Assertions.assertThat(page.evaluate("globalThis.GraalVM === undefined")).isEqualTo(true);
          var second = page.locator("[data-second-count]");
          button(page, "Second increment").click();
          assertThat(second).hasAttribute("data-second-count", "1");
          button(page, "Increment").click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 1");
          button(page, "Second increment").click();
          assertThat(second).hasAttribute("data-second-count", "2");

          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("a closed root remounts on its host without affecting its siblings")
  void closedRootRemountsOnItsHostWithoutAffectingItsSiblings(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var log = page.locator("[data-lifetime-log]");
          button(page, "Close lifetime root").click();
          assertThat(page.locator("#lifetime")).isEmpty();
          button(page, "Remount lifetime root").click();
          assertThat(page.locator("#lifetime section")).isVisible();

          button(page, "Start timer").click();
          page.waitForFunction(
              "Number(document.querySelector('[data-lifetime-ticks]').dataset.lifetimeTicks) >= 2");
          assertThat(log).hasAttribute("data-lifetime-log", "root disposed,start 0");
          button(page, "Increment").click();
          assertThat(page.getByRole(AriaRole.STATUS)).hasText("Count: 1");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("mounting a host that already holds a root is reported and leaves the root intact")
  void mountingAHostThatAlreadyHoldsARootIsReported(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          button(page, "Mount lifetime twice").click();
          assertReported(
              page,
              failures,
              "'click' handler failed in browser root 'lifetime-control'",
              "Cannot mount a browser root on 'lifetime': the host already holds a browser root");
          button(page, "Start timer").click();
          assertThat(page.locator("[data-lifetime-log]"))
              .hasAttribute("data-lifetime-log", "start 0");
        });
  }

  @BothEngines
  @DisplayName("ids used by another entry's root or by the host page reject the candidate")
  void idsUsedByAnotherEntrysRootOrByTheHostPageRejectTheCandidate(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector(
              "#second section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          var collisions = page.locator("#collisions");
          assertThat(collisions.locator("section")).isVisible();
          var before = collisions.innerHTML();

          button(page, "Claim shared id").click();
          assertReported(
              page,
              failures,
              "Render rejected in browser root 'collisions'",
              "DOM id 'shared-id' is already used by browser root 'second'");
          Assertions.assertThat(collisions.innerHTML()).isEqualTo(before);

          button(page, "Release ids").click();
          button(page, "Claim page id").click();
          page.waitForCondition(() -> failures.size() == 2);
          Assertions.assertThat(failures.getLast())
              .contains("DOM id 'error' is already used by the host page");
          button(page, "Release ids").click();
          settleFrames(page);
          Assertions.assertThat(page.evaluate("document.querySelectorAll('#shared-id').length"))
              .isEqualTo(1);
          Assertions.assertThat(failures).hasSize(2);
        });
  }

  @BothEngines
  @DisplayName("a committed id stays owned by its root, and a removed id is checked when re-added")
  void committedIdStaysOwnedByItsRootAndARemovedIdIsCheckedWhenReAdded(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          var renders = page.locator("[data-collisions-renders]");
          assertThat(page.locator("#collisions #collisions-note")).hasText("Owned");
          // Placed first, so the document resolves the id to the host page's element.
          page.evaluate(
              "() => { const copy = document.createElement('p'); copy.id = 'collisions-note';"
                  + " document.body.prepend(copy); }");

          var committed = renderCount(renders, "data-collisions-renders");
          button(page, "Release ids").click();
          assertThat(renders)
              .hasAttribute("data-collisions-renders", Integer.toString(committed + 1));
          assertThat(page.locator("#collisions #collisions-note")).hasText("Owned");
          button(page, "Toggle note").click();
          assertThat(renders)
              .hasAttribute("data-collisions-renders", Integer.toString(committed + 2));
          assertThat(page.locator("#collisions #collisions-note")).hasCount(0);
          Assertions.assertThat(failures).isEmpty();

          button(page, "Toggle note").click();
          assertReported(
              page,
              failures,
              "Render rejected in browser root 'collisions'",
              "DOM id 'collisions-note' is already used by the host page");
          assertThat(renders)
              .hasAttribute("data-collisions-renders", Integer.toString(committed + 2));
        });
  }

  @BothEngines
  @DisplayName("a child claiming the host's own id is rejected before publication")
  void aChildClaimingTheHostsOwnIdIsRejectedBeforePublication(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector(
              "#collisions section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          var collisions = page.locator("#collisions");
          var before = collisions.innerHTML();

          button(page, "Claim host id").click();
          assertReported(
              page,
              failures,
              "Render rejected in browser root 'collisions'",
              "DOM id 'collisions' is already used by the host element of this root");
          Assertions.assertThat(collisions.innerHTML()).isEqualTo(before);
          Assertions.assertThat(page.evaluate("document.querySelectorAll('#collisions').length"))
              .isEqualTo(1);

          // The rejected candidate ran the view once, so the retry commits the next count.
          var renders = collisions.locator("[data-collisions-renders]");
          var committed = renderCount(renders, "data-collisions-renders");
          button(page, "Release ids").click();
          assertThat(renders)
              .hasAttribute("data-collisions-renders", Integer.toString(committed + 2));
          Assertions.assertThat(failures).hasSize(1);
        });
  }

  @BothEngines
  @DisplayName(
      "a commit whose retiring cleanup closes the root releases the replacement it created")
  void aCommitWhoseRetiringCleanupClosesTheRootReleasesTheReplacementItCreated(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector(
              "#closure section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          var log = page.locator("[data-closure-log]");
          // Retain the button the closing commit creates, so it can be clicked after teardown.
          page.evaluate(
              """
              () => {
                window.replacement = null;
                new MutationObserver(records => {
                  for (const record of records) {
                    for (const node of record.addedNodes) {
                      if (node.textContent === 'Action 2') window.replacement = node;
                    }
                  }
                }).observe(document.getElementById('closure'), {childList: true, subtree: true});
              }\
              """);

          button(page, "Replace action").click();
          assertThat(page.locator("#closure")).isEmpty();
          settleFrames(page);
          assertThat(log).hasAttribute("data-closure-log", "root disposed");
          Assertions.assertThat(page.evaluate("window.replacement !== null")).isEqualTo(true);

          page.evaluate("window.replacement.click()");
          settleFrames(page);
          assertThat(log).hasAttribute("data-closure-log", "root disposed");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("closing during expansion discards the candidate and publishes nothing")
  void closingDuringExpansionDiscardsTheCandidateAndPublishesNothing(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector(
              "#closure section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          button(page, "Close during render").click();
          assertThat(page.locator("#closure")).isEmpty();
          assertThat(page.locator("[data-never-published]")).hasCount(0);
          assertThat(page.locator("[data-closure-log]"))
              .hasAttribute("data-closure-log", "root disposed");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("closing during effect startup starts no later effect in the batch")
  void closingDuringEffectStartupStartsNoLaterEffectInTheBatch(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          button(page, "Close during startup").click();
          assertThat(page.locator("#closure")).isEmpty();
          assertThat(page.locator("[data-closure-log]"))
              .hasAttribute(
                  "data-closure-log", "started closing-start,disposed closing-start,root disposed");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName("closing during an after-render task stops the rest of the batch")
  void closingDuringAnAfterRenderTaskStopsTheRestOfTheBatch(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          button(page, "Close in after-render").click();
          assertThat(page.locator("#closure")).isEmpty();
          assertThat(page.locator("[data-closure-log]"))
              .hasAttribute("data-closure-log", "after-render first,root disposed");
          Assertions.assertThat(failures).isEmpty();
        });
  }

  @BothEngines
  @DisplayName(
      "a failing effect start is reported, preserves the root, and closing releases it once")
  void aFailingEffectStartIsReportedPreservesTheRootAndClosingReleasesItOnce(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          page.waitForSelector(
              "#closure section", new Page.WaitForSelectorOptions().setTimeout(30_000));
          button(page, "Fail effect start").click();
          assertReported(
              page,
              failures,
              "Effect failed to start in browser root 'closure'",
              "Simulated effect start failure");
          assertThat(page.locator("#closure section")).isVisible();

          page.evaluate(
              "window.action = Array.from(document.querySelectorAll('#closure button'))"
                  + ".find(button => button.textContent === 'Action 1')");
          button(page, "Close closure root").click();
          assertThat(page.locator("#closure")).isEmpty();
          var log = page.locator("[data-closure-log]");
          assertThat(log).hasAttribute("data-closure-log", "disposed failing,root disposed");

          page.evaluate("window.action.click()");
          settleFrames(page);
          assertThat(log).hasAttribute("data-closure-log", "disposed failing,root disposed");
          Assertions.assertThat(failures).hasSize(1);
        });
  }

  @BothEngines
  @DisplayName("a failing cleanup is reported and the remaining cleanups still release once")
  void aFailingCleanupIsReportedAndTheRemainingCleanupsStillReleaseOnce(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.navigate(url);
          button(page, "Add failing cleanups").click();
          button(page, "Remove failing cleanups").click();
          assertReported(
              page,
              failures,
              "Effect cleanup failed in browser root 'closure'",
              "Simulated cleanup failure");
          assertThat(page.locator("#closure section")).isVisible();
          assertThat(page.locator("[data-closure-log]"))
              .hasAttribute("data-closure-log", "disposed victim,disposed survivor");
          Assertions.assertThat(failures).hasSize(1);
        });
  }

  @BothEngines
  @DisplayName("a host declaring a view its entry lacks is reported and keeps its fallback")
  void hostDeclaringAViewItsEntryLacksIsReportedAndKeepsItsFallback(String engine) {
    withPage(
        engine,
        (page, url, failures) -> {
          page.route(
              url + "/",
              route -> {
                var response = route.fetch();
                route.fulfill(
                    new Route.FulfillOptions()
                        .setResponse(response)
                        .setBody(
                            response
                                .text()
                                .replace(
                                    "data-tessera-view=\"counter\"",
                                    "data-tessera-view=\"missing\"")));
              });
          page.navigate(url + "/");
          page.waitForSelector("#app main", new Page.WaitForSelectorOptions().setTimeout(30_000));
          assertReported(
              page,
              failures,
              "Browser host 'second' declares view 'missing'",
              "available views: [counter]");
          assertThat(page.locator("#second")).hasText("The second entry has not started.");
        });
  }

  private static int ruleCount(Page page) {
    return ((Number)
            page.evaluate(
                "document.adoptedStyleSheets.reduce((rules, sheet) => rules + sheet.cssRules.length, 0)"))
        .intValue();
  }

  private static void startFrameCounter(Page page) {
    page.evaluate(
        """
        () => {
          window.scheduleFrames = 0;
          const tick = () => {
            scheduleFrames++;
            requestAnimationFrame(tick);
          };
          requestAnimationFrame(tick);
        }
        """);
  }

  private static int frameCount(Page page) {
    return ((Number) page.evaluate("window.scheduleFrames")).intValue();
  }

  private static void settleFrames(Page page) {
    page.evaluate(
        "() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))");
  }

  private static void assertSelection(Locator select, List<String> values) {
    assertThat(select).hasValues(values.toArray(String[]::new));
  }

  private static final String SEMANTICS =
      """
      root => {
        const nodes = [root, ...root.querySelectorAll('*')];
        const ids = new Map(nodes.filter(node => node.id).map((node, index) => [node.id, 'id-' + index]));
        return nodes.map(node => ({
          tag: node.localName,
          attributes: Object.fromEntries(Array.from(node.attributes)
            .filter(attribute => !['class', 'data-scope', 'data-scope-id'].includes(attribute.name))
            .map(({name, value}) => [name, ['id', 'for', 'aria-describedby'].includes(name)
              ? value.split(' ').map(id => ids.get(id)).join(' ') : value])),
          text: Array.from(node.childNodes).filter(child => child.nodeType === Node.TEXT_NODE)
            .map(child => child.textContent).join('').trim()
        }));
      }
      """;

  private static final String STYLES =
      """
      root => [root, ...root.querySelectorAll('input, button')].map(node => {
        const style = getComputedStyle(node);
        return Object.fromEntries(['display', 'color', 'background-color', 'border-top-style',
          'font-size', 'font-weight', 'cursor'].map(name => [name, style.getPropertyValue(name)]));
      })
      """;

  private static Locator button(Page page, String name) {
    return page.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  private static void assertOrder(Page page, List<String> expected) {
    assertThat(page.locator("[data-entry]")).hasCount(expected.size());
    page.waitForFunction(
        "expected => JSON.stringify(Array.from(document.querySelectorAll('[data-entry]'), node => node.dataset.entry)) === JSON.stringify(expected)",
        expected);
  }

  private static void clickDetachedBeta(Page page) {
    page.evaluate(
        "betaButton.click(); new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))");
  }

  @BeforeAll
  static void startServer() {
    var site = Path.of(System.getProperty("wasm.site")).toAbsolutePath();
    server =
        SimpleFileServer.createFileServer(
            new InetSocketAddress("127.0.0.1", 0), site, SimpleFileServer.OutputLevel.NONE);
    var policed = server.createContext("/csp/", SimpleFileServer.createFileHandler(site));
    policed
        .getFilters()
        .add(
            Filter.beforeHandler(
                "Content security policy",
                exchange ->
                    exchange
                        .getResponseHeaders()
                        .set("Content-Security-Policy", "style-src 'self'")));
    server.createContext(
        "/static",
        exchange -> {
          var html =
              Compiler.compile(
                      new PageDocument(
                          "Keyed entries",
                          Elements.main()
                              .child(KeyedListDemo.content(List.of("a", "b", "c"), true, false))))
                  .html();
          respond(exchange, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
        });
    var formHtml =
        Compiler.compile(
                new PageDocument(
                    "Portable controls",
                    Elements.main().child(ControlledFormDemo.nativeDefaults())))
            .html();
    server.createContext(
        "/control-defaults",
        exchange ->
            respond(
                exchange, "text/html; charset=utf-8", formHtml.getBytes(StandardCharsets.UTF_8)));
    server.createContext(
        "/slow",
        exchange -> {
          try {
            SLOW.await(10, TimeUnit.SECONDS);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          }
          var bytes = "late".getBytes(StandardCharsets.UTF_8);
          try {
            exchange.sendResponseHeaders(200, bytes.length);
            try (var body = exchange.getResponseBody()) {
              body.write(bytes);
            }
          } catch (IOException aborted) {
            exchange.close();
          }
        });
    server.createContext("/text", exchange -> respond(exchange, "text/plain", TEXT));
    server.createContext("/tone.wav", exchange -> respond(exchange, "audio/wav", silence()));
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.start();
    playwright = Playwright.create();
  }

  private static final byte[] TEXT = "hello from tessera".getBytes(StandardCharsets.UTF_8);

  private static void respond(HttpExchange exchange, String type, byte[] body) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.sendResponseHeaders(200, body.length);
    try (var out = exchange.getResponseBody()) {
      out.write(body);
    }
  }

  /// Two seconds of 8 kHz, 8-bit mono PCM silence, so playback advances in every engine.
  private static byte[] silence() {
    var samples = 16_000;
    var wav = ByteBuffer.allocate(44 + samples).order(ByteOrder.LITTLE_ENDIAN);
    wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + samples);
    wav.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
    wav.putShort((short) 1).putShort((short) 1).putInt(8_000).putInt(8_000);
    wav.putShort((short) 1).putShort((short) 8);
    wav.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples);
    for (var index = 0; index < samples; index++) {
      wav.put((byte) 128);
    }
    return wav.array();
  }

  @AfterAll
  static void stopServer() {
    BROWSERS.values().forEach(Browser::close);
    BROWSERS.clear();
    if (playwright != null) {
      playwright.close();
    }
    if (server != null) {
      server.stop(0);
    }
  }

  /// One browser per engine for the class; each test gets an isolated context. Runs every engine
  /// through the same parameterized test.
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"chromium", "firefox"})
  private @interface BothEngines {}

  /// A browser test body; `failures` collects uncaught page errors.
  @FunctionalInterface
  private interface PageTest {
    void run(Page page, String url, List<String> failures);
  }

  private static Locator label(Page page, String text) {
    return page.getByLabel(text, new Page.GetByLabelOptions().setExact(true));
  }

  /// Waits for exactly one reported page error containing every fragment.
  private static void assertReported(Page page, List<String> failures, String... fragments) {
    page.waitForCondition(() -> !failures.isEmpty());
    Assertions.assertThat(failures)
        .singleElement()
        .satisfies(error -> Assertions.assertThat(error).contains(fragments));
  }

  private static int renderCount(Locator renders, String attribute) {
    return Integer.parseInt(renders.getAttribute(attribute));
  }

  private static void withPage(String engine, PageTest test) {
    var browser =
        BROWSERS.computeIfAbsent(
            engine,
            name ->
                (name.equals("chromium") ? playwright.chromium() : playwright.firefox()).launch());
    try (var context = browser.newContext()) {
      var page = context.newPage();
      var failures = new ArrayList<String>();
      page.onPageError(failures::add);
      test.run(page, "http://127.0.0.1:" + server.getAddress().getPort(), failures);
    }
  }
}
