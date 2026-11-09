package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.LateElement;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnceElement;
import dev.nthings.tessera.core.RenderViewElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Expands authoring elements into [ViewNode]s for one page.
///
/// Collects styles and delegates placement. Identities, transitions and diagnostics go to the
/// [CompilationState]; [LayoutPass] orders the document and [ReferenceLinker] links it.
final class ViewExpansion {
  private final CompilationState state;
  private final Placement placement;

  interface Placement {
    ViewNode defer(Supplier<ViewNode> content, int priority);

    ViewNode late(Supplier<ViewNode> content);
  }

  ViewExpansion(CompilationState state, Placement placement) {
    this.state = state;
    this.placement = placement;
  }

  ViewNode expand(Element element, RenderContext context) {
    return switch (element) {
      case EmptyElement _ -> ViewNode.Empty.INSTANCE;
      case TextElement(var content) -> new ResolvedText(content);
      case RawHtmlElement(var html) -> new ResolvedRaw(html);
      case DeferredElement child ->
          placement.defer(() -> expand(child.child(), context), child.priority());
      case LateElement(var content) -> placement.late(() -> expand(context, "Late", content));
      case FragmentElement fragment ->
          new ViewNode.Fragment(children(fragment.childrenList(), context));
      case RenderOnceElement(var component, var source) -> {
        var child =
            context.withComponent(
                component.getClass().isSynthetic()
                    ? "RenderOnce"
                    : component.getClass().getSimpleName(),
                source);
        yield expand(child, "Render", component::render);
      }
      case RenderViewElement(var view, var source) -> {
        var id = view.entityId();
        var scoped =
            context.withViewScope(
                id != null ? id : state.nextId(view.viewType(), context.viewScope()));
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
        yield scoped(expand(child, "Render", view::render), type, scopeId, child);
      }
      case TagElement tag -> tag(tag, null, context);
    };
  }

  /// Expands an authoring callback's result; a failure names its component.
  ViewNode expand(
      RenderContext context, String phase, Function<RenderContext, ? extends IntoElement> content) {
    return expand(callback(context, phase, () -> content.apply(context).intoElement()), context);
  }

  /// Adds the view's scope attributes to its root element, or wraps a root that is not one.
  private ViewNode scoped(ViewNode node, String type, String scopeId, RenderContext context) {
    var scope = new LinkedHashMap<String, String>();
    if (node instanceof ViewNode.Tag tag) {
      scope.putAll(tag.attributes());
    }
    scope.put("data-view", type);
    scope.put("data-view-id", scopeId);
    if (node instanceof ViewNode.Tag tag) {
      return tag.withAttributes(scope);
    }
    return new ViewNode.Tag(
        HtmlTag.DIV,
        new ViewLocation(context.componentPath(), HtmlTag.DIV.tagName(), null, context.origin()),
        scope,
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
                  new Diagnostic(
                      "callback-failure",
                      phase + " callback failed: " + failure.getMessage(),
                      "Correct the callback before compiling again.",
                      state.location(
                          new ViewLocation(
                              context.componentPath(), "", null, context.origin()))))));
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

  /// Expands `element`. `forcedId` (already registered) is used for companions, not bound.
  private ViewNode.Tag tag(TagElement element, @Nullable String forcedId, RenderContext context) {
    var attrs = new LinkedHashMap<>(element.attributes());
    var source = element.source() != null ? element.source() : context.origin();
    var location =
        new ViewLocation(context.componentPath(), element.tag().tagName(), attrs.get("id"), source);
    var domId = forcedId;
    var ref = element.boundRef();
    if (domId == null && ref != null) {
      domId = state.bind(ref, context.viewScope(), location);
    }
    if (domId != null) {
      attrs.put("id", domId);
      location = location.withDomId(domId);
    } else {
      var explicit = location.domId();
      if (explicit != null && !explicit.isBlank()) {
        state.registerDomId(explicit, location);
      }
    }

    var style = identityStyles(element, domId, location);
    if (element.tag() == HtmlTag.IMG) {
      PageValidator.validateImage(attrs, style, state, location);
    }

    var transition = element.transitionKey();
    if (transition != null) {
      state.registerTransition(transition, location);
      var transitionClass = transition.transitionClass();
      if (transitionClass != null) {
        context.styles().registerTransitionClass(transitionClass);
      }
    }

    var classes = context.styles().registerElementStyles(style, element.interactivity());
    if (!classes.isEmpty()) {
      var existing = attrs.get("class");
      var joined = String.join(" ", classes);
      attrs.put("class", PageValidator.isNonBlank(existing) ? existing + " " + joined : joined);
    }

    ViewNode.Tag companion = null;
    var tooltip = element.interestTooltip();
    if (tooltip != null) {
      var tooltipId = domId + "--tooltip";
      attrs.put("interestfor", tooltipId);
      state.registerDomId(tooltipId, location);
      context.contributeBehavior(TooltipBehavior.INSTANCE);
      companion = tag(tooltip, tooltipId, context);
    }

    var references = element.idReferences();
    return new ViewNode.Tag(
        element.tag(),
        location,
        attrs,
        references.isEmpty() ? Map.of() : new LinkedHashMap<>(references),
        children(element.childrenList(), context),
        companion);
  }

  /// Adds declarations derived from identity: anchor names and view-transition keys.
  private StyleRefinement identityStyles(
      TagElement element, @Nullable String domId, ViewLocation location) {
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
      var target = state.boundId(anchor, usage, location);
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
}
