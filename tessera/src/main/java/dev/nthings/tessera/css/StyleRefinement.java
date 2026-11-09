package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/// A mutable set of declarations and custom properties, written through the [Styled] methods.
/// Elements hold one for their base style, and state, selector and scope callbacks receive one to
/// fill. The last call wins: writing a property again replaces its value, and writing a shorthand
/// (such as [#p(CssLength)] or [#border(Border)]) removes the longhands it covers (such as
/// [#pt(CssLength)] or [#borderBlockEnd(Border)]), so a longhand only wins when written after
/// its shorthand.
public final class StyleRefinement
    implements Styled<StyleRefinement>, FluentBuilder<StyleRefinement> {

  private final EnumMap<CssProperty, String> declarations = new EnumMap<>(CssProperty.class);
  private final Map<String, String> customProperties = new LinkedHashMap<>();

  /// Creates an empty refinement, for building a style outside an element.
  public StyleRefinement() {}

  @Override
  public StyleRefinement self() {
    return this;
  }

  @Override
  public StyleRefinement style() {
    return this;
  }

  /// Every declaration goes through here, so a shorthand removes the longhands it covers.
  StyleRefinement put(CssProperty property, String value) {
    property.longhands().forEach(declarations::remove);
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

  /// Defines custom property `name`, which must be a dashed identifier such as `--gap`. Throws
  /// IllegalArgumentException otherwise.
  public StyleRefinement customProperty(CssIdent name, CssValue value) {
    customProperties.put(name.requireCustom().value(), value.toCss());
    return this;
  }

  /// Writes `other`'s declarations and custom properties into this one as if their style methods
  /// were called here: `other` wins on conflicts, and its shorthands remove this one's longhands.
  public StyleRefinement mergeFrom(StyleRefinement other) {
    if (other == this) {
      return this;
    }
    other.declarations.forEach(this::put);
    customProperties.putAll(other.customProperties);
    return this;
  }

  /// Compiler-facing: whether an `aspect-ratio` is declared, which reserves an image's layout space
  /// in place of `width` and `height` attributes.
  public boolean hasAspectRatio() {
    return declarations.containsKey(CssProperty.ASPECT_RATIO);
  }

  boolean isEmpty() {
    return declarations.isEmpty() && customProperties.isEmpty();
  }

  /// A content hash of the declarations, used to name the generated class.
  long contentHash() {
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

  /// The declarations as a CSS block body, without braces.
  String toCssBlockBody() {
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
