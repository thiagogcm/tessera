package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.css.CssAccess;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// Keys authored styles by structural hashes and serializes them on demand, so the stylesheet
/// serializes each distinct rule once however many elements and scopes register it.
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
    Rule interactive = null;
    if (style.hasNestedRulesBesideContainers()) {
      var key = "i-" + Fnv1a64.toBase36Hash(style.nestedRulesHash());
      interactive = new Rule(key, () -> "." + key + "{" + style.nestedRules() + "}");
    }
    var containers =
        style.containerRules().entrySet().stream()
            .map(entry -> container(entry.getKey(), entry.getValue()))
            .toList();
    return new ElementRules(interactive, containers, List.copyOf(frames));
  }

  private static Rule container(ContainerQuery query, Declarations declarations) {
    var prelude = query.prelude();
    var hash = declarations.updateBody(Declarations.updateText(Fnv1a64.OFFSET_BASIS, prelude));
    var key = "cq-" + Fnv1a64.toBase36Hash(hash);
    return new Rule(key, () -> prelude + "{." + key + "{" + declarations.toCssBody() + "}}");
  }

  @Override
  public ScopeRules collectScope(ScopeBlock configure) {
    var scope = new ScopeStyle();
    configure.configure(scope);
    var frames = new LinkedHashSet<Keyframes>();
    scope.collectKeyframes(frames);
    var rule =
        scope.isEmpty()
            ? null
            : new Rule(
                Fnv1a64.toBase36Hash(scope.updateHash(Fnv1a64.OFFSET_BASIS)), scope::toCssBody);
    return new ScopeRules(rule, List.copyOf(frames));
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
