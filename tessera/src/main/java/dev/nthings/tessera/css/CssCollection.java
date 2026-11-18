package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;
import dev.nthings.tessera.internal.css.CssAccess;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// Serializes authored styles before the stylesheet combines and deduplicates their rules.
final class CssCollection extends CssAccess {
  private static final ElementRules EMPTY = new ElementRules(null, List.of(), List.of());

  @Override
  public @Nullable String utilityKey(Declarations style) {
    return style.hasDeclarations() ? "u-" + Fnv1a64.toBase36Hash(style.contentHash()) : null;
  }

  @Override
  public String utilityRule(String key, Declarations style) {
    return "." + key + "{" + style.toCssBlockBody() + "}";
  }

  @Override
  public ElementRules collectConditional(Declarations style) {
    if (!style.hasConditionalStyles()) {
      return EMPTY;
    }
    var frames = new LinkedHashSet<Keyframes>();
    style.collectKeyframes(frames);
    var nested = style.nestedRules();
    Rule interactive = null;
    if (!nested.isEmpty()) {
      var key = "i-" + Fnv1a64.toBase36Hash(Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, nested));
      interactive = new Rule(key, "." + key + "{" + nested + "}");
    }
    var containers =
        style.containerRules().entrySet().stream()
            .map(entry -> container(entry.getKey(), entry.getValue()))
            .toList();
    return new ElementRules(interactive, containers, List.copyOf(frames));
  }

  private static Rule container(Condition.Query query, Declarations declarations) {
    var prelude = Declarations.prelude(query);
    var body = declarations.toCssBody();
    var hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, prelude);
    hash = Fnv1a64.updateChars(hash, body);
    var key = "cq-" + Fnv1a64.toBase36Hash(hash);
    return new Rule(key, prelude + "{." + key + "{" + body + "}}");
  }

  @Override
  public ScopeRules collectScope(ScopeBlock configure) {
    var scope = new ScopeStyle();
    configure.configure(scope);
    var frames = new LinkedHashSet<Keyframes>();
    scope.collectKeyframes(frames);
    return new ScopeRules(scope.toCssBody(), List.copyOf(frames));
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
  public boolean hasAspectRatio(Declarations style) {
    return style.hasAspectRatio();
  }

  @Override
  public void viewTransition(Declarations style, String name, @Nullable String transitionClass) {
    style.viewTransition(name, transitionClass);
  }
}
