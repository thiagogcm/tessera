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

/// A mutable set of declarations and custom properties, written through the [Styled] methods, plus
/// the conditional styles written with [#on(Condition, StyleBlock)] and
/// [#startingStyle(StyleBlock)]. Elements hold one for their style, and state, selector and scope
/// callbacks receive one to fill. The last call wins: writing a property again replaces its value,
/// and writing a shorthand (such as [#p(CssLength)] or [#border(Border)]) removes the longhands it
/// covers (such as [#pt(CssLength)] or [#borderBlockEnd(Border)]), so a longhand only wins when
/// written after its shorthand.
public final class Declarations implements Styled<Declarations>, FluentBuilder<Declarations> {

  /// Nested rules are emitted states first (in [State] order), then media, then container queries
  /// (in first-use order), so a later state wins over an earlier one at equal specificity.
  private static final Comparator<Condition> EMISSION_ORDER =
      Comparator.comparingInt(Declarations::emissionRank);

  private final EnumMap<CssProperty, String> declarations = new EnumMap<>(CssProperty.class);
  private final Map<String, String> customProperties = new LinkedHashMap<>();

  /// Created on first use, so a plain style allocates no conditional state.
  private @Nullable Map<Condition, Declarations> conditional;

  private @Nullable Set<Keyframes> keyframes;
  private @Nullable Declarations startingStyle;

  /// Creates an empty refinement, for building a style outside an element.
  public Declarations() {}

  @Override
  public Declarations self() {
    return this;
  }

  /// Every declaration goes through here, so a shorthand removes the longhands it covers.
  Declarations put(CssProperty property, String value) {
    property.longhands().forEach(declarations::remove);
    declarations.put(property, value);
    return this;
  }

  Declarations put(CssProperty property, CssValue value) {
    return put(property, value.toCss());
  }

  /// A comma-separated list of values, as `box-shadow` and `transition-property` take.
  Declarations put(CssProperty property, List<? extends CssValue> values) {
    return put(property, values.stream().map(CssValue::toCss).collect(Collectors.joining(", ")));
  }

  Declarations put(CssProperty property, CssValue first, CssValue... rest) {
    var values = new ArrayList<CssValue>(rest.length + 1);
    values.add(first);
    values.addAll(List.of(rest));
    return put(property, values);
  }

  /// Defines `property` on the elements this style applies to, as `--name: value`.
  public Declarations customProperty(CustomProperty property, CssValue value) {
    customProperties.put(property.toCss(), value.toCss());
    return this;
  }

  /// Runs `styles` on the values an entry transition starts from, emitted as a nested
  /// `@starting-style` block: when the element first renders or leaves `display: none`, it starts
  /// from these values and transitions to its normal style. Calling again extends the same block.
  public Declarations startingStyle(StyleBlock styles) {
    var start = startingStyle;
    if (start == null) {
      start = new Declarations();
      startingStyle = start;
    }
    styles.configure(start);
    return this;
  }

  /// Writes `other`'s declarations, custom properties and conditional styles into this one as if
  /// their style methods were called here: `other` wins on conflicts, and its shorthands remove
  /// this one's longhands.
  public Declarations mergeFrom(Declarations other) {
    if (other == this) {
      return this;
    }
    other.declarations.forEach(this::put);
    customProperties.putAll(other.customProperties);
    mergeConditionalFrom(other);
    return this;
  }

  /// Writes `other`'s conditional styles, keyframes and starting style into this one.
  private void mergeConditionalFrom(Declarations other) {
    var otherConditional = other.conditional;
    if (otherConditional != null) {
      otherConditional.forEach((condition, style) -> nested(condition).mergeFrom(style));
    }
    var otherKeyframes = other.keyframes;
    if (otherKeyframes != null) {
      keyframes().addAll(otherKeyframes);
    }
    var otherStart = other.startingStyle;
    if (otherStart != null) {
      startingStyle(start -> start.mergeFrom(otherStart));
    }
  }

  /// The refinement holding the styles for `condition`, created on first use.
  Declarations nested(Condition condition) {
    var rules = conditional;
    if (rules == null) {
      rules = new LinkedHashMap<>();
      conditional = rules;
    }
    return rules.computeIfAbsent(condition, _ -> new Declarations());
  }

  private Set<Keyframes> keyframes() {
    var used = keyframes;
    if (used == null) {
      used = new LinkedHashSet<>();
      keyframes = used;
    }
    return used;
  }

  /// Records `animation`'s keyframes for registration and writes it under [Media#MOTION_SAFE].
  Declarations animate(Animation animation) {
    keyframes().add(animation.keyframes());
    animation.writeTo(nested(Media.MOTION_SAFE));
    return this;
  }

  /// Whether conditional styles, a starting style or keyframes were written, so the style may emit
  /// more than its plain declarations.
  boolean hasConditionalStyles() {
    return conditional != null || startingStyle != null || keyframes != null;
  }

  /// Every keyframes rule this style or a nested one uses.
  void collectKeyframes(Set<Keyframes> target) {
    var used = keyframes;
    if (used != null) {
      target.addAll(used);
    }
    var rules = conditional;
    if (rules != null) {
      rules.values().forEach(style -> style.collectKeyframes(target));
    }
    var start = startingStyle;
    if (start != null) {
      start.collectKeyframes(target);
    }
  }

  /// Compiler-facing: sets `view-transition-name` to `name` and, when `transitionClass` is not
  /// null, `view-transition-class`. Authors attach a `TransitionKey` to the element instead; the
  /// compiler passes that key's name and class, which the key has already validated, so they are
  /// emitted as given.
  Declarations viewTransition(String name, @Nullable String transitionClass) {
    put(CssProperty.VIEW_TRANSITION_NAME, name);
    if (transitionClass != null) {
      put(CssProperty.VIEW_TRANSITION_CLASS, transitionClass);
    }
    return this;
  }

  /// The `--tl-<property>` custom property each live declaration reads its value from. Indexed by
  /// [CssProperty#ordinal()].
  private static final CssIdent[] LIVE_NAMES = liveNames();

  private static CssIdent[] liveNames() {
    var properties = CssProperty.values();
    var names = new CssIdent[properties.length];
    for (var property : properties) {
      names[property.ordinal()] = CssIdent.custom("tl-" + property.cssName());
    }
    return names;
  }

  /// Writes the live form of this style into `into` for a live backend: each plain declaration
  /// reads its value from the custom property `--tl-<property>` on the element, so the rule stays
  /// the same however the values change. `values` receives each of those properties with its
  /// current value, plus this style's own custom properties, which are live values as they are;
  /// `generated` receives the names of the `--tl-` properties. Conditional and starting styles are
  /// written as they are.
  void writeLiveShape(Declarations into, Map<String, String> values, Consumer<String> generated) {
    declarations.forEach((property, value) -> {
      var name = LIVE_NAMES[property.ordinal()];
      into.put(property, name.varReference());
      values.put(name.toCss(), value);
      generated.accept(name.toCss());
    });
    values.putAll(customProperties);
    into.mergeConditionalFrom(this);
  }

  /// Compiler-facing: whether an `aspect-ratio` is declared, which reserves an image's layout space
  /// in place of `width` and `height` attributes.
  boolean hasAspectRatio() {
    return declarations.containsKey(CssProperty.ASPECT_RATIO);
  }

  /// Whether this style emits nothing, including its nested rules.
  boolean isEmpty() {
    return !hasDeclarations() && !hasNestedRules();
  }

  /// Whether plain declarations or custom properties are set.
  boolean hasDeclarations() {
    return !declarations.isEmpty() || !customProperties.isEmpty();
  }

  /// Whether a nested rule, container queries included, emits something.
  private boolean hasNestedRules() {
    var start = startingStyle;
    if (start != null && !start.isEmpty()) {
      return true;
    }
    var rules = conditional;
    if (rules != null) {
      for (var style : rules.values()) {
        if (!style.isEmpty()) {
          return true;
        }
      }
    }
    return false;
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

  /// The full block body, without braces: the declarations, then the state and media rules, the
  /// container queries and `@starting-style`.
  String toCssBody() {
    var nested = new StringBuilder();
    appendStateAndMediaRules(nested);
    containerRules().forEach((query, style) -> appendRule(nested, prelude(query), style));
    appendStartingStyle(nested);
    var sb = new StringBuilder(toCssBlockBody());
    if (!sb.isEmpty() && !nested.isEmpty()) {
      sb.append(';');
    }
    return sb.append(nested).toString();
  }

  /// The nested rules except container queries: state and media rules, then `@starting-style`.
  /// Empty when none emits anything.
  String nestedRules() {
    var sb = new StringBuilder();
    appendStateAndMediaRules(sb);
    appendStartingStyle(sb);
    return sb.toString();
  }

  /// The container-query rules that emit something, in first-use order.
  Map<ContainerQuery, Declarations> containerRules() {
    var rules = conditional;
    if (rules == null) {
      return Map.of();
    }
    var queries = new LinkedHashMap<ContainerQuery, Declarations>();
    rules.forEach((condition, style) -> {
      if (condition instanceof ContainerQuery query && !style.isEmpty()) {
        queries.put(query, style);
      }
    });
    return queries;
  }

  private void appendStateAndMediaRules(StringBuilder sb) {
    var rules = conditional;
    if (rules == null) {
      return;
    }
    rules.entrySet().stream()
        .filter(entry -> !(entry.getKey() instanceof ContainerQuery))
        .sorted(Map.Entry.comparingByKey(EMISSION_ORDER))
        .forEach(entry -> appendRule(sb, prelude(entry.getKey()), entry.getValue()));
  }

  private void appendStartingStyle(StringBuilder sb) {
    var start = startingStyle;
    if (start != null) {
      appendRule(sb, "@starting-style", start);
    }
  }

  private static void appendRule(StringBuilder sb, String prelude, Declarations style) {
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
