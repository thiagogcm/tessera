package dev.nthings.tessera.core;

import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implicit lexical render context propagated across component boundaries via JDK 25 {@link
 * ScopedValue}.
 */
public final class RenderContext {

  public record FootnoteEntry(
      int number, String slug, String refDomId, String noteDomId, Element content) {}

  private static final class FootnoteState {
    private final Map<String, FootnoteEntry> entries = new LinkedHashMap<>();
    private boolean sectionRendered = false;
  }

  private static final ScopedValue<RenderContext> CURRENT = ScopedValue.newInstance();

  private final GlobalElementId viewScope;
  private final StyleSheetCollector styles;
  private final Tokens tokens;
  private final FootnoteState footnotes;

  public RenderContext(GlobalElementId viewScope, StyleSheetCollector styles, Tokens tokens) {
    this(viewScope, styles, tokens, new FootnoteState());
  }

  private RenderContext(
      GlobalElementId viewScope,
      StyleSheetCollector styles,
      Tokens tokens,
      FootnoteState footnotes) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.tokens = tokens;
    this.footnotes = footnotes;
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
    return footnotes.entries.computeIfAbsent(
        key,
        k -> {
          int number = footnotes.entries.size() + 1;
          String refDomId = viewScope.extend(ElementId.of("fnref-" + slug)).domId();
          String noteDomId = "fn-" + k;
          return new FootnoteEntry(number, slug, refDomId, noteDomId, content);
        });
  }

  public Collection<FootnoteEntry> footnotes() {
    return Collections.unmodifiableCollection(footnotes.entries.values());
  }

  public boolean hasUnrenderedFootnotes() {
    return !footnotes.entries.isEmpty() && !footnotes.sectionRendered;
  }

  public void markFootnotesRendered() {
    footnotes.sectionRendered = true;
  }

  public <R> R withViewScope(ElementId viewEntityId, Supplier<R> work) {
    var childContext = new RenderContext(viewScope.extend(viewEntityId), styles, tokens, footnotes);
    return ScopedValue.where(CURRENT, childContext).call(work::get);
  }

  public static <R> R runRoot(StyleSheetCollector styles, Tokens tokens, Supplier<R> work) {
    var rootContext = new RenderContext(GlobalElementId.ROOT, styles, tokens);
    return ScopedValue.where(CURRENT, rootContext).call(work::get);
  }
}
