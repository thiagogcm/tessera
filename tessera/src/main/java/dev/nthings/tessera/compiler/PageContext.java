package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.css.StyleSheet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// The compiler's [DocumentContext]: one expansion turn's id scope and component path over the
/// page's shared state. Derived contexts never change their parent.
final class PageContext implements DocumentContext {

  private final IdScope idScope;
  private final StyleSheet styles;
  private final DocumentState docState;
  private final ExpansionState state;
  private final List<String> componentPath;
  private final @Nullable SourceLocation origin;
  private final @Nullable String keyOwner;

  private PageContext(
      IdScope idScope,
      StyleSheet styles,
      DocumentState docState,
      ExpansionState state,
      List<String> componentPath,
      @Nullable SourceLocation origin,
      @Nullable String keyOwner) {
    this.idScope = idScope;
    this.styles = styles;
    this.docState = docState;
    this.state = state;
    this.componentPath = List.copyOf(componentPath);
    this.origin = origin;
    this.keyOwner = keyOwner;
  }

  /// The root context of one page compilation.
  static PageContext forDocument(StyleSheet styles, DocumentState docState, ExpansionState state) {
    return new PageContext(IdScope.ROOT, styles, docState, state, List.of(), null, null);
  }

  @Override
  public <T> T document(DocumentKey<T> key) {
    return docState.getOrCreate(key);
  }

  @Override
  public void contributeHead(IntoElement element) {
    docState.contributeHead(element.intoElement(), location());
  }

  @Override
  public void atDocumentEnd(
      DocumentKey<?> owner, Function<DocumentContext, ? extends IntoElement> section) {
    docState.atDocumentEnd(owner, section, location());
  }

  @Override
  public void contributeScript(PageScript script) {
    docState.contributeScript(script);
  }

  @Override
  public void registerHeading(ElementRef target, String title, int level) {
    docState.registerHeading(new DocumentHeading(target, title, level), location());
  }

  @Override
  public List<DocumentHeading> headings() {
    return docState.headings();
  }

  @Override
  public void report(String rule, String message, String fix) {
    state.report(location(), rule, message, fix);
  }

  IdScope idScope() {
    return idScope;
  }

  StyleSheet styles() {
    return styles;
  }

  List<String> componentPath() {
    return componentPath;
  }

  /// The call site of the innermost enclosing component, when source tracking is on. Elements that
  /// component builds itself are attributed to it.
  @Nullable SourceLocation origin() {
    return origin;
  }

  /// The current component, as a location without an element.
  Location location() {
    return location("", null, origin);
  }

  /// An element or component location at the current component path.
  Location location(String tag, @Nullable String domId, @Nullable SourceLocation source) {
    return new Location(componentPath, tag, domId, source);
  }

  /// The local name of the nearest enclosing element in this id scope that is bound to a named
  /// ref, or `null` if there is none. Keyed lists use it to tell themselves apart.
  @Nullable String keyOwner() {
    return keyOwner;
  }

  /// A context whose id scope is nested under the sanitized local id `scopeId`. `generated` marks
  /// that id as an encounter-order counter, which every nested scope keeps.
  PageContext withIdScope(String scopeId, boolean generated) {
    return new PageContext(
        idScope.extend(scopeId, generated), styles, docState, state, componentPath, origin, null);
  }

  /// A context for keyed content, whose id scope is nested under the sanitized local id `slug`.
  PageContext withKeyedScope(String slug) {
    return new PageContext(
        idScope.extendKeyed(slug), styles, docState, state, componentPath, origin, null);
  }

  /// A context inside an element bound to a ref named `owner`, in the same id scope.
  PageContext withKeyOwner(String owner) {
    return new PageContext(idScope, styles, docState, state, componentPath, origin, owner);
  }

  /// Enters `component` for diagnostics, keeping the id scope. Without `placedAt`, the enclosing
  /// origin is kept.
  PageContext withComponent(String component, @Nullable SourceLocation placedAt) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withPlacement(new Location(path, "", null, placedAt != null ? placedAt : origin));
  }

  /// A context with the component path and origin of `placedBy`, keeping the id scope.
  PageContext withPlacement(Location placedBy) {
    return new PageContext(
        idScope, styles, docState, state, placedBy.components(), placedBy.source(), keyOwner);
  }
}
