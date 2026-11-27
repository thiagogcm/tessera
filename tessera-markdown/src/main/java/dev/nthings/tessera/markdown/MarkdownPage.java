package dev.nthings.tessera.markdown;

import dev.nthings.tessera.core.DocumentContent;

/// A parsed Markdown page: its front matter and its body as document content. Place the body in a
/// [dev.nthings.tessera.compiler.PageDocument], usually inside a layout such as
/// [dev.nthings.tessera.document.components.DocLayout].
///
/// @param frontMatter the page's front matter, or [FrontMatter#NONE]
/// @param body the converted Markdown
public record MarkdownPage(FrontMatter frontMatter, DocumentContent body) {}
