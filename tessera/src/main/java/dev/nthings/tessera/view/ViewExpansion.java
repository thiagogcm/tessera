package dev.nthings.tessera.view;

import dev.nthings.tessera.core.ClassNames;
import dev.nthings.tessera.core.ComponentElement;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.EffectElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.EmptyElement;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.KeyedElement;
import dev.nthings.tessera.core.LateElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.core.RawHtmlElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.ScopeElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TextElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Backend-facing: expands authoring elements into [ViewNode]s for one page or one live render,
/// then links them into [ViewSnapshot] nodes.
///
/// Expansion runs component and view render callbacks, binds element refs to DOM ids within view
/// scopes and keyed children, registers styles in the [StyleSheet], keeps the per-page
/// [DocumentRegistry], and checks the contracts every backend shares (ids, references, transition
/// keys, sibling keys, controlled form properties). What differs between backends goes through the
/// [Backend], which reports what it rejects through the [Reporter] it is handed. Diagnostics are
/// collected, not thrown; the backend decides how to fail.
public final class ViewExpansion {

  /// Backend-facing: what an output backend decides during expansion: placement of deferred and
  /// late elements, document contributions, raw HTML, effects, live styles and backend-specific
  /// element checks. A hook that cannot honour a request reports it through its [Reporter].
  public interface Backend {

    /// The context attached, as a suppressed exception, to a failing render callback. The
    /// application's exception keeps its type.
    default RuntimeException failureContext(Diagnostic diagnostic) {
      return new ViewException(List.of(diagnostic));
    }

    /// Places `content` after the main flow, ordered by `priority`, or rejects it.
    ViewNode defer(Supplier<ViewNode> content, int priority, Location placedBy, Reporter reporter);

    /// Expands `content` after the main flow, in its original position, into a [ViewNode.Slot] the
    /// backend fills; or rejects it.
    ViewNode late(Supplier<ViewNode> content, Location placedBy, Reporter reporter);

    /// Places trusted raw HTML, or rejects it.
    ViewNode raw(String html, Location placedBy, Reporter reporter);

    /// Places work that a live backend starts after commit; a document rejects it.
    ViewNode effect(ViewSnapshot.Effect effect, Reporter reporter);

    /// Writes the declarations compiled for an element's `live` styles into `into`, which already
    /// holds the element's other declarations. A live backend reads values from custom properties,
    /// which it records in `properties` and registers with its stylesheet
    /// ([StyleSheet#registerLive]); a document compiles the values themselves.
    void liveDeclarations(Declarations live, Map<String, String> properties, Declarations into);

    /// Checks an expanded element against this backend's output contract; `style` holds the
    /// element's compiled declarations. Does nothing by default.
    default void checkElement(ViewNode.Tag tag, Declarations style, Reporter reporter) {}

    /// Adds `element` to the document's `<head>`, or rejects it.
    void contributeHead(Element element, Location placedBy, Reporter reporter);

    /// Appends `section` to the end of the document's body, or rejects it.
    void atDocumentEnd(
        DocumentKey<?> owner,
        Function<DocumentContext, ? extends IntoElement> section,
        Location placedBy,
        Reporter reporter);

    /// Adds `behavior` to the document once, or rejects it.
    void contributeScript(PageScript behavior, Location placedBy, Reporter reporter);
  }

  /// Backend-facing: where a [Backend] reports what it rejects or finds wrong, as a [Diagnostic] of
  /// the expansion.
  @FunctionalInterface
  public interface Reporter {

    /// Reports `rule` at `location`.
    void report(Location location, String rule, String message, String fix);

    /// Reports `rule` at `location` and returns the [ViewNode.Empty] a rejected placement leaves.
    default ViewNode reject(Location location, String rule, String message, String fix) {
      report(location, rule, message, fix);
      return ViewNode.Empty.INSTANCE;
    }
  }

  private final Backend backend;
  private final ViewState state = new ViewState();
  private final Reporter reporter = state::report;
  private final DocumentRegistry document = new DocumentRegistry();
  private final ViewContext root;

  /// An expansion of page-level content for `backend`, registering styles in `styles`.
  public ViewExpansion(Backend backend, StyleSheet styles) {
    this(backend, styles, ViewScope.ROOT);
  }

  ViewExpansion(Backend backend, StyleSheet styles, ViewScope scope) {
    this.backend = backend;
    this.root = new ViewContext(scope, styles, this, List.of(), null, null);
  }

  /// Expands `content` at the top level.
  public ViewNode expand(IntoElement content) {
    return expand(content.intoElement(), root);
  }

  /// Expands what `content` returns in the component context of `placedBy`, at the top level of the
  /// id scope; a failure names `phase` and the component.
  public ViewNode expand(
      Location placedBy, String phase, Function<DocumentContext, ? extends IntoElement> content) {
    return expand(root.withPlacement(placedBy), phase, content);
  }

  private ViewNode expand(
      ViewContext context, String phase, Function<DocumentContext, ? extends IntoElement> content) {
    return expand(callback(context, phase, () -> content.apply(context).intoElement()), context);
  }

  private ViewNode expand(Element element, ViewContext context) {
    return switch (element) {
      case EmptyElement _ -> ViewNode.Empty.INSTANCE;
      case EffectElement(var effect) ->
          backend.effect(new ViewSnapshot.Effect(effect, context.location()), reporter);
      case TextElement(var content) -> new ViewSnapshot.Text(content);
      case RawHtmlElement(var html) -> backend.raw(html, context.location(), reporter);
      case FragmentElement fragment ->
          new ViewNode.Fragment(
              null, context.location(), children(fragment.childrenList(), context));
      case KeyedElement keyed -> keyed(keyed, context);
      case DeferredElement deferred ->
          backend.defer(
              () -> expand(deferred.child(), context),
              deferred.priority(),
              context.location(),
              reporter);
      case LateElement(var content) ->
          backend.late(() -> expand(context, "Late", content), context.location(), reporter);
      case ComponentElement component -> {
        var child = context.withComponent(component.name(), component.source());
        yield boundary(
            null, component.type(), child, expand(child, "Expand", component.component()::expand));
      }
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
        var scopeKey =
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
        var inner = expand(child, "Expand", scope::expand);
        yield new ViewNode.ScopeBoundary(
            null,
            scope.scopeName(),
            child.location(),
            List.of(scoped(inner, scope.scopeName(), scopeKey, scopeId, child)));
      }
      case TagElement tag -> tag(tag, null, context);
    };
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

  /// The key as hexadecimal UTF-16 units, so distinct keys always give distinct scopes.
  private static String encodeKey(String key) {
    var encoded = new StringBuilder(key.length() * 4);
    for (var index = 0; index < key.length(); index++) {
      var hex = Integer.toHexString(key.charAt(index));
      encoded.append("0000", 0, 4 - hex.length()).append(hex);
    }
    return encoded.toString();
  }

  /// Expands keyed content in the context [#keyedContext] made for it. A generated enclosing id
  /// scope reports `generated-scope-id` once for this key, then expansion continues. A component or
  /// fragment keeps its own boundary and takes the key on it; anything else is wrapped so the key
  /// still names one sibling.
  private ViewNode keyed(KeyedElement keyed, ViewContext context) {
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
    if (keyed.content() instanceof ComponentElement component) {
      return boundary(
          keyed.key(),
          component.type(),
          child,
          expand(child, "Expand", component.component()::expand));
    }
    var content = expand(keyed.content(), child);
    return switch (content) {
      case ViewNode.Boundary component when component.key() == null ->
          new ViewNode.Boundary(
              keyed.key(), component.definition(), component.location(), component.children());
      case ViewNode.ScopeBoundary scope when scope.key() == null ->
          new ViewNode.ScopeBoundary(
              keyed.key(), scope.definition(), scope.location(), scope.children());
      case ViewNode.Fragment fragment when fragment.key() == null ->
          new ViewNode.Fragment(keyed.key(), fragment.location(), fragment.children());
      default -> boundary(keyed.key(), keyed.content().getClass(), child, content);
    };
  }

  /// The context of keyed content and a component path entry naming the key. A keyed component
  /// takes the key on its own entry instead of gaining another.
  ///
  /// Its slug depends only on the enclosing id scope, the [ViewContext#keyOwner()] and the key,
  /// never on a sibling index. A generated id anywhere in that enclosing scope is reported as
  /// `generated-scope-id`, because that counter does change when siblings move: `key-<hex key>`
  /// without an owner and `<owner>-key-<hex key>` with one. The key's hexadecimal UTF-16 units
  /// contain no `-`, so the slug splits back at its last `-key-` and distinct keys or owner slugs
  /// never share a scope.
  private static ViewContext keyedContext(KeyedElement keyed, ViewContext context) {
    var owner = context.keyOwner();
    var slug = (owner == null ? "" : owner + "-") + "key-" + encodeKey(keyed.key());
    var scoped = context.withKeyedScope(slug);
    var label = "[key=" + keyed.key() + "]";
    if (keyed.content() instanceof ComponentElement component) {
      return scoped.withComponent(component.name() + label, component.source());
    }
    return scoped.withComponent(label, null);
  }

  private static ViewNode.Boundary boundary(
      @Nullable String key, Object definition, ViewContext context, ViewNode content) {
    return new ViewNode.Boundary(key, definition, context.location(), List.of(content));
  }

  /// Puts `data-scope` on the one element host `node` resolves to. Component boundaries and keyed
  /// wrappers are transparent. Another scope keeps its own host. Anything else reports `scope-root`
  /// and is left unchanged: expansion does not invent a wrapper. A `generated-scope-id` reported
  /// for a keyed host does not make that host invalid.
  private ViewNode scoped(
      ViewNode node, String scopeName, String type, String scopeId, ViewContext context) {
    return switch (node) {
      case ViewNode.Boundary boundary when boundary.children().size() == 1 ->
          new ViewNode.Boundary(
              boundary.key(),
              boundary.definition(),
              boundary.location(),
              List.of(scoped(boundary.children().getFirst(), scopeName, type, scopeId, context)));
      case ViewNode.ScopeBoundary boundary -> {
        state.report(
            context.location(),
            "scope-root",
            "Scope '"
                + scopeName
                + "' returns another scope as its root. That scope keeps its own data-scope",
            "Return one element, such as a div, ul, or tr, and place the nested scope inside it.");
        yield boundary;
      }
      case ViewNode.Tag tag -> {
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
  private static String rootKind(ViewNode node) {
    return switch (node) {
      case ViewNode.Fragment _ -> "a fragment";
      case ViewSnapshot.Text _ -> "text";
      case ViewSnapshot.Raw _ -> "raw HTML";
      case ViewNode.Empty _ -> "nothing";
      case ViewNode.Slot _ -> "late content";
      default -> "content that is not an element";
    };
  }

  /// How to give `node` an element host.
  private static String rootFix(ViewNode node) {
    return switch (node) {
      case ViewNode.Fragment _ ->
          "Return one element host. Put several children in an element you choose, such as ul or"
              + " div.";
      case ViewSnapshot.Text _ -> "Return one element host and place the text inside it.";
      case ViewSnapshot.Raw _ -> "Return one element host instead of raw HTML.";
      case ViewNode.Empty _ ->
          "Return one element host. Deferred content is moved out of the scope and cannot be its"
              + " root.";
      case ViewNode.Slot _ -> "Return one element host and place the late content inside it.";
      default -> "Return one element host, such as a div, ul, or tr.";
    };
  }

  /// Runs an authoring callback; a failure keeps its type and gains the component path as a
  /// suppressed diagnostic from the backend.
  private <T> T callback(ViewContext context, String phase, Supplier<T> work) {
    try {
      return work.get();
    } catch (RuntimeException failure) {
      failure.addSuppressed(
          backend.failureContext(
              new Diagnostic(
                  "callback-failure",
                  phase + " callback failed: " + failure.getMessage(),
                  "Correct the callback before expanding again.",
                  context.location())));
      throw failure;
    }
  }

  /// Lays out `elements` in order, dropping the ones that emit nothing.
  private List<ViewNode> children(List<Element> elements, ViewContext context) {
    var result = new ArrayList<ViewNode>(elements.size());
    for (var element : elements) {
      var node = expand(element, context);
      if (node != ViewNode.Empty.INSTANCE) {
        result.add(node);
      }
    }
    return result;
  }

  /// Expands `element`. `forcedId` (already registered) is used for companions, not bound.
  private ViewNode.Tag tag(TagElement element, @Nullable String forcedId, ViewContext context) {
    var attrs = new LinkedHashMap<>(element.attributes());
    var source = element.source() != null ? element.source() : context.origin();
    var location = new Location(context.components(), element.tag().tagName(), null, source);
    var domId = forcedId;
    var refs = element.boundRefs();
    if (domId == null && !refs.isEmpty()) {
      domId = state.bind(refs, context.idScope(), location);
    }
    if (domId != null) {
      attrs.put("id", domId);
      location = location.withDomId(domId);
    }
    validateControls(element, attrs, location);

    var style = identityStyles(element, domId, location);
    Map<String, String> live = Map.of();
    var liveStyle = element.liveStyle();
    if (liveStyle != null) {
      var properties = new LinkedHashMap<String, String>();
      // identityStyles returns the builder's own declarations or a fresh copy.
      if (style == element.style()) {
        style = new Declarations().mergeFrom(style);
      }
      backend.liveDeclarations(liveStyle, properties, style);
      live = properties;
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
      attrs.put("class", ViewValidation.isNonBlank(existing) ? existing + " " + joined : joined);
    }

    ViewNode.Tag companion = null;
    var tooltip = element.interestTooltip();
    if (tooltip != null) {
      var tooltipId = domId + "--tooltip";
      attrs.put("interestfor", tooltipId);
      state.registerId(tooltipId, location);
      companion = tag(tooltip, tooltipId, context);
    }

    var navigation = element.navigationHeading();
    if (navigation != null) {
      context.collectHeading(navigation, location);
    }

    // The builder keeps its own maps live, so the node takes copies.
    var references = element.idReferences();
    var events = element.events();
    var owner = keyOwner(refs);
    var node =
        new ViewNode.Tag(
            new TagCore(
                element.tag(),
                location,
                events.isEmpty() ? Map.of() : new LinkedHashMap<>(events),
                element.controlProperties(),
                live),
            attrs,
            references.isEmpty() ? Map.of() : new LinkedHashMap<>(references),
            children(element.childrenList(), owner == null ? context : context.withKeyOwner(owner)),
            companion);
    if (companion != null) {
      // Interest invokers need the tooltip fallback where the browser has no native one.
      backend.contributeScript(TooltipScript.INSTANCE, location, reporter);
    }
    backend.checkElement(node, style, reporter);
    ViewValidation.checkElement(node, style, reporter);
    return node;
  }

  /// Controlled properties need a native host that has them.
  private void validateControls(
      TagElement element, Map<String, String> attributes, Location location) {
    var properties = element.controlProperties();
    if (!properties.isControlled()) {
      return;
    }
    var tag = element.tag();
    var type = attributes.getOrDefault("type", "text");
    var multiple = attributes.containsKey("multiple");
    if (properties.value() != null
        && !(tag == HtmlTag.TEXTAREA
            || tag == HtmlTag.SELECT && !multiple
            || tag == HtmlTag.INPUT && !"file".equalsIgnoreCase(type))) {
      state.report(
          location,
          "control-value",
          "A controlled value requires an input other than file, a textarea or a single select",
          "Use selectedValues for a multiple select and do not bind file input values.");
    }
    if (properties.checked() != null
        && !(tag == HtmlTag.INPUT
            && ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type)))) {
      state.report(
          location,
          "control-checked",
          "A controlled checked state requires a checkbox or radio input",
          "Use checked only on checkable inputs.");
    }
    if (properties.selectedValues() != null && !(tag == HtmlTag.SELECT && multiple)) {
      state.report(
          location,
          "control-selection",
          "Controlled selected values require a multiple select",
          "Use value for a single select, or enable multiple selection.");
    }
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

  /// Pairs `anchor-name` on the anchor with `position-anchor` on the elements anchored to it.
  private static Anchor anchorIdent(String domId) {
    return Anchor.named("anchor-" + domId);
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

  /// Resolves references, late slots and tooltip companions of `node` into linked nodes. Call it
  /// once every element of the expansion is expanded, so every ref is bound.
  public List<ViewSnapshot.Node> link(ViewNode node) {
    return new ReferenceLinker(state).link(node);
  }

  /// The DOM id bound to `ref`, or `null` when it is ambiguous or unbound; an unbound ref is
  /// reported as `unbound-reference`, naming `usage`.
  public @Nullable String boundId(ElementRef ref, String usage, Location location) {
    return state.boundId(ref, usage, location);
  }

  /// The per-page values and headings registered so far.
  public DocumentRegistry document() {
    return document;
  }

  Backend backend() {
    return backend;
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
