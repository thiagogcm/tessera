package dev.nthings.tessera.core;

/**
 * Globally unique, collision-free path identifier formed by folding a chain of {@link ElementId}
 * values along active {@link RenderView} boundaries.
 */
public record GlobalElementId(long pathHash, String domId) {

  public static final GlobalElementId ROOT = new GlobalElementId(Fnv1a64.OFFSET_BASIS, "");

  public GlobalElementId extend(ElementId childId) {
    long nextHash = childId.foldHash(pathHash);
    String nextDomId = domId.isEmpty() ? childId.slug() : domId + "--" + childId.slug();
    return new GlobalElementId(nextHash, nextDomId);
  }

  public String toAnchorCustomIdent() {
    return "--anchor-" + (domId.isEmpty() ? Fnv1a64.toBase36Hash(pathHash) : domId);
  }

  public String toCompactHash() {
    return Fnv1a64.toBase36Hash(pathHash);
  }
}
