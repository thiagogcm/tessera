package dev.nthings.tessera.render;

import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;

import dev.nthings.tessera.core.ClientBehavior;
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
        context -> {
          for (var behavior : behaviors) {
            context.contributeBehavior(behavior);
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
  @DisplayName("an external behavior becomes an asset referenced by the page")
  void externalBehaviorBecomesAsset() {
    var page =
        PageCompiler.compile(page(BEHAVIOR), new Tokens(), OutputMode.externalAsset("/assets"));

    var asset = page.assets().getFirst();
    assertThat(asset.path()).endsWith(".js");
    assertThat(page.html()).contains("src=\"" + asset.path() + "\"");
  }
}
