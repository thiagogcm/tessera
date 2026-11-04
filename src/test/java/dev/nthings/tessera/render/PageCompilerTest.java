package dev.nthings.tessera.render;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.render;

import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.support.CssBlocks;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PageCompiler")
class PageCompilerTest {

  @Nested
  @DisplayName("compiling a view with scoped ids, invokers, tooltips, actions and portals")
  class WorkspaceView {

    private static RenderedPage page;

    @BeforeAll
    static void compileView() {
      var document = new PageDocument("en", "Tessera Workspace", new WorkspacePanelView("core"));
      page =
          RenderedPage.of(PageCompiler.compile(document, new Tokens(), OutputMode.inlineStyle()));
    }

    @Test
    @DisplayName("inlines its styles, so there is no external stylesheet")
    void inlinesItsStyles() {
      assertThat(page.compiled().externalCssPath()).isNull();
      assertThat(page.element("head > style").data()).contains("@scope ([data-view=");
    }

    @Test
    @DisplayName("keys the view's scoped rules by its data-view value")
    void keysScopedRulesByDataViewValue() {
      var view = page.element("main[data-view-id=workspace-core]");

      assertThat(page.css()).contains("@scope ([data-view=\"" + view.attr("data-view") + "\"])");
    }

    @Test
    @DisplayName("puts container-query rules in the container layer behind a generated class")
    void putsContainerQueriesInContainerLayer() {
      var card = page.element("main[data-view-id=workspace-core] section");
      var cqClass = CssBlocks.generatedClass(card.className(), "cq-");

      assertThat(CssBlocks.block(page.css(), "@layer container{"))
          .contains("@container metric-card (inline-size >= 320px){." + cqClass + "{");
    }

    @Test
    @DisplayName("resolves invoker commands to view-scoped ids")
    void resolvesInvokerCommandsToViewScopedIds() {
      assertThat(page.elementById("workspace-core--open-archive-btn"))
          .hasAttribute("commandfor", "workspace-core--confirm-archive");
    }

    @Test
    @DisplayName("renders the interest tooltip as the trigger's adjacent sibling, never inside it")
    void rendersInterestTooltipBesideTrigger() {
      var trigger = page.elementById("workspace-core--open-archive-btn");
      var tooltip = trigger.nextElementSibling();

      assertThat(trigger.select("[popover]")).isEmpty();
      assertThat(tooltip)
          .hasId(trigger.attr("interestfor"))
          .hasAttribute("popover", "hint")
          .hasAttribute("role", "tooltip");
    }

    @Test
    @DisplayName("dispatches a custom Action as a namespaced command with payload attributes")
    void dispatchesCustomActionWithPayload() {
      assertThat(page.elementById("workspace-core--dispatch-custom-btn"))
          .hasAttribute("commandfor", "workspace-core--archive-controller")
          .hasAttribute("command", "--workspace-archive")
          .hasAttribute("data-project-id", "core");
    }

    @Test
    @DisplayName("hoists a deferred element to the end of <body>, keeping its lexical scoped id")
    void hoistsDeferredElementKeepingScopedId() {
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
  @DisplayName("hoists deferred portals to the end of <body>, ordered by priority then sequence")
  void ordersDeferredPortalsByPriorityThenSequence() {
    var page =
        render(
            div()
                .children(
                    deferred(div().id("toast-high-1").text("High 1"), 20),
                    deferred(div().id("modal-low").text("Low"), 5),
                    deferred(div().id("toast-high-2").text("High 2"), 20)));

    var bodyChildren = page.dom().body().children();

    assertThat(bodyChildren).hasSize(4);
    assertThat(bodyChildren.subList(1, 4))
        .extracting(e -> e.id())
        .containsExactly("modal-low", "toast-high-1", "toast-high-2");
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
              PageCompiler.compile(
                  document, new Tokens(), OutputMode.externalAsset("/static/assets/")));
    }

    @Test
    @DisplayName("is emitted at a content-hashed path under the asset prefix")
    void isEmittedAtHashedPath() {
      assertThat(page.compiled().externalCssPath())
          .startsWith("/static/assets/styles-")
          .endsWith(".css");
    }

    @Test
    @DisplayName("is linked from <head>")
    void isLinkedFromHead() {
      assertThat(page.element("head > link[rel=stylesheet]"))
          .hasAttribute("href", page.compiled().externalCssPath());
    }
  }

  @Nested
  @DisplayName("reproducibility")
  class Reproducibility {

    private static TagElement customButton() {
      return button().attr("class", "custom").px(CssLength.rem(1)).text("Go");
    }

    private static TagElement pageWith(TagElement button) {
      return main().children(Callout.note("one"), Callout.tip("two"), button);
    }

    private static String compileHtml(TagElement body) {
      return render(body).html();
    }

    @Test
    @DisplayName("compiling the same tree twice yields identical HTML")
    void compilingSameTreeTwiceYieldsIdenticalHtml() {
      var body = pageWith(customButton());

      assertThat(compileHtml(body)).isEqualTo(compileHtml(body));
    }

    @Test
    @DisplayName("rebuilding an equivalent tree yields identical HTML")
    void rebuildingEquivalentTreeYieldsIdenticalHtml() {
      assertThat(compileHtml(pageWith(customButton())))
          .isEqualTo(compileHtml(pageWith(customButton())));
    }

    @Test
    @DisplayName("compiling never mutates the input tree")
    void compilingNeverMutatesInputTree() {
      var button = customButton();

      compileHtml(pageWith(button));

      assertThat(button.attributes()).containsEntry("class", "custom");
    }
  }
}
