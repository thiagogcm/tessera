/// Ready-made UI components built on the core authoring API. Each component comes from a static
/// factory, such as `Button.of`, `Card.create`, `Overlays.modal`, `Tabs.contentTabs`,
/// `Toc.section`, or `Forms.field`, and is added to its parent with `.child(...)`. A component
/// that takes an `id` names its instance in the enclosing view scope, and the DOM ids it binds
/// are prefixed with that scope (`<id>--<local>`), so repeated instances need distinct ids.
/// Document sections (`Toc.section`, `ContentSection`) are the exception: their `id` is a
/// page-level DOM id, so other pages can link to `#<id>`.
/// Arguments a component can check throw IllegalArgumentException when the method is called;
/// wiring mistakes are reported as compile diagnostics.
@NullMarked
package dev.nthings.tessera.components;

import org.jspecify.annotations.NullMarked;
