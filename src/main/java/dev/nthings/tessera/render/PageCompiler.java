package dev.nthings.tessera.render;

import dev.nthings.tessera.components.Footnotes;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnceElement;
import dev.nthings.tessera.core.RenderViewElement;
import dev.nthings.tessera.core.StatefulTagElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.css.StyleSheetCollector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.owasp.encoder.Encode;

/**
 * Three-phase static HTML5/CSS document compiler ({@code requestLayout} -> {@code prepaint} ->
 * {@code paint}).
 *
 * <p>Operates on an immutable intermediate {@code ResolvedNode} representation so input {@link
 * Element} and {@link dev.nthings.tessera.core.RenderOnce} trees are never mutated and can be
 * reused across multiple render passes.
 */
public final class PageCompiler {

  public record CompiledPage(String html, @Nullable String externalCssPath, String cssContent) {}

  private sealed interface ResolvedNode
      permits ResolvedTag, ResolvedText, ResolvedRaw, ResolvedEmpty {}

  private record ResolvedTag(
      HtmlTag tag,
      Map<String, String> attributes,
      List<ResolvedNode> children,
      @Nullable ResolvedTag companionSibling)
      implements ResolvedNode {}

  private record ResolvedText(String content) implements ResolvedNode {}

  private record ResolvedRaw(String trustedHtml) implements ResolvedNode {}

  private enum ResolvedEmpty implements ResolvedNode {
    INSTANCE
  }

  private record ResolvedDeferred(int priority, long sequence, ResolvedNode node)
      implements Comparable<ResolvedDeferred> {
    @Override
    public int compareTo(ResolvedDeferred other) {
      int cmp = Integer.compare(this.priority, other.priority);
      return cmp != 0 ? cmp : Long.compare(this.sequence, other.sequence);
    }
  }

  private static final class CompilationState {
    private final PriorityQueue<ResolvedDeferred> deferredQueue = new PriorityQueue<>();
    private final Set<String> registeredDomIds = new LinkedHashSet<>();
    private long deferredSequence = 0L;

    void enqueueDeferred(int priority, ResolvedNode node) {
      deferredQueue.add(new ResolvedDeferred(priority, deferredSequence++, node));
    }
  }

  private PageCompiler() {}

  public static CompiledPage compile(PageDocument document) {
    return compile(document, OutputMode.inlineStyle());
  }

  public static CompiledPage compile(PageDocument document, OutputMode outputMode) {
    var styles = new StyleSheetCollector();
    var state = new CompilationState();

    // Phase 1: requestLayout (component expansion, view scoping, CSS interning, lexical
    // deferred resolution, and automatic endnotes hoisting)
    ResolvedNode resolvedRoot =
        RenderContext.runRoot(
            styles,
            document.tokens(),
            () -> {
              ResolvedNode root = requestLayout(document.bodyRoot().intoElement(), state);
              if (RenderContext.current().hasUnrenderedFootnotes()) {
                ResolvedNode autoFootnotes =
                    requestLayout(Footnotes.section().intoElement(), state);
                state.enqueueDeferred(Integer.MAX_VALUE, autoFootnotes);
              }
              return root;
            });

    // Phase 2: prepaint (WCAG/HTML5 validation, invoker target verification, deferred portal
    // hoisting)
    List<ResolvedNode> bodyNodes = prepaint(resolvedRoot, state);

    // Phase 3: paint (streaming HTML5 & CSS emission)
    String cssContent = styles.toCssStylesheet(document.tokens());
    var htmlBuffer = new StringBuilder(4096);
    String externalCssPath = paint(document, bodyNodes, cssContent, outputMode, htmlBuffer);

    return new CompiledPage(htmlBuffer.toString(), externalCssPath, cssContent);
  }

  private static ResolvedNode requestLayout(Element element, CompilationState state) {
    var ctx = RenderContext.current();
    return switch (element) {
      case EmptyElement _ -> ResolvedEmpty.INSTANCE;
      case TextElement(var content) -> new ResolvedText(content);
      case RawHtmlElement(var html) -> new ResolvedRaw(html);
      case DeferredElement(var child, int priority) -> {
        ResolvedNode resolvedDeferredChild = requestLayout(child, state);
        state.enqueueDeferred(priority, resolvedDeferredChild);
        yield ResolvedEmpty.INSTANCE;
      }
      case RenderOnceElement(var component) -> requestLayout(component.render(), state);
      case RenderViewElement(var view) ->
          ctx.withViewScope(
              view.entityId(),
              () -> {
                String scopeId = RenderContext.current().viewScope().domId();
                RenderContext.current().styles().registerViewScope(scopeId, view.viewScopeRule());
                ResolvedNode inner = requestLayout(view.render(), state);
                return attachViewScopeAttribute(inner, scopeId);
              });
      case TagElement tag -> resolveTag(tag, null, state);
      case StatefulTagElement stateful -> {
        String domId = ctx.viewScope().extend(stateful.elementId()).domId();
        state.registeredDomIds.add(domId);
        yield resolveTag(stateful.delegate(), domId, state);
      }
    };
  }

  private static ResolvedNode attachViewScopeAttribute(ResolvedNode node, String scopeId) {
    if (node instanceof ResolvedTag tag) {
      var updatedAttrs = new LinkedHashMap<>(tag.attributes());
      updatedAttrs.put("data-view", scopeId);
      return new ResolvedTag(
          tag.tag(),
          Collections.unmodifiableMap(updatedAttrs),
          tag.children(),
          tag.companionSibling());
    }
    var wrapperAttrs = Map.of("data-view", scopeId);
    return new ResolvedTag(HtmlTag.DIV, wrapperAttrs, List.of(node), null);
  }

  private static ResolvedTag resolveTag(
      TagElement tag, @Nullable String resolvedDomId, CompilationState state) {
    var ctx = RenderContext.current();
    var attrs = new LinkedHashMap<>(tag.attributes());

    if (resolvedDomId != null) {
      attrs.put("id", resolvedDomId);
    } else {
      String explicitId = attrs.get("id");
      if (explicitId != null && !explicitId.isBlank()) {
        state.registeredDomIds.add(explicitId);
      }
    }

    List<String> generatedClasses =
        ctx.styles().registerElementStyles(tag.style(), tag.interactivity());
    if (!generatedClasses.isEmpty()) {
      String existingClass = attrs.get("class");
      String joined = String.join(" ", generatedClasses);
      attrs.put(
          "class",
          existingClass == null || existingClass.isBlank() ? joined : existingClass + " " + joined);
    }

    var commandTargetId = tag.interactivity().commandTargetId();
    var commandAction = tag.interactivity().commandAction();
    if (commandTargetId != null && commandAction != null) {
      String targetDomId = ctx.viewScope().extend(commandTargetId).domId();
      attrs.put("commandfor", targetDomId);
      attrs.put("command", commandAction);
    }

    ResolvedTag companionTooltip = null;
    var tooltipElement = tag.interactivity().interestTooltip();
    if (tooltipElement != null && resolvedDomId != null) {
      String tooltipDomId = resolvedDomId + "--tooltip";
      attrs.put("interestfor", tooltipDomId);
      state.registeredDomIds.add(tooltipDomId);
      companionTooltip = resolveTag(tooltipElement, tooltipDomId, state);
    }

    var resolvedChildren = new ArrayList<ResolvedNode>(tag.childrenList().size());
    for (var child : tag.childrenList()) {
      ResolvedNode resolvedChild = requestLayout(child, state);
      if (!(resolvedChild instanceof ResolvedEmpty)) {
        resolvedChildren.add(resolvedChild);
      }
    }

    return new ResolvedTag(
        tag.tag(),
        Collections.unmodifiableMap(attrs),
        Collections.unmodifiableList(resolvedChildren),
        companionTooltip);
  }

  private static List<ResolvedNode> prepaint(ResolvedNode root, CompilationState state) {
    validateNodeTree(root, state.registeredDomIds);

    var bodyNodes = new ArrayList<ResolvedNode>();
    if (!(root instanceof ResolvedEmpty)) {
      bodyNodes.add(root);
    }

    while (!state.deferredQueue.isEmpty()) {
      ResolvedDeferred deferred = state.deferredQueue.poll();
      if (deferred != null && !(deferred.node() instanceof ResolvedEmpty)) {
        validateNodeTree(deferred.node(), state.registeredDomIds);
        bodyNodes.add(deferred.node());
      }
    }

    return Collections.unmodifiableList(bodyNodes);
  }

  private static void validateNodeTree(ResolvedNode node, Set<String> registeredDomIds) {
    switch (node) {
      case ResolvedEmpty _, ResolvedText _, ResolvedRaw _ -> {}
      case ResolvedTag tag -> {
        validateTagAccessibility(tag, registeredDomIds);
        for (var child : tag.children()) {
          validateNodeTree(child, registeredDomIds);
        }
        var companion = tag.companionSibling();
        if (companion != null) {
          validateNodeTree(companion, registeredDomIds);
        }
      }
    }
  }

  private static void validateTagAccessibility(ResolvedTag tag, Set<String> registeredDomIds) {
    Map<String, String> attrs = tag.attributes();

    if (tag.tag() == HtmlTag.IMG) {
      String alt = attrs.get("alt");
      if (alt == null) {
        throw new AccessibilityViolationException(
            "<img> element is missing a required 'alt' attribute");
      }
      if (alt.isEmpty()
          && !"presentation".equals(attrs.get("role"))
          && !"true".equals(attrs.get("aria-hidden"))) {
        throw new AccessibilityViolationException(
            "Decorative <img> with empty alt=\"\" must declare role=\"presentation\" or aria-hidden=\"true\"");
      }
    }

    if (tag.tag() == HtmlTag.BUTTON || tag.tag() == HtmlTag.A) {
      if (!hasAccessibleLabel(tag)) {
        throw new AccessibilityViolationException(
            "Interactive <"
                + tag.tag().tagName()
                + "> element must have text content or an aria-label / aria-labelledby / title attribute");
      }
    }

    String commandFor = attrs.get("commandfor");
    if (commandFor != null && !registeredDomIds.contains(commandFor)) {
      throw new AccessibilityViolationException(
          "Invoker attribute commandfor=\""
              + commandFor
              + "\" references a non-existent target ID in the document");
    }

    String interestFor = attrs.get("interestfor");
    if (interestFor != null && !registeredDomIds.contains(interestFor)) {
      throw new AccessibilityViolationException(
          "Interest invoker attribute interestfor=\""
              + interestFor
              + "\" references a non-existent target ID in the document");
    }
  }

  private static boolean hasAccessibleLabel(ResolvedTag tag) {
    Map<String, String> attrs = tag.attributes();
    if (isNonBlank(attrs.get("aria-label"))
        || isNonBlank(attrs.get("aria-labelledby"))
        || isNonBlank(attrs.get("title"))) {
      return true;
    }
    return containsReadableText(tag.children());
  }

  private static boolean containsReadableText(List<ResolvedNode> children) {
    for (var child : children) {
      boolean found =
          switch (child) {
            case ResolvedText(var text) -> !text.isBlank();
            case ResolvedRaw(var raw) -> !raw.isBlank();
            case ResolvedEmpty _ -> false;
            case ResolvedTag nested ->
                (nested.tag() == HtmlTag.IMG && isNonBlank(nested.attributes().get("alt")))
                    || isNonBlank(nested.attributes().get("aria-label"))
                    || containsReadableText(nested.children());
          };
      if (found) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNonBlank(@Nullable String value) {
    return value != null && !value.isBlank();
  }

  private static @Nullable String paint(
      PageDocument doc,
      List<ResolvedNode> bodyNodes,
      String cssContent,
      OutputMode outputMode,
      Appendable out) {
    try {
      out.append("<!doctype html>\n<html lang=\"");
      out.append(Encode.forHtmlAttribute(doc.lang()));
      out.append("\">\n<head>\n<meta charset=\"utf-8\">\n");
      out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
      out.append("<title>");
      out.append(Encode.forHtmlContent(doc.title()));
      out.append("</title>\n");

      String externalCssPath = null;
      switch (outputMode) {
        case OutputMode.InlineStyle _ -> {
          out.append("<style>\n");
          out.append(cssContent);
          out.append("</style>\n");
        }
        case OutputMode.ExternalAsset(var basePath) -> {
          long cssHash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, cssContent);
          String normalizedBase =
              basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
          externalCssPath = normalizedBase + "/styles-" + Fnv1a64.toBase36Hash(cssHash) + ".css";
          out.append("<link rel=\"stylesheet\" href=\"");
          out.append(Encode.forHtmlAttribute(externalCssPath));
          out.append("\">\n");
        }
      }

      out.append("</head>\n<body>\n");
      for (var node : bodyNodes) {
        paintNode(node, out);
      }
      out.append("\n</body>\n</html>\n");
      return externalCssPath;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void paintNode(ResolvedNode node, Appendable out) throws IOException {
    switch (node) {
      case ResolvedEmpty _ -> {}
      case ResolvedText(var content) -> out.append(Encode.forHtmlContent(content));
      case ResolvedRaw(var trustedHtml) -> out.append(trustedHtml);
      case ResolvedTag tag -> {
        out.append('<').append(tag.tag().tagName());
        for (var attr : tag.attributes().entrySet()) {
          out.append(' ').append(attr.getKey());
          if (!attr.getValue().isEmpty()) {
            out.append("=\"").append(Encode.forHtmlAttribute(attr.getValue())).append('"');
          }
        }
        out.append('>');

        if (!tag.tag().isVoid()) {
          for (var child : tag.children()) {
            paintNode(child, out);
          }
          out.append("</").append(tag.tag().tagName()).append('>');
        }

        var companion = tag.companionSibling();
        if (companion != null) {
          paintNode(companion, out);
        }
      }
    }
  }
}
