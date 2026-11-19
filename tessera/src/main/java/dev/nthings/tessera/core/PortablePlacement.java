package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.PortableNode;

/// Closed wrapper around internal portable placements so `Element` permits only same-package types.
record PortablePlacement(PortableNode node) implements Element {
  static final Element EMPTY = new PortablePlacement(PortableNode.Empty.INSTANCE);

  static {
    AuthoringAccess.registerPortable(
        element -> ((PortablePlacement) element).node(), PortablePlacement::new);
  }

  @Override
  public Element key(String key) {
    if (node instanceof PortableNode.Keyed keyed) {
      return new PortablePlacement(new PortableNode.Keyed(key, keyed.content()));
    }
    return new PortablePlacement(new PortableNode.Keyed(key, this));
  }
}
