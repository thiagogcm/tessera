package dev.nthings.tessera.core;

/// ES module contributed to a document as browser behavior.
///
/// A behavior is identified by [#id] and emitted once per compiled page or site. When the page
/// publishes external assets, the module becomes a content-hashed file; otherwise it is inlined as
/// a module script.
public record ClientBehavior(String id, String module) {}
