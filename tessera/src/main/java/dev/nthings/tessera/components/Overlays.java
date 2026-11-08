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
import dev.nthings.tessera.core.RenderContext;
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

/// Modal dialogs and side drawers that open and close without author JavaScript. Create one with
/// [#modal(String, String)] or [#drawer(String, String)], place it on the page (directly or with
/// `asDeferred`), and open it with its `triggerButton` or a button that targets its
/// `dialogRef()`. Each has a close button, and Escape or a click outside dismisses it.
public final class Overlays {

  private Overlays() {}

  /// Edge a [Drawer] docks to. `INLINE_START` is the left edge in left-to-right text.
  public enum DrawerSide {
    INLINE_START,
    INLINE_END
  }

  /// Creates a centered modal with the given `id` and title.
  public static Modal modal(String id, String title) {
    return new Modal(ElementId.of(id), title);
  }

  /// Creates a drawer on the inline-end edge with the given `id` and title.
  public static Drawer drawer(String id, String title) {
    return new Drawer(ElementId.of(id), title, DrawerSide.INLINE_END);
  }

  /// Creates a drawer on `side` with the given `id` and title.
  public static Drawer drawer(String id, String title, DrawerSide side) {
    return new Drawer(ElementId.of(id), title, side);
  }

  /// Centered modal dialog with a title, optional description, body, footer actions and a close
  /// button. Create one with [Overlays#modal(String, String)].
  public static final class Modal implements RenderView {

    private final ElementId id;
    private ElementRef dialog = ElementRef.named("dialog");
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

    /// Adds an action to the footer, before the close button.
    public Modal action(IntoElement action) {
      footerActions.add(action.intoElement());
      return this;
    }

    public Modal closeLabel(String label) {
      this.closeLabel = label;
      return this;
    }

    /// Renders the dialog with `open` on load, so it is visible and non-modal.
    public Modal opened() {
      opened = true;
      return this;
    }

    /// Creates a button that opens this modal with `command="show-modal"`. The button is neutral
    /// and outlined. `buttonId` is local to the enclosing view scope.
    public Button triggerButton(String buttonId, String label) {
      return showModalTrigger(dialog, buttonId, label);
    }

    /// Reference to the `<dialog>`, for invokers placed elsewhere. The modal must still be placed
    /// on the page, directly or through [#asDeferred(int)]; otherwise the compiler reports
    /// `unbound-reference` for the invoker.
    public ElementRef dialogRef() {
      return dialog;
    }

    /// Uses an application-owned reference for the `<dialog>`, so a callback that outlives the
    /// render can still target it. Reuse the same reference on every render.
    public Modal dialogRef(ElementRef dialog) {
      this.dialog = dialog;
      return this;
    }

    /// Hoists this modal to the end of `<body>`. Lower priority values come first among
    /// deferred elements.
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
    public Element render(RenderContext context) {
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

  /// Sheet that slides in from the edge set by its [DrawerSide]. Create one with
  /// [Overlays#drawer(String, String)]; its close button sits in the header.
  public static final class Drawer implements RenderView {

    private final ElementId id;
    private ElementRef dialog = ElementRef.named("dialog");
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

    /// Creates a button that opens this drawer with `command="show-modal"`. The button is neutral
    /// and outlined. `buttonId` is local to the enclosing view scope.
    public Button triggerButton(String buttonId, String label) {
      return showModalTrigger(dialog, buttonId, label);
    }

    /// Reference to the `<dialog>`, for invokers placed elsewhere. The drawer must still be placed
    /// on the page, directly or through [#asDeferred(int)], or the invoker reports
    /// `unbound-reference`.
    public ElementRef dialogRef() {
      return dialog;
    }

    /// Uses an application-owned reference for the `<dialog>`; see [Modal#dialogRef(ElementRef)].
    public Drawer dialogRef(ElementRef dialog) {
      this.dialog = dialog;
      return this;
    }

    /// Hoists this drawer to the end of `<body>`. Lower priority values come first among
    /// deferred elements.
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
    public Element render(RenderContext context) {
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
