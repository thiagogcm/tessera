package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.h2;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ClosedBy;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssFilter;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.StyleBlock;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.components.WidgetBridge.DialogParts;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Options shared by [Modal] and [Drawer]: a titled modal `<dialog>` with an optional description,
/// a body and a close button. Each setter returns the same dialog, for chaining.
///
/// @param <D> the concrete dialog type
abstract class DialogView<D extends DialogView<D>> extends BuiltinScope<D> {

  static {
    WidgetBridge.registerDialog((source, context) -> {
      if (!(source instanceof DialogView<?> view)) {
        throw new IllegalArgumentException(
            "Expected a Modal or Drawer dialog, got " + source.getClass().getSimpleName());
      }
      return new DialogParts(view.closedDialog(context), view.opened);
    });
  }

  final String title;
  ElementRef dialog = ElementRef.scoped("dialog");
  boolean opened;
  @Nullable String description;
  final List<Element> body = new ArrayList<>();
  String closeLabel = "Close";

  DialogView(String title) {
    this.title = title;
  }

  /// Sets muted text under the title.
  public D description(String description) {
    this.description = description;
    return self();
  }

  /// Adds content to the body, in order.
  public D body(IntoElement... content) {
    for (var child : content) {
      body.add(child.intoElement());
    }
    return self();
  }

  /// Renders the dialog with `open` on load, so it is visible and non-modal.
  public D opened() {
    opened = true;
    return self();
  }

  /// Sets the close button's text. The default is `Close`.
  public D closeLabel(String label) {
    this.closeLabel = label;
    return self();
  }

  /// Creates a neutral, outlined button that opens this dialog with `command="show-modal"`.
  public Button triggerButton(String label) {
    return Button.of(label)
        .tone(Tone.NEUTRAL)
        .variant(Button.Variant.OUTLINE)
        .command(dialog, BuiltinCommand.SHOW_MODAL);
  }

  /// Returns the ref bound to the `<dialog>`, for invokers placed elsewhere. The dialog must still
  /// be placed on the page, directly or through `Documents.deferred`, or the invoker reports
  /// `unbound-reference`.
  public ElementRef dialogRef() {
    return dialog;
  }

  /// Binds the `<dialog>` to `dialog` instead of a generated ref, so a callback that outlives the
  /// render can still target it. Reuse the same ref on every render.
  public D dialogRef(ElementRef dialog) {
    this.dialog = dialog;
    return self();
  }

  @Override
  final TagElement expand(ViewContext context) {
    return closedDialog(context).onlyIf(opened, el -> el.boolAttr("open"));
  }

  /// Builds the `<dialog>` without `open`; [#expand] adds it to a dialog authored `opened()`.
  abstract TagElement closedDialog(ViewContext context);

  /// The `<dialog>` element, named by the heading bound to `titleRef` and dismissed by Escape or a
  /// click outside.
  TagElement dialogShell(ElementRef titleRef) {
    return Elements.dialog().id(dialog).ariaLabelledBy(titleRef).closedBy(ClosedBy.ANY);
  }

  /// The `<h2>` title, bound to `titleRef`.
  TagElement heading(ElementRef titleRef) {
    return h2().id(titleRef).text(title);
  }

  /// The button that closes the dialog; its id is `<instance id>--close`.
  Button closeButton() {
    return Button.of(closeLabel)
        .id(ElementRef.scoped("close"))
        .command(dialog, BuiltinCommand.CLOSE);
  }

  /// Styles for the `::backdrop` behind the open dialog: dimmed, and blurred by `blur`.
  static StyleBlock backdrop(CssLength blur) {
    return s -> s.bg(CssColor.oklch(0, 0, 0, 0.45)).backdropFilter(CssFilter.blur(blur));
  }
}
