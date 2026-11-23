package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/// A mutable set of declarations and custom properties, written through the [Styled] methods, plus
/// the conditional styles written with [#on(Condition, StyleBlock)] and
/// [#startingStyle(StyleBlock)]. Elements hold one for their style, and state, selector and scope
/// callbacks receive one to fill. The last call wins: writing a property again replaces its value,
/// and writing a shorthand (such as [#p(CssLength)] or [#border(Border)]) removes the longhands it
/// covers (such as [#pt(CssLength)] or [#borderBlockEnd(Border)]), so a longhand only wins when
/// written after its shorthand.
public final class Declarations implements Styled<Declarations>, FluentBuilder<Declarations> {

  /// The state and media conditions in emission order: states first (in [State] order), then media.
  /// Container queries follow them in first-use order, so a later state wins over an earlier one at
  /// equal specificity.
  private static final List<Condition> STATE_AND_MEDIA_ORDER =
      Stream.concat(Stream.of(State.values()), Stream.of(Media.values()))
          .map(Condition.class::cast)
          .toList();

  // Markers that make the structural hash prefix-free: each nested rule opens with its kind, and
  // every rule list ends with RULE_END.
  private static final int RULE_END = 0;
  private static final int STATE_OR_MEDIA_RULE = 1;
  private static final int CONTAINER_RULE = 2;
  private static final int STARTING_STYLE_RULE = 3;

  private final EnumMap<CssProperty, String> declarations = new EnumMap<>(CssProperty.class);
  private final Map<String, String> customProperties = new LinkedHashMap<>();

  /// Created on first use, so a plain style allocates no conditional state.
  private @Nullable Map<Condition, Declarations> conditional;

  private @Nullable Set<Keyframes> keyframes;
  private @Nullable Declarations startingStyle;

  /// The [Media] features an enclosing rule already requires, as a bit set of their ordinals. A
  /// nested rule for one of them would add nothing, so [#nested(Condition)] writes it here instead.
  private final int enclosingMedia;

  /// Creates an empty refinement, for building a style outside an element.
  public Declarations() {
    this(0);
  }

  /// An empty refinement inside rules that already require the [Media] features in
  /// `enclosingMedia`, a bit set of their ordinals.
  Declarations(int enclosingMedia) {
    this.enclosingMedia = enclosingMedia;
  }

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
      start = new Declarations(enclosingMedia);
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

  /// The refinement holding the styles for `condition`, created on first use. A media feature an
  /// enclosing rule already requires adds nothing, so this refinement holds those styles itself.
  Declarations nested(Condition condition) {
    var media = enclosingMedia;
    if (condition instanceof Media feature) {
      var bit = mediaBit(feature);
      if ((media & bit) != 0) {
        return this;
      }
      media |= bit;
    }
    var rules = conditional;
    if (rules == null) {
      rules = new LinkedHashMap<>();
      conditional = rules;
    }
    var nestedMedia = media;
    return rules.computeIfAbsent(condition, _ -> new Declarations(nestedMedia));
  }

  /// The bit of `feature` in an enclosing-media bit set.
  static int mediaBit(Media feature) {
    return 1 << feature.ordinal();
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

  /// A content hash of the plain declarations and custom properties, used to name the generated
  /// utility class. Each text is preceded by its length, so `--col-1:23` and `--col-12:3` hash
  /// apart.
  long contentHash() {
    return updateDeclarations(Fnv1a64.OFFSET_BASIS);
  }

  /// A structural hash of [#nestedRules()], computed without serializing it: equal styles hash
  /// equal, so the hash names the generated interactive class.
  long nestedRulesHash() {
    var hash = updateStateAndMediaRules(Fnv1a64.OFFSET_BASIS);
    return Fnv1a64.updateByte(updateStartingStyle(hash), RULE_END);
  }

  /// Folds everything [#toCssBody()] serializes into `hash`, in the same order, without serializing
  /// it. Counts and rule markers delimit each part, so different structures never fold the same
  /// sequence, and equal styles fold equal hashes.
  long updateBody(long hash) {
    var h = updateStateAndMediaRules(updateDeclarations(hash));
    var rules = conditional;
    if (rules != null) {
      for (var entry : rules.entrySet()) {
        if (entry.getKey() instanceof ContainerQuery query && !entry.getValue().isEmpty()) {
          h = Fnv1a64.updateByte(h, CONTAINER_RULE);
          h = updateText(h, query.prelude());
          h = entry.getValue().updateBody(h);
        }
      }
    }
    return Fnv1a64.updateByte(updateStartingStyle(h), RULE_END);
  }

  private long updateDeclarations(long hash) {
    var h = Fnv1a64.updateInt(hash, declarations.size());
    for (var entry : declarations.entrySet()) {
      h = Fnv1a64.updateInt(h, entry.getKey().ordinal());
      h = updateText(h, entry.getValue());
    }
    h = Fnv1a64.updateInt(h, customProperties.size());
    for (var entry : customProperties.entrySet()) {
      h = updateText(h, entry.getKey());
      h = updateText(h, entry.getValue());
    }
    return h;
  }

  private long updateStateAndMediaRules(long hash) {
    var h = hash;
    var rules = conditional;
    if (rules == null) {
      return h;
    }
    for (int i = 0, count = STATE_AND_MEDIA_ORDER.size(); i < count; i++) {
      var style = rules.get(STATE_AND_MEDIA_ORDER.get(i));
      if (style != null && !style.isEmpty()) {
        h = Fnv1a64.updateInt(Fnv1a64.updateByte(h, STATE_OR_MEDIA_RULE), i);
        h = style.updateBody(h);
      }
    }
    return h;
  }

  private long updateStartingStyle(long hash) {
    var start = startingStyle;
    if (start == null || start.isEmpty()) {
      return hash;
    }
    return start.updateBody(Fnv1a64.updateByte(hash, STARTING_STYLE_RULE));
  }

  /// Folds `text`, preceded by its length, into `hash`.
  static long updateText(long hash, String text) {
    return Fnv1a64.updateChars(Fnv1a64.updateInt(hash, text.length()), text);
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

  /// Whether a state or media rule, or the starting style, emits something: whether
  /// [#nestedRules()] is not empty.
  boolean hasNestedRulesBesideContainers() {
    var start = startingStyle;
    if (start != null && !start.isEmpty()) {
      return true;
    }
    var rules = conditional;
    if (rules != null) {
      for (var condition : STATE_AND_MEDIA_ORDER) {
        var style = rules.get(condition);
        if (style != null && !style.isEmpty()) {
          return true;
        }
      }
    }
    return false;
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
    for (var condition : STATE_AND_MEDIA_ORDER) {
      var style = rules.get(condition);
      if (style != null) {
        appendRule(sb, prelude(condition), style);
      }
    }
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
