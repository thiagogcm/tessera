package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.authoring.BundledBehavior;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Page scripts")
class PageScriptTest {

  private static final PageScript BEHAVIOR =
      new PageScript(
          "test-widget", "customElements.define('test-widget', class extends HTMLElement {});");

  private static PageDocument page(PageScript... behaviors) {
    DocumentComponent component =
        context -> {
          for (var behavior : behaviors) {
            context.contributeScript(behavior);
          }
          return DocumentContent.of(custom("test-widget"));
        };
    return new PageDocument("Behavior", DocumentElement.of(main()).child(component));
  }

  @Test
  @DisplayName("an inline script is emitted once per page")
  void emitsInlineScriptOnce() {
    var duplicate =
        new PageScript(
            "test-widget", "customElements.define('other-widget', class extends HTMLElement {});");
    var page = Compiler.compile(page(BEHAVIOR, duplicate));
    var rendered = RenderedPage.of(page);

    var script = rendered.element("head > script[type=module]");
    assertThat(rendered.elements("head > script[type=module]")).hasSize(1);
    assertThat(script).doesNotHaveAttribute("src");
    assertThat(script.data().strip()).isEqualTo(BEHAVIOR.module());
    assertThat(rendered.html()).doesNotContain(duplicate.module());
    assertThat(page.assets()).isEmpty();
  }

  @Test
  @DisplayName("a portable element's bundled behavior is contributed once per page")
  void contributesBundledBehaviorOnce() {
    var rendered =
        RenderedPage.render(
            main()
                .children(
                    ContentTabs.of("Example").tab("one", "One", p().text("First")),
                    ContentTabs.of("Another").tab("two", "Two", p().text("Second"))));

    assertThat(rendered.elements("head > script[type=module]")).hasSize(1);
    assertThat(rendered.element("head > script[type=module]").data().strip())
        .isEqualTo(BundledBehavior.TABS.script().module().strip());
  }

  @Test
  @DisplayName("an external script becomes an asset referenced by the page")
  void publishesExternalScriptAsset() {
    var page = Compiler.compile(page(BEHAVIOR), Theme.defaults(), OutputMode.external("/assets"));
    var rendered = RenderedPage.of(page);

    assertThat(page.assets()).hasSize(2);
    assertThat(page.assets().getFirst().path()).startsWith("/assets/styles-").endsWith(".css");
    var script = page.assets().getLast();
    assertThat(script.path()).startsWith("/assets/scripts/test-widget-").endsWith(".js");
    assertThat(script.contentType()).isEqualTo("text/javascript; charset=utf-8");
    assertThat(script.textContent()).isEqualTo(BEHAVIOR.module());
    assertThat(rendered.elements("head > script[type=module]")).hasSize(1);
    assertThat(rendered.element("head > script[type=module]")).hasAttribute("src", script.path());
    assertThat(rendered.elements("head > script:not([src])")).isEmpty();
  }
}
