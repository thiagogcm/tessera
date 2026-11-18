package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.render;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.support.CssBlocks;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Compiler")
class CompilerTest {

  @Nested
  @DisplayName("workspace view output")
  class WorkspaceView {

    private static RenderedPage page;

    @BeforeAll
    static void compileView() {
      var document = new PageDocument("en", "Tessera Workspace", new WorkspacePanelView("core"));
      page = RenderedPage.of(Compiler.compile(document, Theme.defaults(), OutputMode.inline()));
    }

    @Test
    @DisplayName("inlines its styles, keyed by the scope's data-scope value")
    void inlinesScopedStyles() {
      var view = page.element("main[data-scope-id=workspace-core]");

      assertThat(page.compiled().assets()).isEmpty();
      assertThat(page.element("head > style").data()).contains(page.css());
      assertThat(page.css()).contains("@scope ([data-scope=\"" + view.attr("data-scope") + "\"])");
    }

    @Test
    @DisplayName("container-query rules sit in the container layer behind a generated class")
    void layersContainerQueries() {
      var card = page.element("main[data-scope-id=workspace-core] section");
      var cqClass = CssBlocks.generatedClass(card.className(), "cq-");
      var container = CssBlocks.block(page.css(), "@container metric-card (inline-size >= 320px){");

      assertThat(container).contains("." + cqClass + "{").contains("flex-direction:row");
    }

    @Test
    @DisplayName("resolves invoker commands to ids in the enclosing scope")
    void resolvesScopedInvokerCommands() {
      assertThat(page.elementById("workspace-core--open-archive-btn"))
          .hasAttribute("commandfor", "workspace-core--confirm-archive")
          .hasAttribute("command", "show-modal");
    }

    @Test
    @DisplayName("renders the interest tooltip as the trigger's adjacent sibling, never inside it")
    void rendersTooltipBesideTrigger() {
      var trigger = page.elementById("workspace-core--open-archive-btn");
      var tooltip = trigger.nextElementSibling();

      assertThat(trigger.select("[popover]")).isEmpty();
      assertThat(tooltip)
          .hasId(trigger.attr("interestfor"))
          .hasAttribute("popover", "hint")
          .hasAttribute("role", "tooltip");
      assertThat(tooltip.text()).isEqualTo("Open archive confirmation dialog");
    }

    @Test
    @DisplayName("runs a custom command as a namespaced command with payload attributes")
    void runsCustomCommandWithPayload() {
      assertThat(page.elementById("workspace-core--dispatch-custom-btn"))
          .hasAttribute("commandfor", "workspace-core--archive-controller")
          .hasAttribute("command", "--workspace-archive")
          .hasAttribute("data-project-id", "core");
    }

    @Test
    @DisplayName("hoists a deferred element to the end of <body>, keeping its lexical scoped id")
    void hoistsDeferredWithScopedId() {
      var bodyChildren = page.dom().body().children();

      assertThat(bodyChildren).extracting(e -> e.tagName()).containsExactly("main", "dialog");
      assertThat(bodyChildren.get(1)).hasId("workspace-core--confirm-archive");
    }
  }

  @Nested
  @DisplayName("encoding untrusted content")
  class Encoding {

    private static RenderedPage page;

    @BeforeAll
    static void compileUntrustedContent() {
      page =
          RenderedPage.of(
              new PageDocument(
                  "Q&A <draft>",
                  main()
                      .children(
                          p().attr("title", "say \"hi\"").text("<script>alert(1)</script>"))));
    }

    @Test
    @DisplayName("encodes the document title")
    void encodesDocumentTitle() {
      assertThat(page.html()).contains("<title>Q&amp;A &lt;draft&gt;</title>");
    }

    @Test
    @DisplayName("escapes markup in text content")
    void escapesMarkupInTextContent() {
      assertThat(page.html()).contains("&lt;script&gt;").doesNotContain("<script>alert");
      assertThat(page.element("main p")).hasText("<script>alert(1)</script>");
    }

    @Test
    @DisplayName("escapes quotes in attribute values")
    void escapesQuotesInAttributeValues() {
      assertThat(page.html()).doesNotContain("title=\"say \"hi\"\"");
      assertThat(page.element("main p")).hasAttribute("title", "say \"hi\"");
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

  @Nested
  @DisplayName("external stylesheet output")
  class ExternalStylesheet {

    private static RenderedPage page;

    @BeforeAll
    static void compileWithExternalAsset() {
      var document = new PageDocument("External CSS", main().p(CssLength.rem(2)).text("Hello"));
      page =
          RenderedPage.of(
              Compiler.compile(document, Theme.defaults(), OutputMode.external("/static/assets/")));
    }

    @Test
    @DisplayName("the stylesheet is the first asset, linked from head under the base")
    void publishesLinkedStylesheetAsset() {
      var stylesheet = page.compiled().assets().getFirst();

      assertThat(stylesheet.path()).startsWith("/static/assets/styles-").endsWith(".css");
      assertThat(stylesheet.contentType()).isEqualTo("text/css; charset=utf-8");
      assertThat(stylesheet.textContent()).isEqualTo(page.css());
      assertThat(page.element("head > link[rel=stylesheet]"))
          .hasAttribute("href", stylesheet.path());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"assets", "/assets//nested", "/assets/../nested"})
  @DisplayName("external output rejects a relative, empty-segment, or parent-traversing asset base")
  void externalOutputRejectsInvalidBase(String base) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OutputMode.external(base))
        .withMessage("External asset base must be an absolute path such as /assets: " + base);
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
                end.contributeHead(Elements.el(HtmlTag.META).attr("name", "from-section"));
                return DocumentContent.of(p().attr("data-from-head", "").text("End section"));
              });
          return DocumentContent.of(Elements.el(HtmlTag.META).attr("name", "from-head"));
        };
    DocumentComponent body =
        context -> {
          context.contributeHead(head);
          context.atDocumentEnd(headOwner, _ -> DocumentContent.of(Elements.empty()));
          return DocumentContent.of(main().child(p().text("Body")));
        };

    var rendered = RenderedPage.of(Compiler.compile(new PageDocument("Order", body)));

    assertThat(rendered.elements("head > meta[name=from-head]")).hasSize(1);
    assertThat(rendered.elements("head > meta[name=from-section]")).hasSize(1);
    assertThat(rendered.elements("body > p[data-from-head]")).hasSize(1);
  }

  @Test
  @DisplayName("a document script keeps its trusted body inside one script element")
  void documentScriptKeepsItsBodyInsideOneScriptElement() {
    var rendered =
        RenderedPage.render(
            DocumentElement.of(main().child(p().text("Body")))
                .child(Documents.script("application/json", "{\"end\":\"</script><b>x</b>\"}")));

    var script = rendered.element("main > script[type=application/json]");
    assertThat(script.data()).isEqualTo("{\"end\":\"<\\/script><b>x</b>\"}");
    assertThat(rendered.elements("main > b")).isEmpty();
  }
}
