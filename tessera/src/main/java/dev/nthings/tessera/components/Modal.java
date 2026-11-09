package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
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
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Transition;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Centered modal `<dialog>` with a title, optional description, body, footer actions and a close
/// button. It opens and closes without author JavaScript: create one with [#of(String)], place it
/// on the page (directly or with `Elements.deferred`), and open it with its [#triggerButton] or a
/// button that targets its [#dialogRef()]. Escape or a click outside dismisses it.
public final class Modal extends BuiltinView {

  private final String title;
  private ElementRef dialog = ElementRef.scoped("dialog");
  private @Nullable String description;
  private final List<Element> body = new ArrayList<>();
  private final List<Element> actions = new ArrayList<>();
  private String closeLabel = "Close";
  private boolean opened;

  private Modal(String title) {
    this.title = title;
  }

  /// Creates a modal whose heading, which also names the dialog, is `title`.
  public static Modal of(String title) {
    return new Modal(title);
  }

  /// Names this instance in the enclosing view scope; the dialog, title and close button ids are
  /// prefixed with it, as in `<id>--dialog`. Without it, the compiler assigns `modal-<n>`.
  public Modal id(String id) {
    this.id = id;
    return this;
  }

  /// Sets muted text under the title.
  public Modal description(String description) {
    this.description = description;
    return this;
  }

  /// Adds content to the body, in order.
  public Modal body(IntoElement... content) {
    for (var child : content) {
      body.add(child.intoElement());
    }
    return this;
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

  /// Sets the close button's text. The default is `Close`.
  public Modal closeLabel(String label) {
    this.closeLabel = label;
    return this;
  }

  /// Renders the dialog with `open` on load, so it is visible and non-modal.
  public Modal opened() {
    opened = true;
    return this;
  }

  /// Creates a neutral, outlined button that opens this modal with `command="show-modal"`.
  public Button triggerButton(String label) {
    return Button.of(label)
        .tone(Tone.NEUTRAL)
        .variant(Button.Variant.OUTLINE)
        .command(dialog, BuiltinCommand.SHOW_MODAL);
  }

  /// Returns the ref bound to the `<dialog>`, for invokers placed elsewhere. The modal must still
  /// be placed on the page, directly or through `Elements.deferred`, or the invoker reports
  /// `unbound-reference`.
  public ElementRef dialogRef() {
    return dialog;
  }

  /// Binds the `<dialog>` to `dialog` instead of a generated ref, so a callback that outlives the
  /// render can still target it. Reuse the same ref on every render.
  public Modal dialogRef(ElementRef dialog) {
    this.dialog = dialog;
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
        .select(
            ":scope::backdrop",
            s ->
                s.bg(CssColor.oklch(0, 0, 0, 0.45)).backdropFilter(CssFilter.blur(CssLength.px(4))))
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
            .child(
                h2().id(titleRef)
                    .fontSize(Tokens.FONT_SIZE_XL)
                    .textWrap(TextWrap.BALANCE)
                    .text(title))
            .whenPresent(
                description,
                (h, desc) ->
                    h.child(
                        p().textColor(Tokens.TEXT_MUTED).fontSize(Tokens.FONT_SIZE_SM).text(desc)));

    var contentEl = div().flex().flexCol().gap(Tokens.SPACE_3);
    contentEl.children(body);

    var closeBtn =
        Button.of(closeLabel).id(ElementRef.scoped("close")).command(dialog, BuiltinCommand.CLOSE);

    var footerEl =
        footer().flex().itemsCenter().justifyEnd().gap(Tokens.SPACE_3).pt(Tokens.SPACE_2);
    footerEl.children(actions);
    footerEl.child(closeBtn);

    return Elements.dialog()
        .id(dialog)
        .ariaLabelledBy(titleRef)
        .closedBy(ClosedBy.ANY)
        .boolAttr("open", opened)
        .child(div().flex().flexCol().gap(Tokens.SPACE_5).children(headerEl, contentEl, footerEl));
  }
}
