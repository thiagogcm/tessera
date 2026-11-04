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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.OutputMode;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TesseraCompilerTest {

  record MetricCard(String id, String label, String value, boolean highlighted)
      implements RenderOnce {

    @Override
    public Element render() {
      var tokens = RenderContext.current().tokens();
      return section()
          .id("metric-" + (Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, id) & 0xFFFF))
          .container("metric-card")
          .flex()
          .flexCol()
          .gap(CssLength.rem(0.5))
          .p(CssLength.rem(1.25))
          .rounded(CssLength.rem(0.75))
          .bg(Tokens.SURFACE_CARD)
          .borderSubtle(Tokens.BORDER_SUBTLE)
          .when(highlighted, s -> s.focusRing(Tokens.ACCENT_PRIMARY))
          .hover(s -> s.bg(Tokens.SURFACE_SUBTLE))
          .onContainer(
              "metric-card", CssLength.px(320), s -> s.flexRow().itemsCenter().justifyBetween())
          .viewTransitionName("metric-card")
          .children(
              span().textColor(Tokens.TEXT_MUTED).text(label),
              h2().fontSize(
                      CssLength.fluid(CssLength.rem(1.25), CssLength.cqi(4), CssLength.rem(2.0)))
                  .textColor(tokens.all().iterator().next())
                  .textBalance()
                  .text(value));
    }
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

  record WorkspacePanelView(String workspaceSlug, List<MetricCard> metrics) implements RenderView {

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
              div()
                  .childrenWindowed(
                      metrics,
                      2,
                      pair ->
                          div()
                              .gridAutoFit(CssLength.rem(16))
                              .gap(CssLength.rem(1))
                              .children(pair)),
              div()
                  .flex()
                  .gap(CssLength.rem(1))
                  .children(openModalTrigger, dispatchCustomBtn, customActionTarget),
              deferred(confirmModal, 10));
    }
  }

  @Test
  @DisplayName("Compiles three-phase document with baseline 2026 platform capabilities")
  void compilesBaselineDocument() {
    var cards =
        List.of(
            new MetricCard("lat", "P99 Latency", "14.2 ms", true),
            new MetricCard("rps", "Throughput", "48,200 req/s", false),
            new MetricCard("err", "Error Budget", "99.98%", false));

    var view = new WorkspacePanelView("core", cards);
    var doc = new PageDocument("en", "Tessera Workspace", view);

    PageCompiler.CompiledPage compiled = PageCompiler.compile(doc, OutputMode.inlineStyle());

    assertThat(compiled.externalCssPath()).isNull();
    assertThat(compiled.cssContent())
        .startsWith("@layer reset, base, theme, components, utilities, interactive, container;\n")
        .contains("interpolate-size:allow-keywords")
        .contains("@view-transition{navigation:auto}")
        .contains("@function --fluid(")
        .contains("@layer components{@scope ([data-view=\"workspace-panel-view-")
        .contains("@layer utilities{")
        .contains("@layer interactive{")
        .contains("@layer container{@container metric-card (inline-size >= 320px)");

    Document parsed = Jsoup.parse(compiled.html());
    assertThat(parsed.title()).isEqualTo("Tessera Workspace");

    var mainEl =
        parsed.selectFirst("main[data-view^=workspace-panel-view-][data-view-id=workspace-core]");
    assertThat(mainEl).isNotNull();

    // Verify Invoker Commands & view-scoped IDs
    var triggerBtn = parsed.getElementById("workspace-core--open-archive-btn");
    assertThat(triggerBtn).isNotNull();
    assertThat(triggerBtn.attr("commandfor")).isEqualTo("workspace-core--confirm-archive");
    assertThat(triggerBtn.attr("command")).isEqualTo("show-modal");
    assertThat(triggerBtn.attr("interestfor"))
        .isEqualTo("workspace-core--open-archive-btn--tooltip");

    // Verify companion tooltip is emitted as an adjacent sibling (NOT inside <button>)
    assertThat(triggerBtn.select("div[popover=hint]")).isEmpty();
    var nextSibling = triggerBtn.nextElementSibling();
    assertThat(nextSibling).isNotNull();
    assertThat(nextSibling.id()).isEqualTo("workspace-core--open-archive-btn--tooltip");
    assertThat(nextSibling.attr("popover")).isEqualTo("hint");
    assertThat(nextSibling.attr("role")).isEqualTo("tooltip");

    // Verify custom Action invoker command
    var customBtn = parsed.getElementById("workspace-core--dispatch-custom-btn");
    assertThat(customBtn).isNotNull();
    assertThat(customBtn.attr("commandfor")).isEqualTo("workspace-core--archive-controller");
    assertThat(customBtn.attr("command")).isEqualTo("--workspace-archive");
    assertThat(customBtn.attr("data-project-id")).isEqualTo("core");

    // Verify DeferredElement portal is hoisted to end of <body> while keeping lexical view ID
    var bodyChildren = parsed.body().children();
    assertThat(bodyChildren).hasSize(2);
    assertThat(bodyChildren.get(0).tagName()).isEqualTo("main");
    var hoistedDialog = bodyChildren.get(1);
    assertThat(hoistedDialog.tagName()).isEqualTo("dialog");
    assertThat(hoistedDialog.id()).isEqualTo("workspace-core--confirm-archive");
    assertThat(hoistedDialog.attr("closedby")).isEqualTo("any");
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
  @DisplayName("Preserves element immutability across compilations")
  void preservesElementImmutability() {
    TagElement reusableButton =
        button()
            .attr("class", "custom-btn")
            .px(CssLength.rem(1))
            .hover(s -> s.opacity(0.8))
            .text("Reusable Action");

    var doc = new PageDocument("Idempotency", div().child(reusableButton));
    String firstHtml = PageCompiler.compile(doc).html();
    String secondHtml = PageCompiler.compile(doc).html();

    assertThat(secondHtml).isEqualTo(firstHtml);
    assertThat(reusableButton.attributes().get("class")).isEqualTo("custom-btn");
  }

  @Test
  @DisplayName("Emits external hashed stylesheet and head link")
  void emitsExternalHashedStylesheet() {
    var doc = new PageDocument("External CSS", main().p(CssLength.rem(2)).text("Hello"));
    var compiled = PageCompiler.compile(doc, OutputMode.externalAsset("/static/assets/"));

    assertThat(compiled.externalCssPath()).startsWith("/static/assets/styles-").endsWith(".css");
    Document parsed = Jsoup.parse(compiled.html());
    var link = parsed.selectFirst("head > link[rel=stylesheet]");
    assertThat(link).isNotNull();
    assertThat(link.attr("href")).isEqualTo(compiled.externalCssPath());
  }

  @Test
  @DisplayName("Enforces void element and accessibility invariants")
  void enforcesVoidAndAccessibilityRules() {
    // 1. Void element rejects children at construction time
    assertThatThrownBy(() -> img("/hero.webp", "Hero").child(span().text("Invalid")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("HTML5 void element <img> cannot have children");

    // 2. Empty button without accessible label fails in Phase 2 (prepaint)
    var unlabeledBtnDoc = new PageDocument("Invalid Button", div().child(button()));
    assertThatThrownBy(() -> PageCompiler.compile(unlabeledBtnDoc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("Interactive <button> element must have text content");

    // 3. Decorative img with empty alt="" without role=presentation fails in Phase 2
    var invalidImgDoc = new PageDocument("Invalid Img", div().child(img("/deco.svg", "")));
    assertThatThrownBy(() -> PageCompiler.compile(invalidImgDoc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("Decorative <img> with empty alt=\"\"");

    // 4. Dangling commandfor reference fails in Phase 2
    var danglingInvokerDoc =
        new PageDocument(
            "Dangling Invoker",
            div()
                .child(
                    button()
                        .id("trigger")
                        .command(ElementRef.named("missing-dialog"), BuiltinCommand.SHOW_MODAL)
                        .text("Open")));
    assertThatThrownBy(() -> PageCompiler.compile(danglingInvokerDoc))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("commandfor> references ElementRef(missing-dialog)");

    // 5. Valid decorative img and aria-labeled link pass
    var validDoc =
        new PageDocument(
            "Valid A11y",
            div()
                .children(
                    img("/deco.svg", "").role("presentation").dimensions(100, 100),
                    a("/home")
                        .ariaLabel("Home page")
                        .child(new TagElement(HtmlTag.SPAN).ariaHidden(true))));
    assertThat(PageCompiler.compile(validDoc).html()).contains("role=\"presentation\"");
  }
}
