package dev.nthings.tessera.css;

import dev.nthings.tessera.core.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deduplicating stylesheet collector emitting the 7-layer Chrome 156+ cascade: {@code @layer reset,
 * base, theme, components, utilities, interactive, container;}.
 */
public final class StyleSheetCollector {

  private final Map<String, String> utilityRules = new LinkedHashMap<>();
  private final Map<String, String> interactiveRules = new LinkedHashMap<>();
  private final Map<String, String> containerRules = new LinkedHashMap<>();
  private final Map<String, String> viewScopeBodies = new LinkedHashMap<>();
  private final Set<String> keyframeRules = new LinkedHashSet<>();

  public List<String> registerElementStyles(
      StyleRefinement baseStyle, Interactivity interactivity) {
    var classNames = new ArrayList<String>(3);

    keyframeRules.addAll(interactivity.keyframes());

    if (!baseStyle.isEmpty()) {
      String utilClass = "u-" + Fnv1a64.toBase36Hash(baseStyle.contentHash());
      utilityRules.computeIfAbsent(
          utilClass, k -> "." + k + "{" + baseStyle.toCssBlockBody() + "}");
      classNames.add(utilClass);
    }

    if (interactivity.hasInteractiveStyles()) {
      String body = buildInteractiveBlock(interactivity);
      long hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, body);
      String interactiveClass = "i-" + Fnv1a64.toBase36Hash(hash);
      interactiveRules.computeIfAbsent(interactiveClass, k -> "." + k + "{" + body + "}");
      classNames.add(interactiveClass);
    }

    if (interactivity.hasContainerRules()) {
      for (var rule : interactivity.containerRules()) {
        if (!rule.style().isEmpty()) {
          String cqClass = "cq-" + Fnv1a64.toBase36Hash(rule.contentHash());
          containerRules.computeIfAbsent(
              cqClass,
              k ->
                  "@container "
                      + rule.containerName()
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
    }

    return classNames;
  }

  /**
   * Registers a view's {@code @scope} CSS body using content-addressed deduplication.
   *
   * <p>If {@code viewType} has not been registered or was registered with the exact same {@code
   * scopedCss} body, returns {@code viewType}. If a different {@code scopedCss} body was already
   * registered under {@code viewType}, qualifies the key as {@code <viewType>-<hash>} so distinct
   * parameterized rules never overwrite each other.
   *
   * @return the resolved {@code data-view} attribute value
   */
  public String registerViewScope(String viewType, String scopedCss) {
    String safeType = Fnv1a64.sanitizeIdentifier(viewType);
    if (scopedCss.isBlank()) {
      return safeType;
    }
    String existing = viewScopeBodies.get(safeType);
    if (existing == null) {
      viewScopeBodies.put(safeType, scopedCss);
      return safeType;
    }
    if (existing.equals(scopedCss)) {
      return safeType;
    }
    long hash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, scopedCss);
    String qualifiedKey = safeType + "-" + Fnv1a64.toBase36Hash(hash);
    viewScopeBodies.putIfAbsent(qualifiedKey, scopedCss);
    return qualifiedKey;
  }

  public void registerKeyframeRule(String keyframeRule) {
    keyframeRules.add(keyframeRule);
  }

  /** Merges another collector's rules into this collector in deterministic insertion order. */
  public void mergeFrom(StyleSheetCollector other) {
    other.utilityRules.forEach(utilityRules::putIfAbsent);
    other.interactiveRules.forEach(interactiveRules::putIfAbsent);
    other.containerRules.forEach(containerRules::putIfAbsent);
    other.viewScopeBodies.forEach(viewScopeBodies::putIfAbsent);
    keyframeRules.addAll(other.keyframeRules);
  }

  private static String buildInteractiveBlock(Interactivity interactivity) {
    var sb = new StringBuilder();
    var hover = interactivity.hoverStyle();
    if (hover != null && !hover.isEmpty()) {
      sb.append("&:hover:not(:disabled){").append(hover.toCssBlockBody()).append('}');
    }
    var focusVisible = interactivity.focusVisibleStyle();
    if (focusVisible != null && !focusVisible.isEmpty()) {
      sb.append("&:focus-visible{").append(focusVisible.toCssBlockBody()).append('}');
    }
    var active = interactivity.activeStyle();
    if (active != null && !active.isEmpty()) {
      sb.append("&:active:not(:disabled){").append(active.toCssBlockBody()).append('}');
    }
    for (var entry : interactivity.groupHoverStyles().entrySet()) {
      if (!entry.getValue().isEmpty()) {
        sb.append("[data-group=\"")
            .append(entry.getKey())
            .append("\"]:hover &{")
            .append(entry.getValue().toCssBlockBody())
            .append('}');
      }
    }
    for (var entry : interactivity.hasStyles().entrySet()) {
      if (!entry.getValue().isEmpty()) {
        sb.append("&:has(")
            .append(entry.getKey())
            .append("){")
            .append(entry.getValue().toCssBlockBody())
            .append('}');
      }
    }
    var motionSafe = interactivity.motionSafeStyle();
    if (motionSafe != null && !motionSafe.isEmpty()) {
      sb.append("@media (prefers-reduced-motion: no-preference){")
          .append(motionSafe.toCssBlockBody())
          .append('}');
    }
    return sb.toString();
  }

  public String toCssStylesheet(Tokens tokens) {
    var sb = new StringBuilder(2048);
    sb.append("@layer reset, base, theme, components, utilities, interactive, container;\n");

    // 1. @layer reset
    sb.append(
        "@layer reset{"
            + "*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}"
            + ":root{interpolate-size:allow-keywords;color-scheme:light dark}"
            + "}\n");

    // 2. @layer base
    sb.append(
        "@layer base{"
            + "@media (prefers-reduced-motion: no-preference){@view-transition{navigation:auto}}"
            + "body{font-family:system-ui,-apple-system,sans-serif;"
            + "background:var(--surface-canvas);color:var(--text-primary);line-height:1.5}"
            + "[popover=\"hint\"]{position-area:block-start;"
            + "position-try-fallbacks:flip-block,flip-inline;margin:0.25rem}");
    if (!keyframeRules.isEmpty()) {
      for (var kf : keyframeRules) {
        sb.append(kf);
      }
    }
    sb.append("}\n");

    // 3. @layer theme
    sb.append("@layer theme{");
    sb.append(
        "@function --fluid(--min <length>, --pref <length>, --max <length>) returns <length>{"
            + "result:clamp(var(--min), var(--pref), var(--max))"
            + "}");
    sb.append(":root{");
    for (var token : tokens.all()) {
      sb.append(token.customPropertyName()).append(':').append(token.cssDefinition()).append(';');
    }
    sb.append("}}\n");

    // 4. @layer components
    if (!viewScopeBodies.isEmpty()) {
      sb.append("@layer components{");
      for (var entry : viewScopeBodies.entrySet()) {
        sb.append("@scope ([data-view=\"")
            .append(entry.getKey())
            .append("\"]) to ([data-view]){")
            .append(entry.getValue())
            .append('}');
      }
      sb.append("}\n");
    }

    // 5. @layer utilities
    if (!utilityRules.isEmpty()) {
      sb.append("@layer utilities{");
      for (var rule : utilityRules.values()) {
        sb.append(rule);
      }
      sb.append("}\n");
    }

    // 6. @layer interactive
    if (!interactiveRules.isEmpty()) {
      sb.append("@layer interactive{");
      for (var rule : interactiveRules.values()) {
        sb.append(rule);
      }
      sb.append("}\n");
    }

    // 7. @layer container
    if (!containerRules.isEmpty()) {
      sb.append("@layer container{");
      for (var rule : containerRules.values()) {
        sb.append(rule);
      }
      sb.append("}\n");
    }

    return sb.toString();
  }
}
