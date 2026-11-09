package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/// A mutable set of declarations and custom properties, written through the [Styled] methods,
/// plus the conditional styles written with [#on(Condition, Consumer)] and
/// [#startingStyle(Consumer)]. Elements hold one for their style, and state, selector and scope
/// callbacks receive one to fill. The last call wins: writing a property again replaces its value,
/// and writing a shorthand (such as [#p(CssLength)] or [#border(Border)]) removes the longhands it
/// covers (such as [#pt(CssLength)] or [#borderBlockEnd(Border)]), so a longhand only wins when
/// written after its shorthand.
public final class StyleRefinement
    implements Styled<StyleRefinement>, FluentBuilder<StyleRefinement> {

  /// Nested rules are emitted states first (in [State] order), then media, then container queries
  /// (in first-use order), so a later state wins over an earlier one at equal specificity.
  private static final Comparator<Condition> EMISSION_ORDER =
      Comparator.comparingInt(StyleRefinement::emissionRank);

  private final EnumMap<CssProperty, String> declarations = new EnumMap<>(CssProperty.class);
  private final Map<String, String> customProperties = new LinkedHashMap<>();
  private final Map<Condition, StyleRefinement> conditional = new LinkedHashMap<>();
  private final Set<Keyframes> keyframes = new LinkedHashSet<>();
  private @Nullable StyleRefinement startingStyle;

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

  /// Defines `property` on the elements this style applies to, as `--name: value`.
  public StyleRefinement customProperty(CustomProperty property, CssValue value) {
    customProperties.put(property.toCss(), value.toCss());
    return this;
  }

  /// Runs `styles` on the values an entry transition starts from, emitted as a nested
  /// `@starting-style` block: when the element first renders or leaves `display: none`, it starts
  /// from these values and transitions to its normal style. Calling again extends the same block.
  public StyleRefinement startingStyle(Consumer<StyleRefinement> styles) {
    var start = startingStyle;
    if (start == null) {
      start = new StyleRefinement();
      startingStyle = start;
    }
    styles.accept(start);
    return this;
  }

  /// Writes `other`'s declarations, custom properties and conditional styles into this one as if
  /// their style methods were called here: `other` wins on conflicts, and its shorthands remove
  /// this one's longhands.
  public StyleRefinement mergeFrom(StyleRefinement other) {
    if (other == this) {
      return this;
    }
    other.declarations.forEach(this::put);
    customProperties.putAll(other.customProperties);
    other.conditional.forEach((condition, style) -> nested(condition).mergeFrom(style));
    keyframes.addAll(other.keyframes);
    var otherStart = other.startingStyle;
    if (otherStart != null) {
      startingStyle(start -> start.mergeFrom(otherStart));
    }
    return this;
  }

  /// The refinement holding the styles for `condition`, created on first use.
  StyleRefinement nested(Condition condition) {
    return conditional.computeIfAbsent(condition, _ -> new StyleRefinement());
  }

  /// Records `animation`'s keyframes for registration and writes it under [Media#MOTION_SAFE].
  StyleRefinement animate(Animation animation) {
    keyframes.add(animation.keyframes());
    animation.apply(nested(Media.MOTION_SAFE));
    return this;
  }

  /// Every keyframes rule this style or a nested one uses.
  void collectKeyframes(Set<Keyframes> target) {
    target.addAll(keyframes);
    conditional.values().forEach(style -> style.collectKeyframes(target));
    var start = startingStyle;
    if (start != null) {
      start.collectKeyframes(target);
    }
  }

  /// Compiler-facing: sets `view-transition-name` to `name` and, when `transitionClass` is not
  /// null, `view-transition-class`. Authors attach a `TransitionKey` to the element instead.
  /// Throws IllegalArgumentException if either is not a CSS identifier or is a CSS keyword.
  public StyleRefinement viewTransition(String name, @Nullable String transitionClass) {
    put(CssProperty.VIEW_TRANSITION_NAME, CssIdent.of(name));
    if (transitionClass != null) {
      put(CssProperty.VIEW_TRANSITION_CLASS, CssIdent.of(transitionClass));
    }
    return this;
  }

  /// Compiler-facing: whether an `aspect-ratio` is declared, which reserves an image's layout space
  /// in place of `width` and `height` attributes.
  public boolean hasAspectRatio() {
    return declarations.containsKey(CssProperty.ASPECT_RATIO);
  }

  /// Whether this style emits nothing, including its nested rules.
  boolean isEmpty() {
    return !hasDeclarations() && !hasNestedRules(true);
  }

  /// Whether plain declarations or custom properties are set.
  boolean hasDeclarations() {
    return !declarations.isEmpty() || !customProperties.isEmpty();
  }

  /// Whether a nested rule emits something; container queries count only if `includeContainers`.
  boolean hasNestedRules(boolean includeContainers) {
    var start = startingStyle;
    return (start != null && !start.isEmpty())
        || conditional.entrySet().stream()
            .anyMatch(
                entry ->
                    (includeContainers || !(entry.getKey() instanceof ContainerQuery))
                        && !entry.getValue().isEmpty());
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

  /// The full block body, without braces: the declarations, then the nested rules.
  String toCssBody() {
    var sb = new StringBuilder(toCssBlockBody());
    var nested = nestedRules(true);
    if (!sb.isEmpty() && !nested.isEmpty()) {
      sb.append(';');
    }
    return sb.append(nested).toString();
  }

  /// The nested rules only: state and media rules, then container queries when
  /// `includeContainers` is set, then `@starting-style`.
  String nestedRules(boolean includeContainers) {
    var sb = new StringBuilder();
    conditional.entrySet().stream()
        .filter(entry -> includeContainers || !(entry.getKey() instanceof ContainerQuery))
        .sorted(Map.Entry.comparingByKey(EMISSION_ORDER))
        .forEach(entry -> appendRule(sb, prelude(entry.getKey()), entry.getValue()));
    var start = startingStyle;
    if (start != null) {
      appendRule(sb, "@starting-style", start);
    }
    return sb.toString();
  }

  /// The container-query rules, in first-use order.
  Map<ContainerQuery, StyleRefinement> containerRules() {
    var rules = new LinkedHashMap<ContainerQuery, StyleRefinement>();
    conditional.forEach(
        (condition, style) -> {
          if (condition instanceof ContainerQuery query && !style.isEmpty()) {
            rules.put(query, style);
          }
        });
    return rules;
  }

  private static void appendRule(StringBuilder sb, String prelude, StyleRefinement style) {
    if (!style.isEmpty()) {
      sb.append(prelude).append('{').append(style.toCssBody()).append('}');
    }
  }

  /// The prelude that opens a condition's nested rule.
  static String prelude(Condition condition) {
    return switch (condition) {
      case State state -> state.prelude();
      case Media media -> media.prelude();
      case ContainerQuery query -> query.prelude();
    };
  }

  private static int emissionRank(Condition condition) {
    return switch (condition) {
      case State state -> state.ordinal();
      case Media media -> State.values().length + media.ordinal();
      case ContainerQuery _ -> State.values().length + Media.values().length;
    };
  }

  /// The plain declarations and custom properties as a CSS block body, without braces.
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
