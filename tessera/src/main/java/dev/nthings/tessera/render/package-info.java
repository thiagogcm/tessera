/// Compiles one [PageDocument] to HTML. [PageCompiler#compile(PageDocument)] returns a
/// [CompiledPage] with inline CSS; pass `Tokens` and an [OutputMode] to theme the page or to emit
/// a content-hashed external stylesheet and client behavior modules. Authoring errors are
/// collected and thrown as one [CompilationException] before any HTML is produced.
/// [PageCompiler#explain(PageDocument)] and [SourceTracking] are for debugging;
/// [PageCompiler#prepare] and [PageCompiler#paintPrepared] are the phase split that the site
/// compiler uses.
@NullMarked
@SourceBoundary
package dev.nthings.tessera.render;

import dev.nthings.tessera.core.SourceBoundary;
import org.jspecify.annotations.NullMarked;
