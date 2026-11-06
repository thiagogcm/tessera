package dev.nthings.tessera.core;

import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Implicit lexical render context propagated across component boundaries via JDK 25
/// [ScopedValue].
public final class RenderContext {

  private static final ScopedValue<RenderContext> CURRENT = ScopedValue.newInstance();

  private final ViewScope viewScope;
  private final StyleSheetCollector styles;
  private final Tokens tokens;
  private final DocumentState docState;
  private final List<String> componentPath;
  private final @Nullable SourceLocation origin;

  public RenderContext(ViewScope viewScope, StyleSheetCollector styles, Tokens tokens) {
    this(viewScope, styles, tokens, new DocumentState(), List.of(), null);
  }

  private RenderContext(
      ViewScope viewScope,
      StyleSheetCollector styles,
      Tokens tokens,
      DocumentState docState,
      List<String> componentPath,
      @Nullable SourceLocation origin) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.tokens = tokens;
    this.docState = docState;
    this.componentPath = List.copyOf(componentPath);
    this.origin = origin;
  }

  public static RenderContext current() {
    return CURRENT.orElseThrow(
        () ->
            new IllegalStateException(
                "RenderContext accessed outside an active PageCompiler pass"));
  }

  public ViewScope viewScope() {
    return viewScope;
  }

  public StyleSheetCollector styles() {
    return styles;
  }

  public Tokens tokens() {
    return tokens;
  }

  /// Per-page state for `key`, created on first use.
  public <T> T document(DocumentKey<T> key) {
    return docState.getOrCreate(key);
  }

  /// Adds `element` to the page's `<head>` (after `<title>`).
  public void contributeHead(IntoElement element) {
    docState.contributeHead(element.intoElement(), componentPath, origin);
  }

  /// Appends `section` to the end of `<body>` after layout; only the first registration per
  /// `owner` counts.
  public void atDocumentEnd(DocumentKey<?> owner, Supplier<? extends IntoElement> section) {
    docState.atDocumentEnd(owner, section, componentPath, origin);
  }

  public DocumentState documentState() {
    return docState;
  }

  /// Registers metadata for a local element id in the current view scope.
  public DocumentHeading registerHeading(String localId, String title, int level) {
    var heading =
        new DocumentHeading(viewScope.extend(ElementId.of(localId)).domId(), title, level);
    docState.registerHeading(heading);
    return heading;
  }

  /// Runs `work` with this context current again (used to resolve [LateElement]s in place).
  public <R> R reenter(Supplier<R> work) {
    return ScopedValue.where(CURRENT, this).call(work::get);
  }

  public <R> R withViewScope(ElementId viewEntityId, Supplier<R> work) {
    var childContext =
        new RenderContext(
            viewScope.extend(viewEntityId), styles, tokens, docState, componentPath, origin);
    return ScopedValue.where(CURRENT, childContext).call(work::get);
  }

  /// Enclosing components of the current expansion, retained by document contributions.
  public List<String> componentPath() {
    return componentPath;
  }

  /// The call site that placed the innermost enclosing component, while [SourceTracking] is
  /// active. Elements a component builds itself are attributed to it.
  public @Nullable SourceLocation origin() {
    return origin;
  }

  /// Enters a component without changing its element identity scope. `placedAt` is where
  /// authoring code placed the component; without one, the enclosing component's origin stays.
  public <R> R withComponent(
      String component, @Nullable SourceLocation placedAt, Supplier<R> work) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withComponentPath(path, placedAt != null ? placedAt : origin, work);
  }

  /// Expands registered document content with the component path and origin captured at
  /// registration.
  public <R> R withComponentPath(
      List<String> path, @Nullable SourceLocation origin, Supplier<R> work) {
    var context = new RenderContext(viewScope, styles, tokens, docState, path, origin);
    return context.reenter(work);
  }

  public static <R> R runRoot(StyleSheetCollector styles, Tokens tokens, Supplier<R> work) {
    var rootContext = new RenderContext(ViewScope.ROOT, styles, tokens);
    return ScopedValue.where(CURRENT, rootContext).call(work::get);
  }
}
