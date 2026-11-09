package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Compiler-facing: the stylesheet of one page or site. The compiler registers each element's
/// typed styles and each view scope, receiving the class names or scope keys to emit, and
/// identical rules are deduplicated. [#render(Tokens)] writes the layers in the order `reset, base,
/// theme, components, utilities, interactive, container`.
public final class StyleSheet {

  private final Map<String, String> utilityRules = new LinkedHashMap<>();
  private final Map<String, String> interactiveRules = new LinkedHashMap<>();
  private final Map<String, String> containerRules = new LinkedHashMap<>();
  private final Map<String, String> viewScopeBodies = new LinkedHashMap<>();
  private final Set<String> keyframeRules = new LinkedHashSet<>();
  private final Set<String> transitionClasses = new LinkedHashSet<>();

  /// Creates an empty stylesheet.
  public StyleSheet() {}

  /// Compiler-facing: registers an element's base, interactive and container rules, and returns
  /// the class names that reference them, in emission order (empty for an unstyled element).
  public List<String> registerElement(StyleRefinement baseStyle, Interactivity interactivity) {
    var classNames = new ArrayList<String>(3);

    keyframeRules.addAll(interactivity.keyframes());

    if (!baseStyle.isEmpty()) {
      var utilClass = "u-" + Fnv1a64.toBase36Hash(baseStyle.contentHash());
      utilityRules.computeIfAbsent(
          utilClass, k -> "." + k + "{" + baseStyle.toCssBlockBody() + "}");
      classNames.add(utilClass);
    }

    if (interactivity.hasInteractiveStyles()) {
      var body = buildInteractiveBlock(interactivity);
      var hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, body);
      var interactiveClass = "i-" + Fnv1a64.toBase36Hash(hash);
      interactiveRules.computeIfAbsent(interactiveClass, k -> "." + k + "{" + body + "}");
      classNames.add(interactiveClass);
    }

    for (var rule : interactivity.containerRules()) {
      if (!rule.style().isEmpty()) {
        var cqClass = "cq-" + Fnv1a64.toBase36Hash(rule.contentHash());
        containerRules.computeIfAbsent(
            cqClass,
            k ->
                "@container "
                    + rule.containerName().toCss()
                    + " "
                    + rule.condition()
                    + "{."
                    + k
                    + "{"
                    + rule.style().toCssBlockBody()
                    + "}}");
        classNames.add(cqClass);
      }
    }

    return classNames;
  }

  /// Compiler-facing: registers `scope`, the styles of a view of `viewType`, and returns its
  /// `data-view` value, `<viewType>-<hash>`. Identical scopes share one `@scope` block, and
  /// stylesheets from different pages merge without conflicts. A view without scoped styles keeps
  /// the bare (sanitized) type as its scope boundary.
  public String registerViewScope(String viewType, ScopeStyle scope) {
    var safeType = Fnv1a64.sanitizeIdentifier(viewType);
    var scopedCss = scope.toCssBody();
    if (scopedCss.isBlank()) {
      return safeType;
    }
    var hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, scopedCss);
    var key = safeType + "-" + Fnv1a64.toBase36Hash(hash);
    viewScopeBodies.putIfAbsent(key, scopedCss);
    return key;
  }

  /// Compiler-facing: records a view transition class whose group uses the shared 0.35s timing.
  public void registerTransitionClass(CssIdent transitionClass) {
    transitionClasses.add(transitionClass.toCss());
  }

  /// Compiler-facing: merges another stylesheet's rules into this one, keeping first-registration
  /// order.
  public void mergeFrom(StyleSheet other) {
    other.utilityRules.forEach(utilityRules::putIfAbsent);
    other.interactiveRules.forEach(interactiveRules::putIfAbsent);
    other.containerRules.forEach(containerRules::putIfAbsent);
    other.viewScopeBodies.forEach(viewScopeBodies::putIfAbsent);
    keyframeRules.addAll(other.keyframeRules);
    transitionClasses.addAll(other.transitionClasses);
  }

  private static String buildInteractiveBlock(Interactivity interactivity) {
    var sb = new StringBuilder();
    interactivity
        .states()
        .forEach(
            (rule, style) -> {
              if (!style.isEmpty()) {
                sb.append(rule.openRule()).append(style.toCssBlockBody()).append('}');
              }
            });
    return sb.toString();
  }

  /// Compiler-facing: renders the complete stylesheet, with `tokens` as the `:root` custom
  /// properties in `@layer theme`.
  public String render(Tokens tokens) {
    var sb = new StringBuilder(2048);
    sb.append("@layer reset, base, theme, components, utilities, interactive, container;\n");

    sb.append(
        "@layer reset{"
            + "*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}"
            + ":root{interpolate-size:allow-keywords;color-scheme:light dark;"
            + "scrollbar-gutter:stable}"
            + "img,svg,video{max-inline-size:100%;block-size:auto}"
            + "}\n");

    sb.append("@layer base{");
    sb.append("@media (prefers-reduced-motion: no-preference){");
    sb.append("@view-transition{navigation:auto}");
    sb.append("::view-transition-old(root),::view-transition-new(root){animation-duration:0.2s}");
    for (var cls : transitionClasses) {
      sb.append("::view-transition-group(*.")
          .append(cls)
          .append("){animation-duration:0.35s;animation-timing-function:cubic-bezier(0.2,0,0,1)}");
    }
    sb.append("}");
    sb.append(
        "body{font-family:var(--font-body);font-size:var(--font-size-md);"
            + "background:var(--surface-canvas);color:var(--text-primary);line-height:var(--line-height-body)}"
            + "button,input,select,textarea{font-family:inherit}"
            + "dialog{margin:auto;overscroll-behavior:contain}");
    keyframeRules.forEach(sb::append);
    sb.append("}\n");

    sb.append("@layer theme{");
    sb.append(":root{");
    for (var token : tokens.all()) {
      sb.append(Tokens.declaration(token)).append(';');
    }
    sb.append("}}\n");

    if (!viewScopeBodies.isEmpty()) {
      sb.append("@layer components{");
      for (var entry : viewScopeBodies.entrySet()) {
        sb.append("@scope ([data-view=\"")
            .append(entry.getKey())
            .append("\"]) to (:scope [data-view] > *){")
            .append(entry.getValue())
            .append('}');
      }
      sb.append("}\n");
    }

    appendLayer(sb, "utilities", utilityRules.values());

    appendLayer(sb, "interactive", interactiveRules.values());

    appendLayer(sb, "container", containerRules.values());

    return sb.toString();
  }

  private static void appendLayer(StringBuilder sb, String layer, Collection<String> rules) {
    if (rules.isEmpty()) {
      return;
    }
    sb.append("@layer ").append(layer).append('{');
    rules.forEach(sb::append);
    sb.append("}\n");
  }
}
