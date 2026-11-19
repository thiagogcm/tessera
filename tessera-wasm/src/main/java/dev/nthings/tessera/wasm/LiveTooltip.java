package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.view.WidgetAccess;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// A live interest tooltip associated with its portable trigger.
public final class LiveTooltip extends BuiltinLiveComponent {
  private static final int POINTER_LEAVE_DELAY = 120;

  private final TagElement source;

  private LiveTooltip(TagElement source) {
    this.source = source;
  }

  /// Adapts a trigger configured with `TagElement.interestTooltip`.
  public static LiveTooltip of(TagElement trigger) {
    return new LiveTooltip(trigger);
  }

  @Override
  LiveContent expand(LiveContext context) {
    var parts = WidgetAccess.tooltip(source);
    return Live.fragment(
        LiveElement.of(parts.trigger()).widget(new Binding(parts.popupRef())),
        LiveContent.of(parts.popup()));
  }

  private record Binding(ElementRef popup) implements WidgetBinding {
    @Override
    public WidgetController create(JSObject node, BrowserRoot root) {
      return new Controller(node, root);
    }
  }

  private static final class Controller extends WidgetController {
    private @Nullable JSObject popup;
    private @Nullable JSObject popupEnter;
    private @Nullable JSObject popupLeave;
    private @Nullable JSObject timer;
    private boolean overTrigger;
    private boolean overPopup;
    private boolean focused;
    private boolean dismissed;

    Controller(JSObject node, BrowserRoot root) {
      super(node, root);
      listen(
          node,
          "pointerenter",
          event -> {
            if (!Dom.eventString(event, "pointerType").equals("touch")) {
              overTrigger = true;
              refresh();
            }
          });
      listen(
          node,
          "pointerleave",
          _ -> {
            overTrigger = false;
            refresh();
          });
      listen(
          node,
          "focusin",
          _ -> {
            focused = true;
            refresh();
          });
      listen(
          node,
          "focusout",
          event -> {
            focused = WidgetDom.contains(node, WidgetDom.related(event));
            refresh();
          });
      listen(WidgetDom.document(), "keydown", this::dismiss);
      focused = WidgetDom.contains(node, Dom.activeElement());
    }

    @Override
    void update(WidgetBinding binding) {
      // One id lookup: a patch can replace the popup while it retains the trigger.
      var next = root.resolve(((Binding) binding).popup());
      if (!WidgetDom.same(popup, next)) {
        releasePopup();
        popup = next;
        popupEnter =
            listen(
                next,
                "pointerenter",
                _ -> {
                  overPopup = true;
                  refresh();
                });
        popupLeave =
            listen(
                next,
                "pointerleave",
                _ -> {
                  overPopup = false;
                  refresh();
                });
        refresh();
      }
    }

    private void refresh() {
      stopTimer();
      var target = popup;
      if (target == null) {
        return;
      }
      if (overTrigger || overPopup || focused) {
        if (!dismissed && !Dom.popoverOpen(target)) {
          Dom.showPopover(target);
        }
      } else {
        dismissed = false;
        if (Dom.popoverOpen(target)) {
          timer =
              Dom.timeout(
                  POINTER_LEAVE_DELAY,
                  () -> {
                    timer = null;
                    if (Dom.popoverOpen(target)) {
                      Dom.hidePopover(target);
                    }
                  });
        }
      }
    }

    private void dismiss(JSObject event) {
      var target = popup;
      if (target != null
          && Dom.popoverOpen(target)
          && !WidgetDom.skipKey(event)
          && Dom.eventString(event, "key").equals("Escape")) {
        Dom.preventDefault(event);
        dismissed = true;
        overPopup = false;
        stopTimer();
        Dom.hidePopover(target);
      }
    }

    private void stopTimer() {
      var pending = timer;
      timer = null;
      if (pending != null) {
        Dom.release(pending);
      }
    }

    private void releasePopup() {
      stopTimer();
      var target = popup;
      popup = null;
      overPopup = false;
      if (popupEnter != null) {
        unlisten(popupEnter);
        popupEnter = null;
      }
      if (popupLeave != null) {
        unlisten(popupLeave);
        popupLeave = null;
      }
      if (target != null && Dom.popoverOpen(target)) {
        Dom.hidePopover(target);
      }
    }

    @Override
    void dispose() {
      // WidgetController has already removed listeners from both nodes.
      popupEnter = null;
      popupLeave = null;
      releasePopup();
    }
  }
}
