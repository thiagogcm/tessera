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
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Translation;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Viewport-aware popover action dropdown backed by Declarative Invoker Commands (`commandfor` /
/// `command="toggle-popover"`), implicit CSS Anchor Positioning (`position-area: block-end
/// span-inline-end`, `position-try-fallbacks: flip-block, flip-inline, flip-block flip-inline`),
/// and `@starting-style` top-layer entry/exit transitions.
public final class Dropdown implements RenderView {
  private final ElementRef panel = ElementRef.named("panel");

  private final ElementId id;
  private final String triggerLabel;
  private final List<Element> items = new ArrayList<>();

  private Dropdown(String id, String triggerLabel) {
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
    items.add(hr().borderNone().borderBlockStartSubtle(Tokens.BORDER_SUBTLE).my(Tokens.SPACE_1));
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
                    .gap(Tokens.SPACE_2)
                    .px(Tokens.SPACE_3_5)
                    .py(Tokens.SPACE_2)
                    .rounded(Tokens.RADIUS_LG)
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .fontWeight(600)
                    .cursorPointer())
        .select(":scope > .dropdown-trigger:hover", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope > [popover]",
            s ->
                s.positionArea(
                        PositionArea.of(
                            PositionArea.Part.BLOCK_END, PositionArea.Part.SPAN_INLINE_END))
                    .inset(CssLength.auto())
                    .positionTryFallbacks(
                        PositionTry.flip(PositionTry.Flip.BLOCK),
                        PositionTry.flip(PositionTry.Flip.INLINE),
                        PositionTry.flip(PositionTry.Flip.BLOCK, PositionTry.Flip.INLINE))
                    .mt(Tokens.SPACE_1_5)
                    .minInlineSize(CssLength.rem(14))
                    .p(Tokens.SPACE_1_5)
                    .rounded(Tokens.RADIUS_XL)
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .shadow(
                        Shadow.drop(
                            CssLength.px(12),
                            CssLength.px(32),
                            CssLength.px(-4),
                            CssColor.oklch(0, 0, 0, 0.18))))
        .select(":scope .dropdown-panel-items", s -> s.flex().flexCol().gap(Tokens.SPACE_0_5))
        .select(
            ":scope .dropdown-item",
            s ->
                s.flex()
                    .itemsCenter()
                    .justifyBetween()
                    .gap(Tokens.SPACE_4)
                    .fullWidth()
                    .px(Tokens.SPACE_3)
                    .py(Tokens.SPACE_2)
                    .rounded(Tokens.RADIUS_MD)
                    .bg(CssColor.transparent())
                    .textColor(Tokens.TEXT_PRIMARY)
                    .fontSize(Tokens.FONT_SIZE_SM)
                    .borderNone()
                    .textAlign(TextAlign.START)
                    .cursorPointer()
                    .textDecorationNone())
        .select(":scope .dropdown-item:hover", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope .dropdown-hint",
            s -> s.textColor(Tokens.TEXT_MUTED).fontSize(Tokens.FONT_SIZE_XS).ms(CssLength.auto()))
        .motionSafe(
            m ->
                m.select(
                        ":scope > [popover]",
                        s ->
                            s.opacity(0)
                                .translate(Translation.y(CssLength.rem(-0.25)))
                                .motion(
                                    Motion.of(
                                            CssDuration.ms(150),
                                            Easing.easeOut(),
                                            Animatable.OPACITY,
                                            Animatable.TRANSLATE,
                                            Animatable.DISPLAY,
                                            Animatable.OVERLAY)
                                        .allowingDiscrete()))
                    .selectWithStartingStyle(
                        ":scope > [popover]:popover-open",
                        s -> s.opacity(1).translate(Translation.none()),
                        start -> start.opacity(0).translate(Translation.y(CssLength.rem(-0.25)))));
  }

  /// The popover panel, for opening or closing it from outside this view.
  public ElementRef panelRef() {
    return panel;
  }

  @Override
  public Element render(RenderContext context) {
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
