package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ClosedBy;
import dev.nthings.tessera.core.Command;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.document.Documents;
import java.util.Map;

/// Exercises scoped ids, invokers, interest tooltips, custom commands, container queries and
/// portals in one scope. [CompilerTest] asserts each feature separately.
record WorkspacePanelView(String workspaceSlug) implements DocumentScope {

  private static final Container METRIC_CARD =
      Container.named("metric-card", Container.Type.INLINE_SIZE);

  /// A container-query card: stacked by default, a row once its container is 320px wide.
  private static TagElement metricCard(String label, String value) {
    return section()
        .container(METRIC_CARD)
        .on(METRIC_CARD.atLeast(CssLength.px(320)), s -> s.flexRow().itemsCenter().justifyBetween())
        .children(span().text(label), h2().text(value));
  }

  @Override
  public String scopeId() {
    return "workspace-" + workspaceSlug;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope.root(s -> s.grid().gap(CssLength.rem(1.5)));
  }

  @Override
  public DocumentElement expand(DocumentContext context) {
    var confirmRef = ElementRef.scoped("confirm-archive");
    var confirmModal =
        dialog()
            .id(confirmRef)
            .ariaLabel("Archive workspace")
            .closedBy(ClosedBy.ANY)
            .p(CssLength.rem(1.5))
            .rounded(CssLength.rem(0.75))
            .bg(Tokens.SURFACE_CARD)
            .children(
                h2().textWrap(TextWrap.BALANCE).text("Archive workspace?"),
                p().textWrap(TextWrap.PRETTY).text("This will move all metrics to cold storage."),
                div()
                    .flex()
                    .gap(CssLength.rem(0.75))
                    .children(
                        button()
                            .id(ElementRef.scoped("cancel-archive"))
                            .command(confirmRef, BuiltinCommand.REQUEST_CLOSE)
                            .text("Cancel")));

    var openModalTrigger =
        button()
            .id(ElementRef.scoped("open-archive-btn"))
            .command(confirmModal.ref(), BuiltinCommand.SHOW_MODAL)
            .interestTooltip(tip ->
                tip.p(CssLength.rem(0.5))
                    .bg(Tokens.SURFACE_SUBTLE)
                    .text("Open archive confirmation dialog"))
            .px(CssLength.rem(1))
            .py(CssLength.rem(0.5))
            .bg(Tokens.ACCENT_PRIMARY)
            .on(State.HOVER, s -> s.opacity(0.9))
            .on(State.FOCUS_VISIBLE, s -> s.focusRing(Tokens.ACCENT_PRIMARY))
            .text("Archive Workspace");

    var customActionTarget =
        div()
            .id(ElementRef.scoped("archive-controller"))
            .popover(PopoverMode.AUTO)
            .text("Archive controller ready");

    var dispatchCustomBtn =
        button()
            .id(ElementRef.scoped("dispatch-custom-btn"))
            .command(
                customActionTarget.ref(),
                Command.custom("workspace", "archive", Map.of("data-project-id", workspaceSlug)))
            .text("Quick Archive");

    return DocumentElement.of(
            main()
                .flex()
                .flexCol()
                .gap(CssLength.rem(1.5))
                .children(
                    h1().textWrap(TextWrap.BALANCE).text("Workspace: " + workspaceSlug),
                    metricCard("P99 Latency", "14.2 ms"),
                    div()
                        .flex()
                        .gap(CssLength.rem(1))
                        .children(openModalTrigger, dispatchCustomBtn, customActionTarget)))
        .child(Documents.deferred(confirmModal, 10));
  }
}
