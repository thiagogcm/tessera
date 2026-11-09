package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.header;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ClosedBy;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssFilter;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.Translation;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Modal `<dialog>` sheet that slides in from an edge, with a title, optional description, body
/// and a close button in its header. Create one with [#of(String)]; it docks to the inline-end
/// edge unless [#side(Side)] changes it. Place it and open it as a [Modal].
public final class Drawer extends BuiltinView {

  /// Edge a drawer docks to. `INLINE_START` is the left edge in left-to-right text.
  public enum Side {
    INLINE_START,
    INLINE_END
  }

  private final String title;
  private ElementRef dialog = ElementRef.scoped("dialog");
  private Side side = Side.INLINE_END;
  private @Nullable String description;
  private final List<Element> body = new ArrayList<>();
  private String closeLabel = "Close";

  private Drawer(String title) {
    this.title = title;
  }

  /// Creates a drawer on the inline-end edge whose heading, which also names the dialog, is
  /// `title`.
  public static Drawer of(String title) {
    return new Drawer(title);
  }

  /// Names this instance in the enclosing view scope; the dialog, title and close button ids are
  /// prefixed with it. Without it, the compiler assigns `drawer-start-<n>` or `drawer-end-<n>`.
  public Drawer id(String id) {
    this.id = id;
    return this;
  }

  /// Sets the edge the drawer docks to. The default is `INLINE_END`.
  public Drawer side(Side side) {
    this.side = side;
    return this;
  }

  /// Sets muted text under the title.
  public Drawer description(String description) {
    this.description = description;
    return this;
  }

  /// Adds content to the body, in order.
  public Drawer body(IntoElement... content) {
    for (var child : content) {
      body.add(child.intoElement());
    }
    return this;
  }

  /// Sets the close button's text. The default is `Close`.
  public Drawer closeLabel(String label) {
    this.closeLabel = label;
    return this;
  }

  /// Creates a neutral, outlined button that opens this drawer with `command="show-modal"`.
  public Button triggerButton(String label) {
    return Button.of(label)
        .tone(Tone.NEUTRAL)
        .variant(Button.Variant.OUTLINE)
        .command(dialog, BuiltinCommand.SHOW_MODAL);
  }

  /// Returns the ref bound to the `<dialog>`, for invokers placed elsewhere. The drawer must still
  /// be placed on the page, directly or through `Elements.deferred`, or the invoker reports
  /// `unbound-reference`.
  public ElementRef dialogRef() {
    return dialog;
  }

  /// Binds the `<dialog>` to `dialog` instead of a generated ref, so a callback that outlives the
  /// render can still target it. Reuse the same ref on every render.
  public Drawer dialogRef(ElementRef dialog) {
    this.dialog = dialog;
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
        .select(
            ":scope::backdrop",
            s ->
                s.bg(CssColor.oklch(0, 0, 0, 0.45)).backdropFilter(CssFilter.blur(CssLength.px(3))))
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
    var closeBtn =
        Button.of(closeLabel)
            .id(ElementRef.scoped("close"))
            .tone(Tone.NEUTRAL)
            .variant(Button.Variant.OUTLINE)
            .size(Size.SM)
            .command(dialog, BuiltinCommand.CLOSE);

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
                    .child(h2().id(titleRef).fontSize(Tokens.FONT_SIZE_LG).text(title))
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

    return Elements.dialog()
        .id(dialog)
        .ariaLabelledBy(titleRef)
        .closedBy(ClosedBy.ANY)
        .child(div().flex().flexCol().blockSize(CssLength.pct(100)).children(headerEl, contentEl));
  }
}
