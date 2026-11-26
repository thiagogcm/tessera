package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.BundledBehavior;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.internal.document.DocumentAccess;
import dev.nthings.tessera.internal.document.DocumentNode;
import dev.nthings.tessera.internal.view.ExpansionContext;
import dev.nthings.tessera.internal.view.LinkedNode;
import dev.nthings.tessera.internal.view.ViewExpansion;
import dev.nthings.tessera.internal.view.ViewNode;
import dev.nthings.tessera.internal.view.ViewScope;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Lowers document placements into shared expansion. The static compiler controls when document
/// contributions are evaluated; the only backend-specific leaf payload is trusted HTML.
final class DocumentExpansion extends ViewExpansion<String> {

  private static final Map<BundledBehavior, PageScript> BUNDLED_SCRIPTS = new ConcurrentHashMap<>();

  /// Restores the scheme `color-scheme.js` saved, before the body paints. A module script runs only
  /// after parsing, too late to avoid a flash of the system scheme.
  private static final String RESTORE_COLOR_SCHEME =
      "try{var s=localStorage.getItem(\"tessera-color-scheme\");"
          + "if(s===\"light\"||s===\"dark\")document.documentElement.dataset.colorScheme=s}"
          + "catch(e){}";

  private final DocumentPass assembly;
  private boolean colorSchemeRestored;

  DocumentExpansion(DocumentPass assembly, StyleSheet styles) {
    super(styles, ViewScope.ROOT);
    this.assembly = assembly;
  }

  ViewNode<String> expandDocument(Location placedBy, DocumentNode content) {
    return expandDocument(content, root().withPlacement(placedBy));
  }

  ViewNode<String> expandDocument(
      Location placedBy,
      String phase,
      Function<DocumentContext, ? extends DocumentContent> content) {
    return callback(root().withPlacement(placedBy), phase, content);
  }

  private ViewNode<String> callback(
      ExpansionContext context,
      String phase,
      Function<DocumentContext, ? extends DocumentContent> expand) {
    var content =
        callback(
            context,
            phase,
            () -> DocumentNode.capture(expand.apply(new DocumentView(context, assembly))));
    return expandDocument(content, context);
  }

  private ViewNode<String> expandDocument(DocumentNode content, ExpansionContext context) {
    return switch (content) {
      case DocumentNode.Portable(var element) -> expandPortable(element, context);
      case DocumentNode.Raw(var html) -> new LinkedNode.Special<>(html, !html.isBlank());
      case DocumentNode.Deferred deferred ->
          assembly.defer(
              () -> expandDocument(deferred.child(), context.withNamespace(TagName.Namespace.HTML)),
              deferred.priority());
      case DocumentNode.Late(var expand) -> assembly.late(() -> callback(context, "Late", expand));
      case DocumentNode.Host host -> documentHost(host, context);
      case DocumentNode.Mount mount -> {
        var host = Elements.div().id(ElementRef.page(mount.id()));
        assembly.mountAttributes(mount).forEach(host::attr);
        // The fallback takes the ids the live root will render, so its first commit can hand
        // focus and the reading position over by id.
        var fallbackScope = ViewScope.host(mount.id());
        yield tag(
            host,
            null,
            child -> List.of(expandDocument(mount.fallback(), child.withRootScope(fallbackScope))),
            null,
            context);
      }
      case DocumentNode.Fragment(var children) ->
          new ViewNode.Fragment<>(
              null, context.location(), children(children, context, this::expandDocument));
      case DocumentNode.Keyed(var key, var child) ->
          keyed(
              key,
              definition(child),
              placement(child),
              context,
              inner -> expandDocument(child, inner));
      case DocumentNode.ComponentPlacement placement -> component(placement(placement), context);
      case DocumentNode.ScopePlacement(var scope, var source) ->
          scope(
              scope,
              source,
              context,
              child ->
                  documentHost(
                      callback(
                          child,
                          "Expand",
                          () ->
                              DocumentAccess.host(scope.expand(new DocumentView(child, assembly)))),
                      child));
    };
  }

  private ViewNode.Tag<String> documentHost(DocumentNode.Host host, ExpansionContext context) {
    return tag(
        host.host(),
        null,
        child -> children(host.children(), child, this::expandDocument),
        null,
        context);
  }

  private static Class<?> definition(DocumentNode child) {
    return switch (child) {
      case DocumentNode.Portable(var element) -> element.getClass();
      case DocumentNode.Host _ -> TagElement.class;
      default -> child.getClass();
    };
  }

  private @Nullable Placement<String> placement(DocumentNode child) {
    return switch (child) {
      case DocumentNode.Portable(var element) -> portablePlacement(element);
      case DocumentNode.ComponentPlacement component -> placement(component);
      default -> null;
    };
  }

  private Placement<String> placement(DocumentNode.ComponentPlacement component) {
    return new Placement<>(
        component.type(),
        component.source(),
        child -> callback(child, "Expand", component.expand()));
  }

  @Override
  protected RuntimeException failureContext(Diagnostic diagnostic) {
    return assembly.failureContext(diagnostic);
  }

  @Override
  protected Map<String, String> liveDeclarations(Declarations live, Declarations into) {
    into.mergeFrom(live);
    return Map.of();
  }

  @Override
  protected ViewNode.Tag<String> tag(
      TagElement host,
      @Nullable String forcedId,
      Function<ExpansionContext, List<ViewNode<String>>> extra,
      @Nullable String payload,
      ExpansionContext context) {
    var node = super.tag(host, forcedId, extra, payload, context);
    var behavior = AuthoringAccess.tag(host).behavior;
    if (behavior != null) {
      assembly.contributeScript(bundledScript(behavior));
      if (behavior == BundledBehavior.COLOR_SCHEME && !colorSchemeRestored) {
        colorSchemeRestored = true;
        assembly.contributeHead(
            DocumentNode.capture(Documents.script("text/javascript", RESTORE_COLOR_SCHEME)),
            context.location());
      }
    }
    if (node.companion() != null) {
      assembly.contributeScript(TooltipScript.INSTANCE);
    }
    return node;
  }

  private static PageScript bundledScript(BundledBehavior behavior) {
    return BUNDLED_SCRIPTS.computeIfAbsent(
        behavior,
        b -> PageScript.fromResource(b.element().tagName(), DocumentExpansion.class, resource(b)));
  }

  private static String resource(BundledBehavior behavior) {
    return switch (behavior) {
      case TABS -> "tessera-tabs.js";
      case MENU -> "tessera-menu.js";
      case SEARCH -> "search-palette.js";
      case COLOR_SCHEME -> "color-scheme.js";
    };
  }
}
