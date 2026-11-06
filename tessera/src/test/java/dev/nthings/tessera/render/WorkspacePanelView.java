package dev.nthings.tessera.render;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.Map;

/// A view that exercises scoped ids, invokers, interest tooltips, custom actions, container
/// queries and deferred portals together. [PageCompilerTest] asserts each feature separately.
record WorkspacePanelView(String workspaceSlug) implements RenderView {

  private static final CssIdent METRIC_CARD = CssIdent.of("metric-card");

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

  /// A container-query card: stacked by default, a row once its container is 320px wide.
  private static TagElement metricCard(String label, String value) {
    return section()
        .containerInlineSize(METRIC_CARD)
        .onContainer(
            METRIC_CARD, CssLength.px(320), s -> s.flexRow().itemsCenter().justifyBetween())
        .children(span().text(label), h2().text(value));
  }

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
            .ariaLabel("Archive workspace")
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
