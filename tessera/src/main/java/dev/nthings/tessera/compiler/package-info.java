/// Compiles pages and sites to HTML, CSS and static files.
///
/// - One page: [Compiler#compile(PageDocument)] returns a [CompiledPage] with inline CSS. Pass
///   `Tokens` and [OutputMode#external(String)] to theme it and emit a content-hashed stylesheet
///   and page scripts as [Asset]s; publish every asset at its path.
/// - A site: describe routes and files with [SiteDefinition#builder()], compile them with
///   [Compiler#compile(SiteDefinition)], then write the [CompiledSite] with [SiteWriter] or serve
///   it locally with [SitePreviewServer]. Site compilation checks links, fragments and transition
///   keys across pages, shares one stylesheet, and adds `search-index.json` and, with a base URL,
///   `sitemap.xml`.
/// - Errors: authoring errors are collected and thrown as one [CompilationException] listing every
///   diagnostic before any HTML is produced.
/// - Debugging: [Compiler#explain(PageDocument)] returns the element tree as a [PageTree];
///   [SourceTracking] records the Java line that built each element.
@NullMarked
@SourceBoundary
package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.SourceBoundary;
import org.jspecify.annotations.NullMarked;
