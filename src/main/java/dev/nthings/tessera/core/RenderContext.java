package dev.nthings.tessera.core;

import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.function.Supplier;

/// Implicit lexical render context propagated across component boundaries via JDK 25
/// [ScopedValue].
public final class RenderContext {

  private static final ScopedValue<RenderContext> CURRENT = ScopedValue.newInstance();

  private final ViewScope viewScope;
  private final StyleSheetCollector styles;
  private final Tokens tokens;
  private final DocumentState docState;

  public RenderContext(ViewScope viewScope, StyleSheetCollector styles, Tokens tokens) {
    this(viewScope, styles, tokens, new DocumentState());
  }

  private RenderContext(
      ViewScope viewScope, StyleSheetCollector styles, Tokens tokens, DocumentState docState) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.tokens = tokens;
    this.docState = docState;
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
    docState.contributeHead(element.intoElement());
  }

  /// Appends `section` to the end of `<body>` after layout; only the first registration per
  /// `owner` counts.
  public void atDocumentEnd(DocumentKey<?> owner, Supplier<? extends IntoElement> section) {
    docState.atDocumentEnd(owner, section);
  }

  public DocumentState documentState() {
    return docState;
  }

  /// Runs `work` with this context current again (used to resolve [LateElement]s in place).
  public <R> R reenter(Supplier<R> work) {
    return ScopedValue.where(CURRENT, this).call(work::get);
  }

  public <R> R withViewScope(ElementId viewEntityId, Supplier<R> work) {
    var childContext = new RenderContext(viewScope.extend(viewEntityId), styles, tokens, docState);
    return ScopedValue.where(CURRENT, childContext).call(work::get);
  }

  public static <R> R runRoot(StyleSheetCollector styles, Tokens tokens, Supplier<R> work) {
    var rootContext = new RenderContext(ViewScope.ROOT, styles, tokens);
    return ScopedValue.where(CURRENT, rootContext).call(work::get);
  }
}
