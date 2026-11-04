package dev.nthings.tessera.render;

import dev.nthings.tessera.components.Footnotes;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderOnceElement;
import dev.nthings.tessera.core.RenderViewElement;
import dev.nthings.tessera.core.StatefulTagElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssDeclaration;
import dev.nthings.tessera.css.CssProperty;
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
 * Element} and {@link RenderOnce} trees are never mutated and can be reused across multiple render
 * passes.
 */
public final class PageCompiler {

  public record CompiledPage(String html, @Nullable String externalCssPath, String cssContent) {}

  /**
   * Intermediate result after Phase 1 ({@code requestLayout}) and Phase 2 ({@code prepaint}), used
   * by {@code SiteCompiler} to merge stylesheets and verify cross-page links before Phase 3 ({@code
   * paint}).
   */
  public record PreparedPage(
      PageDocument document,
      StyleSheetCollector styles,
      List<ResolvedNode> bodyNodes,
      Set<String> registeredDomIds,
      Set<String> outgoingHrefs,
      Set<TransitionKey> transitionKeys,
      @Nullable String heroImagePreloadHref,
      List<RenderContext.TocEntry> tocEntries) {

    public PreparedPage(
        PageDocument document,
        StyleSheetCollector styles,
        List<ResolvedNode> bodyNodes,
        Set<String> registeredDomIds,
        Set<String> outgoingHrefs) {
      this(document, styles, bodyNodes, registeredDomIds, outgoingHrefs, Set.of(), null, List.of());
    }
  }

  public sealed interface ResolvedNode
      permits ResolvedTag, ResolvedText, ResolvedRaw, ResolvedEmpty {}

  public record ResolvedTag(
      HtmlTag tag,
      Map<String, String> attributes,
      List<ResolvedNode> children,
      @Nullable ResolvedTag companionSibling,
      boolean hasAspectRatio)
      implements ResolvedNode {

    public ResolvedTag(
        HtmlTag tag,
        Map<String, String> attributes,
        List<ResolvedNode> children,
        @Nullable ResolvedTag companionSibling) {
      this(tag, attributes, children, companionSibling, false);
    }
  }

  public record ResolvedText(String content) implements ResolvedNode {}

  public record ResolvedRaw(String trustedHtml) implements ResolvedNode {}

  public enum ResolvedEmpty implements ResolvedNode {
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
    private final Set<String> outgoingHrefs = new LinkedHashSet<>();
    private final Set<TransitionKey> transitionKeys = new LinkedHashSet<>();
    private final Set<String> viewTransitionNames = new LinkedHashSet<>();
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
    PreparedPage prepared = prepare(document);
    String cssContent = prepared.styles().toCssStylesheet(document.tokens());
    return paintPrepared(prepared, cssContent, outputMode);
  }

  public static PreparedPage prepare(PageDocument document) {
    var styles = new StyleSheetCollector();
    var state = new CompilationState();

    record Phase1Result(
        ResolvedNode root,
        @Nullable String heroImageHref,
        List<RenderContext.TocEntry> tocEntries) {}

    // Phase 1: requestLayout
    Phase1Result p1 =
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
              return new Phase1Result(
                  root,
                  RenderContext.current().heroImageHref(),
                  RenderContext.current().tocEntries());
            });

    // Phase 2: prepaint
    List<ResolvedNode> bodyNodes = prepaint(p1.root(), state);

    return new PreparedPage(
        document,
        styles,
        bodyNodes,
        Collections.unmodifiableSet(state.registeredDomIds),
        Collections.unmodifiableSet(state.outgoingHrefs),
        Collections.unmodifiableSet(state.transitionKeys),
        p1.heroImageHref(),
        p1.tocEntries());
  }

  public static CompiledPage paintPrepared(
      PreparedPage prepared, String cssContent, OutputMode outputMode) {
    var htmlBuffer = new StringBuilder(4096);
    String externalCssPath =
        paint(
            prepared.document(),
            prepared.bodyNodes(),
            cssContent,
            outputMode,
            prepared.heroImagePreloadHref(),
            htmlBuffer);
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
                String instanceScopeId = RenderContext.current().viewScope().domId();
                String resolvedViewType =
                    RenderContext.current()
                        .styles()
                        .registerViewScope(view.viewType(), view.viewScopeRule());
                ResolvedNode inner = requestLayout(view.render(), state);
                return attachViewScopeAttributes(inner, resolvedViewType, instanceScopeId);
              });
      case TagElement tag -> resolveTag(tag, null, state);
      case StatefulTagElement stateful -> {
        String domId = ctx.viewScope().extend(stateful.elementId()).domId();
        state.registeredDomIds.add(domId);
        yield resolveTag(stateful.delegate(), domId, state);
      }
    };
  }

  private static ResolvedNode attachViewScopeAttributes(
      ResolvedNode node, String viewType, String instanceScopeId) {
    if (node instanceof ResolvedTag tag) {
      var updatedAttrs = new LinkedHashMap<>(tag.attributes());
      updatedAttrs.put("data-view", viewType);
      updatedAttrs.put("data-view-id", instanceScopeId);
      return new ResolvedTag(
          tag.tag(),
          Collections.unmodifiableMap(updatedAttrs),
          tag.children(),
          tag.companionSibling(),
          tag.hasAspectRatio());
    }
    var wrapperAttrs = new LinkedHashMap<String, String>(2);
    wrapperAttrs.put("data-view", viewType);
    wrapperAttrs.put("data-view-id", instanceScopeId);
    return new ResolvedTag(
        HtmlTag.DIV, Collections.unmodifiableMap(wrapperAttrs), List.of(node), null, false);
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

    boolean hasAspectRatio = tag.style().get(CssProperty.ASPECT_RATIO) != null;

    if (tag.transitionKey() != null) {
      TransitionKey tk = tag.transitionKey();
      if (!state.viewTransitionNames.add(tk.name())) {
        throw new AccessibilityViolationException(
            "Duplicate TransitionKey name '"
                + tk.name()
                + "' found on the same page. TransitionKey names must be unique per document.");
      }
      state.transitionKeys.add(tk);
      if (tk.transitionClass() != null) {
        ctx.styles().registerTransitionClass(tk.transitionClass());
      }
    } else {
      var vtnDecl = tag.style().get(CssProperty.VIEW_TRANSITION_NAME);
      if (vtnDecl instanceof CssDeclaration.ViewTransitionName(var ident)) {
        if (!state.viewTransitionNames.add(ident)) {
          throw new AccessibilityViolationException(
              "Duplicate view-transition-name '"
                  + ident
                  + "' found on the same page. TransitionKey names must be unique per document.");
        }
        var vtcDecl = tag.style().get(CssProperty.VIEW_TRANSITION_CLASS);
        String vtc = (vtcDecl instanceof CssDeclaration.ViewTransitionClass(var cls)) ? cls : null;
        state.transitionKeys.add(new TransitionKey(ident, vtc));
        if (vtc != null) {
          ctx.styles().registerTransitionClass(vtc);
        }
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
        companionTooltip,
        hasAspectRatio);
  }

  private static List<ResolvedNode> prepaint(ResolvedNode root, CompilationState state) {
    validateNodeTree(root, state);

    var bodyNodes = new ArrayList<ResolvedNode>();
    if (!(root instanceof ResolvedEmpty)) {
      bodyNodes.add(root);
    }

    while (!state.deferredQueue.isEmpty()) {
      ResolvedDeferred deferred = state.deferredQueue.poll();
      if (deferred != null && !(deferred.node() instanceof ResolvedEmpty)) {
        validateNodeTree(deferred.node(), state);
        bodyNodes.add(deferred.node());
      }
    }

    return Collections.unmodifiableList(bodyNodes);
  }

  private static void validateNodeTree(ResolvedNode node, CompilationState state) {
    switch (node) {
      case ResolvedEmpty _, ResolvedText _, ResolvedRaw _ -> {}
      case ResolvedTag tag -> {
        validateTagAccessibility(tag, state);
        for (var child : tag.children()) {
          validateNodeTree(child, state);
        }
        var companion = tag.companionSibling();
        if (companion != null) {
          validateNodeTree(companion, state);
        }
      }
    }
  }

  private static void validateTagAccessibility(ResolvedTag tag, CompilationState state) {
    Map<String, String> attrs = tag.attributes();
    Set<String> registeredDomIds = state.registeredDomIds;

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
      boolean hasWidthAndHeight = attrs.containsKey("width") && attrs.containsKey("height");
      boolean hasAspectRatio =
          tag.hasAspectRatio()
              || (attrs.containsKey("style") && attrs.get("style").contains("aspect-ratio"));
      if (!hasWidthAndHeight && !hasAspectRatio) {
        throw new AccessibilityViolationException(
            "<img> element with src=\""
                + attrs.getOrDefault("src", "")
                + "\" must declare both 'width' and 'height' attributes or a CSS 'aspect-ratio' to prevent Cumulative Layout Shift (CLS)");
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

    if (tag.tag() == HtmlTag.A) {
      String href = attrs.get("href");
      if (href != null && !href.isBlank()) {
        state.outgoingHrefs.add(href);
        if (href.startsWith("#") && href.length() > 1) {
          String fragment = href.substring(1);
          if (!registeredDomIds.contains(fragment)) {
            throw new AccessibilityViolationException(
                "Anchor link href=\""
                    + href
                    + "\" references a non-existent fragment ID in the document");
          }
        }
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
      @Nullable String heroImagePreloadHref,
      Appendable out) {
    try {
      out.append("<!doctype html>\n<html lang=\"");
      out.append(Encode.forHtmlAttribute(doc.lang()));
      out.append("\">\n<head>\n<meta charset=\"utf-8\">\n");
      out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
      out.append("<title>");
      out.append(Encode.forHtmlContent(doc.title()));
      out.append("</title>\n");
      if (heroImagePreloadHref != null && !heroImagePreloadHref.isBlank()) {
        out.append("<link rel=\"preload\" as=\"image\" href=\"");
        out.append(Encode.forHtmlAttribute(heroImagePreloadHref));
        out.append("\" fetchpriority=\"high\">\n");
      }

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
