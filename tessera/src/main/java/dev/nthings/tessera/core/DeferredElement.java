package dev.nthings.tessera.core;

/// Compiler-facing: element moved to the end of `<body>`, created by [Elements#deferred]. Lower
/// priorities are placed first, and equal priorities keep their placement order. The element keeps
/// the id scope and DOM ids it was placed with.
public record DeferredElement(Element child, int priority) implements Element {}
