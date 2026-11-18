package dev.nthings.tessera.view;

import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.document.DocumentHeading;
import dev.nthings.tessera.internal.css.StyleSheet;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// The portable [ViewContext] of one expansion turn: its id scope and component path over the state
/// shared by the whole expansion. Expansion state and headings go to the expansion's
/// [ExpansionRegistry]. Document and live callbacks receive a context derived from it with their
/// family's operations. Derived contexts never change their parent.
final class ExpansionContext implements ViewContext {

  private final ViewScope viewScope;
  private final StyleSheet styles;
  private final ViewExpansion<?> expansion;
  private final List<String> componentPath;
  private final @Nullable SourceLocation origin;
  private final @Nullable String keyOwner;
  private final TagName.Namespace namespace;

  ExpansionContext(
      ViewScope viewScope,
      StyleSheet styles,
      ViewExpansion<?> expansion,
      List<String> componentPath,
      @Nullable SourceLocation origin,
      @Nullable String keyOwner,
      TagName.Namespace namespace) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.expansion = expansion;
    this.componentPath = componentPath;
    this.origin = origin;
    this.keyOwner = keyOwner;
    this.namespace = namespace;
  }

  /// The namespace of the nearest enclosing element: the namespace an element placed here is parsed
  /// in by an HTML parser.
  TagName.Namespace namespace() {
    return namespace;
  }

  /// A context for the children of an element in `namespace`.
  ExpansionContext withNamespace(TagName.Namespace namespace) {
    return namespace == this.namespace
        ? this
        : new ExpansionContext(
            viewScope, styles, expansion, componentPath, origin, keyOwner, namespace);
  }

  @Override
  public <T> T state(ViewKey<T> key) {
    return expansion.registry().value(key);
  }

  @Override
  public void report(String rule, String message, String fix) {
    expansion.reporter().report(location(), rule, message, fix);
  }

  /// Records outline metadata found on an element. The first heading for a target is kept.
  void collectHeading(DocumentHeading heading, Location placedBy) {
    expansion.registry().registerHeading(heading, placedBy);
  }

  ViewExpansion<?> expansion() {
    return expansion;
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

  /// The local name of the nearest enclosing element in this id scope that is bound to a named ref,
  /// or `null` if there is none. Keyed lists use it to tell themselves apart.
  @Nullable String keyOwner() {
    return keyOwner;
  }

  /// A context whose id scope is nested under the sanitized local id `scopeId`. `generated` marks
  /// that id as an encounter-order counter, which every nested scope keeps. A new id scope clears
  /// [#keyOwner()], which belongs to the scope it was bound in.
  ExpansionContext withIdScope(String scopeId, boolean generated) {
    return new ExpansionContext(
        viewScope.extend(scopeId, generated),
        styles,
        expansion,
        componentPath,
        origin,
        null,
        namespace);
  }

  /// A context for keyed content, whose id scope is nested under the sanitized local id `slug`.
  ExpansionContext withKeyedScope(String slug) {
    return new ExpansionContext(
        viewScope.extendKeyed(slug), styles, expansion, componentPath, origin, null, namespace);
  }

  /// A context inside an element bound to a ref named `owner`, in the same id scope.
  ExpansionContext withKeyOwner(String owner) {
    return new ExpansionContext(
        viewScope, styles, expansion, componentPath, origin, owner, namespace);
  }

  /// Enters `component` for diagnostics, keeping the id scope. Without `placedAt`, the enclosing
  /// origin is kept.
  ExpansionContext withComponent(String component, @Nullable SourceLocation placedAt) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withPlacement(new Location(path, "", null, placedAt != null ? placedAt : origin));
  }

  /// A context with the component path and origin of `placedBy`, keeping the id scope.
  ExpansionContext withPlacement(Location placedBy) {
    return new ExpansionContext(
        viewScope,
        styles,
        expansion,
        placedBy.components(),
        placedBy.source(),
        keyOwner,
        namespace);
  }
}
