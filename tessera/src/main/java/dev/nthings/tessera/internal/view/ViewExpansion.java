package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.ScopeDefinition;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.SvgTag;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.ClassNames;
import dev.nthings.tessera.internal.authoring.PortableNode;
import dev.nthings.tessera.internal.authoring.TagData;
import dev.nthings.tessera.internal.css.CssAccess;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Shared expansion for one page or one live render. It expands portable authoring, establishes
/// component and ID scopes, collects styles, and links the result. Each backend lowers its own
/// placements using the same operations.
public abstract class ViewExpansion<M> {

  /// A component placement of any family: its authoring class, diagnostic name, call site and the
  /// expansion of its body in the component's context.
  public record Placement<M>(
      Class<?> type,
      @Nullable SourceLocation source,
      Function<ExpansionContext, ViewNode<M>> body) {
    String name() {
      return ClassNames.componentName(type);
    }
  }

  private final ViewState state = new ViewState();
  // A lambda rather than `state::report`, whose receiver null check SpotBugs reads as a constructor
  // throw.
  private final DiagnosticSink reporter =
      (location, rule, message, fix) -> state.report(location, rule, message, fix);
  private final ExpansionRegistry registry = new ExpansionRegistry();
  private final ViewScope rootScope;
  private @Nullable ExpansionContext root;
  protected final StyleSheet styles;

  protected ViewExpansion(StyleSheet styles, ViewScope scope) {
    this.styles = styles;
    this.rootScope = scope;
  }

  protected final ExpansionContext root() {
    var context = root;
    if (context == null) {
      context =
          new ExpansionContext(rootScope, this, List.of(), null, null, TagName.Namespace.HTML);
      root = context;
    }
    return context;
  }

  protected final ViewNode<M> expandPortable(Element node, ExpansionContext context) {
    return switch (node) {
      case FragmentElement fragment ->
          new ViewNode.Fragment<>(
              null,
              context.location(),
              children(AuthoringAccess.fragment(fragment), context, this::expandPortable));
      case TagElement tag -> tag(tag, null, _ -> List.of(), null, context);
      default ->
          switch (AuthoringAccess.portable(node)) {
            case PortableNode.Empty _ -> new ViewNode.Empty<>();
            case PortableNode.Text(var text) -> new LinkedNode.Text<>(text);
            case PortableNode.Keyed(var key, var content) ->
                keyed(
                    key,
                    content.getClass(),
                    portablePlacement(content),
                    context,
                    child -> expandPortable(content, child));
            case PortableNode.ScopePlacement(var scope, var source) ->
                scope(
                    scope,
                    source,
                    context,
                    child ->
                        tag(
                            callback(child, "Expand", () -> scope.expand(child)),
                            null,
                            _ -> List.of(),
                            null,
                            child));
            case PortableNode.ComponentPlacement component ->
                component(portablePlacement(component), context);
          };
    };
  }

  protected final @Nullable Placement<M> portablePlacement(Element node) {
    return !(node instanceof TagElement || node instanceof FragmentElement)
            && AuthoringAccess.portable(node) instanceof PortableNode.ComponentPlacement component
        ? portablePlacement(component)
        : null;
  }

  private Placement<M> portablePlacement(PortableNode.ComponentPlacement component) {
    return new Placement<>(
        component.type(),
        component.source(),
        child ->
            expandPortable(
                callback(child, "Expand", () -> component.component().expand(child).intoElement()),
                child));
  }

  protected final ViewNode<M> component(Placement<M> placement, ExpansionContext context) {
    var child = context.withComponent(placement.name(), placement.source());
    return boundary(null, placement.type(), child, placement.body().apply(child));
  }

  protected abstract RuntimeException failureContext(Diagnostic diagnostic);

  /// Compiles the values of this element's live declarations for this backend.
  protected abstract Map<String, String> liveDeclarations(Declarations live, Declarations into);

  /// Validates a host's backend-specific bindings while its authoring location is available.
  protected void validateHost(
      @Nullable M payload, TagName tag, Map<String, String> attributes, Location location) {}

  /// Opens the id scope of `scope`, registers its styles and marks the explicit host that `host`
  /// expands in the scope's context with `data-scope` and `data-scope-id`.
  protected final ViewNode<M> scope(
      ScopeDefinition scope,
      @Nullable SourceLocation source,
      ExpansionContext context,
      Function<ExpansionContext, ViewNode.Tag<M>> host) {
    var name = scope.scopeName();
    var unstableDefault = name.isEmpty() && ClassNames.isUnstable(scope.getClass());
    if (unstableDefault) {
      name = "scope";
    }
    var explicit = scope.scopeId();
    var scoped =
        context.withIdScope(
            explicit != null
                ? Fnv1a64.sanitizeLocalId(explicit)
                : state.nextId(name, context.idScope()),
            explicit == null);
    var scopeId = scoped.idScope().domId();
    var child = scoped.withComponent(name + "[" + scopeId + "]", source);
    if (unstableDefault) {
      state.report(
          child.location(),
          "unstable-scope-name",
          "Scope "
              + scope.getClass().getName()
              + " is an anonymous class or lambda, so its default scopeName() changes between"
              + " builds",
          "Implement the scope as a named class or record, or override scopeName().");
    }
    var scopeKey =
        styles.registerScope(
            name,
            block ->
                callback(
                    child,
                    "Style",
                    () -> {
                      scope.configureScope(block);
                      return block;
                    }));
    var tag = host.apply(child);
    var attributes = new LinkedHashMap<>(tag.attributes());
    attributes.put("data-scope", scopeKey);
    attributes.put("data-scope-id", scopeId);
    return new ViewNode.ScopeBoundary<>(
        null, name, child.location(), List.of(tag.withAttributes(attributes)));
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

  /// Expands keyed `content` in the context [#keyedContext] made for it.
  protected final ViewNode<M> keyed(
      String key,
      Class<?> definition,
      @Nullable Placement<M> component,
      ExpansionContext context,
      Function<ExpansionContext, ViewNode<M>> expand) {
    var child = keyedContext(key, component, context);
    if (context.idScope().generated()) {
      state.report(
          child.location(),
          "generated-scope-id",
          "Keyed content is inside scope '"
              + context.idScope().domId()
              + "', whose id was generated",
          "Set scopeId() on each enclosing scope. A generated id is numbered in encounter"
              + " order, so moving a sibling scope renames every keyed id inside it.");
    }
    if (component != null) {
      return boundary(key, component.type(), child, component.body().apply(child));
    }
    var expanded = expand.apply(child);
    return switch (expanded) {
      case ViewNode.Boundary<M> boundary when boundary.key() == null ->
          new ViewNode.Boundary<>(
              key, boundary.definition(), boundary.location(), boundary.children());
      case ViewNode.ScopeBoundary<M> scope when scope.key() == null ->
          new ViewNode.ScopeBoundary<>(key, scope.definition(), scope.location(), scope.children());
      case ViewNode.Fragment<M> fragment when fragment.key() == null ->
          new ViewNode.Fragment<>(key, fragment.location(), fragment.children());
      default -> boundary(key, definition, child, expanded);
    };
  }

  private static ExpansionContext keyedContext(
      String key, @Nullable Placement<?> component, ExpansionContext context) {
    var owner = context.keyOwner();
    var slug = (owner == null ? "" : owner + "-") + "key-" + encodeKey(key);
    var scoped = context.withKeyedScope(slug);
    var label = "[key=" + key + "]";
    if (component != null) {
      return scoped.withComponent(component.name() + label, component.source());
    }
    return scoped.withComponent(label, null);
  }

  private ViewNode.Boundary<M> boundary(
      @Nullable String key, Object definition, ExpansionContext context, ViewNode<M> content) {
    return new ViewNode.Boundary<>(key, definition, context.location(), List.of(content));
  }

  /// Runs an authoring callback; a failure keeps its type and gains the component path as a
  /// suppressed diagnostic from the backend.
  protected final <T> T callback(ExpansionContext context, String phase, Supplier<T> work) {
    try {
      return work.get();
    } catch (RuntimeException failure) {
      failure.addSuppressed(
          failureContext(
              new Diagnostic(
                  "callback-failure",
                  phase + " callback failed: " + failure.getMessage(),
                  "Correct the callback before expanding again.",
                  context.location())));
      throw failure;
    }
  }

  /// Lays out `nodes` in order, dropping those that emit nothing.
  protected final <N> List<ViewNode<M>> children(
      List<N> nodes,
      ExpansionContext context,
      BiFunction<N, ExpansionContext, ViewNode<M>> expand) {
    var result = new ArrayList<ViewNode<M>>(nodes.size());
    for (var element : nodes) {
      var node = expand.apply(element, context);
      if (!(node instanceof ViewNode.Empty<M>)) {
        result.add(node);
      }
    }
    return result;
  }

  /// Expands a portable host, then any additional children its backend contributes.
  protected ViewNode.Tag<M> tag(
      TagElement host,
      @Nullable String forcedId,
      Function<ExpansionContext, List<ViewNode<M>>> extra,
      @Nullable M payload,
      ExpansionContext context) {
    var element = AuthoringAccess.tag(host);
    var tagName = element.tag;
    var attrs = new LinkedHashMap<String, String>();
    var behavior = element.behavior;
    if (behavior != null) {
      // The host is the custom element its bundled script defines, defined or not.
      if (behavior.customizesBuiltIn()) {
        attrs.put("is", behavior.element().tagName());
      } else {
        tagName = behavior.element();
      }
    }
    attrs.putAll(element.attributes);
    var source = element.source != null ? element.source : context.origin();
    var location = new Location(context.components(), tagName.tagName(), null, source);
    var domId = forcedId;
    var refs = element.refs;
    if (domId == null && !refs.isEmpty()) {
      domId = state.bind(refs, context.idScope(), location);
    }
    if (domId != null) {
      attrs.put("id", domId);
      location = ViewState.withDomId(location, domId);
    }
    validateHost(payload, tagName, attrs, location);
    validateNamespace(tagName, context.namespace(), location);

    var style = identityStyles(element, domId, location);
    Map<String, String> live = Map.of();
    var liveStyle = element.live;
    if (liveStyle != null) {
      if (style == element.style) {
        style = new Declarations().mergeFrom(style);
      }
      live = liveDeclarations(liveStyle, style);
    }

    var transition = element.transitionKey;
    if (transition != null) {
      state.registerTransition(transition, location);
      var transitionClass = transition.transitionClass();
      if (transitionClass != null) {
        styles.registerTransitionClass(transitionClass);
      }
    }

    var classes = styles.registerElement(style);
    if (!classes.isEmpty()) {
      var existing = attrs.get("class");
      var joined = String.join(" ", classes);
      attrs.put("class", ViewValidation.isNonBlank(existing) ? existing + " " + joined : joined);
    }

    ViewNode.Tag<M> companion = null;
    var tooltip = element.interestTooltip;
    if (tooltip != null) {
      var tooltipId = domId + "--tooltip";
      attrs.put("interestfor", tooltipId);
      state.registerId(tooltipId, location);
      companion = tag(tooltip, tooltipId, _ -> List.of(), null, context);
    }

    var navigation = element.navigationHeading;
    if (navigation != null) {
      context.collectHeading(navigation, location);
    }

    // The builder keeps its own maps live, so the node takes copies.
    var references = element.idReferences;
    var owner = keyOwner(refs);
    var childContext =
        (owner == null ? context : context.withKeyOwner(owner)).withNamespace(tagName.namespace());
    var children = children(element.children, childContext, this::expandPortable);
    children.addAll(extra.apply(childContext));
    var node =
        new ViewNode.Tag<>(
            new TagCore<>(tagName, location, payload, live, element.initial),
            attrs,
            references.isEmpty() ? Map.of() : new LinkedHashMap<>(references),
            children,
            companion);
    ViewValidation.checkElement(node, style, element.unknownDimensions, reporter);
    return node;
  }

  private void validateNamespace(TagName tag, TagName.Namespace parent, Location location) {
    if (tag.namespace() == parent || tag == SvgTag.SVG) {
      return;
    }
    if (parent == TagName.Namespace.SVG) {
      state.report(
          location,
          "svg-namespace",
          "<" + tag.tagName() + "> is placed inside SVG content",
          "Place HTML elements outside the svg element.");
    } else {
      state.report(
          location,
          "svg-namespace",
          "SVG <" + tag.tagName() + "> is placed outside an svg element",
          "Place SVG shapes inside Elements.svg(SvgTag.SVG) or Elements.svgIcon(...).");
    }
  }

  private Declarations identityStyles(TagData element, @Nullable String domId, Location location) {
    var anchor = element.anchorTarget;
    var transition = element.transitionKey;
    if (!element.anchorSelf && anchor == null && transition == null) {
      return element.style;
    }
    var style = new Declarations().mergeFrom(element.style);
    if (domId != null && element.anchorSelf) {
      style.anchorName(anchorIdent(domId));
    }
    if (anchor != null) {
      var usage = "<" + element.tag.tagName() + "> anchoredTo (anchor must be laid out earlier)";
      var target = state.boundId(anchor, usage, location);
      if (target != null) {
        style.positionAnchor(anchorIdent(target));
      }
    }
    if (transition != null) {
      CssAccess.get().viewTransition(style, transition.name(), transition.transitionClass());
    }
    return style;
  }

  private static Anchor anchorIdent(String domId) {
    var sanitized = Fnv1a64.sanitizeIdentifier(domId);
    if (sanitized.equals(domId)) {
      return Anchor.named("anchor-" + domId);
    }
    var hash = Fnv1a64.toBase36Hash(Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, domId));
    return Anchor.named("anchor-" + sanitized + "-" + hash);
  }

  private static @Nullable String keyOwner(List<ElementRef> refs) {
    for (var ref : refs) {
      var name = AuthoringAccess.reference(ref).name();
      if (name != null) {
        var slug = Fnv1a64.sanitizeLocalId(name).replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? null : slug;
      }
    }
    return null;
  }

  /// Resolves references, late slots and tooltip companions of `node` into linked nodes.
  public List<LinkedNode<M>> link(ViewNode<M> node) {
    return new ReferenceLinker<M>(state).link(node);
  }

  /// The DOM id bound to `ref`, or `null` when it is ambiguous or unbound; an unbound ref is
  /// reported as `unbound-reference`, naming `usage`.
  public @Nullable String boundId(ElementRef ref, String usage, Location location) {
    return state.boundId(ref, usage, location);
  }

  /// The expansion state and headings registered so far.
  public ExpansionRegistry registry() {
    return registry;
  }

  public DiagnosticSink reporter() {
    return reporter;
  }

  /// The diagnostics reported so far, in order.
  public List<Diagnostic> diagnostics() {
    return state.diagnostics();
  }

  /// Every DOM id registered so far, in registration order.
  public Set<String> ids() {
    return state.ids();
  }

  /// The DOM ids registered more than once, which references cannot resolve.
  public Set<String> ambiguousIds() {
    return state.ambiguousIds();
  }

  /// Each transition key and where it was first used.
  public Map<TransitionKey, Location> transitions() {
    return state.transitions();
  }

  public Map<ElementRef, String> references() {
    return state.references();
  }
}
