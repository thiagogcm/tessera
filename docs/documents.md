# Documents

Compose static HTML pages that need document-level capabilities: head contributions, deferred overlays, late callbacks, document outlines, footnotes, hero image preloads, and page scripts.

Document APIs live in `tessera-compiler` under `dev.nthings.tessera.document` and `dev.nthings.tessera.document.components`. Portable builders in `dev.nthings.tessera.core` never accept `DocumentContent`, keeping portable components reusable in [browser roots](browser-rendering.md).

## Mix portable and document content

A `PageDocument` body accepts either portable `IntoElement` or `DocumentContent`. To mix portable markup with document-only children, wrap a portable `TagElement` with `DocumentElement.of(...)`:

```java
var engine = Footnote.of("latest Chromium and Firefox.");
var body =
    DocumentElement.of(Elements.main().child(Elements.h1().text("Guide")))
        .child(DocumentElement.of(Elements.p().text("Supported engines")).child(engine.reference()))
        .child(Documents.deferred(Modal.of("Help").scopeId("help"), 10));
```

| Need | Document API |
| --- | --- |
| Wrap a portable element to hold document children | `DocumentElement.of(tag).child(...)` |
| Group document siblings without a wrapper | `Documents.fragment(...)` |
| Convert portable content where a `DocumentContent` value is required | `DocumentContent.of(content)` (builders accept `IntoElement` directly) |
| Move content to the end of `<body>` while keeping its lexical ID scope | `Documents.deferred(content, priority)` |
| Resolve content after body expansion finishes | `Documents.late(context -> ...)` |
| Emit pre-sanitized HTML verbatim | `Documents.rawHtml(trustedHtml)` |
| Emit a `<script>` element with trusted source | `Documents.script(type, source)` |
| Mount a compiled WebAssembly bundle with static fallback | `Documents.wasmMount(id, view, bundle, fallback)` |

### Placement order and trust boundaries

- **Deferred content**: `Documents.deferred(content, priority)` expands in its lexical identity scope and emits at the end of `<body>`. Lower priority numbers come first (default `0`), and equal priorities keep placement order. Use it for modals, drawers, and search palettes that should sit outside constrained ancestors.
- **Late callbacks**: `Documents.late(context -> ...)` resolves first in, first out after body expansion completes, before document-end sections and head contributions. Use it when content depends on data collected from the rest of the body, such as headings.
- **Raw HTML and inline scripts**: `Documents.rawHtml(...)` bypasses escaping and compiler validation of IDs, links, and accessibility. Pass only trusted, pre-sanitized HTML; use `text(...)` for user-controlled input. `Documents.script(type, source)` escapes `</script` sequences in `source` but does not validate script code.
- **Document sections**: A portable `Section` accepts only portable children. Wrap it with `DocumentSection.of(section, ...)` when its body includes footnotes or other document content.

## Implement document components and scopes

Implement `DocumentComponent` or `DocumentScope` when a reusable component needs document assembly. Both receive a `DocumentContext`, which extends `ViewContext` with document-level registrations:

| Operation | Behavior |
| --- | --- |
| `state(ViewKey<T>)` | Page-local expansion state, created on first use |
| `contributeHead(content)` | Appends elements to `<head>` in registration order; use `ViewKey` state to deduplicate |
| `atDocumentEnd(owner, callback)` | Registers an end-of-body section; the first registration per owner wins |
| `contributeScript(script)` | Registers a [`PageScript`](#contribute-a-page-script); each script ID emits once per page |
| `address()` | Returns the page's [`PageAddress`](sites.md#know-the-current-page-in-a-shared-layout) in a compiled site, or empty for a standalone page |
| `headings()` | Returns a snapshot of collected outline headings (complete inside `Documents.late`) |
| `report(rule, message, fix)` | Records a located diagnostic that fails compilation |

A `DocumentScope` returns a single `DocumentElement` host and follows the same naming and identity rules as portable [`Scope`](components.md#scope-hosts-names-and-state).

## Outline headings and build a table of contents

`Heading` (portable) renders native `h1` through `h6` elements. A heading joins the document outline only when you call `outline(target)` and bind `target` on the page. The table of contents and site search index read the plain label while preserving rich inline markup in the HTML:

```java
var install = ElementRef.page("install");
Heading.of(2, "Install", Elements.code().text("mvn"), Elements.text(" verify"))
    .id(install)
    .outline(install);
```

- `Heading.of(level, text)` uses `text` for both visible markup and outline label. Levels outside `1` through `6` throw `IllegalArgumentException`.
- `.permalink()` ends the heading with a link to its own anchor, named "Link to <label>" and shown while the heading is hovered or the link has keyboard focus. A permalink on a heading without `id(...)` reports `heading-permalink`. Copying the URL to the clipboard is left to a page script.
- `Section.of(id, title)` binds a page-level ID on `<section>` and outlines that ID automatically for heading levels `2` through `4`. Use `Heading` directly for levels `1`, `5`, `6`, or rich heading phrasing.
- Ordinary HTML headings, dialog titles, and the footnotes heading are not outlined. If two headings outline the same target, the first is kept.

`TableOfContents.of(title)` uses a late callback to render the page's collected outline in document order, so you can place it before the sections. Heading levels 3 through 6 indent progressively. Call `.levels(min, max)` (within `1` through `6`) to filter entries to a heading level range, use `TableOfContents.headings(list)` for a fixed list, or use `DocLayout.of(sections...)` to pair sections with a sticky table of contents.

## Place footnotes

A `Footnote` defines one note; `reference()` places an inline reference as `DocumentContent`. The first reference on a page assigns the note number. Every reference gets its own marker and backlink, while the note body expands once inside `Footnotes`.

```java
var engine = Footnote.of("latest Chromium and Firefox.").id("engine");
DocumentElement.of(Elements.p().text("Tessera targets current browsers"))
    .child(engine.reference());
Footnotes.of().title("Notes");
```

- **Previews**: Plain-text notes automatically generate a text preview. Notes with markup bodies have no preview unless you call `preview(String)` or `preview(IntoElement)`. Because an element preview is inserted at each reference, keep IDs out of reusable preview elements and prefer a string preview when referencing a note multiple times.
- **Placement and IDs**: `Footnotes.of()` may appear before or after references; if omitted, the list is appended automatically at the end of the document. Placing a second `Footnotes` list repeats entries and reports `duplicate-id`. Call `id(...)` before the first reference to name the endnote `fn-<id>` in the footnotes scope.

## Preload a hero image

Wrap a portable [`Image`](sites.md#register-assets-and-images) with `HeroImage.of(image)` from `dev.nthings.tessera.document.components` to mark the primary above-the-fold image. It configures eager loading, `fetchpriority="high"`, and a `<head>` preload link. Placing more than one hero image on the same page reports `duplicate-hero-image`.

## Add link preview tags

Place `SocialMeta` from `dev.nthings.tessera.document.components` in a page or a shared layout to add Open Graph and Twitter card tags to `<head>`:

```java
SocialMeta.of(title).description(description).image("/og.png").siteName("nthings.dev").article();
```

It sets `og:title`, `og:type`, `og:url` (from the page's absolute URL), and, when given, the description, site name, and image, with the matching `twitter:` tags. A site-relative image resolves against the page's absolute URL, so it needs a site base URL; without one it reports `social-meta-url`.

## Contribute a page script

A `PageScript` represents an ES module with a stable ID. Contribute application scripts through `DocumentContext`:

```java
DocumentComponent widget =
    context -> {
      context.contributeScript(new PageScript("my-widget", moduleSource));
      return DocumentContent.of(Elements.custom("my-widget"));
    };
```

Each page emits a script ID at most once, keeping the first registration. Keep the module source identical across all registrations of the same ID. Built-in portable widgets such as `ContentTabs` and `SearchPalette` declare their bundled modules automatically during document compilation; see [Browser support](browser-support.md#component-scripts).

How scripts are packaged (embedded or external hashed files) is governed by the [output mode](sites.md#choose-an-output-mode).
