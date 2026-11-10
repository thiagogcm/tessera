package dev.nthings.tessera.view;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.css.StyleSheet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// The [DocumentContext] of one expansion turn: its id scope and component path over the state
/// shared by the whole expansion. Per-page values and headings go to the expansion's
/// [DocumentRegistry], document contributions to the backend. Derived contexts never change their
/// parent.
final class ViewContext implements DocumentContext {

  private final ViewScope viewScope;
  private final StyleSheet styles;
  private final ViewExpansion expansion;
  private final List<String> componentPath;
  private final @Nullable SourceLocation origin;
  private final @Nullable String keyOwner;

  ViewContext(
      ViewScope viewScope,
      StyleSheet styles,
      ViewExpansion expansion,
      List<String> componentPath,
      @Nullable SourceLocation origin,
      @Nullable String keyOwner) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.expansion = expansion;
    this.componentPath = componentPath;
    this.origin = origin;
    this.keyOwner = keyOwner;
  }

  @Override
  public <T> T document(DocumentKey<T> key) {
    return expansion.document().value(key);
  }

  @Override
  public void contributeHead(IntoElement element) {
    expansion.backend().contributeHead(element.intoElement(), location(), expansion.reporter());
  }

  @Override
  public void atDocumentEnd(
      DocumentKey<?> owner, Function<DocumentContext, ? extends IntoElement> section) {
    expansion.backend().atDocumentEnd(owner, section, location(), expansion.reporter());
  }

  @Override
  public void contributeScript(PageScript behavior) {
    expansion.backend().contributeScript(behavior, location(), expansion.reporter());
  }

  /// Records outline metadata found on an element. The first heading for a target is kept.
  void collectHeading(DocumentHeading heading, Location placedBy) {
    expansion.document().registerHeading(heading, placedBy);
  }

  @Override
  public List<DocumentHeading> headings() {
    return expansion.document().headings();
  }

  @Override
  public void report(String rule, String message, String fix) {
    expansion.reporter().report(location(), rule, message, fix);
  }

  ViewScope idScope() {
    return viewScope;
  }

  StyleSheet styles() {
    return styles;
  }

  List<String> components() {
    return componentPath;
  }

  /// The call site of the innermost enclosing component, when source tracking is on. Elements that
  /// component builds itself are attributed to it.
  @Nullable SourceLocation origin() {
    return origin;
  }

  /// The current component, as a location without an element.
  Location location() {
    return new Location(componentPath, "", null, origin);
  }

  /// The local name of the nearest enclosing element in this id scope that is bound to a named
  /// ref, or `null` if there is none. Keyed lists use it to tell themselves apart.
  @Nullable String keyOwner() {
    return keyOwner;
  }

  /// A context whose id scope is nested under the sanitized local id `scopeId`. `generated` marks
  /// that id as an encounter-order counter, which every nested scope keeps. A new id scope clears
  /// [#keyOwner()], which belongs to the scope it was bound in.
  ViewContext withIdScope(String scopeId, boolean generated) {
    return new ViewContext(
        viewScope.extend(scopeId, generated), styles, expansion, componentPath, origin, null);
  }

  /// A context for keyed content, whose id scope is nested under the sanitized local id `slug`.
  ViewContext withKeyedScope(String slug) {
    return new ViewContext(
        viewScope.extendKeyed(slug), styles, expansion, componentPath, origin, null);
  }

  /// A context inside an element bound to a ref named `owner`, in the same id scope.
  ViewContext withKeyOwner(String owner) {
    return new ViewContext(viewScope, styles, expansion, componentPath, origin, owner);
  }

  /// Enters `component` for diagnostics, keeping the id scope. Without `placedAt`, the enclosing
  /// origin is kept.
  ViewContext withComponent(String component, @Nullable SourceLocation placedAt) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withPlacement(new Location(path, "", null, placedAt != null ? placedAt : origin));
  }

  /// A context with the component path and origin of `placedBy`, keeping the id scope.
  ViewContext withPlacement(Location placedBy) {
    return new ViewContext(
        viewScope, styles, expansion, placedBy.components(), placedBy.source(), keyOwner);
  }
}
