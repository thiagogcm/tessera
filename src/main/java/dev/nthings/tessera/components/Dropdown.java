package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.hr;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Viewport-aware popover action dropdown backed by Declarative Invoker Commands (`commandfor` /
/// `command="toggle-popover"`), implicit CSS Anchor Positioning (`position-area: block-end
/// span-inline-end`, `position-try-fallbacks: flip-block, flip-inline, flip-block flip-inline`),
/// and `@starting-style` top-layer entry/exit transitions.
public final class Dropdown implements RenderView {

  private final ElementId id;
  private final String triggerLabel;
  private final List<Element> items = new ArrayList<>();

  public Dropdown(String id, String triggerLabel) {
    this.id = ElementId.of(id);
    this.triggerLabel = triggerLabel;
  }

  public static Dropdown create(String id, String triggerLabel) {
    return new Dropdown(id, triggerLabel);
  }

  public Dropdown linkItem(String href, String label, @Nullable String shortcutHint) {
    var link =
        a(href)
            .attr("class", "dropdown-item")
            .child(span().text(label))
            .whenPresent(
                shortcutHint,
                (el, hint) ->
                    el.child(span().attr("class", "dropdown-hint").ariaHidden(true).text(hint)));
    items.add(link);
    return this;
  }

  public Dropdown commandItem(
      String buttonId, ElementRef target, BuiltinCommand command, String label) {
    var btn =
        button()
            .id(buttonId)
            .attr("class", "dropdown-item")
            .command(target, command)
            .child(span().text(label));
    items.add(btn);
    return this;
  }

  public Dropdown item(IntoElement customItem) {
    items.add(customItem.intoElement());
    return this;
  }

  public Dropdown separator() {
    items.add(
        hr().borderNone().borderBlockStartSubtle(Tokens.BORDER_SUBTLE).my(CssLength.rem(0.25)));
    return this;
  }

  @Override
  public ElementId entityId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.inlineFlex().relative())
        .select(
            ":scope > .dropdown-trigger",
            s ->
                s.inlineFlex()
                    .itemsCenter()
                    .gap(CssLength.rem(0.5))
                    .px(CssLength.rem(0.875))
                    .py(CssLength.rem(0.5))
                    .rounded(CssLength.rem(0.5))
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .fontWeight(600)
                    .cursorPointer())
        .select(":scope > .dropdown-trigger:hover", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope > [popover]",
            s ->
                s.positionArea("block-end span-inline-end")
                    .inset("auto")
                    .positionTryFallbacks("flip-block, flip-inline, flip-block flip-inline")
                    .mt(CssLength.rem(0.375))
                    .minInlineSize(CssLength.rem(14))
                    .p(CssLength.rem(0.375))
                    .rounded(CssLength.rem(0.625))
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .shadow("0 12px 32px -4px oklch(0% 0 0 / 0.18)"))
        .select(":scope .dropdown-panel-items", s -> s.flex().flexCol().gap(CssLength.rem(0.125)))
        .select(
            ":scope .dropdown-item",
            s ->
                s.flex()
                    .itemsCenter()
                    .justifyBetween()
                    .gap(CssLength.rem(1))
                    .fullWidth()
                    .px(CssLength.rem(0.75))
                    .py(CssLength.rem(0.5))
                    .rounded(CssLength.rem(0.375))
                    .bgRaw("transparent")
                    .textColor(Tokens.TEXT_PRIMARY)
                    .fontSize(CssLength.rem(0.875))
                    .borderNone()
                    .textAlign("start")
                    .cursorPointer()
                    .textDecorationNone())
        .select(":scope .dropdown-item:hover", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope .dropdown-hint",
            s -> s.textColor(Tokens.TEXT_MUTED).fontSize(CssLength.rem(0.75)).ms(CssLength.auto()))
        .motionSafe(
            m ->
                m.select(
                        ":scope > [popover]",
                        s ->
                            s.opacity(0)
                                .translate("0 -0.25rem")
                                .transitionProperty("opacity, translate, display, overlay")
                                .transitionDuration("0.15s")
                                .transitionTimingFunction("ease-out")
                                .transitionAllowDiscrete())
                    .selectWithStartingStyle(
                        ":scope > [popover]:popover-open",
                        s -> s.opacity(1).translate("0 0"),
                        start -> start.opacity(0).translate("0 -0.25rem")));
  }

  @Override
  public Element render() {
    var panel = ElementRef.named("panel");
    var trigger =
        button()
            .id("trigger")
            .attr("class", "dropdown-trigger")
            .command(panel, BuiltinCommand.TOGGLE_POPOVER)
            .children(
                span().text(triggerLabel),
                span().ariaHidden(true).textColor(Tokens.TEXT_MUTED).text("▾"));

    var itemsWrapper = div().attr("class", "dropdown-panel-items");
    itemsWrapper.children(items);

    return div().children(trigger, div().id(panel).popover(PopoverMode.AUTO).child(itemsWrapper));
  }
}
