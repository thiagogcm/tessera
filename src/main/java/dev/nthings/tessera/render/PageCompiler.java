package dev.nthings.tessera.render;

import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.DocumentState;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.LateElement;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnceElement;
import dev.nthings.tessera.core.RenderViewElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.core.ViewScope;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.owasp.encoder.Encode;

/// Three-phase static HTML5/CSS document compiler (`requestLayout` -> `prepaint` -> `paint`).
///
/// Operates on an immutable intermediate `ResolvedNode` representation so input [Element] and
/// [dev.nthings.tessera.core.RenderOnce] trees are never mutated and can be reused across multiple
/// render passes.
public final class PageCompiler {

  public record CompiledPage(String html, @Nullable String externalCssPath, String cssContent) {}

  /// Intermediate result after Phase 1 (`requestLayout`) and Phase 2 (`prepaint`), used by
  /// `SiteCompiler` to merge stylesheets and verify cross-page links before Phase 3 (`paint`).
  public record PreparedPage(
      PageDocument document,
      StyleSheetCollector styles,
      List<ResolvedNode> bodyNodes,
      Set<String> registeredDomIds,
      Set<String> outgoingHrefs,
      Set<TransitionKey> transitionKeys,
      List<ResolvedNode> headNodes,
      DocumentState documentState) {}

  public sealed interface ResolvedNode
      permits ResolvedTag, ResolvedText, ResolvedRaw, ResolvedEmpty, ResolvedSlot {}

  /// Resolved element; `idReferences` are pending until the prepaint link pass folds them into
  /// `attributes`.
  public record ResolvedTag(
      HtmlTag tag,
      Map<String, String> attributes,
      Map<String, IdReference> idReferences,
      List<ResolvedNode> children,
      @Nullable ResolvedTag companionSibling)
      implements ResolvedNode {}

  public record ResolvedText(String content) implements ResolvedNode {}

  public record ResolvedRaw(String trustedHtml) implements ResolvedNode {}

  public enum ResolvedEmpty implements ResolvedNode {
    INSTANCE
  }

  /// Position of a [LateElement]; replaced by its resolved content in the link pass.
  public record ResolvedSlot(int index) implements ResolvedNode {}

  private record ResolvedDeferred(int priority, long sequence, ResolvedNode node)
      implements Comparable<ResolvedDeferred> {
    @Override
    public int compareTo(ResolvedDeferred other) {
      var cmp = Integer.compare(this.priority, other.priority);
      return cmp != 0 ? cmp : Long.compare(this.sequence, other.sequence);
    }
  }

  private static final class CompilationState {
    private final PriorityQueue<ResolvedDeferred> deferredQueue = new PriorityQueue<>();
    private final Set<String> registeredDomIds = new LinkedHashSet<>();
    private final Set<String> outgoingHrefs = new LinkedHashSet<>();
    private final Set<TransitionKey> transitionKeys = new LinkedHashSet<>();
    private final Set<String> viewTransitionNames = new LinkedHashSet<>();
    private final Map<ElementRef, String> boundDomIds = new IdentityHashMap<>();
    private final Map<String, Integer> autoIdCounters = new HashMap<>();
    private final List<@Nullable ResolvedNode> lateSlots = new ArrayList<>();
    private final ArrayDeque<Runnable> pendingLate = new ArrayDeque<>();
    private long deferredSequence = 0L;

    void enqueueDeferred(int priority, ResolvedNode node) {
      deferredQueue.add(new ResolvedDeferred(priority, deferredSequence++, node));
    }

    /// Deterministic per-page `<prefix>-<n>` local id.
    ElementId nextAutoId(String prefix) {
      return ElementId.of(prefix + "-" + autoIdCounters.merge(prefix, 1, Integer::sum));
    }

    void registerDomId(String domId) {
      if (!registeredDomIds.add(domId)) {
        throw new AccessibilityViolationException(
            "Duplicate DOM id '" + domId + "' on the same page");
      }
    }

    String bind(ElementRef ref, ViewScope scope, HtmlTag tag) {
      var local = ref.localId();
      var domId = scope.extend(local != null ? local : nextAutoId(tag.tagName())).domId();
      if (boundDomIds.putIfAbsent(ref, domId) != null) {
        throw new AccessibilityViolationException(
            ref + " is bound to more than one element on the same page");
      }
      registerDomId(domId);
      return domId;
    }

    String boundDomId(ElementRef ref, String usage) {
      var domId = boundDomIds.get(ref);
      if (domId == null) {
        throw new AccessibilityViolationException(
            usage + " references " + ref + ", which is not bound to any element on this page");
      }
      return domId;
    }
  }

  /// Attributes whose URL values are collected into [PreparedPage#outgoingHrefs] for site-wide
  /// route and asset validation.
  private static final List<String> URL_ATTRIBUTES = List.of("href", "src", "poster");

  private PageCompiler() {}

  /// Compiles `document` with the default [Tokens] and an inline `<style>`.
  public static CompiledPage compile(PageDocument document) {
    return compile(document, new Tokens(), OutputMode.inlineStyle());
  }

  public static CompiledPage compile(PageDocument document, Tokens tokens, OutputMode outputMode) {
    var prepared = prepare(document, tokens, List.of());
    var cssContent = prepared.styles().toCssStylesheet(tokens);
    return paintPrepared(prepared, cssContent, outputMode);
  }

  /// Runs layout and prepaint for `document` under `tokens`, with `siteHead` elements (shared by
  /// every page of a site) placed in `<head>` ahead of anything the page contributes.
  public static PreparedPage prepare(
      PageDocument document, Tokens tokens, List<? extends IntoElement> siteHead) {
    var styles = new StyleSheetCollector();
    var state = new CompilationState();

    record Phase1Result(ResolvedNode root, List<ResolvedNode> headNodes, DocumentState state) {}

    // Phase 1: requestLayout, then late elements, then end-of-document sections
    var p1 =
        RenderContext.runRoot(
            styles,
            tokens,
            () -> {
              siteHead.forEach(RenderContext.current()::contributeHead);
              var root = requestLayout(document.bodyRoot().intoElement(), state);
              resolveLateElements(state);
              var docState = RenderContext.current().documentState();
              var resolvedSections = 0;
              List<Supplier<? extends IntoElement>> sections;
              while ((sections = docState.documentEndSections()).size() > resolvedSections) {
                var section = sections.get(resolvedSections++);
                state.enqueueDeferred(
                    Integer.MAX_VALUE, requestLayout(section.get().intoElement(), state));
                resolveLateElements(state);
              }
              // Indexed: laying out a head element may contribute further head elements.
              var headNodes = new ArrayList<ResolvedNode>();
              for (var i = 0; i < docState.headElements().size(); i++) {
                headNodes.add(requestLayout(docState.headElements().get(i), state));
                resolveLateElements(state);
              }
              return new Phase1Result(root, headNodes, docState);
            });

    // Phase 2: prepaint
    var bodyNodes = prepaint(p1.root(), state);
    var headNodes = new ArrayList<ResolvedNode>(p1.headNodes().size());
    for (var head : p1.headNodes()) {
      var linked = link(head, state);
      validateNodeTree(linked, state);
      headNodes.add(linked);
    }

    return new PreparedPage(
        document,
        styles,
        bodyNodes,
        Collections.unmodifiableSet(state.registeredDomIds),
        Collections.unmodifiableSet(state.outgoingHrefs),
        Collections.unmodifiableSet(state.transitionKeys),
        Collections.unmodifiableList(headNodes),
        p1.state());
  }

  public static CompiledPage paintPrepared(
      PreparedPage prepared, String cssContent, OutputMode outputMode) {
    var htmlBuffer = new StringBuilder(4096);
    var externalCssPath =
        paint(
            prepared.document(),
            prepared.bodyNodes(),
            cssContent,
            outputMode,
            prepared.headNodes(),
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
        var resolvedDeferredChild = requestLayout(child, state);
        state.enqueueDeferred(priority, resolvedDeferredChild);
        yield ResolvedEmpty.INSTANCE;
      }
      case RenderOnceElement(var component) -> requestLayout(component.render(), state);
      case LateElement(var content) -> {
        var slot = state.lateSlots.size();
        state.lateSlots.add(null);
        state.pendingLate.add(
            () ->
                state.lateSlots.set(
                    slot, ctx.reenter(() -> requestLayout(content.get().intoElement(), state))));
        yield new ResolvedSlot(slot);
      }
      case RenderViewElement(var view) -> {
        var entityId = view.entityId();
        yield ctx.withViewScope(
            entityId != null ? entityId : state.nextAutoId(view.viewType()),
            () -> {
              var instanceScopeId = RenderContext.current().viewScope().domId();
              var scope = new ScopeStyle();
              view.configureScope(scope);
              var resolvedViewType =
                  RenderContext.current()
                      .styles()
                      .registerViewScope(view.viewType(), scope.toCssBody());
              var inner = requestLayout(view.render(), state);
              return attachViewScopeAttributes(inner, resolvedViewType, instanceScopeId);
            });
      }
      case TagElement tag -> resolveTag(tag, null, state);
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
          tag.idReferences(),
          tag.children(),
          tag.companionSibling());
    }
    var wrapperAttrs = new LinkedHashMap<String, String>(2);
    wrapperAttrs.put("data-view", viewType);
    wrapperAttrs.put("data-view-id", instanceScopeId);
    return new ResolvedTag(
        HtmlTag.DIV, Collections.unmodifiableMap(wrapperAttrs), Map.of(), List.of(node), null);
  }

  /// Resolves `tag`; `forcedDomId` (already registered) overrides ref binding for companions.
  private static ResolvedTag resolveTag(
      TagElement tag, @Nullable String forcedDomId, CompilationState state) {
    var ctx = RenderContext.current();
    var attrs = new LinkedHashMap<>(tag.attributes());

    var domId = forcedDomId;
    var ref = tag.boundRef();
    if (domId == null && ref != null) {
      domId = state.bind(ref, ctx.viewScope(), tag.tag());
    }
    if (domId != null) {
      attrs.put("id", domId);
    } else {
      var explicitId = attrs.get("id");
      if (explicitId != null && !explicitId.isBlank()) {
        state.registerDomId(explicitId);
      }
    }

    var style = withIdentityStyles(tag, domId, state);
    if (tag.tag() == HtmlTag.IMG) {
      validateImage(attrs, style);
    }

    var transitionKey = tag.transitionKey();
    if (transitionKey != null) {
      if (!state.viewTransitionNames.add(transitionKey.name())) {
        throw new AccessibilityViolationException(
            "Duplicate TransitionKey name '"
                + transitionKey.name()
                + "' found on the same page. TransitionKey names must be unique per document.");
      }
      state.transitionKeys.add(transitionKey);
      var transitionClass = transitionKey.transitionClass();
      if (transitionClass != null) {
        ctx.styles().registerTransitionClass(transitionClass);
      }
    }

    var generatedClasses = ctx.styles().registerElementStyles(style, tag.interactivity());
    if (!generatedClasses.isEmpty()) {
      var existingClass = attrs.get("class");
      var joined = String.join(" ", generatedClasses);
      attrs.put(
          "class",
          existingClass == null || existingClass.isBlank() ? joined : existingClass + " " + joined);
    }

    ResolvedTag companionTooltip = null;
    var tooltipElement = tag.interestTooltip();
    if (tooltipElement != null && domId != null) {
      var tooltipDomId = domId + "--tooltip";
      attrs.put("interestfor", tooltipDomId);
      state.registerDomId(tooltipDomId);
      companionTooltip = resolveTag(tooltipElement, tooltipDomId, state);
    }

    var resolvedChildren = new ArrayList<ResolvedNode>(tag.childrenList().size());
    for (var child : tag.childrenList()) {
      var resolvedChild = requestLayout(child, state);
      if (!(resolvedChild instanceof ResolvedEmpty)) {
        resolvedChildren.add(resolvedChild);
      }
    }

    return new ResolvedTag(
        tag.tag(),
        Collections.unmodifiableMap(attrs),
        Map.copyOf(tag.idReferences()),
        Collections.unmodifiableList(resolvedChildren),
        companionTooltip);
  }

  /// Adds declarations derived from identity: anchor names and view-transition keys.
  private static StyleRefinement withIdentityStyles(
      TagElement tag, @Nullable String domId, CompilationState state) {
    var anchorTarget = tag.anchorTarget();
    var transitionKey = tag.transitionKey();
    if (!tag.isAnchor() && anchorTarget == null && transitionKey == null) {
      return tag.style();
    }
    var style = new StyleRefinement().mergeFrom(tag.style());
    if (domId != null && tag.isAnchor()) {
      style.put(CssProperty.ANCHOR_NAME, anchorIdent(domId));
    }
    if (anchorTarget != null) {
      var usage = "<" + tag.tag().tagName() + "> anchoredTo (anchor must be laid out earlier)";
      style.put(CssProperty.POSITION_ANCHOR, anchorIdent(state.boundDomId(anchorTarget, usage)));
    }
    if (transitionKey != null) {
      style.put(CssProperty.VIEW_TRANSITION_NAME, transitionKey.name());
      var transitionClass = transitionKey.transitionClass();
      if (transitionClass != null) {
        style.put(CssProperty.VIEW_TRANSITION_CLASS, transitionClass);
      }
    }
    return style;
  }

  private static String anchorIdent(String domId) {
    return "--anchor-" + domId;
  }

  /// `<img>` contracts checkable from the element alone: alt text, then CLS-safe dimensions.
  private static void validateImage(Map<String, String> attrs, StyleRefinement style) {
    var alt = attrs.get("alt");
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
    var hasWidthAndHeight = attrs.containsKey("width") && attrs.containsKey("height");
    if (!hasWidthAndHeight && style.get(CssProperty.ASPECT_RATIO) == null) {
      throw new AccessibilityViolationException(
          "<img> element with src=\""
              + attrs.getOrDefault("src", "")
              + "\" must declare both 'width' and 'height' attributes or a CSS 'aspect-ratio' to"
              + " prevent Cumulative Layout Shift (CLS)");
    }
  }

  private static void resolveLateElements(CompilationState state) {
    Runnable next;
    while ((next = state.pendingLate.poll()) != null) {
      next.run();
    }
  }

  /// Substitutes late-element slots and folds pending [IdReference]s into attributes, now that
  /// every ref on the page is bound.
  private static ResolvedNode link(ResolvedNode node, CompilationState state) {
    if (node instanceof ResolvedSlot(int index)) {
      var content = state.lateSlots.get(index);
      if (content == null) {
        throw new IllegalStateException("Late element was linked before it was laid out");
      }
      return link(content, state);
    }
    if (!(node instanceof ResolvedTag tag)) {
      return node;
    }
    var attrs = tag.attributes();
    if (!tag.idReferences().isEmpty()) {
      var linked = new LinkedHashMap<>(attrs);
      tag.idReferences()
          .forEach(
              (name, reference) -> {
                var ids = new ArrayList<String>(reference.targets().size());
                for (var target : reference.targets()) {
                  ids.add(state.boundDomId(target, "<" + tag.tag().tagName() + " " + name + ">"));
                }
                linked.put(name, reference.prefix() + String.join(" ", ids));
              });
      attrs = Collections.unmodifiableMap(linked);
    }
    var children = new ArrayList<ResolvedNode>(tag.children().size());
    for (var child : tag.children()) {
      children.add(link(child, state));
    }
    var companion = tag.companionSibling();
    return new ResolvedTag(
        tag.tag(),
        attrs,
        Map.of(),
        Collections.unmodifiableList(children),
        companion != null ? (ResolvedTag) link(companion, state) : null);
  }

  private static List<ResolvedNode> prepaint(ResolvedNode root, CompilationState state) {
    var bodyNodes = new ArrayList<ResolvedNode>();
    bodyNodes.add(link(root, state));
    for (ResolvedDeferred deferred; (deferred = state.deferredQueue.poll()) != null; ) {
      bodyNodes.add(link(deferred.node(), state));
    }
    for (var node : bodyNodes) {
      validateNodeTree(node, state);
    }
    return Collections.unmodifiableList(bodyNodes);
  }

  private static void validateNodeTree(ResolvedNode node, CompilationState state) {
    switch (node) {
      case ResolvedEmpty _, ResolvedText _, ResolvedRaw _ -> {}
      case ResolvedSlot _ -> throw unlinked();
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
    var attrs = tag.attributes();
    var registeredDomIds = state.registeredDomIds;

    if (tag.tag() == HtmlTag.BUTTON || tag.tag() == HtmlTag.A) {
      if (!hasAccessibleLabel(tag)) {
        throw new AccessibilityViolationException(
            "Interactive <"
                + tag.tag().tagName()
                + "> element must have text content or an aria-label / aria-labelledby / title attribute");
      }
    }

    for (var urlAttribute : URL_ATTRIBUTES) {
      var url = attrs.get(urlAttribute);
      if (url != null && !url.isBlank()) {
        state.outgoingHrefs.add(url);
      }
    }

    if (tag.tag() == HtmlTag.A) {
      var href = attrs.get("href");
      if (href != null) {
        if (href.startsWith("#") && href.length() > 1) {
          var fragment = href.substring(1);
          if (!registeredDomIds.contains(fragment)) {
            throw new AccessibilityViolationException(
                "Anchor link href=\""
                    + href
                    + "\" references a non-existent fragment ID in the document");
          }
        }
      }
    }

    var commandFor = attrs.get("commandfor");
    if (commandFor != null && !registeredDomIds.contains(commandFor)) {
      throw new AccessibilityViolationException(
          "Invoker attribute commandfor=\""
              + commandFor
              + "\" references a non-existent target ID in the document");
    }

    var interestFor = attrs.get("interestfor");
    if (interestFor != null && !registeredDomIds.contains(interestFor)) {
      throw new AccessibilityViolationException(
          "Interest invoker attribute interestfor=\""
              + interestFor
              + "\" references a non-existent target ID in the document");
    }
  }

  private static boolean hasAccessibleLabel(ResolvedTag tag) {
    var attrs = tag.attributes();
    if (isNonBlank(attrs.get("aria-label"))
        || isNonBlank(attrs.get("aria-labelledby"))
        || isNonBlank(attrs.get("title"))) {
      return true;
    }
    return containsReadableText(tag.children());
  }

  private static boolean containsReadableText(List<ResolvedNode> children) {
    for (var child : children) {
      var found =
          switch (child) {
            case ResolvedText(var text) -> !text.isBlank();
            case ResolvedRaw(var raw) -> !raw.isBlank();
            case ResolvedEmpty _ -> false;
            case ResolvedSlot _ -> throw unlinked();
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

  private static IllegalStateException unlinked() {
    return new IllegalStateException("Late-element slot reached validation/paint before linking");
  }

  private static boolean isNonBlank(@Nullable String value) {
    return value != null && !value.isBlank();
  }

  private static @Nullable String paint(
      PageDocument doc,
      List<ResolvedNode> bodyNodes,
      String cssContent,
      OutputMode outputMode,
      List<ResolvedNode> headNodes,
      StringBuilder out) {
    out.append("<!doctype html>\n<html lang=\"");
    out.append(Encode.forHtmlAttribute(doc.lang()));
    out.append("\">\n<head>\n<meta charset=\"utf-8\">\n");
    out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
    // rem-based layouts follow the OS text size setting, not only the browser's.
    out.append("<meta name=\"text-scale\" content=\"scale\">\n");
    out.append("<title>");
    out.append(Encode.forHtmlContent(doc.title()));
    out.append("</title>\n");
    for (var head : headNodes) {
      paintNode(head, out);
      out.append('\n');
    }

    String externalCssPath = null;
    switch (outputMode) {
      case OutputMode.InlineStyle _ -> {
        out.append("<style>\n");
        out.append(cssContent);
        out.append("</style>\n");
      }
      case OutputMode.ExternalAsset(var basePath) -> {
        var cssHash = Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, cssContent);
        var normalizedBase =
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
  }

  private static void paintNode(ResolvedNode node, StringBuilder out) {
    switch (node) {
      case ResolvedEmpty _ -> {}
      case ResolvedSlot _ -> throw unlinked();
      case ResolvedText(var content) -> out.append(Encode.forHtmlContent(content));
      case ResolvedRaw(var trustedHtml) -> out.append(trustedHtml);
      case ResolvedTag tag -> {
        out.append('<').append(tag.tag().tagName());
        for (var attr : tag.attributes().entrySet()) {
          var value = attr.getValue();
          out.append(' ').append(attr.getKey());
          if (!value.isEmpty()) {
            out.append("=\"").append(Encode.forHtmlAttribute(value)).append('"');
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
