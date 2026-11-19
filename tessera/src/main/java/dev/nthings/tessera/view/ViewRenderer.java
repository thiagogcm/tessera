package dev.nthings.tessera.view;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import dev.nthings.tessera.internal.view.ExpansionContext;
import dev.nthings.tessera.internal.view.ViewExpansion;
import dev.nthings.tessera.internal.view.ViewNode;
import dev.nthings.tessera.internal.view.ViewScope;
import dev.nthings.tessera.internal.view.ViewValidation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// Renders one turn of a live root. The caller owns the type of its bindings and effect payloads.
public final class ViewRenderer {

  private ViewRenderer() {}

  @FunctionalInterface
  public interface HostValidator<M> {
    void validate(
        M payload,
        TagName tag,
        Map<String, String> attributes,
        Location location,
        DiagnosticReporter reporter);
  }

  /// Renders into a linked snapshot with embedded CSS. Generated and scoped DOM ids are prefixed
  /// with `scope`, so roots on one page keep distinct ids. Authoring errors are reported together
  /// in a [ViewException].
  public static <M> ViewSnapshot<M> render(
      Function<ViewContext, LiveNode<M>> content,
      String scope,
      UnaryOperator<ViewContext> liveContext,
      HostValidator<M> validator) {
    var styles = new StyleSheet();
    var expansion = new LiveExpansion<>(styles, scope, liveContext, validator);
    var nodes = expansion.link(expansion.expandRoot(content));
    ViewValidation.check(
        nodes, List.of(), expansion.ids(), expansion.ambiguousIds(), expansion.reporter());
    if (!expansion.diagnostics().isEmpty()) {
      throw new ViewException(expansion.diagnostics());
    }
    var rules =
        styles.embeddedRules().stream()
            .map(r -> new EmbeddedRule(r.key(), r.layer(), r.prefix(), r.body(), r.suffix()))
            .toList();
    return new ViewSnapshot<>(
        ViewSnapshot.project(nodes), rules, expansion.references(), expansion.ids());
  }

  private static final class LiveExpansion<M> extends ViewExpansion<M> {
    private final UnaryOperator<ViewContext> liveContext;
    private final HostValidator<M> validator;

    LiveExpansion(
        StyleSheet styles,
        String scope,
        UnaryOperator<ViewContext> liveContext,
        HostValidator<M> validator) {
      super(
          styles,
          scope.isEmpty()
              ? ViewScope.ROOT
              : ViewScope.ROOT.extend(Fnv1a64.sanitizeLocalId(scope), false));
      this.liveContext = liveContext;
      this.validator = validator;
    }

    ViewNode<M> expandRoot(Function<ViewContext, LiveNode<M>> content) {
      var root = root();
      return expandLive(
          callback(root, "Expand", () -> content.apply(liveContext.apply(root))), root);
    }

    private ViewNode<M> expandLive(LiveNode<M> content, ExpansionContext context) {
      return switch (content) {
        case LiveNode.Portable<M>(var element) -> expandPortable(element, context);
        case LiveNode.Host<M> host -> liveHost(host, context);
        case LiveNode.Fragment<M>(var children) ->
            new ViewNode.Fragment<>(
                null, context.location(), children(children, context, this::expandLive));
        case LiveNode.Keyed<M>(var key, var child) ->
            keyed(
                key,
                definition(child),
                placement(child),
                context,
                inner -> expandLive(child, inner));
        case LiveNode.ComponentPlacement<M> component -> component(placement(component), context);
        case LiveNode.ScopePlacement<M>(var scope, var source, var expand) ->
            scope(
                scope,
                source,
                context,
                child ->
                    liveHost(
                        callback(child, "Expand", () -> expand.apply(liveContext.apply(child))),
                        child));
        case LiveNode.EffectPlacement<M>(var payload) -> new ViewNode.Special<>(payload, false);
      };
    }

    private ViewNode.Tag<M> liveHost(LiveNode.Host<M> host, ExpansionContext context) {
      return tag(
          host.host(),
          null,
          child -> children(host.children(), child, this::expandLive),
          host.payload(),
          context);
    }

    private static Class<?> definition(LiveNode<?> child) {
      return switch (child) {
        case LiveNode.Portable<?>(var element) -> element.getClass();
        case LiveNode.Host<?> _ -> TagElement.class;
        default -> child.getClass();
      };
    }

    private @Nullable Placement<M> placement(LiveNode<M> child) {
      return switch (child) {
        case LiveNode.Portable<M>(var element) -> portablePlacement(element);
        case LiveNode.ComponentPlacement<M> component -> placement(component);
        default -> null;
      };
    }

    private Placement<M> placement(LiveNode.ComponentPlacement<M> component) {
      return new Placement<>(
          component.type(),
          component.source(),
          child ->
              expandLive(
                  callback(
                      child, "Expand", () -> component.expand().apply(liveContext.apply(child))),
                  child));
    }

    @Override
    protected RuntimeException failureContext(Diagnostic diagnostic) {
      return new ViewException(List.of(diagnostic));
    }

    @Override
    protected Map<String, String> liveDeclarations(Declarations live, Declarations into) {
      var values = new LinkedHashMap<String, String>();
      styles.registerLive(live, values, into);
      return values;
    }

    @Override
    protected void validateHost(
        @Nullable M payload, TagName tag, Map<String, String> attributes, Location location) {
      if (payload != null) {
        validator.validate(payload, tag, attributes, location, reporter()::report);
      }
    }
  }
}
