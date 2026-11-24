package dev.nthings.tessera.internal.css;

import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.Keyframes;
import dev.nthings.tessera.css.ScopeBlock;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Compiler-facing: the stylesheet of one page, site or live render. The compiler registers each
/// element's typed styles and each id scope, receiving the class names or scope keys to emit, and
/// identical rules are deduplicated. [#render(Theme)] writes a document stylesheet with the layers
/// in the order `reset, base, theme, components, utilities, container, interactive`.
///
/// A live backend that mounts views inside a host page uses the embedded output instead: it
/// declares [#LAYER_ORDER] once, inserts [#embeddedBaseRules(Theme)] once per mounted root and each
/// render's [#embeddedRules()], and marks the root's host element with [#ROOT_ATTRIBUTE] and
/// [#THEME_ATTRIBUTE]. Embedded rules apply beneath that host rather than to `:root` and `body`, so
/// mounting never restyles the rest of the page. Both outputs are written from the same registered
/// rules and the same reset and base rules, which take the root they apply beneath.
public final class StyleSheet {

  /// Backend-facing: the attribute marking the host element of an embedded root; the embedded reset
  /// and base rules apply beneath it.
  public static final String ROOT_ATTRIBUTE = "data-tessera-root";

  /// Backend-facing: the attribute selecting an embedded host's token values; its value is
  /// [#themeKey(Theme)].
  public static final String THEME_ATTRIBUTE = "data-tessera-theme";

  /// Backend-facing: the cascade layer order statement. An embedder keeps it ahead of every layered
  /// rule it inserts.
  public static final String LAYER_ORDER =
      "@layer reset, base, theme, components, utilities, container, interactive;";

  /// Backend-facing: one self-contained rule of this stylesheet. Its `key` is short and identifies
  /// the rule's text (a class name for element rules, and a selector, property name or the rule
  /// itself for the others), so an embedder can keep the text it built for a key, compare renders
  /// by key and build [#css()] only for rules it does not hold. The rule is `prefix + body +
  /// suffix`, joined only when its text is needed.
  public record Rule(
      String key, @Nullable String layer, String prefix, String body, String suffix) {
    public Rule(String key, @Nullable String layer, String rule) {
      this(key, layer, "", rule, "");
    }

    /// The rule wrapped in its cascade layer; built on each call.
    public String css() {
      var rule = prefix + body + suffix;
      return layer == null ? rule : "@layer " + layer + "{" + rule + "}";
    }

    void appendRule(StringBuilder into) {
      into.append(prefix).append(body).append(suffix);
    }
  }

  private static final String MOTION_SAFE = "@media (prefers-reduced-motion: no-preference){";

  private final Map<String, String> utilityRules = new LinkedHashMap<>();
  private final Map<String, String> interactiveRules = new LinkedHashMap<>();
  private final Map<String, String> containerRules = new LinkedHashMap<>();
  private final Map<String, String> scopeBodies = new LinkedHashMap<>();
  private final Set<Keyframes> keyframes = new LinkedHashSet<>();
  private final Set<String> transitionClasses = new LinkedHashSet<>();
  private final Set<String> liveProperties = new LinkedHashSet<>();

  /// The rule text of an earlier stylesheet, by kind and key, reused instead of serializing again.
  private final Map<String, String> reusedUtility;

  private final Map<String, String> reusedInteractive;
  private final Map<String, String> reusedContainer;
  private final Map<String, String> reusedScopes;

  /// Creates an empty stylesheet.
  public StyleSheet() {
    this(Map.of(), Map.of(), Map.of(), Map.of());
  }

  /// Backend-facing: creates an empty stylesheet for the render after `previous`'s, which reuses
  /// the text `previous` built for every rule registered again. Keys are structural hashes of the
  /// styles a rule serializes, so an equal key means equal text. Only `previous`'s own rules are
  /// referenced, not those it reused from its predecessor, so a chain of renders keeps one render's
  /// rules alive.
  public StyleSheet(StyleSheet previous) {
    this(
        previous.utilityRules,
        previous.interactiveRules,
        previous.containerRules,
        previous.scopeBodies);
  }

  private StyleSheet(
      Map<String, String> reusedUtility,
      Map<String, String> reusedInteractive,
      Map<String, String> reusedContainer,
      Map<String, String> reusedScopes) {
    this.reusedUtility = reusedUtility;
    this.reusedInteractive = reusedInteractive;
    this.reusedContainer = reusedContainer;
    this.reusedScopes = reusedScopes;
  }

  /// Compiler-facing: registers an element's style and returns the class names that reference its
  /// rules, in emission order (empty for an unstyled element): a `u-` class for the plain
  /// declarations (`utilities` layer), an `i-` class for state, media and starting-style rules
  /// (`interactive` layer), and one `cq-` class per container query (`container` layer). Each class
  /// is named by a structural hash of its styles, and a rule is serialized only the first time this
  /// stylesheet sees its class, unless the stylesheet it reuses built it. The keyframes the style
  /// uses are registered too.
  public List<String> registerElement(Declarations style) {
    var css = CssAccess.get();
    var utility = css.utilityKey(style);
    if (utility != null) {
      utilityRules.computeIfAbsent(
          utility, key -> text(reusedUtility, key, () -> css.utilityRule(key, style)));
    }
    var rules = css.collectConditional(style);
    keyframes.addAll(rules.keyframes());
    if (rules.interactive() == null && rules.containers().isEmpty()) {
      return utility == null ? List.of() : List.of(utility);
    }
    var classes = new ArrayList<String>(3);
    if (utility != null) {
      classes.add(utility);
    }
    var interactive = rules.interactive();
    if (interactive != null) {
      interactiveRules.computeIfAbsent(
          interactive.key(), key -> text(reusedInteractive, key, interactive::css));
      classes.add(interactive.key());
    }
    for (var rule : rules.containers()) {
      containerRules.computeIfAbsent(rule.key(), key -> text(reusedContainer, key, rule::css));
      classes.add(rule.key());
    }
    return classes;
  }

  private static String text(Map<String, String> reused, String key, Supplier<String> build) {
    var text = reused.get(key);
    return text != null ? text : build.get();
  }

  /// Compiler-facing: registers the styles of a scope named `scopeName`, which `configure` writes
  /// into a fresh `ScopeStyle`, and returns its `data-scope` value, `<scopeName>-<hash>`, where the
  /// hash is structural, so a scope already registered here is not serialized again. Identical
  /// scopes share one `@scope` block, and stylesheets from different pages merge without conflicts.
  /// A scope without scoped styles keeps the bare (sanitized) name as its scope boundary. Throws
  /// IllegalArgumentException if `scopeName` is empty.
  public String registerScope(String scopeName, ScopeBlock configure) {
    var safeType = Fnv1a64.sanitizeIdentifier(scopeName);
    var scope = CssAccess.get().collectScope(configure);
    keyframes.addAll(scope.keyframes());
    var rule = scope.rule();
    if (rule == null) {
      return safeType;
    }
    var key = safeType + "-" + rule.key();
    scopeBodies.computeIfAbsent(key, k -> text(reusedScopes, k, rule::css));
    return key;
  }

  /// Compiler-facing: records a view transition class whose group uses the shared 0.35s timing. The
  /// compiler passes the class of a `TransitionKey`, which the key has already validated, so it is
  /// emitted as given.
  public void registerTransitionClass(String transitionClass) {
    transitionClasses.add(transitionClass);
  }

  /// Backend-facing: writes into `into` the declarations a live backend registers for an element's
  /// `live` styles. Each plain declaration of `live` reads its value from a `--tl-<property>`
  /// custom property, so the written declarations, and the rule registered for them, stay the same
  /// however the values change. `values` receives every custom property the backend sets on the
  /// element, with its current value: the generated ones and `live`'s own custom properties.
  /// Embedded output declares the generated properties non-inheriting, so changing one restyles
  /// only its element.
  public void registerLive(Declarations live, Map<String, String> values, Declarations into) {
    CssAccess.get().writeLiveShape(live, into, values, liveProperties::add);
  }

  /// Compiler-facing: merges another stylesheet's rules into this one, keeping first-registration
  /// order.
  public void mergeFrom(StyleSheet other) {
    other.utilityRules.forEach(utilityRules::putIfAbsent);
    other.interactiveRules.forEach(interactiveRules::putIfAbsent);
    other.containerRules.forEach(containerRules::putIfAbsent);
    other.scopeBodies.forEach(scopeBodies::putIfAbsent);
    keyframes.addAll(other.keyframes);
    transitionClasses.addAll(other.transitionClasses);
    liveProperties.addAll(other.liveProperties);
  }

  /// Compiler-facing: renders the complete stylesheet, with `theme`'s tokens as the `:root` custom
  /// properties in `@layer theme`.
  public String render(Theme theme) {
    // The base layer opens with fixed rules; the others are emitted only when they have rules.
    var base = new StringBuilder();
    var layers = new LinkedHashMap<String, StringBuilder>();
    for (var layer : List.of("components", "utilities", "container", "interactive")) {
      layers.put(layer, new StringBuilder());
    }
    // A document declares no live properties, so it has no unlayered rules.
    registeredRules(rule -> {
      var layer = rule.layer();
      if (layer != null) {
        rule.appendRule(layer.equals("base") ? base : Objects.requireNonNull(layers.get(layer)));
      }
    });

    var sb = new StringBuilder(2048);
    sb.append(LAYER_ORDER).append('\n');
    sb.append("@layer reset{").append(resetRules(null)).append("}\n");
    sb.append("@layer base{")
        .append(MOTION_SAFE)
        .append("@view-transition{navigation:auto}")
        .append("::view-transition-old(root),::view-transition-new(root){animation-duration:0.2s}");
    transitionClasses.forEach(cls -> sb.append(transitionGroupRule(cls)));
    sb.append('}').append(baseRules(null)).append(base).append("}\n");
    sb.append("@layer theme{:root{").append(tokenDeclarations(theme)).append("}}\n");
    layers.forEach((layer, rules) -> {
      if (!rules.isEmpty()) {
        sb.append("@layer ").append(layer).append('{').append(rules).append("}\n");
      }
    });
    return sb.toString();
  }

  /// Backend-facing: the reset, base and theme rules of a root embedded in a host page rather than
  /// of a whole document, one rule per entry. They depend only on `theme`, so an embedder inserts
  /// them once per mounted root, after [#LAYER_ORDER] and alongside each render's
  /// [#embeddedRules()]. They select [#ROOT_ATTRIBUTE] and [#THEME_ATTRIBUTE] hosts instead of
  /// `:root` and `body`.
  public static List<String> embeddedBaseRules(Theme theme) {
    var host = "[" + ROOT_ATTRIBUTE + "]";
    var declarations = tokenDeclarations(theme);
    return List.of(
        "@layer reset{" + resetRules(host) + "}",
        "@layer base{" + baseRules(host) + "}",
        "@layer theme{["
            + THEME_ATTRIBUTE
            + "=\""
            + themeKey(declarations)
            + "\"]{"
            + declarations
            + "}}");
  }

  /// Backend-facing: the rules registered here, for a root embedded with
  /// [#embeddedBaseRules(Theme)]. Each entry is one self-contained rule wrapped in its layer, so an
  /// embedder can insert, deduplicate and retain them individually across renders and roots.
  /// Cross-document navigation transitions are omitted; live property registrations come as
  /// `@property` rules.
  public List<Rule> embeddedRules() {
    var rules = new ArrayList<Rule>();
    for (var cls : transitionClasses) {
      rules.add(
          new Rule(
              "::view-transition-group(*." + cls + ")",
              "base",
              MOTION_SAFE,
              transitionGroupRule(cls),
              "}"));
    }
    registeredRules(rules::add);
    return rules;
  }

  /// Every rule registered here except transition timings, which each output places itself, in
  /// emission order: keyframes, live property registrations (unlayered), then the component,
  /// utility, interactive and container rules.
  private void registeredRules(Consumer<Rule> sink) {
    var css = CssAccess.get();
    keyframes.forEach(keyframesRule -> {
      var rule = css.keyframesRule(keyframesRule);
      sink.accept(new Rule(rule, "base", rule));
    });
    liveProperties.forEach(name ->
        sink.accept(new Rule(name, null, "@property ", name, "{syntax:\"*\";inherits:false}")));
    scopeBodies.forEach((key, body) -> {
      var selector = "[data-scope=\"" + key + "\"]";
      sink.accept(
          new Rule(
              selector,
              "components",
              "@scope (" + selector + ") to (:scope [data-scope] > *){",
              body,
              "}"));
    });
    utilityRules.forEach((key, rule) -> sink.accept(new Rule(key, "utilities", rule)));
    interactiveRules.forEach((key, rule) -> sink.accept(new Rule(key, "interactive", rule)));
    containerRules.forEach((key, rule) -> sink.accept(new Rule(key, "container", rule)));
  }

  /// The reset rules beneath `host`, an embedded root's selector, or of the whole document when
  /// `host` is null.
  private static String resetRules(@Nullable String host) {
    var root =
        host == null
            ? ":root{interpolate-size:allow-keywords;color-scheme:light dark;"
                + "scrollbar-gutter:stable}"
            : host + "{interpolate-size:allow-keywords;color-scheme:light dark}";
    var boxes =
        within(host, "*")
            + ","
            + within(host, "*::before")
            + ","
            + within(host, "*::after")
            + "{box-sizing:border-box;margin:0;padding:0}";
    var media = within(host, "img,svg,video") + "{max-inline-size:100%;block-size:auto}";
    // The rules set disjoint properties, so their order, kept from each output's history, does not
    // affect the cascade.
    return host == null ? boxes + root + media : root + boxes + media;
  }

  /// The base rules beneath `host`, an embedded root's selector, or of the whole document when
  /// `host` is null.
  private static String baseRules(@Nullable String host) {
    return (host == null ? "body" : host)
        + "{font-family:var(--font-body);font-size:var(--font-size-md);"
        + "background:var(--surface-canvas);color:var(--text-primary);"
        + "line-height:var(--line-height-body)}"
        + within(host, "button,input,select,textarea")
        + "{font-family:inherit}"
        + within(host, "dialog")
        + "{margin:auto;overscroll-behavior:contain}";
  }

  /// `selectors` as they apply beneath `host`, or as given for a whole document.
  private static String within(@Nullable String host, String selectors) {
    if (host == null) {
      return selectors;
    }
    return host + " " + (selectors.indexOf(',') < 0 ? selectors : ":is(" + selectors + ")");
  }

  private static String transitionGroupRule(String transitionClass) {
    return "::view-transition-group(*."
        + transitionClass
        + "){animation-duration:0.35s;animation-timing-function:cubic-bezier(0.2,0,0,1)}";
  }

  /// Backend-facing: a content-addressed key of `theme`'s values, the [#THEME_ATTRIBUTE] value of
  /// an embedded host. Equal themes have equal keys.
  public static String themeKey(Theme theme) {
    return themeKey(tokenDeclarations(theme));
  }

  private static String themeKey(String declarations) {
    return "t-" + Fnv1a64.toBase36Hash(Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, declarations));
  }

  private static String tokenDeclarations(Theme theme) {
    return CssAccess.get().tokens(theme);
  }
}
