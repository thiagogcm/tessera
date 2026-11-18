package dev.nthings.tessera.wasm;

import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Locator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Input method composition in the focused text control.
///
/// Chromium composes through its real input method pipeline over the DevTools protocol. Playwright
/// cannot drive Firefox's input method, so there the same composition is dispatched as script
/// events in the order the UI Events specification gives: the last `input` of a composition still
/// reports `isComposing`, and `compositionend` follows it.
final class Ime {
  private static final String COMPOSE =
      """
      (node, [text, commit]) => {
        if (!node.imeState) {
          node.dispatchEvent(new CompositionEvent('compositionstart', {bubbles: true, data: ''}));
          node.imeState = {
            prefix: node.value.slice(0, node.selectionStart),
            suffix: node.value.slice(node.selectionEnd)
          };
        }
        const {prefix, suffix} = node.imeState;
        node.dispatchEvent(new CompositionEvent('compositionupdate', {bubbles: true, data: text}));
        node.value = prefix + text + suffix;
        node.setSelectionRange(prefix.length + text.length, prefix.length + text.length);
        node.dispatchEvent(new InputEvent('input', {
          bubbles: true, isComposing: true, inputType: 'insertCompositionText', data: text}));
        if (commit) {
          delete node.imeState;
          node.dispatchEvent(new CompositionEvent('compositionend', {bubbles: true, data: text}));
        }
      }
      """;

  private final @Nullable CDPSession devtools;

  Ime(Session session) {
    var page = session.page();
    devtools = session.chromium() ? page.context().newCDPSession(page) : null;
  }

  /// Shows provisional `text` in the composition, replacing earlier provisional text.
  void compose(Locator control, String text) {
    if (devtools != null) {
      var params = new JsonObject();
      params.addProperty("text", text);
      params.addProperty("selectionStart", text.length());
      params.addProperty("selectionEnd", text.length());
      devtools.send("Input.imeSetComposition", params);
    } else {
      control.evaluate(COMPOSE, List.of(text, false));
    }
  }

  /// Commits `text`, ending the composition.
  void commit(Locator control, String text) {
    if (devtools != null) {
      var params = new JsonObject();
      params.addProperty("text", text);
      devtools.send("Input.insertText", params);
    } else {
      control.evaluate(COMPOSE, List.of(text, true));
    }
  }
}
