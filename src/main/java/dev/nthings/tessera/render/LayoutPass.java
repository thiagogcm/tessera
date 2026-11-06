package dev.nthings.tessera.render;

import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.DocumentState;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.LateElement;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnceElement;
import dev.nthings.tessera.core.RenderViewElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.ResolvedNode.ResolvedEmpty;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedSlot;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
    return RenderContext.runRoot(
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
          return new Result(root, headNodes, docState);
        });
  }

  static ResolvedNode requestLayout(Element element, CompilationState state) {
    var ctx = RenderContext.current();
    return switch (element) {
      case EmptyElement _ -> ResolvedEmpty.INSTANCE;
      case TextElement(var content) -> new ResolvedText(content);
      case RawHtmlElement(var html) -> new ResolvedRaw(html);
      case DeferredElement deferred -> {
        var resolvedDeferredChild = requestLayout(deferred.child(), state);
        state.enqueueDeferred(deferred.priority(), resolvedDeferredChild);
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
      PageValidator.validateImage(attrs, style);
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

  static void resolveLateElements(CompilationState state) {
    Runnable next;
    while ((next = state.pendingLate.poll()) != null) {
      next.run();
    }
  }
}
