package dev.nthings.tessera.view;

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
import dev.nthings.tessera.core.ViewException;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.foundation.Fnv1a64;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.ClassNames;
import dev.nthings.tessera.internal.authoring.PortableNode;
import dev.nthings.tessera.internal.authoring.TagData;
import dev.nthings.tessera.internal.css.CssAccess;
import dev.nthings.tessera.internal.css.StyleSheet;
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
public abstract sealed class ViewExpansion<M>
    permits DocumentExpansion, ViewRenderer.LiveExpansion {

  /// Backend-facing: where expansion and validation report what they find wrong, as a [Diagnostic]
  /// of the expansion.
  @FunctionalInterface
  public interface Reporter {

    /// Reports `rule` at `location`.
    void report(Location location, String rule, String message, String fix);
  }

  /// A component placement of any family: its authoring class, diagnostic name, call site and the
  /// expansion of its body in the component's context.
  record Placement<M>(
      Class<?> type,
      String name,
      @Nullable SourceLocation source,
      Function<ExpansionContext, ViewNode<M>> body) {}

  private final ViewState state = new ViewState();
  private final Reporter reporter = state::report;
  private final ExpansionRegistry registry = new ExpansionRegistry();
  final ExpansionContext root;

  ViewExpansion(StyleSheet styles, ViewScope scope) {
    root = new ExpansionContext(scope, styles, this, List.of(), null, null, TagName.Namespace.HTML);
  }

  final ViewNode<M> expandPortable(Element node, ExpansionContext context) {
    return switch (node) {
      case PortableNode.Empty _ -> new ViewNode.Empty<>();
      case PortableNode.Text(var text) -> new ViewSnapshot.Text<>(text);
      case FragmentElement fragment ->
          new ViewNode.Fragment<>(
              null,
              context.location(),
              children(AuthoringAccess.fragment(fragment), context, this::expandPortable));
      case PortableNode.Keyed(var key, var content) ->
          keyed(
              key,
              content.getClass(),
              portablePlacement(content),
              context,
              child -> expandPortable(content, child));
      case TagElement tag -> tag(tag, null, _ -> List.of(), null, context);
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
  }

  final @Nullable Placement<M> portablePlacement(Element node) {
    return node instanceof PortableNode.ComponentPlacement component
        ? portablePlacement(component)
        : null;
  }

  private Placement<M> portablePlacement(PortableNode.ComponentPlacement component) {
    return new Placement<>(
        component.type(),
        ClassNames.componentName(component.type()),
        component.source(),
        child ->
            expandPortable(
                callback(child, "Expand", () -> component.component().expand(child).intoElement()),
                child));
  }

  final ViewNode<M> component(Placement<M> placement, ExpansionContext context) {
    var child = context.withComponent(placement.name(), placement.source());
    return boundary(null, placement.type(), child, placement.body().apply(child));
  }

  RuntimeException failureContext(Diagnostic diagnostic) {
    return new ViewException(List.of(diagnostic));
  }

  /// Compiles the values of this element's live declarations for this backend.
  abstract Map<String, String> liveDeclarations(
      Declarations live, Declarations into, StyleSheet styles);

  /// Validates a host's backend-specific bindings while its authoring location is available.
  void validateHost(
      @Nullable M payload, TagName tag, Map<String, String> attributes, Location location) {}

  /// Opens the id scope of `scope`, registers its styles and marks the explicit host that `host`
  /// expands in the scope's context with `data-scope` and `data-scope-id`.
  final ViewNode<M> scope(
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
        child
            .styles()
            .registerScope(
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

  /// Expands keyed `content` in the context [#keyedContext] made for it. A generated enclosing id
  /// scope reports `generated-scope-id` once for this key, then expansion continues. A component or
  /// fragment keeps its own boundary and takes the key on it; anything else is wrapped so the key
  /// still names one sibling.
  final ViewNode<M> keyed(
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

  /// The context of keyed content and a component path entry naming the key. A keyed component
  /// takes the key on its own entry instead of gaining another.
  ///
  /// Its slug depends only on the enclosing id scope, the [ExpansionContext#keyOwner()] and the
  /// key, never on a sibling index. A generated id anywhere in that enclosing scope is reported as
  /// `generated-scope-id`, because that counter does change when siblings move: `key-<hex key>`
  /// without an owner and `<owner>-key-<hex key>` with one. The key's hexadecimal UTF-16 units
  /// contain no `-`, so the slug splits back at its last `-key-` and distinct keys or owner slugs
  /// never share a scope.
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
  final <T> T callback(ExpansionContext context, String phase, Supplier<T> work) {
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
  final <N> List<ViewNode<M>> children(
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
  ViewNode.Tag<M> tag(
      TagElement host,
      @Nullable String forcedId,
      Function<ExpansionContext, List<ViewNode<M>>> extra,
      @Nullable M payload,
      ExpansionContext context) {
    var element = AuthoringAccess.tag(host);
    var attrs = new LinkedHashMap<>(element.attributes);
    var source = element.source != null ? element.source : context.origin();
    var location = new Location(context.components(), element.tag.tagName(), null, source);
    var domId = forcedId;
    var refs = element.refs;
    if (domId == null && !refs.isEmpty()) {
      domId = state.bind(refs, context.idScope(), location);
    }
    if (domId != null) {
      attrs.put("id", domId);
      location = location.withDomId(domId);
    }
    validateHost(payload, element.tag, attrs, location);
    validateNamespace(element.tag, context.namespace(), location);

    var style = identityStyles(element, domId, location);
    Map<String, String> live = Map.of();
    var liveStyle = element.live;
    if (liveStyle != null) {
      // identityStyles returns the builder's own declarations or a fresh copy.
      if (style == element.style) {
        style = new Declarations().mergeFrom(style);
      }
      live = liveDeclarations(liveStyle, style, context.styles());
    }

    var transition = element.transitionKey;
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
        (owner == null ? context : context.withKeyOwner(owner))
            .withNamespace(element.tag.namespace());
    var children = children(element.children, childContext, this::expandPortable);
    children.addAll(extra.apply(childContext));
    var node =
        new ViewNode.Tag<>(
            new TagCore<>(element.tag, location, payload, live, element.initial),
            attrs,
            references.isEmpty() ? Map.of() : new LinkedHashMap<>(references),
            children,
            companion);
    ViewValidation.checkElement(node, style, reporter);
    return node;
  }

  /// An element must belong to the namespace its parent establishes, or an HTML parser and a DOM
  /// backend would put it in different namespaces: SVG content needs an `svg` root, and HTML
  /// content cannot sit inside SVG.
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

  /// Adds declarations derived from identity: anchor names and view-transition keys.
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

  /// Pairs `anchor-name` on the anchor with `position-anchor` on the elements anchored to it.
  private static Anchor anchorIdent(String domId) {
    return Anchor.named("anchor-" + domId);
  }

  /// The name keyed descendants of an element bound to `refs` are told apart by: its first named
  /// ref's name, without leading or trailing `-` so it never forms the `--` scope separator, or
  /// `null` when no ref is named.
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

  /// Resolves references, late slots and tooltip companions of `node` into linked nodes. Call it
  /// once every element of the expansion is expanded, so every ref is bound.
  public List<ViewSnapshot.Node<M>> link(ViewNode<M> node) {
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

  Reporter reporter() {
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

  Map<ElementRef, String> references() {
    return state.references();
  }
}
