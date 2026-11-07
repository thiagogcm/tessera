package dev.nthings.tessera.render;

import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;

import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.Tokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Client behaviors")
class ClientBehaviorTest {

  private static final ClientBehavior BEHAVIOR =
      new ClientBehavior(
          "test-widget", "customElements.define('test-widget', class extends HTMLElement {});");

  private static PageDocument page(ClientBehavior... behaviors) {
    RenderOnce component =
        () -> {
          for (var behavior : behaviors) {
            RenderContext.current().contributeBehavior(behavior);
          }
          return custom("test-widget");
        };
    return new PageDocument("Behavior", main().child(component));
  }

  @Test
  @DisplayName("an inline behavior is emitted once per page")
  void inlineBehaviorIsEmittedOnce() {
    var page = PageCompiler.compile(page(BEHAVIOR, BEHAVIOR));

    assertThat(page.html()).containsOnlyOnce("customElements.define");
    assertThat(page.assets()).isEmpty();
  }

  @Test
  @DisplayName("an external behavior becomes a content-hashed asset")
  void externalBehaviorBecomesAsset() {
    var page =
        PageCompiler.compile(page(BEHAVIOR), new Tokens(), OutputMode.externalAsset("/assets"));

    var asset = page.assets().getFirst();
    assertThat(asset.path()).startsWith("/assets/behaviors/test-widget-").endsWith(".js");
    assertThat(page.html())
        .contains("<script type=\"module\" src=\"" + asset.path() + "\"></script>");
  }
}
