/// Ready-made UI components built on the core authoring API.
///
/// Every component is a final class created with a static factory that takes its required
/// arguments, usually `X.of(...)` (`Button.of("Save")`, `Modal.of("Publish?")`,
/// `Section.of("install", "Installation")`); a few with genuinely different shapes have named
/// factories (`Grid.autoFit` and `Grid.columns`, `CodeBlock.of` and `CodeBlock.ofLines`). Options
/// are fluent setters, and a component is placed like any element with `.child(...)`.
///
/// Scoped components take an optional `id(String)` that names the instance in the enclosing id
/// scope; the DOM ids they bind are prefixed with it (`<id>--dialog`). Without one the compiler
/// assigns `<scopeName>-<n>`, numbered per scope, so give an instance an id when other code links
/// to its ids. [Section] ids are the exception: they are page-level DOM ids, so other pages can
/// link to `#<id>`. [Heading] is the native `h1` through `h6`; [Section] uses one and keeps the
/// page id on the `<section>`. Buttons bind their own element with `id(ElementRef)` instead.
///
/// Shared vocabulary: [dev.nthings.tessera.css.Tone] for semantic color, [Size] for size and [Link]
/// for navigation targets. A `label` is an accessible name, a `title` a visible heading, a
/// `description` muted secondary text, and `actions(...)` footer actions. [Forms] and [Formatting]
/// are plain element functions.
///
/// Arguments a component can check throw IllegalArgumentException when the method is called; wiring
/// mistakes found during compilation are reported as diagnostics.
@NullMarked
package dev.nthings.tessera.components;

import org.jspecify.annotations.NullMarked;
