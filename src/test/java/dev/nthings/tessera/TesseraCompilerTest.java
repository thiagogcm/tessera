package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.OutputMode;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TesseraCompilerTest {

  /// A container-query card: stacked by default, a row once its container is 320px wide.
  static TagElement metricCard(String label, String value) {
    return section()
        .container("metric-card")
        .onContainer(
            "metric-card", CssLength.px(320), s -> s.flexRow().itemsCenter().justifyBetween())
        .children(span().text(label), h2().text(value));
  }

  record ArchiveProjectAction(String projectId) implements Action {

    @Override
    public String namespace() {
      return "workspace";
    }

    @Override
    public String name() {
      return "archive";
    }

    @Override
    public Map<String, String> payloadAttributes() {
      return Map.of("data-project-id", projectId);
    }
  }

  record WorkspacePanelView(String workspaceSlug) implements RenderView {

    @Override
    public ElementId entityId() {
      return ElementId.of("workspace-" + workspaceSlug);
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(s -> s.grid().gap(CssLength.rem(1.5)));
    }

    @Override
    public Element render() {
      var confirmRef = ElementRef.named("confirm-archive");
      var confirmModal =
          dialog()
              .id(confirmRef)
              .dialogClosedByAny()
              .p(CssLength.rem(1.5))
              .rounded(CssLength.rem(0.75))
              .bg(Tokens.SURFACE_CARD)
              .children(
                  h2().textBalance().text("Archive workspace?"),
                  p().textPretty().text("This will move all metrics to cold storage."),
                  div()
                      .flex()
                      .gap(CssLength.rem(0.75))
                      .children(
                          button()
                              .id("cancel-archive")
                              .command(confirmRef, BuiltinCommand.REQUEST_CLOSE)
                              .text("Cancel")));

      var openModalTrigger =
          button()
              .id("open-archive-btn")
              .command(confirmModal, BuiltinCommand.SHOW_MODAL)
              .interestTooltip(
                  tip ->
                      tip.p(CssLength.rem(0.5))
                          .bg(Tokens.SURFACE_SUBTLE)
                          .text("Open archive confirmation dialog"))
              .px(CssLength.rem(1))
              .py(CssLength.rem(0.5))
              .bg(Tokens.ACCENT_PRIMARY)
              .hover(s -> s.opacity(0.9))
              .focusVisible(s -> s.focusRing(Tokens.ACCENT_PRIMARY))
              .text("Archive Workspace");

      var customActionTarget =
          div().id("archive-controller").popover(PopoverMode.AUTO).text("Archive controller ready");

      var dispatchCustomBtn =
          button()
              .id("dispatch-custom-btn")
              .dispatchAction(customActionTarget, new ArchiveProjectAction(workspaceSlug))
              .text("Quick Archive");

      return main()
          .flex()
          .flexCol()
          .gap(CssLength.rem(1.5))
          .children(
              h1().textBalance().text("Workspace: " + workspaceSlug),
              metricCard("P99 Latency", "14.2 ms"),
              div()
                  .flex()
                  .gap(CssLength.rem(1))
                  .children(openModalTrigger, dispatchCustomBtn, customActionTarget),
              deferred(confirmModal, 10));
    }
  }

  @Test
  @DisplayName("Compiles a view with scoped ids, invokers, tooltips, actions and portals")
  void compilesBaselineDocument() {
    var doc = new PageDocument("en", "Tessera Workspace", new WorkspacePanelView("core"));
    PageCompiler.CompiledPage compiled =
        PageCompiler.compile(doc, new Tokens(), OutputMode.inlineStyle());

    assertThat(compiled.externalCssPath()).isNull();
    Document parsed = Jsoup.parse(compiled.html());
    String css = compiled.cssContent();

    // The view's scoped rules are keyed by its data-view value
    var mainEl = parsed.selectFirst("main[data-view-id=workspace-core]");
    assertThat(css).contains("@scope ([data-view=\"" + mainEl.attr("data-view") + "\"])");

    // onContainer rules land in the container layer, applied through a generated class
    String cqClass = CssBlocks.generatedClass(mainEl.selectFirst("section").className(), "cq-");
    assertThat(CssBlocks.block(css, "@layer container{"))
        .contains("@container metric-card (inline-size >= 320px){." + cqClass + "{");

    // Invoker commands resolve to view-scoped ids
    var triggerBtn = parsed.getElementById("workspace-core--open-archive-btn");
    assertThat(triggerBtn.attr("commandfor")).isEqualTo("workspace-core--confirm-archive");

    // The interest tooltip is an adjacent sibling, never nested inside the <button>
    assertThat(triggerBtn.select("[popover]")).isEmpty();
    var tooltip = triggerBtn.nextElementSibling();
    assertThat(tooltip.id()).isEqualTo(triggerBtn.attr("interestfor"));
    assertThat(tooltip.attr("popover")).isEqualTo("hint");
    assertThat(tooltip.attr("role")).isEqualTo("tooltip");

    // Custom Action invoker: namespaced command plus payload attributes
    var customBtn = parsed.getElementById("workspace-core--dispatch-custom-btn");
    assertThat(customBtn.attr("commandfor")).isEqualTo("workspace-core--archive-controller");
    assertThat(customBtn.attr("command")).isEqualTo("--workspace-archive");
    assertThat(customBtn.attr("data-project-id")).isEqualTo("core");

    // DeferredElement is hoisted to the end of <body> but keeps its lexical view-scoped id
    var bodyChildren = parsed.body().children();
    assertThat(bodyChildren).extracting(e -> e.tagName()).containsExactly("main", "dialog");
    assertThat(bodyChildren.get(1).id()).isEqualTo("workspace-core--confirm-archive");
  }

  @Test
  @DisplayName("Encodes text, attribute values and the document title")
  void escapesUntrustedContent() {
    var doc =
        new PageDocument(
            "Q&A <draft>",
            main().children(p().attr("title", "say \"hi\"").text("<script>alert(1)</script>")));
    String html = PageCompiler.compile(doc).html();

    assertThat(html)
        .contains("<title>Q&amp;A &lt;draft&gt;</title>")
        .contains("&lt;script&gt;")
        .doesNotContain("<script>alert")
        .doesNotContain("title=\"say \"hi\"\"");
    var p = Jsoup.parse(html).selectFirst("main p");
    assertThat(p.attr("title")).isEqualTo("say \"hi\"");
    assertThat(p.text()).isEqualTo("<script>alert(1)</script>");
  }

  @Test
  @DisplayName("Orders deferred portals by priority and sequence")
  void ordersDeferredPortals() {
    var root =
        div()
            .children(
                deferred(div().id("toast-high-1").text("High 1"), 20),
                deferred(div().id("modal-low").text("Low"), 5),
                deferred(div().id("toast-high-2").text("High 2"), 20));

    var compiled = PageCompiler.compile(new PageDocument("Portals", root));
    Document parsed = Jsoup.parse(compiled.html());
    var bodyChildren = parsed.body().children();

    assertThat(bodyChildren).hasSize(4);
    assertThat(bodyChildren.get(1).id()).isEqualTo("modal-low");
    assertThat(bodyChildren.get(2).id()).isEqualTo("toast-high-1");
    assertThat(bodyChildren.get(3).id()).isEqualTo("toast-high-2");
  }

  @Test
  @DisplayName("Emits external hashed stylesheet and head link")
  void emitsExternalHashedStylesheet() {
    var doc = new PageDocument("External CSS", main().p(CssLength.rem(2)).text("Hello"));
    var compiled =
        PageCompiler.compile(doc, new Tokens(), OutputMode.externalAsset("/static/assets/"));

    assertThat(compiled.externalCssPath()).startsWith("/static/assets/styles-").endsWith(".css");
    Document parsed = Jsoup.parse(compiled.html());
    var link = parsed.selectFirst("head > link[rel=stylesheet]");
    assertThat(link).isNotNull();
    assertThat(link.attr("href")).isEqualTo(compiled.externalCssPath());
  }

  @Test
  @DisplayName("Enforces void element and accessibility invariants")
  void enforcesVoidAndAccessibilityRules() {
    assertThatThrownBy(() -> img("/hero.webp", "Hero").child(span().text("Invalid")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("void element");

    var unlabeledBtnDoc = new PageDocument("Invalid Button", div().child(button()));
    assertThatThrownBy(() -> PageCompiler.compile(unlabeledBtnDoc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("must have text content");

    var invalidImgDoc =
        new PageDocument("Invalid Img", div().child(img("/deco.svg", "").dimensions(1, 1)));
    assertThatThrownBy(() -> PageCompiler.compile(invalidImgDoc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("role=\"presentation\"");

    // Decorative images and links labelled by aria-label are accepted
    var validDoc =
        new PageDocument(
            "Valid A11y",
            div()
                .children(
                    img("/deco.svg", "").role("presentation").dimensions(100, 100),
                    a("/home")
                        .ariaLabel("Home page")
                        .child(new TagElement(HtmlTag.SPAN).ariaHidden(true))));
    assertThatCode(() -> PageCompiler.compile(validDoc)).doesNotThrowAnyException();
  }
}
