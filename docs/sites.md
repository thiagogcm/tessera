# Sites

Define routes and assets, compile them together, then publish the resulting files. The [getting-started example](getting-started.md) provides a complete application. This guide covers the publishing contracts and navigation features.

## Define routes

`SiteDefinition` requires at least one `SiteRoute`. Each route pairs a logical path with a `PageDocument`:

```java
var site = SiteDefinition.builder()
    .route(new SiteRoute("/", new PageDocument("Home", Elements.main().child(Elements.h1().text("Home")))))
    .route(new SiteRoute("/guide", new PageDocument("Guide", Elements.main().child(Elements.h1().text("Guide")))))
    .build();

var compiled = Compiler.compile(site);
SiteWriter.writeToDirectory(compiled, Path.of("target/site"));
```

`PageDocument(lang, title, body)` sets the document language and title. Its two-argument constructor defaults to English. An optional route title changes navigation and search labels; it leaves the document's `<title>` unchanged. Route descriptions feed search metadata.

| Logical route | Public document URL | Generated file     |
| ------------- | ------------------- | ------------------ |
| `/`           | `/`                 | `index.html`       |
| `/guide`      | `/guide/`           | `guide/index.html` |
| `/about.html` | `/about.html`       | `about.html`       |

`route.documentUri()` returns the public URL. `site.route("/guide")` looks up a route as an `Optional`; lookup normalizes paths like route construction does.

The builder accepts a [theme](styling.md#set-a-theme), an output mode, registered assets, a sitemap base URL, and `LinkSpeculation`. Speculation defaults to `PRERENDER`. Pages with the browser bundle deployment policy publish these rules through a response header. Other pages include the rules in the head. Set `baseUrl("https://example.com")` to generate `sitemap.xml`; omit it to omit the sitemap.

## Choose an output mode

A single page uses inline output by default. A site defaults to `OutputMode.external("/assets")`, sharing one merged stylesheet across routes.

| Mode | Stylesheet | Page scripts | Publication contract |
| --- | --- | --- | --- |
| `OutputMode.inline()` | Embedded `<style>` | Embedded module scripts | A standalone page has no generated assets |
| `OutputMode.external("/assets")` | `/assets/styles-<hash>.css` | `/assets/scripts/<sanitized id>-<hash>.js` | Publish every returned asset |

For a standalone page with external resources:

```java
var page = Compiler.compile(document, Theme.defaults(), OutputMode.external("/assets"));
```

The asset base must be an absolute URL path. Relative paths, `//`, and `..` are rejected. Hashes derive from content, so changing a resource changes its URL.

| Result | Accessors |
| --- | --- |
| `CompiledPage` | `html()`, `css()`, `assets()`, and `responseHeaders()` |
| `CompiledSite` | `pages()` in route order, `css()`, and `files()` containing registered and generated assets |

Site files include `search-index.json`, optional `sitemap.xml`, external stylesheets and modules, and any [published browser bundles](browser-rendering.md#publish-a-browser-bundle). `CompiledSite.pageHeaders()` maps routes to deployment response headers. Compilation returns data; filesystem writes belong to `SiteWriter`.

## Register assets and images

Use absolute logical paths for files. Site validation recognizes registered files as link and media destinations:

```java
builder.asset(Asset.file("/assets/logo.svg", Path.of("assets/logo.svg")));
builder.asset(Asset.text("/assets/robots.txt", "User-agent: *\nAllow: /\n"));
```

`Asset.file(...)` reads the input file explicitly. Byte-backed assets travel through compilation and publishing together.

Use `Image.of(src, alt).dimensions(width, height)` to reserve layout space and provide an accessible description. Images load lazily and decode asynchronously. `HeroImage.of(image)` (from `dev.nthings.tessera.document.components`) places the main image with eager loading, `fetchpriority="high"`, and a head preload. A second hero on one page reports `duplicate-hero-image`.

Configure the underlying image through `element(...)` for styles, an aspect ratio, ARIA, references, or transition keys:

```java
Image.of("/assets/divider.svg", "")
    .dimensions(640, 8)
    .element(img -> img.ariaHidden(true).inlineSize(CssLength.pct(100)));
```

The compiler requires alt text and dimensions or an aspect ratio. Decorative images use empty alt text and either `role(Role.PRESENTATION)` or `ariaHidden(true)`.

## Write links as browser URLs

Site compilation resolves local `href`, `src`, and `poster` values from the source route's public document URL. Queries remain in output but do not affect destination lookup. Percent-encoded paths and fragments are decoded for comparison; `+` in a fragment stays a plus sign.

| Source route | Link | Validated destination |
| --- | --- | --- |
| `/guides/start` | `../install?mode=full#requirements` | `/guides/install`, ID `requirements` |
| `/guides/start.html` | `?mode=full#requirements` | Same HTML file, ID `requirements` |
| `/` | `/guides/caf%C3%A9/#caf%C3%A9%2Bintro` | `/guides/café`, ID `café+intro` |
| `/guide` | `../assets/manual%20v1.pdf?download=1` | Asset `/assets/manual v1.pdf` |

Define route and asset paths in their logical form, such as `/guides/café`. Escape links as URLs. Explicit `index.html` links are accepted for directory pages. Absolute and protocol-relative URLs are external and are outside local target validation.

Duplicate routes, conflicting file paths, missing local targets, and malformed URLs produce [compilation diagnostics](diagnostics.md).

## Add headings and navigation

`Section.of(id, title)` binds a page-level fragment on the `<section>` and outlines that id. Its heading levels are 2 through 4. `Heading.of(level, label, content...)` is an `h1` through `h6`. Call `outline(target)` and bind `target` to include it. The outline uses the plain label; rich phrasing stays in the HTML. Ordinary HTML headings, dialog titles, and the footnotes heading are not indexed.

`TableOfContents.of(title)` reads the collected outline through a late callback, so it can appear before the sections. `DocLayout.of(sections...)` combines sections with a sticky table of contents. `TableOfContents.headings(list)` uses a fixed list instead. Collection order is heading order.

The search index reads the same collected headings without depending on `TableOfContents`. `SiteNavigation.searchPalette(site)` derives page entries from routes; `SiteNavigation.docPager(...)` derives page navigation.

## Add search

A `SearchPalette` renders the search input and result links and contributes its module automatically:

```java
var search = SearchPalette.of().id("guide-search")
    .placeholder("Search pages and sections...")
    .item("Guide", "Installation", "/guide/#installation")
    .item("Guide", SearchPalette.Item.of("Publishing", "/guide/#publishing")
        .description("Upload the generated site").badge("New"));

DocumentElement.of(Elements.main().child(search.triggerButton()))
    .child(Documents.deferred(search, 10));
```

The palette filters its rendered links without fetching an index or calling a search endpoint. Use `.item(group, title, href)` for simple entries, or `SearchPalette.Item` for descriptions and badges. The generated `search-index.json` is a separate artifact for other consumers.

| Action | Result |
| --- | --- |
| Type words | Filter group, title, and description; matching ignores case and accents |
| Clear the query | Restore all entries |
| Use arrow keys | Move focus through matching links |
| Press Enter in the input | Open the first match |
| Press Enter on a link | Follow that link |
| Press Escape | Dismiss the dialog |

Module packaging follows the [output mode](#choose-an-output-mode). See [Browser support](browser-support.md#component-scripts) for the module's lifecycle and native dialog behavior.

## Publish and preview

Write the compiled site with `SiteWriter.writeToDirectory(compiled, output)`, then upload the whole directory. For the origin-root examples, enable directory indexes and redirects from paths such as `/guide` to `/guide/`. Setting an asset base changes resource paths; it does not automatically rebase every route or authored link for a hosting subdirectory.

To preview the in-memory result, use `SitePreviewServer.start(compiled, port)` and retain the returned server until shutdown. It listens on loopback, serves a fixed snapshot, redirects directory routes while preserving queries, accepts `index.html` aliases, applies the compiled page headers, and sends `Cache-Control: no-store`. Recompile and restart to see changes.

To inspect files as they will be uploaded:

```sh
just site
jwebserver -b 127.0.0.1 -d target/site -p 8081
```

Use [Diagnostics](diagnostics.md) to locate authoring errors and [Development](development.md) to verify the generated site in browsers.
