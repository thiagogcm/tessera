package dev.nthings.tessera.css;

import dev.nthings.tessera.core.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deduplicating stylesheet collector emitting the 7-layer Chrome 156+ cascade: {@code @layer reset,
 * base, theme, components, utilities, interactive, container;}.
 */
public final class StyleSheetCollector {

  private final Map<String, String> utilityRules = new LinkedHashMap<>();
  private final Map<String, String> interactiveRules = new LinkedHashMap<>();
  private final Map<String, String> containerRules = new LinkedHashMap<>();
  private final Map<String, String> viewScopeRules = new LinkedHashMap<>();

  public List<String> registerElementStyles(
      StyleRefinement baseStyle, Interactivity interactivity) {
    var classNames = new ArrayList<String>(3);

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

  public void registerViewScope(String viewScopeId, String scopedCss) {
    if (!scopedCss.isBlank()) {
      viewScopeRules.putIfAbsent(
          viewScopeId,
          "@scope ([data-view=\"" + viewScopeId + "\"]) to ([data-view]){" + scopedCss + "}");
    }
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
            + "@view-transition{navigation:auto}"
            + "body{font-family:system-ui,-apple-system,sans-serif;"
            + "background:var(--surface-canvas);color:var(--text-primary);line-height:1.5}"
            + "[popover=\"hint\"]{position-area:block-start;"
            + "position-try-fallbacks:flip-block,flip-inline;margin:0.25rem}"
            + "}\n");

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
    if (!viewScopeRules.isEmpty()) {
      sb.append("@layer components{");
      for (var rule : viewScopeRules.values()) {
        sb.append(rule);
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
