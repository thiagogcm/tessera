package dev.nthings.tessera.view;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ScopeDefinition;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.internal.authoring.PortableNode;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Live placements passed from the browser module to shared expansion. The payload is owned by that
/// module. Callbacks take the portable context and add any browser operations themselves.
public sealed interface LiveNode<M> {

  record Portable<M>(Element element) implements LiveNode<M> {
    @Override
    public LiveNode<M> key(String key) {
      return new Portable<>(element.key(key));
    }
  }

  record Host<M>(TagElement host, List<LiveNode<M>> children, M payload) implements LiveNode<M> {
    public Host {
      children = List.copyOf(children);
    }
  }

  record Fragment<M>(List<LiveNode<M>> children) implements LiveNode<M> {
    public Fragment {
      children = List.copyOf(children);
    }
  }

  record Keyed<M>(String key, LiveNode<M> content) implements LiveNode<M> {
    public Keyed {
      PortableNode.requireKey(key);
    }

    @Override
    public LiveNode<M> key(String key) {
      return new Keyed<>(key, content);
    }
  }

  record ComponentPlacement<M>(
      Class<?> type, @Nullable SourceLocation source, Function<ViewContext, LiveNode<M>> expand)
      implements LiveNode<M> {}

  record ScopePlacement<M>(
      ScopeDefinition scope, @Nullable SourceLocation source, Function<ViewContext, Host<M>> expand)
      implements LiveNode<M> {}

  record EffectPlacement<M>(M payload) implements LiveNode<M> {}

  default LiveNode<M> key(String key) {
    return new Keyed<>(key, this);
  }
}
