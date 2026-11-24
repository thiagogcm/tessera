package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.css.CssAccess;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// Keys authored styles by structural hashes and serializes them on demand, so the stylesheet
/// serializes each distinct rule once however many elements and scopes register it. Each rule is
/// described once, as a [RuleWriter]: hashing it names the rule and serializing it gives its text.
final class CssCollection extends CssAccess {
  private static final ElementRules EMPTY = new ElementRules(null, List.of(), List.of());

  /// Writes one rule into a sink, opening its selector with `selector`. The key is the hash of the
  /// rule written with an empty selector, and the text is the rule written with the selector of
  /// that key, so equal keys mean equal text.
  @FunctionalInterface
  private interface RuleWriter {
    void write(CssSink sink, String selector);
  }

  @Override
  public @Nullable Rule utility(Declarations style) {
    return rule(
        "u-",
        null,
        (sink, selector) -> {
          sink.open(selector);
          style.writeDeclarations(sink);
          sink.close();
        });
  }

  @Override
  public ElementRules collectConditional(Declarations style) {
    if (!style.hasConditionalStyles()) {
      return EMPTY;
    }
    var frames = new LinkedHashSet<Keyframes>();
    var interactive =
        rule(
            "i-",
            frames,
            (sink, selector) -> {
              sink.open(selector);
              style.writeNested(sink, false);
              sink.close();
            });
    var containers = new ArrayList<Rule>();
    style.forEachContainerQuery((prelude, body) -> {
      var rule =
          rule(
              "cq-",
              frames,
              (sink, selector) -> {
                sink.open(prelude);
                sink.open(selector);
                body.writeBody(sink);
                sink.close();
                sink.close();
              });
      if (rule != null) {
        containers.add(rule);
      }
    });
    return new ElementRules(interactive, List.copyOf(containers), List.copyOf(frames));
  }

  @Override
  public ScopeRules collectScope(ScopeBlock configure) {
    var scope = new ScopeStyle();
    configure.configure(scope);
    var frames = new LinkedHashSet<Keyframes>();
    var rule = rule("", frames, (sink, _) -> scope.write(sink));
    return new ScopeRules(rule, List.copyOf(frames));
  }

  /// The rule `writer` writes, keyed by `prefix` and its structural hash, or null when it writes
  /// nothing. Adds the keyframes its styles use to `keyframes`, unless that is null.
  private static @Nullable Rule rule(
      String prefix, @Nullable Set<Keyframes> keyframes, RuleWriter writer) {
    var hash = new CssSink.Hash(keyframes);
    writer.write(hash, "");
    if (hash.isEmpty()) {
      return null;
    }
    var key = prefix + Fnv1a64.toBase36Hash(hash.value());
    return new Rule(
        key,
        () -> {
          var text = new CssSink.Text();
          writer.write(text, "." + key);
          return text.toString();
        });
  }

  @Override
  public String keyframesRule(Keyframes keyframes) {
    return keyframes.rule();
  }

  @Override
  public String tokens(Theme theme) {
    var css = new StringBuilder();
    theme.declarations().forEach(declaration -> css.append(declaration).append(';'));
    return css.toString();
  }

  @Override
  public void writeLiveShape(
      Declarations live, Declarations into, Map<String, String> values, Consumer<String> register) {
    live.writeLiveShape(into, values, register);
  }

  @Override
  public String groupClass(Group group) {
    return group.className();
  }

  @Override
  public boolean hasAspectRatio(Declarations style) {
    return style.hasAspectRatio();
  }

  @Override
  public void viewTransition(Declarations style, String name, @Nullable String transitionClass) {
    style.viewTransition(name, transitionClass);
  }
}
