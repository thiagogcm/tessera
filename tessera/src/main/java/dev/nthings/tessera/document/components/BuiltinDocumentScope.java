package dev.nthings.tessera.document.components;

import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.foundation.FluentBuilder;
import org.jspecify.annotations.Nullable;

/// Common configuration for built-in document scopes with a caller-supplied id.
///
/// @param <B> the concrete component type, returned by the setters for chaining
abstract class BuiltinDocumentScope<B extends BuiltinDocumentScope<B>>
    implements DocumentScope, FluentBuilder<B> {

  /// Instance id set through [#id(String)]; `null` lets expansion assign `<scopeName>-<n>`.
  private @Nullable String id;

  /// Names this instance in the enclosing id scope.
  public B id(String id) {
    this.id = id;
    return self();
  }

  /// The stable `data-scope` name and `@scope` key of this component.
  public abstract String scopeName();

  /// Adds the component's scoped styles.
  public void configureScope(ScopeStyle scope) {}

  /// Builds the component's host; called for each placement in a document.
  public abstract DocumentElement expand(DocumentContext context);

  @Override
  public @Nullable String scopeId() {
    return id;
  }
}
