package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
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

  /// The number of [State] constants: states rank first, then group states, then [Media] features.
  private static final int STATE_COUNT = State.values().length;

  private final EnumMap<CssProperty, String> declarations = new EnumMap<>(CssProperty.class);
  private final Map<String, String> customProperties = new LinkedHashMap<>();

  /// The conditional rules in emission order: states (in [State] order), then group states (in
  /// order of their [State], groups in first-use order), then media features (in [Media] order),
  /// then container queries in first-use order, so a later state wins over an earlier one at equal
  /// specificity. Created on first use, so a plain style allocates no conditional state.
  private @Nullable List<Nested> nested;

  private @Nullable Set<Keyframes> keyframes;
  private @Nullable Declarations startingStyle;

  /// The conditions an enclosing rule already requires.
  private final Enclosing enclosing;

  /// A conditional rule: the styles that apply while `condition` holds, opened with `prelude`.
  private record Nested(Condition condition, String prelude, Declarations style) {}

  /// Creates an empty refinement, for building a style outside an element.
  public Declarations() {
    this(Enclosing.NONE);
  }

  /// An empty refinement inside rules that already require the `enclosing` conditions.
  Declarations(Enclosing enclosing) {
    this.enclosing = enclosing;
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
      start = new Declarations(enclosing);
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
    var otherNested = other.nested;
    if (otherNested != null) {
      otherNested.forEach(rule -> nested(rule.condition()).mergeFrom(rule.style()));
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

  /// The refinement holding the styles for `condition`, created on first use. A condition an
  /// enclosing rule already requires adds nothing, so this refinement holds those styles itself.
  Declarations nested(Condition condition) {
    return enclosing.enter(condition, this, inside -> conditionalRule(condition, inside));
  }

  /// The rule for `condition`, created with the `inside` conditions in force if absent, at its
  /// place in emission order.
  private Declarations conditionalRule(Condition condition, Enclosing inside) {
    var rules = nested;
    if (rules == null) {
      rules = new ArrayList<>(2);
      nested = rules;
    }
    for (var rule : rules) {
      if (rule.condition().equals(condition)) {
        return rule.style();
      }
    }
    var rank = rank(condition);
    var index = rules.size();
    while (index > 0 && rank(rules.get(index - 1).condition()) > rank) {
      index--;
    }
    var style = new Declarations(inside);
    rules.add(index, new Nested(condition, prelude(condition), style));
    return style;
  }

  /// The emission rank of `condition`: equal ranks keep first-use order.
  private static int rank(Condition condition) {
    return switch (condition) {
      case State state -> state.ordinal();
      case GroupState group -> STATE_COUNT + group.state().ordinal();
      case Media media -> 2 * STATE_COUNT + media.ordinal();
      case ContainerQuery _ -> Integer.MAX_VALUE;
    };
  }

  /// The prelude that opens a condition's nested rule.
  static String prelude(Condition condition) {
    return switch (condition) {
      case State state -> state.prelude();
      case GroupState group -> group.prelude();
      case Media media -> media.prelude();
      case ContainerQuery query -> query.prelude();
    };
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
    return nested != null || startingStyle != null || keyframes != null;
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

  /// Writes the plain declarations and custom properties into `sink`.
  void writeDeclarations(CssSink sink) {
    declarations.forEach(sink::declaration);
    customProperties.forEach(sink::customProperty);
  }

  /// Writes everything this style emits inside its rule into `sink`: the declarations, then the
  /// state and media rules, the container queries and `@starting-style`.
  void writeBody(CssSink sink) {
    writeDeclarations(sink);
    writeNested(sink, true);
  }

  /// Writes the nested rules into `sink`: the state and media rules, then the container queries
  /// when `containers` is true, then `@starting-style`. Reports the keyframes this style uses, and
  /// those of every nested rule written.
  void writeNested(CssSink sink, boolean containers) {
    var used = keyframes;
    if (used != null) {
      sink.uses(used);
    }
    var rules = nested;
    if (rules != null) {
      for (var rule : rules) {
        if (!containers && rule.condition() instanceof ContainerQuery) {
          // Container queries come last.
          break;
        }
        writeRule(sink, rule.prelude(), rule.style());
      }
    }
    var start = startingStyle;
    if (start != null) {
      writeRule(sink, "@starting-style", start);
    }
  }

  /// Calls `action` with the prelude and styles of each container query, in first-use order.
  void forEachContainerQuery(BiConsumer<String, Declarations> action) {
    var rules = nested;
    if (rules != null) {
      for (var rule : rules) {
        if (rule.condition() instanceof ContainerQuery) {
          action.accept(rule.prelude(), rule.style());
        }
      }
    }
  }

  private static void writeRule(CssSink sink, String prelude, Declarations style) {
    sink.open(prelude);
    style.writeBody(sink);
    sink.close();
  }

  /// The plain declarations and custom properties as a CSS block body, without braces.
  String toCssBlockBody() {
    var text = new CssSink.Text();
    writeDeclarations(text);
    return text.toString();
  }
}
