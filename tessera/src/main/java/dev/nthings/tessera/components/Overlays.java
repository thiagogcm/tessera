package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.header;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssFilter;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Translation;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Zero-JavaScript top-layer [Modal] and slide-over [Drawer] components backed by `<dialog
/// closedby="any">`, Declarative Invoker Commands (`commandfor` / `command="show-modal"`),
/// `@starting-style`, and `transition-behavior: allow-discrete`.
public final class Overlays {

  private Overlays() {}

  public enum DrawerSide {
    INLINE_START,
    INLINE_END
  }

  public static Modal modal(String id, String title) {
    return new Modal(ElementId.of(id), title);
  }

  public static Drawer drawer(String id, String title) {
    return new Drawer(ElementId.of(id), title, DrawerSide.INLINE_END);
  }

  public static Drawer drawer(String id, String title, DrawerSide side) {
    return new Drawer(ElementId.of(id), title, side);
  }

  /// Top-layer modal dialog (`<dialog closedby="any">`) with smooth `@starting-style` scale/fade
  /// transitions and `::backdrop` dimming.
  public static final class Modal implements RenderView {

    private final ElementId id;
    private final ElementRef dialog = ElementRef.named("dialog");
    private final String title;
    private @Nullable String description;
    private final List<Element> bodyNodes = new ArrayList<>();
    private final List<Element> footerActions = new ArrayList<>();
    private String closeLabel = "Close";
    private boolean opened;

    public Modal(ElementId id, String title) {
      this.id = id;
      this.title = title;
    }

    public Modal description(String desc) {
      this.description = desc;
      return this;
    }

    public Modal body(IntoElement... children) {
      for (var child : children) {
        bodyNodes.add(child.intoElement());
      }
      return this;
    }

    public Modal bodyText(String text) {
      bodyNodes.add(p().textColor(Tokens.TEXT_MUTED).textPretty().text(text));
      return this;
    }

    public Modal action(IntoElement action) {
      footerActions.add(action.intoElement());
      return this;
    }

    public Modal closeLabel(String label) {
      this.closeLabel = label;
      return this;
    }

    /// Shows the dialog when the document loads (`open`). The dialog is visible and non-modal.
    public Modal opened() {
      opened = true;
      return this;
    }

    /// Creates a trigger button wired via `command="show-modal"` to this modal.
    public Button triggerButton(String buttonId, String label) {
      return showModalTrigger(dialog, buttonId, label);
    }

    /// The `<dialog>` element, for invokers placed outside this view.
    public ElementRef dialogRef() {
      return dialog;
    }

    /// Wraps this modal in a [DeferredElement] to hoist it to the end of `<body>`.
    public DeferredElement asDeferred(int priority) {
      return deferred(this, priority);
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.m(CssLength.auto())
                      .maxInlineSize(CssLength.rem(32))
                      .inlineSize(CssLength.pct(100).minus(CssLength.rem(2)))
                      .p(Tokens.SPACE_6)
                      .rounded(Tokens.RADIUS_XL)
                      .bg(Tokens.SURFACE_CARD)
                      .textColor(Tokens.TEXT_PRIMARY)
                      .borderSubtle(Tokens.BORDER_SUBTLE)
                      .shadow(
                          Shadow.drop(
                              CssLength.px(24),
                              CssLength.px(48),
                              CssLength.px(-12),
                              CssColor.oklch(0, 0, 0, 0.28))))
          .select(
              ":scope::backdrop",
              s ->
                  s.bg(CssColor.oklch(0, 0, 0, 0.45))
                      .backdropFilter(CssFilter.blur(CssLength.px(4))))
          .motionSafe(
              m ->
                  m.select(
                          ":scope",
                          s ->
                              s.opacity(0)
                                  .transform(Transform.scale(0.95))
                                  .motion(Motion.overlayEntry()))
                      .selectWithStartingStyle(
                          ":scope:open",
                          s -> s.opacity(1).transform(Transform.scale(1)),
                          start -> start.opacity(0).transform(Transform.scale(0.95)))
                      .select(
                          ":scope::backdrop",
                          s ->
                              s.bg(CssColor.oklch(0, 0, 0, 0))
                                  .backdropFilter(CssFilter.blur(CssLength.px(0)))
                                  .motion(
                                      Motion.overlayEntry()
                                          .properties(
                                              Animatable.BACKGROUND,
                                              Animatable.BACKDROP_FILTER,
                                              Animatable.DISPLAY,
                                              Animatable.OVERLAY)))
                      .selectWithStartingStyle(
                          ":scope:open::backdrop",
                          s ->
                              s.bg(CssColor.oklch(0, 0, 0, 0.45))
                                  .backdropFilter(CssFilter.blur(CssLength.px(4))),
                          start ->
                              start
                                  .bg(CssColor.oklch(0, 0, 0, 0))
                                  .backdropFilter(CssFilter.blur(CssLength.px(0)))));
    }

    @Override
    public Element render() {
      var titleRef = ElementRef.named("title");
      var headerEl =
          header()
              .flex()
              .flexCol()
              .gap(Tokens.SPACE_1)
              .child(h2().id(titleRef).fontSize(Tokens.FONT_SIZE_XL).textBalance().text(title))
              .whenPresent(
                  description,
                  (h, desc) ->
                      h.child(
                          p().textColor(Tokens.TEXT_MUTED)
                              .fontSize(Tokens.FONT_SIZE_SM)
                              .text(desc)));

      var contentEl = div().flex().flexCol().gap(Tokens.SPACE_3);
      contentEl.children(bodyNodes);

      var closeBtn =
          Button.of(closeLabel).id(ElementRef.named("close")).command(dialog, BuiltinCommand.CLOSE);

      var footerEl =
          footer().flex().itemsCenter().justifyEnd().gap(Tokens.SPACE_3).pt(Tokens.SPACE_2);
      footerEl.children(footerActions);
      footerEl.child(closeBtn);

      return Elements.dialog()
          .id(dialog)
          .ariaLabelledBy(titleRef)
          .dialogClosedByAny()
          .boolAttr("open", opened)
          .child(
              div().flex().flexCol().gap(Tokens.SPACE_5).children(headerEl, contentEl, footerEl));
    }
  }

  /// Top-layer slide-over sheet (`<dialog closedby="any">`) docked to the inline-start or
  /// inline-end viewport edge and animated via `translate` + `@starting-style` +
  /// `transition-behavior: allow-discrete`.
  public static final class Drawer implements RenderView {

    private final ElementId id;
    private final ElementRef dialog = ElementRef.named("dialog");
    private final String title;
    private final DrawerSide side;
    private @Nullable String subtitle;
    private final List<Element> bodyNodes = new ArrayList<>();
    private String closeLabel = "Close";

    public Drawer(ElementId id, String title, DrawerSide side) {
      this.id = id;
      this.title = title;
      this.side = side;
    }

    public Drawer subtitle(String sub) {
      this.subtitle = sub;
      return this;
    }

    public Drawer body(IntoElement... children) {
      for (var child : children) {
        bodyNodes.add(child.intoElement());
      }
      return this;
    }

    public Drawer closeLabel(String label) {
      this.closeLabel = label;
      return this;
    }

    /// Creates a trigger button wired via `command="show-modal"` to this drawer.
    public Button triggerButton(String buttonId, String label) {
      return showModalTrigger(dialog, buttonId, label);
    }

    /// The `<dialog>` element, for invokers placed outside this view.
    public ElementRef dialogRef() {
      return dialog;
    }

    public DeferredElement asDeferred(int priority) {
      return deferred(this, priority);
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public String viewType() {
      return side == DrawerSide.INLINE_START ? "drawer-start" : "drawer-end";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      var offscreenTranslate =
          Translation.of(
              CssLength.pct(side == DrawerSide.INLINE_START ? -100 : 100), CssLength.zero());
      scope
          .root(
              s -> {
                s.my(CssLength.px(0))
                    .blockSize(CssLength.dvh(100))
                    .maxBlockSize(CssLength.dvh(100))
                    .inlineSize(CssLength.min(CssLength.rem(24), CssLength.dvw(85)))
                    .p(Tokens.SPACE_6)
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .shadow(
                        Shadow.drop(
                            CssLength.px(24),
                            CssLength.px(48),
                            CssLength.px(-12),
                            CssColor.oklch(0, 0, 0, 0.32)));
                if (side == DrawerSide.INLINE_START) {
                  s.ms(CssLength.px(0)).me(CssLength.auto());
                } else {
                  s.ms(CssLength.auto()).me(CssLength.px(0));
                }
                return s;
              })
          .select(
              ":scope::backdrop",
              s ->
                  s.bg(CssColor.oklch(0, 0, 0, 0.45))
                      .backdropFilter(CssFilter.blur(CssLength.px(3))))
          .motionSafe(
              m ->
                  m.select(
                          ":scope",
                          s ->
                              s.opacity(0)
                                  .translate(offscreenTranslate)
                                  .motion(Motion.overlayEntry()))
                      .selectWithStartingStyle(
                          ":scope:open",
                          s -> s.opacity(1).translate(Translation.none()),
                          start -> start.opacity(0).translate(offscreenTranslate))
                      .select(
                          ":scope::backdrop",
                          s ->
                              s.bg(CssColor.oklch(0, 0, 0, 0))
                                  .motion(
                                      Motion.overlayEntry()
                                          .properties(
                                              Animatable.BACKGROUND,
                                              Animatable.DISPLAY,
                                              Animatable.OVERLAY)))
                      .selectWithStartingStyle(
                          ":scope:open::backdrop",
                          s -> s.bg(CssColor.oklch(0, 0, 0, 0.45)),
                          start -> start.bg(CssColor.oklch(0, 0, 0, 0))));
    }

    @Override
    public Element render() {
      var titleRef = ElementRef.named("title");
      var closeBtn =
          Button.of(closeLabel)
              .id(ElementRef.named("close"))
              .tone(Button.Tone.NEUTRAL)
              .variant(Button.Variant.OUTLINE)
              .size(Button.Size.SM)
              .command(dialog, BuiltinCommand.CLOSE);

      var headerEl =
          header()
              .flex()
              .itemsCenter()
              .justifyBetween()
              .gap(Tokens.SPACE_4)
              .py(Tokens.SPACE_2)
              .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
              .children(
                  div()
                      .flex()
                      .flexCol()
                      .gap(Tokens.SPACE_0_5)
                      .child(h2().id(titleRef).fontSize(Tokens.FONT_SIZE_LG).text(title))
                      .whenPresent(
                          subtitle,
                          (d, sub) ->
                              d.child(
                                  p().textColor(Tokens.TEXT_MUTED)
                                      .fontSize(Tokens.FONT_SIZE_XS)
                                      .text(sub))),
                  closeBtn);

      var contentEl = div().flex().flexCol().gap(Tokens.SPACE_4).py(Tokens.SPACE_4);
      contentEl.children(bodyNodes);

      return Elements.dialog()
          .id(dialog)
          .ariaLabelledBy(titleRef)
          .dialogClosedByAny()
          .child(
              div().flex().flexCol().blockSize(CssLength.pct(100)).children(headerEl, contentEl));
    }
  }

  private static Button showModalTrigger(ElementRef dialog, String buttonId, String label) {
    return Button.of(label)
        .id(ElementRef.named(buttonId))
        .tone(Button.Tone.NEUTRAL)
        .variant(Button.Variant.OUTLINE)
        .command(dialog, BuiltinCommand.SHOW_MODAL);
  }
}
