package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
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
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
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

    /// Creates a trigger button wired via `command="show-modal"` to this modal.
    public TagElement triggerButton(String buttonId, String label) {
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
                      .inlineSize(CssLength.expr("calc(100% - 2rem)"))
                      .p(Tokens.SPACE_6.length())
                      .rounded(Tokens.RADIUS_XL.length())
                      .bg(Tokens.SURFACE_CARD)
                      .textColor(Tokens.TEXT_PRIMARY)
                      .borderSubtle(Tokens.BORDER_SUBTLE)
                      .shadow("0 24px 48px -12px oklch(0% 0 0 / 0.28)"))
          .select(
              ":scope::backdrop", s -> s.bgRaw("oklch(0% 0 0 / 0.45)").backdropFilter("blur(4px)"))
          .motionSafe(
              m ->
                  m.select(
                          ":scope",
                          s -> s.opacity(0).transform("scale(0.95)").motion(Motion.overlayEntry()))
                      .selectWithStartingStyle(
                          ":scope:open",
                          s -> s.opacity(1).transform("scale(1)"),
                          start -> start.opacity(0).transform("scale(0.95)"))
                      .select(
                          ":scope::backdrop",
                          s ->
                              s.bgRaw("oklch(0% 0 0 / 0)")
                                  .backdropFilter("blur(0px)")
                                  .motion(
                                      Motion.overlayEntry()
                                          .properties(
                                              "background, backdrop-filter, display, overlay")))
                      .selectWithStartingStyle(
                          ":scope:open::backdrop",
                          s -> s.bgRaw("oklch(0% 0 0 / 0.45)").backdropFilter("blur(4px)"),
                          start -> start.bgRaw("oklch(0% 0 0 / 0)").backdropFilter("blur(0px)")));
    }

    @Override
    public Element render() {
      var titleRef = ElementRef.named("title");
      var headerEl =
          header()
              .flex()
              .flexCol()
              .gap(Tokens.SPACE_1.length())
              .child(
                  h2().id(titleRef)
                      .fontSize(Tokens.FONT_SIZE_XL.length())
                      .textBalance()
                      .text(title))
              .whenPresent(
                  description,
                  (h, desc) ->
                      h.child(
                          p().textColor(Tokens.TEXT_MUTED)
                              .fontSize(Tokens.FONT_SIZE_SM.length())
                              .text(desc)));

      var contentEl = div().flex().flexCol().gap(Tokens.SPACE_3.length());
      contentEl.children(bodyNodes);

      var closeBtn =
          button()
              .id("close")
              .command(dialog, BuiltinCommand.CLOSE)
              .px(Tokens.SPACE_3_5.length())
              .py(Tokens.SPACE_2.length())
              .rounded(Tokens.RADIUS_MD.length())
              .bg(Tokens.ACCENT_PRIMARY)
              .textColorOn(Tokens.ACCENT_PRIMARY)
              .borderNone()
              .fontWeight(600)
              .cursorPointer()
              .text(closeLabel);

      var footerEl =
          footer()
              .flex()
              .itemsCenter()
              .justifyEnd()
              .gap(Tokens.SPACE_3.length())
              .pt(Tokens.SPACE_2.length());
      footerEl.children(footerActions);
      footerEl.child(closeBtn);

      return Elements.dialog()
          .id(dialog)
          .ariaLabelledBy(titleRef)
          .dialogClosedByAny()
          .child(
              div()
                  .flex()
                  .flexCol()
                  .gap(Tokens.SPACE_5.length())
                  .children(headerEl, contentEl, footerEl));
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

    public TagElement triggerButton(String buttonId, String label) {
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
      var offscreenTranslate = side == DrawerSide.INLINE_START ? "-100% 0" : "100% 0";
      scope
          .root(
              s -> {
                s.my(CssLength.px(0))
                    .blockSize(CssLength.dvh(100))
                    .maxBlockSize(CssLength.dvh(100))
                    .inlineSize(CssLength.expr("min(24rem, 85dvw)"))
                    .p(Tokens.SPACE_6.length())
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .shadow("0 24px 48px -12px oklch(0% 0 0 / 0.32)");
                if (side == DrawerSide.INLINE_START) {
                  s.ms(CssLength.px(0)).me(CssLength.auto());
                } else {
                  s.ms(CssLength.auto()).me(CssLength.px(0));
                }
                return s;
              })
          .select(
              ":scope::backdrop", s -> s.bgRaw("oklch(0% 0 0 / 0.45)").backdropFilter("blur(3px)"))
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
                          s -> s.opacity(1).translate("0 0"),
                          start -> start.opacity(0).translate(offscreenTranslate))
                      .select(
                          ":scope::backdrop",
                          s ->
                              s.bgRaw("oklch(0% 0 0 / 0)")
                                  .motion(
                                      Motion.overlayEntry()
                                          .properties("background, display, overlay")))
                      .selectWithStartingStyle(
                          ":scope:open::backdrop",
                          s -> s.bgRaw("oklch(0% 0 0 / 0.45)"),
                          start -> start.bgRaw("oklch(0% 0 0 / 0)")));
    }

    @Override
    public Element render() {
      var titleRef = ElementRef.named("title");
      var closeBtn =
          button()
              .id("close")
              .command(dialog, BuiltinCommand.CLOSE)
              .px(Tokens.SPACE_3.length())
              .py(Tokens.SPACE_1_5.length())
              .rounded(Tokens.RADIUS_MD.length())
              .borderSubtle(Tokens.BORDER_SUBTLE)
              .bg(Tokens.SURFACE_SUBTLE)
              .textColor(Tokens.TEXT_PRIMARY)
              .cursorPointer()
              .text(closeLabel);

      var headerEl =
          header()
              .flex()
              .itemsCenter()
              .justifyBetween()
              .gap(Tokens.SPACE_4.length())
              .py(Tokens.SPACE_2.length())
              .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
              .children(
                  div()
                      .flex()
                      .flexCol()
                      .gap(Tokens.SPACE_0_5.length())
                      .child(h2().id(titleRef).fontSize(Tokens.FONT_SIZE_LG.length()).text(title))
                      .whenPresent(
                          subtitle,
                          (d, sub) ->
                              d.child(
                                  p().textColor(Tokens.TEXT_MUTED)
                                      .fontSize(Tokens.FONT_SIZE_XS.length())
                                      .text(sub))),
                  closeBtn);

      var contentEl =
          div().flex().flexCol().gap(Tokens.SPACE_4.length()).py(Tokens.SPACE_4.length());
      contentEl.children(bodyNodes);

      return Elements.dialog()
          .id(dialog)
          .ariaLabelledBy(titleRef)
          .dialogClosedByAny()
          .child(
              div().flex().flexCol().blockSize(CssLength.pct(100)).children(headerEl, contentEl));
    }
  }

  private static TagElement showModalTrigger(ElementRef dialog, String buttonId, String label) {
    return button()
        .id(buttonId)
        .command(dialog, BuiltinCommand.SHOW_MODAL)
        .px(Tokens.SPACE_3_5.length())
        .py(Tokens.SPACE_2.length())
        .rounded(Tokens.RADIUS_LG.length())
        .borderSubtle(Tokens.BORDER_SUBTLE)
        .bg(Tokens.SURFACE_SUBTLE)
        .textColor(Tokens.TEXT_PRIMARY)
        .fontWeight(600)
        .cursorPointer()
        .text(label);
  }
}
