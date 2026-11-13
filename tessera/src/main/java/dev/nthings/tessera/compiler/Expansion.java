package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.ClassNames;
import dev.nthings.tessera.core.ComponentElement;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.KeyedElement;
import dev.nthings.tessera.core.LateElement;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.ScopeElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Expands authoring elements into [ExpandedNode]s for one page.
///
/// Collects styles and delegates placement. Identities, transitions and diagnostics go to the
/// [ExpansionState]; [DocumentPass] orders the document and [ReferenceLinker] links it.
final class Expansion {
  private final ExpansionState state;
  private final Placement placement;
  private final PageContext root;

  interface Placement {
    RuntimeException failureContext(Diagnostic diagnostic);

    ExpandedNode defer(Supplier<ExpandedNode> content, int priority);

    ExpandedNode late(Supplier<ExpandedNode> content);
  }

  Expansion(Placement placement, StyleSheet styles, DocumentState contributions) {
    this.state = new ExpansionState();
    this.placement = placement;
    this.root = PageContext.forDocument(styles, contributions, state);
  }

  ExpandedNode expand(IntoElement element) {
    return expand(element.intoElement(), root);
  }

  ExpandedNode expand(
      Location placedBy, String phase, Function<DocumentContext, ? extends IntoElement> content) {
    return expand(root.withPlacement(placedBy), phase, content);
  }

  List<ResolvedNode> link(List<ExpandedNode> nodes) {
    return ReferenceLinker.link(nodes, state);
  }

  @Nullable String boundId(ElementRef ref, String usage, Location location) {
    return state.boundId(ref, usage, location);
  }

  CompilationState compilation(String pageTitle) {
    return new CompilationState(pageTitle, state);
  }

  ExpandedNode expand(Element element, PageContext context) {
    return switch (element) {
      case EmptyElement _ -> ExpandedNode.Empty.INSTANCE;
      case TextElement(var content) -> new ResolvedText(content);
      case RawHtmlElement(var html) -> new ResolvedRaw(html);
      case DeferredElement child ->
          placement.defer(() -> expand(child.child(), context), child.priority());
      case LateElement(var content) -> placement.late(() -> expand(context, "Late", content));
      case FragmentElement fragment ->
          new ExpandedNode.Fragment(children(fragment.childrenList(), context));
      case ComponentElement component -> {
        var child = context.withComponent(component.name(), component.source());
        yield new ExpandedNode.Boundary(expand(child, "Expand", component.component()::expand));
      }
      case KeyedElement keyed -> keyed(keyed, context);
      case ScopeElement(var scope, var source) -> {
        var explicit = scope.scopeId();
        var scoped =
            context.withIdScope(
                explicit != null
                    ? Fnv1a64.sanitizeLocalId(explicit)
                    : state.nextId(scope.scopeName(), context.idScope()),
                explicit == null);
        var scopeId = scoped.idScope().domId();
        var child = scoped.withComponent(scope.scopeName() + "[" + scopeId + "]", source);
        if (hasUnstableScopeName(scope)) {
          state.report(
              child.location(),
              "unstable-scope-name",
              "Scope "
                  + scope.getClass().getName()
                  + " is an anonymous class or lambda, so its default scopeName() changes between"
                  + " builds",
              "Implement the scope as a named class or record, or override scopeName().");
        }
        var type =
            child
                .styles()
                .registerScope(
                    scope.scopeName(),
                    block ->
                        callback(
                            child,
                            "Style",
                            () -> {
                              scope.configureScope(block);
                              return block;
                            }));
        yield new ExpandedNode.ScopeBoundary(
            scoped(
                expand(child, "Expand", scope::expand), scope.scopeName(), type, scopeId, child));
      }
      case TagElement tag -> tag(tag, null, context);
    };
  }

  /// Expands an authoring callback's result; a failure names its component.
  ExpandedNode expand(
      PageContext context, String phase, Function<DocumentContext, ? extends IntoElement> content) {
    return expand(callback(context, phase, () -> content.apply(context).intoElement()), context);
  }

  /// Whether `scope` keeps the class-name default of [Scope#scopeName()] on a class without a
  /// stable name: an anonymous class, or a lambda or other hidden class.
  private static boolean hasUnstableScopeName(Scope scope) {
    var type = scope.getClass();
    if (!ClassNames.isUnstable(type)) {
      return false;
    }
    for (var method : type.getMethods()) {
      if (method.getName().equals("scopeName") && method.getParameterCount() == 0) {
        return method.getDeclaringClass() == Scope.class;
      }
    }
    throw new IllegalStateException("Scope without scopeName(): " + type.getName());
  }

  /// Puts `data-scope` on the one element host `node` resolves to. Component boundaries and keyed
  /// wrappers are transparent. Another scope keeps its own host. Anything else reports
  /// `scope-root` and is left unchanged: expansion does not invent a wrapper.
  private ExpandedNode scoped(
      ExpandedNode node, String scopeName, String type, String scopeId, PageContext context) {
    return switch (node) {
      case ExpandedNode.Boundary(var content) ->
          new ExpandedNode.Boundary(scoped(content, scopeName, type, scopeId, context));
      case ExpandedNode.Keyed(var key, var location, var content) ->
          new ExpandedNode.Keyed(key, location, scoped(content, scopeName, type, scopeId, context));
      case ExpandedNode.ScopeBoundary boundary -> {
        state.report(
            context.location(),
            "scope-root",
            "Scope '"
                + scopeName
                + "' returns another scope as its root. That scope keeps its own data-scope",
            "Return one element, such as a div, ul, or tr, and place the nested scope inside it.");
        yield boundary;
      }
      case ExpandedNode.Tag tag -> {
        var attributes = new LinkedHashMap<>(tag.attributes());
        attributes.put("data-scope", type);
        attributes.put("data-scope-id", scopeId);
        yield tag.withAttributes(attributes);
      }
      default -> {
        state.report(
            context.location(),
            "scope-root",
            "Scope '"
                + scopeName
                + "' returns "
                + rootKind(node)
                + " as its root, which cannot carry data-scope",
            rootFix(node));
        yield node;
      }
    };
  }

  /// What an invalid scope root is, for the `scope-root` message.
  private static String rootKind(ExpandedNode node) {
    return switch (node) {
      case ExpandedNode.Fragment _ -> "a fragment";
      case ResolvedText _ -> "text";
      case ResolvedRaw _ -> "raw HTML";
      case ExpandedNode.Empty _ -> "nothing";
      case ExpandedNode.Slot _ -> "late content";
      default -> "content that is not an element";
    };
  }

  /// How to give `node` an element host.
  private static String rootFix(ExpandedNode node) {
    return switch (node) {
      case ExpandedNode.Fragment _ ->
          "Return one element host. Put several children in an element you choose, such as ul or"
              + " div.";
      case ResolvedText _ -> "Return one element host and place the text inside it.";
      case ResolvedRaw _ -> "Return one element host instead of raw HTML.";
      case ExpandedNode.Empty _ ->
          "Return one element host. Deferred content is moved out of the scope and cannot be its"
              + " root.";
      case ExpandedNode.Slot _ -> "Return one element host and place the late content inside it.";
      default -> "Return one element host, such as a div, ul, or tr.";
    };
  }

  /// Runs an authoring callback; a failure keeps its type and gains the component path as a
  /// suppressed diagnostic.
  private <T> T callback(PageContext context, String phase, Supplier<T> work) {
    try {
      return work.get();
    } catch (RuntimeException failure) {
      failure.addSuppressed(
          placement.failureContext(
              new Diagnostic(
                  "callback-failure",
                  phase + " callback failed: " + failure.getMessage(),
                  "Correct the callback before compiling again.",
                  context.location())));
      throw failure;
    }
  }

  /// Lays out `children` in order, dropping the ones that emit nothing. Linking checks sibling
  /// keys once late content has expanded.
  private List<ExpandedNode> children(List<Element> children, PageContext context) {
    var result = new ArrayList<ExpandedNode>(children.size());
    for (var child : children) {
      var node = expand(child, context);
      if (node != ExpandedNode.Empty.INSTANCE) {
        result.add(node);
      }
    }
    return result;
  }

  /// Expands keyed content in the context [#keyedContext] made for it. A generated enclosing id
  /// scope reports `generated-scope-id` once for this key, then expansion continues.
  private ExpandedNode keyed(KeyedElement keyed, PageContext context) {
    var child = keyedContext(keyed, context);
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
    var content =
        keyed.content() instanceof ComponentElement component
            ? expand(child, "Expand", component.component()::expand)
            : expand(keyed.content(), child);
    return new ExpandedNode.Keyed(keyed.key(), child.location(), content);
  }

  /// The context of keyed content and a component path entry naming the key. A keyed component
  /// takes the key on its own entry instead of gaining another.
  ///
  /// Its own slug depends only on the enclosing id scope, the [PageContext#keyOwner()] and the key,
  /// never on a sibling index. A generated id anywhere in that enclosing scope is reported as
  /// `generated-scope-id`, because that counter does change when siblings move:
  /// `key-<hex key>` without an owner and `<owner>-key-<hex key>` with one. The key's hexadecimal
  /// UTF-16 units contain no `-`, so the slug splits back at its last `-key-` and distinct keys or
  /// owner slugs never share a scope.
  private static PageContext keyedContext(KeyedElement keyed, PageContext context) {
    var owner = context.keyOwner();
    var slug = (owner == null ? "" : owner + "-") + "key-" + encodeKey(keyed.key());
    var scoped = context.withKeyedScope(slug);
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
  private ExpandedNode.Tag tag(TagElement element, @Nullable String forcedId, PageContext context) {
    var attrs = new LinkedHashMap<>(element.attributes());
    var source = element.source() != null ? element.source() : context.origin();
    var location = context.location(element.tag().tagName(), null, source);
    var domId = forcedId;
    var refs = element.boundRefs();
    if (domId == null && !refs.isEmpty()) {
      domId = state.bind(refs, context.idScope(), location);
    }
    if (domId != null) {
      attrs.put("id", domId);
      location = location.withDomId(domId);
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

    ExpandedNode.Tag companion = null;
    var tooltip = element.interestTooltip();
    if (tooltip != null) {
      var tooltipId = domId + "--tooltip";
      attrs.put("interestfor", tooltipId);
      state.registerDomId(tooltipId, location);
      context.contributeScript(TooltipScript.INSTANCE);
      companion = tag(tooltip, tooltipId, context);
    }

    var navigation = element.navigationHeading();
    if (navigation != null) {
      context.collectHeading(navigation, location);
    }

    var references = element.idReferences();
    var owner = keyOwner(refs);
    return new ExpandedNode.Tag(
        element.tag(),
        location,
        attrs,
        references.isEmpty() ? Map.of() : references,
        children(element.childrenList(), owner == null ? context : context.withKeyOwner(owner)),
        companion);
  }

  /// The name keyed descendants of an element bound to `refs` are told apart by: its first named
  /// ref's name, without leading or trailing `-` so it never forms the `--` scope separator, or
  /// `null` when no ref is named.
  private static @Nullable String keyOwner(List<ElementRef> refs) {
    for (var ref : refs) {
      var name = ref.name();
      if (name != null) {
        var slug = Fnv1a64.sanitizeLocalId(name).replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? null : slug;
      }
    }
    return null;
  }

  /// Adds declarations derived from identity: anchor names and view-transition keys.
  private Declarations identityStyles(
      TagElement element, @Nullable String domId, Location location) {
    var anchor = element.anchorTarget();
    var transition = element.transitionKey();
    if (!element.isAnchor() && anchor == null && transition == null) {
      return element.style();
    }
    var style = new Declarations().mergeFrom(element.style());
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
