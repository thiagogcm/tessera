package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.header;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.Translation;

/// Modal `<dialog>` sheet that slides in from an edge, with a title, optional description, body
/// and a close button in its header. Create one with [#of(String)]; it docks to the inline-end
/// edge unless [#side(Side)] changes it. Place it and open it as a [Modal].
///
/// The dialog, title and close button ids are prefixed with its [#id(String)]. Without an id, the
/// compiler numbers instances `drawer-start-<n>` or `drawer-end-<n>`.
public final class Drawer extends DialogView<Drawer> {

  /// Edge a drawer docks to. `INLINE_START` is the left edge in left-to-right text.
  public enum Side {
    INLINE_START,
    INLINE_END
  }

  private Side side = Side.INLINE_END;

  private Drawer(String title) {
    super(title);
  }

  /// Creates a drawer on the inline-end edge whose heading, which also names the dialog, is
  /// `title`.
  public static Drawer of(String title) {
    return new Drawer(title);
  }

  /// Sets the edge the drawer docks to. The default is `INLINE_END`.
  public Drawer side(Side side) {
    this.side = side;
    return this;
  }

  @Override
  public Drawer self() {
    return this;
  }

  @Override
  String viewType() {
    return side == Side.INLINE_START ? "drawer-start" : "drawer-end";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    var offscreenTranslate =
        Translation.of(CssLength.pct(side == Side.INLINE_START ? -100 : 100), CssLength.zero());
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
                  .border(Border.hairline(Tokens.BORDER_SUBTLE))
                  .shadow(
                      Shadow.drop(
                          CssLength.px(24),
                          CssLength.px(48),
                          CssLength.px(-12),
                          CssColor.oklch(0, 0, 0, 0.32)));
              if (side == Side.INLINE_START) {
                s.ms(CssLength.px(0)).me(CssLength.auto());
              } else {
                s.ms(CssLength.auto()).me(CssLength.px(0));
              }
            })
        .select(":scope::backdrop", backdrop(CssLength.px(3)))
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(
                        ":scope",
                        s ->
                            s.opacity(0)
                                .translate(offscreenTranslate)
                                .transition(Transition.overlayEntry()))
                    .select(
                        ":scope:open",
                        s ->
                            s.opacity(1)
                                .translate(Translation.none())
                                .startingStyle(
                                    start -> start.opacity(0).translate(offscreenTranslate)))
                    .select(
                        ":scope::backdrop",
                        s ->
                            s.bg(CssColor.oklch(0, 0, 0, 0))
                                .transition(
                                    Transition.overlayEntry()
                                        .properties(
                                            Animatable.BACKGROUND,
                                            Animatable.DISPLAY,
                                            Animatable.OVERLAY)))
                    .select(
                        ":scope:open::backdrop",
                        s ->
                            s.bg(CssColor.oklch(0, 0, 0, 0.45))
                                .startingStyle(start -> start.bg(CssColor.oklch(0, 0, 0, 0)))));
  }

  @Override
  IntoElement render(RenderContext context) {
    var titleRef = ElementRef.scoped("title");
    var closeBtn = closeButton().tone(Tone.NEUTRAL).variant(Button.Variant.OUTLINE).size(Size.SM);

    var headerEl =
        header()
            .flex()
            .itemsCenter()
            .justifyBetween()
            .gap(Tokens.SPACE_4)
            .py(Tokens.SPACE_2)
            .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE))
            .children(
                div()
                    .flex()
                    .flexCol()
                    .gap(Tokens.SPACE_0_5)
                    .child(heading(titleRef).fontSize(Tokens.FONT_SIZE_LG))
                    .whenPresent(
                        description,
                        (d, sub) ->
                            d.child(
                                p().textColor(Tokens.TEXT_MUTED)
                                    .fontSize(Tokens.FONT_SIZE_XS)
                                    .text(sub))),
                closeBtn);

    var contentEl = div().flex().flexCol().gap(Tokens.SPACE_4).py(Tokens.SPACE_4);
    contentEl.children(body);

    return dialogShell(titleRef)
        .child(div().flex().flexCol().blockSize(CssLength.pct(100)).children(headerEl, contentEl));
  }
}
