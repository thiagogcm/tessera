package dev.nthings.tessera.internal.css;

import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.Keyframes;
import dev.nthings.tessera.css.ScopeBlock;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.internal.foundation.Registration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Collection and lowering operations implemented alongside the package-private CSS serializer.
public abstract class CssAccess {
  // Initializing Theme installs the CSS package's accessor.
  private static final Registration<CssAccess> REGISTERED =
      new Registration<>("CSS accessor", Theme::defaults);

  protected CssAccess() {}

  /// A rule keyed by a structural hash of the styles it serializes: equal styles get equal keys
  /// without serializing them. `text` serializes the rule from those styles, so the stylesheet
  /// calls [#css()] only for a key it does not hold yet, before the styles can change.
  public record Rule(String key, Supplier<String> text) {
    public String css() {
      return text.get();
    }
  }

  /// The conditional rules of one element style. The CSS package builds it from lists it already
  /// made immutable.
  public record ElementRules(
      @Nullable Rule interactive, List<Rule> containers, List<Keyframes> keyframes) {}

  /// The styles of one id scope: `rule` holds the body of its `@scope` block, keyed by its base-36
  /// structural hash, and is null when the scope emits no rules.
  public record ScopeRules(@Nullable Rule rule, List<Keyframes> keyframes) {}

  public abstract @Nullable String utilityKey(Declarations style);

  public abstract String utilityRule(String key, Declarations style);

  public abstract ElementRules collectConditional(Declarations style);

  public abstract ScopeRules collectScope(ScopeBlock configure);

  public abstract String keyframesRule(Keyframes keyframes);

  public abstract String tokens(Theme theme);

  public abstract void writeLiveShape(
      Declarations live, Declarations into, Map<String, String> values, Consumer<String> register);

  public abstract boolean hasAspectRatio(Declarations style);

  public abstract void viewTransition(
      Declarations style, String name, @Nullable String transitionClass);

  public static void register(CssAccess access) {
    REGISTERED.register(access);
  }

  public static CssAccess get() {
    return REGISTERED.get();
  }
}
