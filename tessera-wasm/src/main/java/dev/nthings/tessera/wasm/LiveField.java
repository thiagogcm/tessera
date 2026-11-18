package dev.nthings.tessera.wasm;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Field.Validation;
import dev.nthings.tessera.view.WidgetAccess;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// A portable field whose control supports live bindings and application-supplied validation.
public final class LiveField implements LiveComponent {

  private final Field field;
  private final Consumer<LiveElement> bindControl;
  private @Nullable Validation validation;

  private LiveField(Field field, Consumer<LiveElement> bindControl) {
    this.field = field;
    this.bindControl = bindControl;
  }

  /// Adapts the field's existing control. The binder runs during expansion and can bind its live
  /// properties or register event handlers.
  public static LiveField of(Field field, Consumer<LiveElement> bindControl) {
    return new LiveField(field, bindControl);
  }

  /// Supplies the current validation state. Set `invalid` to false to clear the error.
  public LiveField validation(boolean invalid, @Nullable String message) {
    validation = new Validation(invalid, message);
    return this;
  }

  @Override
  public LiveContent expand(LiveContext context) {
    return new WidgetScope(
        WidgetAccess.scope(field),
        _ -> {
          var parts = WidgetAccess.field(field, validation);
          var host = LiveElement.of(parts.host());
          parts.before().forEach(host::child);
          var control = LiveElement.of(parts.control());
          bindControl.accept(control);
          var scope = parts.controlScope();
          host.child(scope == null ? control : new WidgetScope(scope, _ -> control));
          parts.after().forEach(host::child);
          return host;
        });
  }
}
