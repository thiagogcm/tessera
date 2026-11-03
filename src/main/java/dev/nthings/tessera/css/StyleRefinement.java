package dev.nthings.tessera.css;

import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.Fnv1a64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Sparse, cascade-mergeable collection of {@link CssDeclaration} values backed by an {@link
 * EnumMap} for O(1) property overrides and deterministic ordinal-ordered hashing.
 */
public final class StyleRefinement
    implements Styled<StyleRefinement>, FluentBuilder<StyleRefinement> {

  private final EnumMap<CssProperty, CssDeclaration> declarations =
      new EnumMap<>(CssProperty.class);

  @Override
  public StyleRefinement self() {
    return this;
  }

  @Override
  public StyleRefinement style() {
    return this;
  }

  public StyleRefinement put(CssDeclaration declaration) {
    declarations.put(declaration.property(), declaration);
    return this;
  }

  public StyleRefinement mergeFrom(StyleRefinement other) {
    declarations.putAll(other.declarations);
    return this;
  }

  public boolean isEmpty() {
    return declarations.isEmpty();
  }

  public Collection<CssDeclaration> declarations() {
    return Collections.unmodifiableCollection(declarations.values());
  }

  public Map<CssProperty, CssDeclaration> asMap() {
    return Collections.unmodifiableMap(declarations);
  }

  public long contentHash() {
    long hash = Fnv1a64.OFFSET_BASIS;
    for (var entry : declarations.entrySet()) {
      hash = Fnv1a64.updateInt(hash, entry.getKey().ordinal());
      hash = Fnv1a64.updateChars(hash, entry.getValue().cssValue());
    }
    return hash;
  }

  public String toCssBlockBody() {
    var sb = new StringBuilder();
    for (var decl : declarations.values()) {
      if (!sb.isEmpty()) {
        sb.append(';');
      }
      sb.append(decl.toCssRule());
    }
    return sb.toString();
  }
}
