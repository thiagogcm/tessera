package dev.nthings.tessera.wasm;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.view.WidgetAccess;
import java.util.ArrayDeque;
import java.util.function.Function;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Adapts a native dialog or drawer to browser-owned or application-owned open state.
public final class LiveDialog implements LiveComponent {

  /// The native operation used to open the dialog.
  public enum Mode {
    MODAL,
    NON_MODAL
  }

  private final IntoElement dialog;
  private final Function<String, Button> trigger;
  private @Nullable Mode mode;
  private @Nullable Boolean open;
  private @Nullable ElementRef fallback;
  private Runnable opening = () -> {};
  private Runnable dismissal = () -> {};

  private LiveDialog(IntoElement dialog, Function<String, Button> trigger) {
    this.dialog = dialog;
    this.trigger = trigger;
  }

  /// Adapts the portable modal. An authored `opened()` initializes non-modal state.
  public static LiveDialog of(Modal modal) {
    return new LiveDialog(modal, modal::triggerButton);
  }

  /// Adapts the portable drawer.
  public static LiveDialog of(Drawer drawer) {
    return new LiveDialog(drawer, drawer::triggerButton);
  }

  /// Chooses modal or non-modal openings. Defaults to modal for a closed portable dialog.
  public LiveDialog mode(Mode mode) {
    this.mode = mode;
    return this;
  }

  /// Supplies the current open state. A dismissal stays accepted when the application supplies
  /// false.
  public LiveDialog bindOpen(boolean open) {
    this.open = open;
    return this;
  }

  /// Creates a button that invokes this dialog. In bound mode, it requests an open-state update.
  public Button triggerButton(String label) {
    return trigger.apply(label);
  }

  /// Receives an opening request from an invoker while a bound dialog is closed.
  public LiveDialog onOpenRequest(Runnable handler) {
    opening = handler;
    return this;
  }

  /// Receives a native dismissal request. After the callback the root reconciles the bound state.
  public LiveDialog onDismiss(Runnable handler) {
    dismissal = handler;
    return this;
  }

  /// Sets a focus target for when the dialog closes and its opener cannot take focus: the opener
  /// was removed or disabled, or the dialog opened while nothing had focus, as from a timer.
  public LiveDialog focusFallback(ElementRef target) {
    fallback = target;
    return this;
  }

  @Override
  public LiveContent expand(LiveContext context) {
    return new WidgetScope(
        WidgetAccess.scope(dialog),
        scope -> {
          var parts = WidgetAccess.dialog(dialog, scope);
          return LiveElement.of(parts.host())
              .widget(new Binding(mode, open, parts.opened(), fallback, opening, dismissal));
        });
  }

  /// `opened` is whether the portable dialog was authored open.
  private record Binding(
      @Nullable Mode mode,
      @Nullable Boolean open,
      boolean opened,
      @Nullable ElementRef fallback,
      Runnable opening,
      Runnable dismissal)
      implements WidgetBinding {
    @Override
    public WidgetController create(JSObject node, BrowserRoot root) {
      return new Controller(node, root, this);
    }
  }

  private record Closure(long session, boolean application, @Nullable JSObject opener) {}

  private static final class Controller extends WidgetController {
    private Binding binding;
    private final Mode initialMode;
    private final ArrayDeque<Closure> closing = new ArrayDeque<>();
    private long session;
    private @Nullable JSObject opener;
    private @Nullable JSObject invoker;
    private boolean changing;

    Controller(JSObject node, BrowserRoot root, Binding binding) {
      super(node, root);
      this.binding = binding;
      this.initialMode = binding.opened() ? Mode.NON_MODAL : Mode.MODAL;
      listen(node, "beforetoggle", this::beforeToggle);
      listen(node, "command", this::command);
      listen(node, "cancel", this::cancel);
      listen(node, "close", _ -> closed());
      // A browser-owned dialog authored open starts open, like the portable `open` attribute.
      if (binding.open() == null && binding.opened()) {
        changing = true;
        try {
          show(Mode.NON_MODAL);
        } finally {
          changing = false;
        }
      }
    }

    @Override
    void update(WidgetBinding next) {
      binding = (Binding) next;
      var isOpen = Dom.flag(node, "open");
      var wanted = binding.open();
      var shouldOpen = wanted == null ? isOpen : wanted;
      var modeChanged =
          wanted != null && isOpen && Dom.isModal(node) != (openingMode() == Mode.MODAL);
      var previousOpener = opener;
      changing = true;
      try {
        if (isOpen && (!shouldOpen || modeChanged)) {
          Dom.closeDialog(node, "");
        }
        if (shouldOpen && (!isOpen || modeChanged) && !root.closed()) {
          if (modeChanged) {
            invoker = previousOpener;
          }
          show(openingMode());
        } else if (!shouldOpen && isOpen && !root.closed()) {
          restoreFocus(previousOpener);
        }
      } finally {
        invoker = null;
        changing = false;
      }
    }

    private void beforeToggle(JSObject event) {
      if (Dom.eventString(event, "newState").equals("closed")) {
        closing.addLast(new Closure(session, changing, opener));
        opener = null;
        return;
      }
      if (!changing && Boolean.FALSE.equals(binding.open())) {
        Dom.preventDefault(event);
        return;
      }
      session++;
      var source = WidgetDom.source(event);
      opener = invoker != null ? invoker : source != null ? source : Dom.activeElement();
      invoker = null;
    }

    private void command(JSObject event) {
      if (Dom.flag(event, "defaultPrevented")
          || !Dom.eventString(event, "command").equals("show-modal")
          || Dom.flag(node, "open")) {
        return;
      }
      Dom.preventDefault(event);
      invoker = WidgetDom.source(event);
      if (binding.open() == null) {
        show(openingMode());
      } else {
        notifyApplication(binding.opening());
      }
    }

    private Mode openingMode() {
      var mode = binding.mode();
      return mode == null ? initialMode : mode;
    }

    private void show(Mode mode) {
      if (mode == Mode.MODAL) {
        Dom.showModal(node);
      } else {
        Dom.show(node);
      }
    }

    private void cancel(JSObject event) {
      if (binding.open() != null
          && Dom.flag(event, "cancelable")
          && !Dom.flag(event, "defaultPrevented")) {
        Dom.preventDefault(event);
        notifyApplication(binding.dismissal());
      }
    }

    private void closed() {
      var closed = closing.pollFirst();
      if (closed == null || closed.application()) {
        return;
      }
      if (closed.session() == session && !Dom.flag(node, "open")) {
        restoreFocus(closed.opener());
      }
      notifyApplication(binding.dismissal());
    }

    /// Returns focus to `target`, the opener, or to the fallback when the opener is gone, is the
    /// document itself (the dialog opened with nothing focused) or cannot take focus, such as a
    /// button disabled while the dialog was open.
    private void restoreFocus(@Nullable JSObject target) {
      if (target != null
          && !WidgetDom.noFocus(target)
          && Dom.flag(target, "isConnected")
          && !WidgetDom.contains(node, target)) {
        WidgetDom.focus(target);
        if (Dom.isActiveElement(target)) {
          return;
        }
      }
      var fallback = binding.fallback();
      if (fallback != null) {
        var replacement = root.findReference(fallback);
        if (replacement != null) {
          WidgetDom.focus(replacement);
        }
      }
    }

    @Override
    void dispose() {
      if (Dom.flag(node, "open")) {
        Dom.closeDialog(node, "");
        restoreFocus(opener);
      } else {
        var pending = closing.peekLast();
        if (pending != null && !pending.application() && pending.session() == session) {
          restoreFocus(pending.opener());
        }
      }
      opener = null;
      invoker = null;
      closing.clear();
    }
  }
}
