package dev.nthings.tessera.internal.css;

import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.Keyframes;
import dev.nthings.tessera.css.ScopeBlock;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.internal.foundation.Registration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// Collection and lowering operations implemented alongside the package-private CSS serializer.
public abstract class CssAccess {
  // Initializing Theme installs the CSS package's accessor.
  private static final Registration<CssAccess> REGISTERED =
      new Registration<>("CSS accessor", Theme::defaults);

  protected CssAccess() {}

  /// A class rule keyed by its content hash; `prelude` wraps it in a condition when not empty. The
  /// text is built only by [#css()], so a rule already registered under `key` costs no string.
  public record Rule(String key, String prelude, String body) {
    public String css() {
      var rule = "." + key + "{" + body + "}";
      return prelude.isEmpty() ? rule : prelude + "{" + rule + "}";
    }
  }

  public record ElementRules(
      @Nullable Rule interactive, List<Rule> containers, List<Keyframes> keyframes) {
    public ElementRules {
      containers = List.copyOf(containers);
      keyframes = List.copyOf(keyframes);
    }
  }

  public record ScopeRules(String css, List<Keyframes> keyframes) {
    public ScopeRules {
      keyframes = List.copyOf(keyframes);
    }
  }

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
