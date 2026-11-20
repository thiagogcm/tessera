package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.el;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.pre;
import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.render;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Command;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.support.RenderedPage;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Page output")
class PageOutputTest {

  @ParameterizedTest
  @ValueSource(strings = {"Q&A <draft>", "say \"hi\" & 'bye'", "<script>alert(1)</script>"})
  @DisplayName("untrusted strings round-trip through the title, text and attributes")
  void encodesUntrustedStrings(String value) {
    var page =
        RenderedPage.of(PageDocument.of(value, main(p(value).attr("title", value + "\r\n"))));

    assertThat(page.dom().title()).isEqualTo(value);
    assertThat(page.element("main > p").wholeText()).isEqualTo(value);
    assertThat(page.element("main > p")).hasAttribute("title", value + "\r\n");
    assertThat(page.elements("script")).isEmpty();
    assertThat(page.dom().body().children()).hasSize(1);
  }

  @Test
  @DisplayName("inline output embeds the stylesheet and publishes nothing")
  void inlineOutputEmbedsTheStylesheet() {
    var page = render(main().p(CssLength.rem(2)).text("Hello"));

    assertThat(page.compiled().assets()).isEmpty();
    assertThat(page.elements("link[rel=stylesheet]")).isEmpty();
    assertThat(page.element("head > style").data()).contains(page.css());
  }

  @Test
  @DisplayName("external output links one stylesheet asset under the normalized base")
  void externalOutputLinksTheStylesheetAsset() {
    var compiled =
        Compiler.forPage()
            .withOutputMode(OutputMode.external("/static/./assets/"))
            .compile(PageDocument.of("External", main().p(CssLength.rem(2)).text("Hello")));
    var page = RenderedPage.of(compiled);

    var href = page.element("head > link[rel=stylesheet]").attr("href");
    assertThat(compiled.assets())
        .singleElement()
        .satisfies(asset -> {
          assertThat(asset.path()).isEqualTo(href).startsWith("/static/assets/");
          assertThat(asset.contentType()).startsWith("text/css");
          assertThat(asset.textContent()).isEqualTo(compiled.css());
        });
    assertThat(page.elements("style")).isEmpty();
  }

  @Test
  @DisplayName("external output rejects a relative asset base")
  void externalOutputRejectsRelativeBase() {
    assertThatIllegalArgumentException().isThrownBy(() -> OutputMode.external("assets"));
  }

  @Test
  @DisplayName("page language, theme overrides, and tryCompile results reflect the document")
  void pageLanguageThemeAndTryCompile() {
    var theme = Theme.defaults().with(Tokens.ACCENT_PRIMARY, CssColor.oklch(55, 0.18, 150));
    var doc = PageDocument.of("Localized", main(p("Hi"))).withLang(Locale.forLanguageTag("pt-BR"));

    var ok = Compiler.forPage().withTheme(theme).tryCompile(doc);
    var compiled = ok.orThrow();
    assertThat(compiled.html()).contains("lang=\"pt-BR\"", "Hi");
    assertThat(compiled.css()).contains("oklch(55% 0.18 150)");

    var failed = Compiler.forPage().tryCompile(PageDocument.of("Bad", main(button())));
    switch (failed) {
      case CompilationResult.Compiled<CompiledPage> _ ->
          throw new AssertionError("Expected failure");
      case CompilationResult.Failed<CompiledPage>(var diagnostics) ->
          assertThat(diagnostics).isNotEmpty();
    }
  }

  @Test
  @DisplayName("deferred portals follow priority then sequence")
  void ordersDeferredPortals() {
    var page =
        render(
            DocumentElement.of(div())
                .children(
                    deferred(
                        DocumentElement.of(
                                div().id(ElementRef.scoped("toast-high-1")).text("High 1"))
                            .child(
                                deferred(div().id(ElementRef.scoped("nested")).text("Nested"), 20)),
                        20),
                    deferred(div().id(ElementRef.scoped("modal-low")).text("Low"), 5),
                    deferred(div().id(ElementRef.scoped("toast-high-2")).text("High 2"), 20)));

    var bodyChildren = page.dom().body().children();

    assertThat(bodyChildren).hasSize(5);
    assertThat(bodyChildren.subList(1, 5))
        .extracting(e -> e.id())
        .containsExactly("modal-low", "nested", "toast-high-1", "toast-high-2");
  }

  /// A scope whose invokers target a hoisted dialog and a popover through scoped refs.
  private record Workspace() implements DocumentScope {
    @Override
    public String scopeId() {
      return "workspace";
    }

    @Override
    public DocumentElement expand(DocumentContext context) {
      var confirm = dialog().id(ElementRef.scoped("confirm")).ariaLabel("Archive workspace");
      var controller =
          div().id(ElementRef.scoped("controller")).popover(PopoverMode.AUTO).text("Ready");
      return DocumentElement.of(
              div()
                  .children(
                      button()
                          .command(confirm.ref(), BuiltinCommand.SHOW_MODAL)
                          .interestTooltip(tip -> tip.text("Open the confirmation"))
                          .text("Archive"),
                      button()
                          .command(
                              controller.ref(),
                              Command.custom(
                                  "workspace", "archive", Map.of("data-project-id", "core")))
                          .text("Quick archive"),
                      controller))
          .child(deferred(confirm, 10));
    }
  }

  @Test
  @DisplayName("invokers resolve to their targets, including a dialog hoisted to the body")
  void invokersResolveToTheirTargets() {
    var page = render(DocumentElement.of(main()).child(new Workspace()));
    var open = page.element("button[command=show-modal]");
    var custom = page.element("button[command^=--]");

    assertThat(page.target(open, "commandfor")).hasTagName("dialog");
    assertThat(page.target(open, "commandfor").parent()).isEqualTo(page.dom().body());
    assertThat(page.target(custom, "commandfor")).hasAttribute("popover", "auto");
    assertThat(custom).hasAttribute("data-project-id", "core");
    var tooltip = page.target(open, "interestfor");
    assertThat(tooltip).isEqualTo(open.nextElementSibling());
    assertThat(tooltip).hasAttribute("role", "tooltip").hasText("Open the confirmation");
  }

  @Test
  @DisplayName("head content and end sections may register each other in either order")
  void headContentAndEndSectionsMayRegisterEachOther() {
    var sectionOwner = ViewKey.of("section", Object.class, Object::new);
    var headOwner = ViewKey.of("head", Object.class, Object::new);
    DocumentComponent head =
        context -> {
          context.atDocumentEnd(
              sectionOwner,
              end -> {
                end.contributeHead(el(HtmlTag.META).attr("name", "from-section"));
                return DocumentContent.of(p().attr("data-from-head", "").text("End section"));
              });
          return DocumentContent.of(el(HtmlTag.META).attr("name", "from-head"));
        };
    DocumentComponent body =
        context -> {
          context.contributeHead(head);
          context.atDocumentEnd(headOwner, _ -> DocumentContent.of(Elements.empty()));
          return DocumentContent.of(main().child(p().text("Body")));
        };

    var rendered = RenderedPage.of(PageDocument.of("Order", body));

    assertThat(rendered.elements("head > meta[name=from-head]")).hasSize(1);
    assertThat(rendered.elements("head > meta[name=from-section]")).hasSize(1);
    assertThat(rendered.elements("body > p[data-from-head]")).hasSize(1);
  }

  private static final String WIDGET =
      "customElements.define('test-widget', class extends HTMLElement {});";

  private static PageDocument withScripts(PageScript... scripts) {
    DocumentComponent component =
        context -> {
          for (var script : scripts) {
            context.contributeScript(script);
          }
          return DocumentContent.of(custom("test-widget"));
        };
    return PageDocument.of("Scripts", DocumentElement.of(main()).child(component));
  }

  @Test
  @DisplayName("a page script is emitted once per id, inline by default")
  void emitsInlineScriptOncePerId() {
    var page =
        RenderedPage.of(
            withScripts(
                new PageScript("test-widget", WIDGET),
                new PageScript("test-widget", "console.log('second');")));

    assertThat(page.elements("script"))
        .singleElement()
        .satisfies(script -> assertThat(script.data().strip()).isEqualTo(WIDGET));
    assertThat(page.compiled().assets()).isEmpty();
  }

  @Test
  @DisplayName("repeated portable widgets contribute their bundled behavior once")
  void contributesBundledBehaviorOnce() {
    var page =
        render(
            main()
                .children(
                    ContentTabs.of("Example").tab("one", "One", p().text("First")),
                    ContentTabs.of("Another").tab("two", "Two", p().text("Second"))));

    assertThat(page.elements("script[type=module]")).hasSize(1);
  }

  @Test
  @DisplayName("an external page script is a module asset the page loads by URL")
  void publishesExternalScriptAsset() {
    var compiled =
        Compiler.forPage()
            .withOutputMode(OutputMode.external("/assets"))
            .compile(withScripts(new PageScript("test-widget", WIDGET)));
    var script = RenderedPage.of(compiled).element("script[type=module]");

    assertThat(script.data()).isEmpty();
    assertThat(compiled.assets())
        .filteredOn(asset -> asset.path().equals(script.attr("src")))
        .singleElement()
        .satisfies(asset -> {
          assertThat(asset.contentType()).startsWith("text/javascript");
          assertThat(asset.textContent()).isEqualTo(WIDGET);
        });
  }

  @Test
  @DisplayName("an inline page script cannot close its script element early")
  void inlinePageScriptKeepsItsBodyInsideOneScriptElement() {
    var module = "document.body.innerHTML = '<script></script><b>x</b>';";
    var page = RenderedPage.of(withScripts(new PageScript("injector", module)));

    assertThat(page.elements("script")).hasSize(1);
    assertThat(page.element("script").data().strip())
        .isEqualTo(module.replace("</script", "<\\/script"));
    assertThat(page.elements("b")).isEmpty();
  }

  @Test
  @DisplayName("a document script keeps its trusted body inside one script element")
  void documentScriptKeepsItsBodyInsideOneScriptElement() {
    var page =
        render(
            DocumentElement.of(main().child(p().text("Body")))
                .child(Documents.script("application/json", "{\"end\":\"</script><b>x</b>\"}")));

    assertThat(page.element("main > script[type=application/json]").data())
        .isEqualTo("{\"end\":\"<\\/script><b>x</b>\"}");
    assertThat(page.elements("b")).isEmpty();
  }

  @Test
  @DisplayName("style text is raw CSS, not HTML-encoded text")
  void styleTextIsRawCss() {
    var css = "main > p::after{content:\"&\"}";
    DocumentComponent styled =
        context -> {
          context.contributeHead(el(HtmlTag.STYLE).attr("data-own", "").text(css));
          return DocumentContent.of(main().child(p().text("Body")));
        };

    var page = RenderedPage.of(PageDocument.of("Styled", styled));

    assertThat(page.element("style[data-own]").data()).isEqualTo(css);
  }

  @ParameterizedTest
  @ValueSource(strings = {"\nindented", "\n\nblank first line"})
  @DisplayName("preformatted text keeps a leading line feed")
  void preKeepsLeadingLineFeed(String text) {
    assertThat(render(main().child(pre().text(text))).element("pre").wholeText()).isEqualTo(text);
  }
}
