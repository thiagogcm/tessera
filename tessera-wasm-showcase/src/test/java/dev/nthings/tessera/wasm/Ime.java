package dev.nthings.tessera.wasm;

import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;

/// Input method composition in the focused text control, driven through Chromium's real input
/// method pipeline over the DevTools protocol.
final class Ime {
  private final CDPSession devtools;

  Ime(Session session) {
    var page = session.page();
    devtools = page.context().newCDPSession(page);
  }

  /// Shows provisional `text` in the composition, replacing earlier provisional text.
  void compose(String text) {
    var params = new JsonObject();
    params.addProperty("text", text);
    params.addProperty("selectionStart", text.length());
    params.addProperty("selectionEnd", text.length());
    devtools.send("Input.imeSetComposition", params);
  }

  /// Commits `text`, ending the composition.
  void commit(String text) {
    var params = new JsonObject();
    params.addProperty("text", text);
    devtools.send("Input.insertText", params);
  }
}
