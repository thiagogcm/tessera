package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.hr;
import static dev.nthings.tessera.core.Elements.span;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Command;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.Translation;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.BundledBehavior;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Menu of links, command buttons, and custom items, opened from a trigger button. The panel opens
/// below the trigger, flips to another side when it does not fit, and closes on Escape or on an
/// outside click. Create one with [#of(String)] and add [Item]s:
///
/// ```java
/// Dropdown.of("Go to")
///     .item(Dropdown.Item.link("/forms", "Forms").shortcut("G F"))
///     .separator()
///     .item(Dropdown.Item.command("open", inspector, BuiltinCommand.SHOW_MODAL, "Inspect"));
/// ```
///
/// When a command item opens a modal dialog, closing the dialog returns focus to the menu's
/// trigger: a static document then emits the menu as a `tessera-menu` element and loads its script
/// once.
///
/// The trigger, panel and command item ids are prefixed with its [#scopeId(String)]. Without an id,
/// the compiler numbers instances `dropdown-<n>`.
public final class Dropdown extends BuiltinScope<Dropdown> {

  /// One link or command entry of a [Dropdown], added with [Dropdown#item(Item)]. The value is
  /// immutable: [#shortcut(String)] returns a new one.
  @CheckReturnValue
  public static final class Item {
    private final Supplier<TagElement> control;
    private final String label;
    private final @Nullable String shortcut;
    private final boolean command;

    private Item(
        Supplier<TagElement> control, String label, @Nullable String shortcut, boolean command) {
      this.control = control;
      this.label = label;
      this.shortcut = shortcut;
      this.command = command;
    }

    /// A link to `href` showing `label`.
    public static Item link(String href, String label) {
      return new Item(() -> a(href).attr("class", ITEM_CLASS), label, null, false);
    }

    /// A button showing `label` that runs `command` on `target`, under the rules of
    /// [Button#command]. `buttonId` is its DOM id within the menu; reusing an id fails with
    /// `duplicate-id`.
    public static Item command(String buttonId, ElementRef target, Command command, String label) {
      return new Item(
          () ->
              button()
                  .id(ElementRef.scoped(buttonId))
                  .attr("class", ITEM_CLASS)
                  .command(target, command),
          label,
          null,
          true);
    }

    /// The same item with `hint`, such as `G F`, shown at its end. The hint is visual only and is
    /// hidden from assistive technology.
    public Item shortcut(String hint) {
      return new Item(control, label, hint, command);
    }

    TagElement toElement() {
      var element = control.get().child(span().text(label));
      if (shortcut != null) {
        element.child(span().attr("class", "dropdown-hint").ariaHidden(true).text(shortcut));
      }
      return element;
    }
  }

  private static final String ITEM_CLASS = "dropdown-item";

  private final String label;
  private final List<Element> items = new ArrayList<>();
  private boolean hasCommands;
  private ElementRef panel = ElementRef.scoped("panel");

  private Dropdown(String label) {
    this.label = label;
  }

  /// Creates an empty menu whose trigger button shows `label`, which also names it.
  public static Dropdown of(String label) {
    return new Dropdown(label);
  }

  /// Adds a link or command item built with [Item#link] or [Item#command].
  public Dropdown item(Item item) {
    items.add(item.toElement());
    hasCommands |= item.command;
    return this;
  }

  /// Adds a custom item. Give it the `dropdown-item` class to get the menu item styling.
  public Dropdown item(IntoElement customItem) {
    items.add(customItem.intoElement());
    return this;
  }

  /// Adds a horizontal rule between items.
  public Dropdown separator() {
    items.add(
        hr().border(Border.none())
            .borderBlockStart(Border.hairline(Tokens.BORDER_SUBTLE))
            .my(Tokens.SPACE_1));
    return this;
  }

  @Override
  public Dropdown self() {
    return this;
  }

  @Override
  String scopeName() {
    return "dropdown";
  }

  @Override
  void configureScope(ScopeStyle scope) {
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
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .fontWeight(600)
                    .cursor(Cursor.POINTER)
                    .on(State.HOVER, h -> h.bg(Tokens.SURFACE_SUBTLE)))
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
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
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
                    .inlineSize(CssLength.pct(100))
                    .px(Tokens.SPACE_3)
                    .py(Tokens.SPACE_2)
                    .rounded(Tokens.RADIUS_MD)
                    .bg(CssColor.transparent())
                    .textColor(Tokens.TEXT_PRIMARY)
                    .fontSize(Tokens.FONT_SIZE_SM)
                    .border(Border.none())
                    .textAlign(TextAlign.START)
                    .cursor(Cursor.POINTER)
                    .textDecorationNone()
                    .on(State.HOVER, h -> h.bg(Tokens.SURFACE_SUBTLE)))
        .select(
            ":scope .dropdown-hint",
            s -> s.textColor(Tokens.TEXT_MUTED).fontSize(Tokens.FONT_SIZE_XS).ms(CssLength.auto()))
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(
                        ":scope > [popover]",
                        s ->
                            s.opacity(0)
                                .translate(Translation.y(CssLength.rem(-0.25)))
                                .transition(
                                    Transition.of(
                                            CssDuration.ms(150),
                                            Easing.easeOut(),
                                            Animatable.OPACITY,
                                            Animatable.TRANSLATE,
                                            Animatable.DISPLAY,
                                            Animatable.OVERLAY)
                                        .allowingDiscrete()))
                    .select(
                        ":scope > [popover]:popover-open",
                        s ->
                            s.opacity(1)
                                .translate(Translation.none())
                                .startingStyle(start ->
                                    start
                                        .opacity(0)
                                        .translate(Translation.y(CssLength.rem(-0.25))))));
  }

  /// Returns the ref bound to the popover panel, for use as a command target outside the menu.
  public ElementRef panelRef() {
    return panel;
  }

  /// Binds the panel to `panel` instead of a generated ref. Refs compare by identity, so pass the
  /// same ref in every render that targets the panel.
  public Dropdown panelRef(ElementRef panel) {
    this.panel = panel;
    return this;
  }

  @Override
  TagElement expand(ViewContext context) {
    var trigger =
        button()
            .id(ElementRef.scoped("trigger"))
            .attr("class", "dropdown-trigger")
            .command(panel, BuiltinCommand.TOGGLE_POPOVER)
            .children(
                span().text(label), span().ariaHidden(true).textColor(Tokens.TEXT_MUTED).text("▾"));

    var itemsWrapper = div().attr("class", "dropdown-panel-items");
    itemsWrapper.children(items);

    var root = div();
    if (hasCommands) {
      // The bundled behavior emits the host as `<tessera-menu>`.
      AuthoringAccess.tag(root).behavior = BundledBehavior.MENU;
    }
    return root.children(trigger, div().id(panel).popover(PopoverMode.AUTO).child(itemsWrapper));
  }
}
