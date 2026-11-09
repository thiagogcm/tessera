package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.css.StyleSheetCollector;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// The compiler's [RenderContext]: one expansion turn's view scope and component path over the
/// page's shared state. Derived contexts never change their parent.
final class PageContext implements RenderContext {

  private final ViewScope viewScope;
  private final StyleSheetCollector styles;
  private final DocumentState docState;
  private final CompilationState state;
  private final List<String> componentPath;
  private final @Nullable SourceLocation origin;

  private PageContext(
      ViewScope viewScope,
      StyleSheetCollector styles,
      DocumentState docState,
      CompilationState state,
      List<String> componentPath,
      @Nullable SourceLocation origin) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.docState = docState;
    this.state = state;
    this.componentPath = List.copyOf(componentPath);
    this.origin = origin;
  }

  /// The root context of one page compilation.
  static PageContext forDocument(
      StyleSheetCollector styles, DocumentState docState, CompilationState state) {
    return new PageContext(ViewScope.ROOT, styles, docState, state, List.of(), null);
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
      DocumentKey<?> owner, Function<RenderContext, ? extends IntoElement> section) {
    docState.atDocumentEnd(owner, section, location());
  }

  @Override
  public void contributeBehavior(ClientBehavior behavior) {
    docState.contributeBehavior(behavior);
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

  ViewScope viewScope() {
    return viewScope;
  }

  StyleSheetCollector styles() {
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
  ViewLocation location() {
    return new ViewLocation(componentPath, "", null, origin);
  }

  /// A context whose view scope is nested under the sanitized local id `viewId`.
  PageContext withViewScope(String viewId) {
    return new PageContext(
        viewScope.extend(viewId), styles, docState, state, componentPath, origin);
  }

  /// Enters `component` for diagnostics, keeping the view scope. Without `placedAt`, the enclosing
  /// origin is kept.
  PageContext withComponent(String component, @Nullable SourceLocation placedAt) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withPlacement(new ViewLocation(path, "", null, placedAt != null ? placedAt : origin));
  }

  /// A context with the component path and origin of `placedBy`, keeping the view scope.
  PageContext withPlacement(ViewLocation placedBy) {
    return new PageContext(
        viewScope, styles, docState, state, placedBy.componentPath(), placedBy.source());
  }
}
