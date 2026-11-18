package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.PortableNode;

/// Any node of a portable element tree: tags, text, fragments, keyed content, and component or
/// scope placements. Authors build elements through [Elements] and get back [TagElement],
/// [FragmentElement] or plain `Element`; the other variants are backend-facing. The hierarchy is
/// closed and holds nothing that only one backend can execute: raw HTML, deferred and late content
/// belong to the document family, and events and effects to the browser module's live content.
public sealed interface Element extends IntoElement
    permits TagElement, FragmentElement, PortableNode {

  @Override
  default Element intoElement() {
    return this;
  }
}
