package dev.nthings.tessera.core;

/// Backend-facing: an [Effect] placed in a tree. [Elements#effect(String, Effect)] places it as
/// the content of a [KeyedElement], whose key gives the effect its identity among its siblings.
public record EffectElement(Effect effect) implements Element {}
