package dev.nthings.tessera.view;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.DocumentNode;
import dev.nthings.tessera.internal.css.StyleSheet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Lowers document placements into shared expansion. The static compiler controls when document
/// contributions are evaluated; the only backend-specific leaf payload is trusted HTML.
public final class DocumentExpansion extends ViewExpansion<String> {

  public interface Assembly {
    RuntimeException failureContext(Diagnostic diagnostic);

    ViewNode<String> defer(Supplier<ViewNode<String>> content, int priority);

    ViewNode<String> late(Supplier<ViewNode<String>> content);

    void contributeHead(DocumentNode content, Location placedBy);

    void atDocumentEnd(
        ViewKey<?> owner,
        Function<DocumentContext, ? extends DocumentContent> section,
        Location placedBy);

    void contributeScript(PageScript script);

    Map<String, String> mountAttributes(DocumentNode.Mount mount);
  }

  private final Assembly assembly;

  public DocumentExpansion(Assembly assembly, StyleSheet styles) {
    super(styles, ViewScope.ROOT);
    this.assembly = assembly;
  }

  public ViewNode<String> expandDocument(Location placedBy, DocumentNode content) {
    return expandDocument(content, root.withPlacement(placedBy));
  }

  public ViewNode<String> expandDocument(
      Location placedBy,
      String phase,
      Function<DocumentContext, ? extends DocumentContent> content) {
    return callback(root.withPlacement(placedBy), phase, content);
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
      case DocumentNode.Raw(var html) -> new ViewSnapshot.Special<>(html, !html.isBlank());
      case DocumentNode.Deferred deferred ->
          assembly.defer(
              () -> expandDocument(deferred.child(), context.withNamespace(TagName.Namespace.HTML)),
              deferred.priority());
      case DocumentNode.Late(var expand) -> assembly.late(() -> callback(context, "Late", expand));
      case DocumentNode.Host host -> documentHost(host, context);
      case DocumentNode.Mount mount -> {
        var host = Elements.div().id(mount.id());
        assembly.mountAttributes(mount).forEach(host::attr);
        yield documentHost(new DocumentNode.Host(host, List.of(mount.fallback())), context);
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
                              AuthoringAccess.documentHost(
                                  scope.expand(new DocumentView(child, assembly)))),
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
  RuntimeException failureContext(Diagnostic diagnostic) {
    return assembly.failureContext(diagnostic);
  }

  @Override
  Map<String, String> liveDeclarations(Declarations live, Declarations into) {
    into.mergeFrom(live);
    return Map.of();
  }

  @Override
  ViewNode.Tag<String> tag(
      TagElement host,
      @Nullable String forcedId,
      Function<ExpansionContext, List<ViewNode<String>>> extra,
      @Nullable String payload,
      ExpansionContext context) {
    var node = super.tag(host, forcedId, extra, payload, context);
    var behavior = AuthoringAccess.tag(host).behavior;
    if (behavior != null) {
      assembly.contributeScript(behavior.script());
    }
    if (node.companion() != null) {
      assembly.contributeScript(TooltipScript.INSTANCE);
    }
    return node;
  }
}
