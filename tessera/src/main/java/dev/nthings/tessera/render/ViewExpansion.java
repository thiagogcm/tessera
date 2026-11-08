package dev.nthings.tessera.render;

import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.LateElement;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnceElement;
import dev.nthings.tessera.core.RenderViewElement;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.core.ViewScope;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Expands authoring elements into [ViewNode]s for one page.
///
/// Binds element references to DOM ids, numbers generated ids per identity scope, collects styles
/// and schedules deferred and late content. Page-level ids, transitions and diagnostics go to the
/// page's [CompilationState]; [LayoutPass] orders the document and [ReferenceLinker] links the
/// result.
final class ViewExpansion {
  private final CompilationState state;
  private final Map<ElementRef, String> references = new IdentityHashMap<>();
  private final Map<ViewScope, Map<String, Integer>> counters = new HashMap<>();
  private final Set<String> transitionNames = new HashSet<>();
  private final PriorityQueue<Deferred> deferred = new PriorityQueue<>();
  private final List<@Nullable ViewNode> slots = new ArrayList<>();
  private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
  private long sequence;

  private record Deferred(int priority, long sequence, ViewNode node)
      implements Comparable<Deferred> {
    @Override
    public int compareTo(Deferred other) {
      var order = Integer.compare(priority, other.priority);
      return order != 0 ? order : Long.compare(sequence, other.sequence);
    }
  }

  ViewExpansion(CompilationState state) {
    this.state = state;
  }

  ViewNode expand(Element element, RenderContext context) {
    return switch (element) {
      case EmptyElement _ -> ViewNode.Empty.INSTANCE;
      case TextElement(var content) -> new ResolvedText(content);
      case RawHtmlElement(var html) -> new ResolvedRaw(html);
      case DeferredElement child -> {
        defer(child.priority(), expand(child.child(), context));
        yield ViewNode.Empty.INSTANCE;
      }
      case LateElement(var content) -> {
        var index = slots.size();
        slots.add(null);
        pending.add(() -> slots.set(index, expand(content.apply(context).intoElement(), context)));
        yield new ViewNode.Slot(index);
      }
      case FragmentElement fragment ->
          new ViewNode.Fragment(children(fragment.childrenList(), context));
      case RenderOnceElement(var component, var source) -> {
        var child =
            context.withComponent(
                component.getClass().isSynthetic()
                    ? "RenderOnce"
                    : component.getClass().getSimpleName(),
                source);
        yield expand(callback(child, "Render", () -> component.render(child)), child);
      }
      case RenderViewElement(var view, var source) -> {
        var id = view.entityId();
        var scoped =
            context.withViewScope(id != null ? id : nextId(view.viewType(), context.viewScope()));
        var scopeId = scoped.viewScope().domId();
        var child = scoped.withComponent(view.viewType() + "[" + scopeId + "]", source);
        var style = new ScopeStyle();
        callback(
            child,
            "Style",
            () -> {
              view.configureScope(style);
              return style;
            });
        var type = child.styles().registerViewScope(view.viewType(), style.toCssBody());
        var inner = expand(callback(child, "Render", () -> view.render(child)), child);
        yield scoped(inner, type, scopeId, child);
      }
      case TagElement tag -> tag(tag, null, context);
    };
  }

  /// Places `node` after the main flow, ordered by `priority` and then by placement order.
  void defer(int priority, ViewNode node) {
    deferred.add(new Deferred(priority, sequence++, node));
  }

  /// The next deferred node, or `null` once every one was taken.
  @Nullable ViewNode nextDeferred() {
    var next = deferred.poll();
    return next == null ? null : next.node();
  }

  /// Expands the late elements placed so far, including those their content places.
  void resolveLate() {
    for (Runnable next; (next = pending.poll()) != null; ) {
      next.run();
    }
  }

  ViewNode resolveSlot(int index) {
    var node = slots.get(index);
    if (node == null) {
      throw new IllegalStateException("Late element was linked before it was laid out");
    }
    return node;
  }

  /// The DOM id bound to `ref`, or `null` when it is ambiguous or unbound; reports an unbound
  /// reference.
  @Nullable String boundId(ElementRef ref, String usage, CompilationLocation location) {
    var id = references.get(ref);
    if (state.ambiguousDomIds.contains(id)) {
      return null;
    }
    if (id == null) {
      state.report(
          location,
          "unbound-reference",
          usage + " references " + ref + ", which is not bound to any element on this page",
          "Bind this reference with .id(ref) on the intended target.");
    }
    return id;
  }

  /// Adds the view's scope attributes to its root element, or wraps a root that is not one.
  private ViewNode scoped(ViewNode node, String type, String scopeId, RenderContext context) {
    if (node instanceof ViewNode.Tag tag) {
      var attrs = new LinkedHashMap<>(tag.attributes());
      attrs.put("data-view", type);
      attrs.put("data-view-id", scopeId);
      return tag.withAttributes(attrs);
    }
    var attrs = new LinkedHashMap<String, String>(2);
    attrs.put("data-view", type);
    attrs.put("data-view-id", scopeId);
    return new ViewNode.Tag(
        HtmlTag.DIV,
        location(context, HtmlTag.DIV, attrs, context.origin()),
        attrs,
        Map.of(),
        List.of(node),
        null);
  }

  /// Runs an authoring callback; a failure keeps its type and gains the component path as a
  /// suppressed diagnostic.
  private <T> T callback(RenderContext context, String phase, Supplier<T> work) {
    try {
      return work.get();
    } catch (RuntimeException failure) {
      failure.addSuppressed(
          new CompilationException(
              List.of(
                  new CompilationDiagnostic(
                      "callback-failure",
                      state.location(
                          new ViewLocation(context.componentPath(), "", null, context.origin())),
                      phase + " callback failed: " + failure.getMessage(),
                      "Correct the callback before compiling again."))));
      throw failure;
    }
  }

  /// Lays out `children` in order, dropping the ones that emit nothing.
  private List<ViewNode> children(List<Element> children, RenderContext context) {
    var result = new ArrayList<ViewNode>(children.size());
    for (var child : children) {
      var node = expand(child, context);
      if (node != ViewNode.Empty.INSTANCE) {
        result.add(node);
      }
    }
    return result;
  }

  /// Expands `element`; `forcedId` (already registered) overrides reference binding for
  /// companions.
  private ViewNode.Tag tag(TagElement element, @Nullable String forcedId, RenderContext context) {
    var attrs = new LinkedHashMap<>(element.attributes());
    var source = element.source() != null ? element.source() : context.origin();
    var domId = forcedId;
    var ref = element.boundRef();
    if (domId == null && ref != null) {
      domId =
          bind(
              ref,
              context.viewScope(),
              element.tag(),
              location(context, element.tag(), attrs, source));
    }
    if (domId != null) {
      attrs.put("id", domId);
    } else {
      var explicit = attrs.get("id");
      if (explicit != null && !explicit.isBlank()) {
        state.registerDomId(explicit, location(context, element.tag(), attrs, source));
      }
    }

    var location = location(context, element.tag(), attrs, source);
    var style = identityStyles(element, domId, location);
    if (element.tag() == HtmlTag.IMG) {
      PageValidator.validateImage(attrs, style, state, location);
    }

    var transition = element.transitionKey();
    if (transition != null) {
      if (!transitionNames.add(transition.name())) {
        state.report(
            location,
            "duplicate-transition-key",
            "Duplicate TransitionKey name '"
                + transition.name()
                + "' found on the same page. TransitionKey names must be unique per document.",
            "Give each transitioning element a distinct TransitionKey name.");
      }
      state.transitionLocations.putIfAbsent(transition, location);
      var transitionClass = transition.transitionClass();
      if (transitionClass != null) {
        context.styles().registerTransitionClass(transitionClass);
      }
    }

    var classes = context.styles().registerElementStyles(style, element.interactivity());
    if (!classes.isEmpty()) {
      var existing = attrs.get("class");
      var joined = String.join(" ", classes);
      attrs.put("class", existing == null || existing.isBlank() ? joined : existing + " " + joined);
    }

    ViewNode.Tag companion = null;
    var tooltip = element.interestTooltip();
    if (tooltip != null && domId != null) {
      var tooltipId = domId + "--tooltip";
      attrs.put("interestfor", tooltipId);
      state.registerDomId(tooltipId, location);
      context.contributeBehavior(TooltipBehavior.INSTANCE);
      companion = tag(tooltip, tooltipId, context);
    }

    return new ViewNode.Tag(
        element.tag(),
        location,
        attrs,
        element.idReferences(),
        children(element.childrenList(), context),
        companion);
  }

  /// Adds declarations derived from identity: anchor names and view-transition keys.
  private StyleRefinement identityStyles(
      TagElement element, @Nullable String domId, CompilationLocation location) {
    var anchor = element.anchorTarget();
    var transition = element.transitionKey();
    if (!element.isAnchor() && anchor == null && transition == null) {
      return element.style();
    }
    var style = new StyleRefinement().mergeFrom(element.style());
    if (domId != null && element.isAnchor()) {
      style.anchorName(anchorIdent(domId));
    }
    if (anchor != null) {
      var usage = "<" + element.tag().tagName() + "> anchoredTo (anchor must be laid out earlier)";
      var target = boundId(anchor, usage, location);
      if (target != null) {
        style.positionAnchor(anchorIdent(target));
      }
    }
    if (transition != null) {
      style.viewTransitionName(CssIdent.of(transition.name()));
      var transitionClass = transition.transitionClass();
      if (transitionClass != null) {
        style.viewTransitionClass(CssIdent.of(transitionClass));
      }
    }
    return style;
  }

  private static CssIdent anchorIdent(String domId) {
    return CssIdent.custom("anchor-" + domId);
  }

  private String bind(ElementRef ref, ViewScope scope, TagName tag, CompilationLocation location) {
    var existing = references.get(ref);
    if (existing != null) {
      state.ambiguousDomIds.add(existing);
      state.report(
          location,
          "duplicate-reference-binding",
          ref + " is bound to more than one element on the same page",
          "Bind this ElementRef once; use a separate reference for the other element.");
      return existing;
    }
    var local = ref.localId();
    var domId = scope.extend(local != null ? local : nextId(tag.tagName(), scope)).domId();
    references.put(ref, domId);
    var view = location.view();
    state.registerDomId(
        domId,
        state.location(
            new ViewLocation(view.componentPath(), view.tagName(), domId, view.source())));
    return domId;
  }

  /// `<prefix>-<n>`, numbered within `scope` so ids elsewhere on the page never shift it.
  private ElementId nextId(String prefix, ViewScope scope) {
    var scoped = counters.computeIfAbsent(scope, _ -> new HashMap<>());
    return ElementId.of(prefix + "-" + scoped.merge(prefix, 1, Integer::sum));
  }

  private CompilationLocation location(
      RenderContext context,
      TagName tag,
      Map<String, String> attrs,
      @Nullable SourceLocation source) {
    return state.location(
        new ViewLocation(context.componentPath(), tag.tagName(), attrs.get("id"), source));
  }
}
