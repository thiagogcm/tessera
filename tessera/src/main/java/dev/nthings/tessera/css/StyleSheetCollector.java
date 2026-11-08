package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Deduplicating stylesheet collector emitting the 7-layer modern cascade: `@layer reset, base,
/// theme, components, utilities, interactive, container;`.
public final class StyleSheetCollector {

  private final Map<String, String> utilityRules = new LinkedHashMap<>();
  private final Map<String, String> interactiveRules = new LinkedHashMap<>();
  private final Map<String, String> containerRules = new LinkedHashMap<>();
  private final Map<String, String> viewScopeBodies = new LinkedHashMap<>();
  private final Set<String> keyframeRules = new LinkedHashSet<>();
  private final Set<String> transitionClasses = new LinkedHashSet<>();

  public List<String> registerElementStyles(
      StyleRefinement baseStyle, Interactivity interactivity) {
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

  /// Registers a view's `@scope` body under a content-addressed key:
  /// `<viewType>-<hash(scopedCss)>`. Identical bodies share one `@scope` block; collectors from
  /// different pages merge without conflicts. Views without scoped CSS keep the bare type as
  /// their scope boundary.
  ///
  /// @return the resolved `data-view` attribute value
  public String registerViewScope(String viewType, String scopedCss) {
    var safeType = Fnv1a64.sanitizeIdentifier(viewType);
    if (scopedCss.isBlank()) {
      return safeType;
    }
    var hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, scopedCss);
    var key = safeType + "-" + Fnv1a64.toBase36Hash(hash);
    viewScopeBodies.putIfAbsent(key, scopedCss);
    return key;
  }

  public void registerTransitionClass(String transitionClass) {
    transitionClasses.add(transitionClass);
  }

  /// Merges another collector's rules into this one, in deterministic insertion order.
  public void mergeFrom(StyleSheetCollector other) {
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

  public String toCssStylesheet(Tokens tokens) {
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
      sb.append(token.customPropertyName()).append(':').append(token.cssDefinition()).append(';');
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
