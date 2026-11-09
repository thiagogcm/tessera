package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.ClassNames;
import dev.nthings.tessera.core.ComponentElement;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.KeyedElement;
import dev.nthings.tessera.core.LateElement;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.core.ViewElement;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  ViewNode expand(Element element, PageContext context) {
    return switch (element) {
      case EmptyElement _ -> ViewNode.Empty.INSTANCE;
      case TextElement(var content) -> new ResolvedText(content);
      case RawHtmlElement(var html) -> new ResolvedRaw(html);
      case DeferredElement child ->
          placement.defer(() -> expand(child.child(), context), child.priority());
      case LateElement(var content) -> placement.late(() -> expand(context, "Late", content));
      case FragmentElement fragment ->
          new ViewNode.Fragment(children(fragment.childrenList(), context));
      case ComponentElement(var component, var name, var source) -> {
        var child = context.withComponent(name, source);
        yield expand(child, "Render", component::render);
      }
      case KeyedElement keyed -> keyed(keyed, keyedContext(keyed, keyPrefix(context), context));
      case ViewElement(var view, var source) -> {
        var id = view.viewId();
        var scoped =
            context.withViewScope(
                id != null
                    ? Fnv1a64.sanitizeLocalId(id)
                    : state.nextId(view.viewType(), context.viewScope()));
        var scopeId = scoped.viewScope().domId();
        var child = scoped.withComponent(view.viewType() + "[" + scopeId + "]", source);
        if (hasUnstableViewType(view)) {
          state.report(
              child.location(),
              "unstable-view-type",
              "View "
                  + view.getClass().getName()
                  + " is an anonymous class or lambda, so its default viewType() changes between"
                  + " builds",
              "Implement the view as a named class or record, or override viewType().");
        }
        var type =
            child
                .styles()
                .registerViewScope(
                    view.viewType(),
                    scope ->
                        callback(
                            child,
                            "Style",
                            () -> {
                              view.configureScope(scope);
                              return scope;
                            }));
        yield scoped(expand(child, "Render", view::render), type, scopeId, child);
      }
      case TagElement tag -> tag(tag, null, context);
    };
  }

  /// Expands an authoring callback's result; a failure names its component.
  ViewNode expand(
      PageContext context, String phase, Function<RenderContext, ? extends IntoElement> content) {
    return expand(callback(context, phase, () -> content.apply(context).intoElement()), context);
  }

  /// Whether `view` keeps the class-name default of [View#viewType()] on a class without a stable
  /// name: an anonymous class, or a lambda or other hidden class.
  private static boolean hasUnstableViewType(View view) {
    var type = view.getClass();
    if (!ClassNames.isUnstable(type)) {
      return false;
    }
    try {
      return type.getMethod("viewType").getDeclaringClass() == View.class;
    } catch (NoSuchMethodException impossible) {
      return false;
    }
  }

  /// Adds the view's scope attributes to its root element, or wraps a root that is not one.
  private ViewNode scoped(ViewNode node, String type, String scopeId, PageContext context) {
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
  private <T> T callback(PageContext context, String phase, Supplier<T> work) {
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
                      state.location(context.location())))));
      throw failure;
    }
  }

  /// Lays out `children` in order, dropping the ones that emit nothing. Keyed children share one
  /// scope prefix, created at the first of them, and report a key an earlier sibling uses.
  private List<ViewNode> children(List<Element> children, PageContext context) {
    var result = new ArrayList<ViewNode>(children.size());
    String prefix = null;
    Set<String> keys = Set.of();
    for (var child : children) {
      ViewNode node;
      if (child instanceof KeyedElement keyed) {
        if (prefix == null) {
          prefix = keyPrefix(context);
          keys = new HashSet<>();
        }
        var keyedContext = keyedContext(keyed, prefix, context);
        if (!keys.add(keyed.key())) {
          state.report(
              keyedContext.location(),
              "duplicate-key",
              "Duplicate sibling key '" + keyed.key() + "'",
              "Give each child of this parent a distinct key.");
        }
        node = keyed(keyed, keyedContext);
      } else {
        node = expand(child, context);
      }
      if (node != ViewNode.Empty.INSTANCE) {
        result.add(node);
      }
    }
    return result;
  }

  /// The scope prefix of one list of keyed siblings in `context`'s view scope; each sibling's key
  /// completes it. Lists are numbered within the view scope, like generated ids.
  private String keyPrefix(PageContext context) {
    return state.nextId("key", context.viewScope()) + "-";
  }

  /// Expands keyed content in the context [#keyedContext] made for it.
  private ViewNode keyed(KeyedElement keyed, PageContext child) {
    if (keyed.content() instanceof ComponentElement component) {
      return expand(child, "Render", component.component()::render);
    }
    return expand(keyed.content(), child);
  }

  /// The context of keyed content: the view scope `prefix` followed by its encoded key, and a
  /// component path entry naming the key. A keyed component takes the key on its own entry
  /// instead of gaining another.
  private static PageContext keyedContext(KeyedElement keyed, String prefix, PageContext context) {
    var scoped = context.withViewScope(prefix + encodeKey(keyed.key()));
    var label = "[key=" + keyed.key() + "]";
    if (keyed.content() instanceof ComponentElement component) {
      return scoped.withComponent(component.name() + label, component.source());
    }
    return scoped.withComponent(label, null);
  }

  /// The key as hexadecimal UTF-16 units, so distinct keys always give distinct scopes.
  private static String encodeKey(String key) {
    var encoded = new StringBuilder(key.length() * 4);
    for (var index = 0; index < key.length(); index++) {
      var hex = Integer.toHexString(key.charAt(index));
      encoded.append("0000", 0, 4 - hex.length()).append(hex);
    }
    return encoded.toString();
  }

  /// Expands `element`. `forcedId` (already registered) is used for companions, not bound.
  private ViewNode.Tag tag(TagElement element, @Nullable String forcedId, PageContext context) {
    var attrs = new LinkedHashMap<>(element.attributes());
    var source = element.source() != null ? element.source() : context.origin();
    var location =
        new ViewLocation(context.componentPath(), element.tag().tagName(), attrs.get("id"), source);
    var domId = forcedId;
    var refs = element.boundRefs();
    if (domId == null && !refs.isEmpty()) {
      domId = state.bind(refs, context.viewScope(), location);
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

    var classes = context.styles().registerElement(style);
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
      style.viewTransition(transition.name(), transition.transitionClass());
    }
    return style;
  }

  private static Anchor anchorIdent(String domId) {
    return Anchor.named("anchor-" + domId);
  }
}
