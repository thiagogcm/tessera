package dev.nthings.tessera.render;

import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.DocumentState;
import dev.nthings.tessera.core.DocumentState.Contribution;
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
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.ResolvedNode.ResolvedEmpty;
import dev.nthings.tessera.render.ResolvedNode.ResolvedFragment;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedSlot;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Expands authoring nodes and collects page-local identities and styles.
final class LayoutPass {
  private LayoutPass() {}

  record Result(ResolvedNode root, List<ResolvedNode> headNodes, DocumentState state) {}

  static Result layout(
      PageDocument document,
      Tokens tokens,
      List<? extends IntoElement> siteHead,
      StyleSheetCollector styles,
      CompilationState state) {
    // Phase 1: requestLayout, then late elements, then end-of-document sections
    var docState = new DocumentState();
    var root = RenderContext.forDocument(styles, tokens, docState);
    siteHead.forEach(root::contributeHead);
    var body = requestLayout(document.bodyRoot().intoElement(), root, state);
    resolveLateElements(state);
    var resolvedSections = 0;
    List<Contribution<Function<RenderContext, ? extends IntoElement>>> sections;
    while ((sections = docState.documentEndSections()).size() > resolvedSections) {
      var section = sections.get(resolvedSections++);
      var context = root.withComponentPath(section.componentPath(), section.origin());
      state.enqueueDeferred(
          Integer.MAX_VALUE,
          requestLayout(section.content().apply(context).intoElement(), context, state));
      resolveLateElements(state);
    }
    // Indexed: laying out a head element may contribute further head elements.
    var headNodes = new ArrayList<ResolvedNode>();
    for (var i = 0; i < docState.headElements().size(); i++) {
      var head = docState.headElements().get(i);
      var context = root.withComponentPath(head.componentPath(), head.origin());
      headNodes.add(requestLayout(head.content(), context, state));
      resolveLateElements(state);
    }
    return new Result(body, headNodes, docState);
  }

  private static ResolvedNode requestLayout(
      Element element, RenderContext ctx, CompilationState state) {
    return switch (element) {
      case EmptyElement _ -> ResolvedEmpty.INSTANCE;
      case TextElement(var content) -> new ResolvedText(content);
      case RawHtmlElement(var html) -> new ResolvedRaw(html);
      case DeferredElement deferred -> {
        var resolvedDeferredChild = requestLayout(deferred.child(), ctx, state);
        state.enqueueDeferred(deferred.priority(), resolvedDeferredChild);
        yield ResolvedEmpty.INSTANCE;
      }
      case RenderOnceElement(var component, var source) -> {
        var child =
            ctx.withComponent(
                component.getClass().isSynthetic()
                    ? "RenderOnce"
                    : component.getClass().getSimpleName(),
                source);
        yield requestLayout(component.render(child), child, state);
      }
      case LateElement(var content) -> {
        var slot = state.lateSlots.size();
        state.lateSlots.add(null);
        state.pendingLate.add(
            () ->
                state.lateSlots.set(
                    slot, requestLayout(content.apply(ctx).intoElement(), ctx, state)));
        yield new ResolvedSlot(slot);
      }
      case FragmentElement fragment ->
          new ResolvedFragment(resolveChildren(fragment.childrenList(), ctx, state));
      case RenderViewElement(var view, var source) -> {
        var entityId = view.entityId();
        var scoped =
            ctx.withViewScope(entityId != null ? entityId : state.nextAutoId(view.viewType()));
        var instanceScopeId = scoped.viewScope().domId();
        var child = scoped.withComponent(view.viewType() + "[" + instanceScopeId + "]", source);
        var scope = new ScopeStyle();
        view.configureScope(scope);
        var resolvedViewType = child.styles().registerViewScope(view.viewType(), scope.toCssBody());
        var inner = requestLayout(view.render(child), child, state);
        yield attachViewScopeAttributes(
            inner,
            resolvedViewType,
            instanceScopeId,
            state.location(child.componentPath(), HtmlTag.DIV, Map.of(), child.origin()));
      }
      case TagElement tag -> resolveTag(tag, null, ctx, state);
    };
  }

  private static ResolvedNode attachViewScopeAttributes(
      ResolvedNode node, String viewType, String instanceScopeId, CompilationLocation location) {
    if (node instanceof ResolvedTag tag) {
      var updatedAttrs = new LinkedHashMap<>(tag.attributes());
      updatedAttrs.put("data-view", viewType);
      updatedAttrs.put("data-view-id", instanceScopeId);
      return new ResolvedTag(
          tag.tag(),
          tag.location(),
          Collections.unmodifiableMap(updatedAttrs),
          tag.idReferences(),
          tag.children(),
          tag.companionSibling());
    }
    var wrapperAttrs = new LinkedHashMap<String, String>(2);
    wrapperAttrs.put("data-view", viewType);
    wrapperAttrs.put("data-view-id", instanceScopeId);
    return new ResolvedTag(
        HtmlTag.DIV,
        location,
        Collections.unmodifiableMap(wrapperAttrs),
        Map.of(),
        List.of(node),
        null);
  }

  /// Lays out `children` in order, dropping the ones that emit nothing.
  private static List<ResolvedNode> resolveChildren(
      List<Element> children, RenderContext ctx, CompilationState state) {
    var resolved = new ArrayList<ResolvedNode>(children.size());
    for (var child : children) {
      var node = requestLayout(child, ctx, state);
      if (!(node instanceof ResolvedEmpty)) {
        resolved.add(node);
      }
    }
    return resolved;
  }

  /// Resolves `tag`; `forcedDomId` (already registered) overrides ref binding for companions.
  private static ResolvedTag resolveTag(
      TagElement tag, @Nullable String forcedDomId, RenderContext ctx, CompilationState state) {
    var attrs = new LinkedHashMap<>(tag.attributes());
    var components = ctx.componentPath();
    var source = tag.source() != null ? tag.source() : ctx.origin();

    var domId = forcedDomId;
    var ref = tag.boundRef();
    if (domId == null && ref != null) {
      domId =
          state.bind(
              ref,
              ctx.viewScope(),
              tag.tag(),
              state.location(components, tag.tag(), attrs, source));
    }
    if (domId != null) {
      attrs.put("id", domId);
    } else {
      var explicitId = attrs.get("id");
      if (explicitId != null && !explicitId.isBlank()) {
        state.registerDomId(explicitId, state.location(components, tag.tag(), attrs, source));
      }
    }

    var location = state.location(components, tag.tag(), attrs, source);
    var style = withIdentityStyles(tag, domId, state, location);
    if (tag.tag() == HtmlTag.IMG) {
      PageValidator.validateImage(attrs, style, state, location);
    }

    var transitionKey = tag.transitionKey();
    if (transitionKey != null) {
      if (!state.viewTransitionNames.add(transitionKey.name())) {
        state.report(
            location,
            "duplicate-transition-key",
            "Duplicate TransitionKey name '"
                + transitionKey.name()
                + "' found on the same page. TransitionKey names must be unique per document.",
            "Give each transitioning element a distinct TransitionKey name.");
      }
      state.transitionLocations.putIfAbsent(transitionKey, location);
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
      state.registerDomId(tooltipDomId, location);
      ctx.contributeBehavior(TooltipBehavior.INSTANCE);
      companionTooltip = resolveTag(tooltipElement, tooltipDomId, ctx, state);
    }

    var resolvedChildren = resolveChildren(tag.childrenList(), ctx, state);

    return new ResolvedTag(
        tag.tag(),
        location,
        Collections.unmodifiableMap(attrs),
        Collections.unmodifiableMap(new LinkedHashMap<>(tag.idReferences())),
        Collections.unmodifiableList(resolvedChildren),
        companionTooltip);
  }

  /// Adds declarations derived from identity: anchor names and view-transition keys.
  private static StyleRefinement withIdentityStyles(
      TagElement tag,
      @Nullable String domId,
      CompilationState state,
      CompilationLocation location) {
    var anchorTarget = tag.anchorTarget();
    var transitionKey = tag.transitionKey();
    if (!tag.isAnchor() && anchorTarget == null && transitionKey == null) {
      return tag.style();
    }
    var style = new StyleRefinement().mergeFrom(tag.style());
    if (domId != null && tag.isAnchor()) {
      style.anchorName(anchorIdent(domId));
    }
    if (anchorTarget != null) {
      var usage = "<" + tag.tag().tagName() + "> anchoredTo (anchor must be laid out earlier)";
      var targetId = state.boundDomId(anchorTarget, usage, location);
      if (targetId != null) {
        style.positionAnchor(anchorIdent(targetId));
      }
    }
    if (transitionKey != null) {
      style.viewTransitionName(CssIdent.of(transitionKey.name()));
      var transitionClass = transitionKey.transitionClass();
      if (transitionClass != null) {
        style.viewTransitionClass(CssIdent.of(transitionClass));
      }
    }
    return style;
  }

  private static CssIdent anchorIdent(String domId) {
    return CssIdent.custom("anchor-" + domId);
  }

  static void resolveLateElements(CompilationState state) {
    Runnable next;
    while ((next = state.pendingLate.poll()) != null) {
      next.run();
    }
  }
}
