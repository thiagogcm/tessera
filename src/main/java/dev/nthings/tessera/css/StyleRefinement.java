package dev.nthings.tessera.css;

import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.Fnv1a64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Sparse, cascade-mergeable collection of {@link CssDeclaration} values and scoped CSS custom
 * properties backed by an {@link EnumMap} for O(1) property overrides and deterministic
 * ordinal-ordered hashing.
 */
public final class StyleRefinement
    implements Styled<StyleRefinement>, FluentBuilder<StyleRefinement> {

  private final EnumMap<CssProperty, CssDeclaration> declarations =
      new EnumMap<>(CssProperty.class);
  private final Map<String, String> customProperties = new LinkedHashMap<>();

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

  public StyleRefinement customProperty(String name, String value) {
    String normalized = name.startsWith("--") ? name : "--" + Fnv1a64.sanitizeIdentifier(name);
    customProperties.put(normalized, value);
    return this;
  }

  public StyleRefinement mergeFrom(StyleRefinement other) {
    declarations.putAll(other.declarations);
    customProperties.putAll(other.customProperties);
    return this;
  }

  public boolean isEmpty() {
    return declarations.isEmpty() && customProperties.isEmpty();
  }

  public @Nullable CssDeclaration get(CssProperty property) {
    return declarations.get(property);
  }

  public Collection<CssDeclaration> declarations() {
    return Collections.unmodifiableCollection(declarations.values());
  }

  public Map<CssProperty, CssDeclaration> asMap() {
    return Collections.unmodifiableMap(declarations);
  }

  public Map<String, String> customProperties() {
    return Collections.unmodifiableMap(customProperties);
  }

  public long contentHash() {
    long hash = Fnv1a64.OFFSET_BASIS;
    for (var entry : declarations.entrySet()) {
      hash = Fnv1a64.updateInt(hash, entry.getKey().ordinal());
      hash = Fnv1a64.updateChars(hash, entry.getValue().cssValue());
    }
    for (var entry : customProperties.entrySet()) {
      hash = Fnv1a64.updateChars(hash, entry.getKey());
      hash = Fnv1a64.updateChars(hash, entry.getValue());
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
    for (var entry : customProperties.entrySet()) {
      if (!sb.isEmpty()) {
        sb.append(';');
      }
      sb.append(entry.getKey()).append(':').append(entry.getValue());
    }
    return sb.toString();
  }
}
