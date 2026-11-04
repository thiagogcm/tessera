package dev.nthings.tessera.core;

import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Implicit lexical render context propagated across component boundaries via JDK 25 {@link
 * ScopedValue}.
 */
public final class RenderContext {

  public record FootnoteEntry(
      int number, String slug, String refDomId, String noteDomId, Element content) {}

  public record TocEntry(String targetDomId, String title, int level) {}

  private static final class DocumentState {
    private final Map<String, FootnoteEntry> footnotes = new LinkedHashMap<>();
    private boolean footnotesRendered = false;
    private final Map<String, TocEntry> tocEntries = new LinkedHashMap<>();
    private @Nullable String heroImageHref = null;
  }

  private static final ScopedValue<RenderContext> CURRENT = ScopedValue.newInstance();

  private final GlobalElementId viewScope;
  private final StyleSheetCollector styles;
  private final Tokens tokens;
  private final DocumentState docState;

  public RenderContext(GlobalElementId viewScope, StyleSheetCollector styles, Tokens tokens) {
    this(viewScope, styles, tokens, new DocumentState());
  }

  private RenderContext(
      GlobalElementId viewScope,
      StyleSheetCollector styles,
      Tokens tokens,
      DocumentState docState) {
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

  public GlobalElementId viewScope() {
    return viewScope;
  }

  public StyleSheetCollector styles() {
    return styles;
  }

  public Tokens tokens() {
    return tokens;
  }

  public FootnoteEntry registerFootnote(String rawId, Element content) {
    String slug = Fnv1a64.sanitizeIdentifier(rawId);
    String key = viewScope.domId().isEmpty() ? slug : viewScope.domId() + "--" + slug;
    return docState.footnotes.computeIfAbsent(
        key,
        k -> {
          int number = docState.footnotes.size() + 1;
          String refDomId = viewScope.extend(ElementId.of("fnref-" + slug)).domId();
          String noteDomId = "fn-" + k;
          return new FootnoteEntry(number, slug, refDomId, noteDomId, content);
        });
  }

  public Collection<FootnoteEntry> footnotes() {
    return Collections.unmodifiableCollection(docState.footnotes.values());
  }

  public boolean hasUnrenderedFootnotes() {
    return !docState.footnotes.isEmpty() && !docState.footnotesRendered;
  }

  public void markFootnotesRendered() {
    docState.footnotesRendered = true;
  }

  public TocEntry registerTocEntry(String rawId, String title, int level) {
    String domId = viewScope.extend(ElementId.of(rawId)).domId();
    return docState.tocEntries.computeIfAbsent(domId, id -> new TocEntry(id, title, level));
  }

  public List<TocEntry> tocEntries() {
    return List.copyOf(docState.tocEntries.values());
  }

  public void registerHeroImage(String href) {
    if (docState.heroImageHref != null) {
      throw new IllegalStateException(
          "Only one hero image allowed per page: already registered '"
              + docState.heroImageHref
              + "', cannot register '"
              + href
              + "'");
    }
    docState.heroImageHref = href;
  }

  public @Nullable String heroImageHref() {
    return docState.heroImageHref;
  }

  public <R> R withViewScope(ElementId viewEntityId, Supplier<R> work) {
    var childContext = new RenderContext(viewScope.extend(viewEntityId), styles, tokens, docState);
    return ScopedValue.where(CURRENT, childContext).call(work::get);
  }

  public static <R> R runRoot(StyleSheetCollector styles, Tokens tokens, Supplier<R> work) {
    var rootContext = new RenderContext(GlobalElementId.ROOT, styles, tokens);
    return ScopedValue.where(CURRENT, rootContext).call(work::get);
  }
}
