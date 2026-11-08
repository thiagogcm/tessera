package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/// Sparse, cascade-mergeable CSS declarations (property → value) and scoped custom properties,
/// backed by an [EnumMap] for O(1) overrides and deterministic ordinal-ordered hashing.
public final class StyleRefinement
    implements Styled<StyleRefinement>, FluentBuilder<StyleRefinement> {

  private final EnumMap<CssProperty, String> declarations = new EnumMap<>(CssProperty.class);
  private final Map<String, String> customProperties = new LinkedHashMap<>();

  @Override
  public StyleRefinement self() {
    return this;
  }

  @Override
  public StyleRefinement style() {
    return this;
  }

  /// Declarations are made through the typed [Styled] methods; this stays package-private so no
  /// declaration is built from raw text outside the value types.
  StyleRefinement put(CssProperty property, String value) {
    declarations.put(property, value);
    return this;
  }

  StyleRefinement put(CssProperty property, CssValue value) {
    return put(property, value.toCss());
  }

  /// A comma-separated list of values, as `box-shadow` and `transition-property` take.
  StyleRefinement put(CssProperty property, List<? extends CssValue> values) {
    return put(property, values.stream().map(CssValue::toCss).collect(Collectors.joining(", ")));
  }

  StyleRefinement put(CssProperty property, CssValue first, CssValue... rest) {
    var values = new ArrayList<CssValue>(rest.length + 1);
    values.add(first);
    values.addAll(List.of(rest));
    return put(property, values);
  }

  /// Defines the custom property `name` (a dashed identifier) as `value`.
  public StyleRefinement customProperty(CssIdent name, CssValue value) {
    customProperties.put(name.requireCustom().value(), value.toCss());
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

  /// Snapshot of computed declarations, independent of this authoring builder.
  public Map<CssProperty, String> properties() {
    return Map.copyOf(declarations);
  }

  public @Nullable String get(CssProperty property) {
    return declarations.get(property);
  }

  public long contentHash() {
    var hash = Fnv1a64.OFFSET_BASIS;
    for (var entry : declarations.entrySet()) {
      hash = Fnv1a64.updateInt(hash, entry.getKey().ordinal());
      hash = Fnv1a64.updateChars(hash, entry.getValue());
    }
    for (var entry : customProperties.entrySet()) {
      hash = Fnv1a64.updateChars(hash, entry.getKey());
      hash = Fnv1a64.updateChars(hash, entry.getValue());
    }
    return hash;
  }

  public String toCssBlockBody() {
    var sb = new StringBuilder();
    for (var entry : declarations.entrySet()) {
      if (!sb.isEmpty()) {
        sb.append(';');
      }
      sb.append(entry.getKey().cssName()).append(':').append(entry.getValue());
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
