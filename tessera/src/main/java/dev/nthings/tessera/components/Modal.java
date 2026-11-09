package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.header;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
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
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Transition;
import java.util.ArrayList;
import java.util.List;

/// Centered modal `<dialog>` with a title, optional description, body, footer actions and a close
/// button. It opens and closes without author JavaScript: create one with [#of(String)], place it
/// on the page (directly or with `Elements.deferred`), and open it with its [#triggerButton] or a
/// button that targets its [#dialogRef()]. Escape or a click outside dismisses it.
///
/// The dialog, title and close button ids are prefixed with its [#id(String)], as in
/// `<id>--dialog`. Without an id, the compiler numbers instances `modal-<n>`.
public final class Modal extends DialogView<Modal> {

  private final List<Element> actions = new ArrayList<>();
  private boolean opened;

  private Modal(String title) {
    super(title);
  }

  /// Creates a modal whose heading, which also names the dialog, is `title`.
  public static Modal of(String title) {
    return new Modal(title);
  }

  /// Adds a muted paragraph to the body.
  public Modal bodyText(String text) {
    body.add(p().textColor(Tokens.TEXT_MUTED).textWrap(TextWrap.PRETTY).text(text));
    return this;
  }

  /// Adds buttons or links to the footer, before the close button.
  public Modal actions(IntoElement... actions) {
    for (var action : actions) {
      this.actions.add(action.intoElement());
    }
    return this;
  }

  /// Renders the dialog with `open` on load, so it is visible and non-modal.
  public Modal opened() {
    opened = true;
    return this;
  }

  @Override
  public Modal self() {
    return this;
  }

  @Override
  String viewType() {
    return "modal";
  }

  @Override
  void configureScope(ScopeStyle scope) {
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
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .shadow(
                        Shadow.drop(
                            CssLength.px(24),
                            CssLength.px(48),
                            CssLength.px(-12),
                            CssColor.oklch(0, 0, 0, 0.28))))
        .select(":scope::backdrop", backdrop(CssLength.px(4)))
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(
                        ":scope",
                        s ->
                            s.opacity(0)
                                .transform(Transform.scale(0.95))
                                .transition(Transition.overlayEntry()))
                    .select(
                        ":scope:open",
                        s ->
                            s.opacity(1)
                                .transform(Transform.scale(1))
                                .startingStyle(
                                    start -> start.opacity(0).transform(Transform.scale(0.95))))
                    .select(
                        ":scope::backdrop",
                        s ->
                            s.bg(CssColor.oklch(0, 0, 0, 0))
                                .backdropFilter(CssFilter.blur(CssLength.px(0)))
                                .transition(
                                    Transition.overlayEntry()
                                        .properties(
                                            Animatable.BACKGROUND,
                                            Animatable.BACKDROP_FILTER,
                                            Animatable.DISPLAY,
                                            Animatable.OVERLAY)))
                    .select(
                        ":scope:open::backdrop",
                        s ->
                            s.bg(CssColor.oklch(0, 0, 0, 0.45))
                                .backdropFilter(CssFilter.blur(CssLength.px(4)))
                                .startingStyle(
                                    start ->
                                        start
                                            .bg(CssColor.oklch(0, 0, 0, 0))
                                            .backdropFilter(CssFilter.blur(CssLength.px(0))))));
  }

  @Override
  IntoElement render(RenderContext context) {
    var titleRef = ElementRef.scoped("title");
    var headerEl =
        header()
            .flex()
            .flexCol()
            .gap(Tokens.SPACE_1)
            .child(heading(titleRef).fontSize(Tokens.FONT_SIZE_XL).textWrap(TextWrap.BALANCE))
            .ifPresent(
                description,
                (h, desc) ->
                    h.child(
                        p().textColor(Tokens.TEXT_MUTED).fontSize(Tokens.FONT_SIZE_SM).text(desc)));

    var contentEl = div().flex().flexCol().gap(Tokens.SPACE_3);
    contentEl.children(body);

    var footerEl =
        footer().flex().itemsCenter().justifyEnd().gap(Tokens.SPACE_3).pt(Tokens.SPACE_2);
    footerEl.children(actions);
    footerEl.child(closeButton());

    return dialogShell(titleRef)
        .onlyIf(opened, el -> el.boolAttr("open"))
        .child(div().flex().flexCol().gap(Tokens.SPACE_5).children(headerEl, contentEl, footerEl));
  }
}
