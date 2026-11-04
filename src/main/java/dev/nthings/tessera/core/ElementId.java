package dev.nthings.tessera.core;

/// Sealed algebraic identifier for stateful elements and scoped views.
public sealed interface ElementId permits ElementId.Named, ElementId.Indexed, ElementId.Composite {

  String slug();

  long foldHash(long seed);

  record Named(String name) implements ElementId {

    public Named {
      Fnv1a64.sanitizeIdentifier(name);
    }

    @Override
    public String slug() {
      return Fnv1a64.sanitizeIdentifier(name);
    }

    @Override
    public long foldHash(long seed) {
      long h = Fnv1a64.updateByte(seed, 1);
      return Fnv1a64.updateChars(h, name);
    }
  }

  record Indexed(String prefix, long index) implements ElementId {

    public Indexed {
      Fnv1a64.sanitizeIdentifier(prefix);
    }

    @Override
    public String slug() {
      return Fnv1a64.sanitizeIdentifier(prefix) + "-" + index;
    }

    @Override
    public long foldHash(long seed) {
      long h = Fnv1a64.updateByte(seed, 2);
      h = Fnv1a64.updateChars(h, prefix);
      return Fnv1a64.updateLong(h, index);
    }
  }

  record Composite(ElementId namespace, ElementId local) implements ElementId {

    @Override
    public String slug() {
      return namespace.slug() + "-" + local.slug();
    }

    @Override
    public long foldHash(long seed) {
      long h = Fnv1a64.updateByte(seed, 3);
      h = namespace.foldHash(h);
      return local.foldHash(h);
    }
  }

  static ElementId of(String name) {
    return new Named(name);
  }

  static ElementId of(String prefix, long index) {
    return new Indexed(prefix, index);
  }

  static ElementId composite(ElementId namespace, ElementId local) {
    return new Composite(namespace, local);
  }
}
