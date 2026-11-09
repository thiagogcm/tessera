package dev.nthings.tessera.components;

/// A link for navigation components such as [Breadcrumbs], [NavTabs] and [DocPager]: the target
/// `href`, written as a browser URL, and the visible `label` that also names the link.
public record Link(String href, String label) {}
