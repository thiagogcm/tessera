# Architecture and separation of concerns

Tessera is a declarative Java 25 HTML/CSS compiler with a component library and static-site tooling. Authors construct mutable element builders and components. Compilation expands them into a separate linked tree, validates that tree, and emits HTML with inline or external CSS. Site compilation coordinates this process across routes and produces a shared stylesheet and generated artifacts.

## Assessment of the original structure

| Finding | Consequence | Refactor |
| --- | --- | --- |
| `core` imported CSS while `css` imported `core.FluentBuilder` and `core.Fnv1a64` | Element model and CSS formed a package cycle | Move shared fluent composition and hashing to `foundation` |
| The 664-line `PageCompiler` combined expansion, identity allocation, linking, validation, encoding, and serialization | Independent concerns shared one implementation boundary | Extract package-private passes behind a public coordinator |
| Public nested `ResolvedNode` types and a public `PreparedPage` record exposed the intermediate representation | Consumers could construct incomplete trees and depend on their representation | Publish an opaque prepared-page handle; hide the entire intermediate tree |
| Prepared results exposed a mutable stylesheet collector and `DocumentState` | Site code could modify compilation state and reach private component registries | Snapshot metadata and privately own CSS contributions |
| Search generation called `Toc.entries(prepared.documentState())` | Infrastructure depended on one component's registry convention | Introduce component-independent `DocumentHeading` metadata |
| `SiteCompiler` owned concurrency, validation, XML/JSON generation, and filesystem writes | Compilation also owned storage and artifact formats | Extract `SiteValidator`, `SiteArtifacts`, and public `SiteWriter` |
| `site.SiteNavigation` constructed UI components | Site infrastructure imported the component catalog | Move route-to-component composition to `navigation` |
| `TagElement.AnchorTarget` was unused | Public abstraction without a role | Remove the unused type |

Several existing abstractions already serve useful boundaries and remain:

- `IntoElement` is the authoring conversion contract. The sealed `Element` hierarchy allows exhaustive compiler dispatch.
- `RenderOnce` expands without adding a scope. `RenderView` establishes an identity namespace and optional CSS scope before expansion.
- `ElementRef` is an identity token, `ElementId` a local identifier, and `IdReference` an attribute reference resolved after expansion.
- `Styled`, `InteractiveElement`, `ParentElement`, and `FluentBuilder` share authoring operations without unchecked self casts.
- `DocumentKey<T>` supports per-compilation component state. Footnotes and image coordination retain their own registries without site-layer knowledge.
- `OutputMode` describes CSS publication independently of filesystem I/O.

## Dependency graph

These are direct imports between production packages, pointing from consumers to providers. The graph has no package cycles.

| Package | Direct project dependencies | Responsibility |
| --- | --- | --- |
| `foundation` | None | Fluent composition and deterministic identifiers/hashing |
| `css` | `foundation` | CSS values, tokens, authoring traits, collection and serialization |
| `core` | `foundation`, `css` | Elements, component contracts, identities, lexical context, document contributions |
| `render` | `foundation`, `css`, `core` | Page compilation and output |
| `components` | `foundation`, `css`, `core` | Reusable authored UI |
| `site` | `css`, `core`, `render` | Routes, site compilation, artifacts, filesystem and preview adapters |
| `navigation` | `components`, `site` | Route-derived search palette and pager composition |
| `showcase` | `components`, `core`, `css`, `navigation`, `render`, `site` | Example application and CLI |

The component catalog and page compiler are sibling consumers of the element/CSS model. Neither imports the other. Site compilation consumes page metadata without knowing which components produced it. Navigation integrates site models and UI.

The reactor builds two child modules under `tessera-parent`:

```text
tessera-showcase --> tessera
    |                  |
    |                  +-- published library and unit tests
    +-- runnable example and Playwright browser tests
```

`dev.nthings:tessera` retains every library package. `tessera-showcase` compiles as an independent public-API consumer, with no dependency on library test helpers. Playwright is test-scoped in the showcase module. Shared compiler and quality configuration belongs to the parent; runtime dependencies belong to their modules. Package-private access still protects internal compiler details within the library.

## User-facing APIs

| Task | Entry points and contracts |
| --- | --- |
| Author HTML | `Elements`, `TagElement`, `IntoElement`, sealed `Element` variants |
| Build components | `RenderOnce`, `RenderView`, `ParentElement`, `InteractiveElement`, `Styled` |
| Author styles | `CssLength`, `CssProperty`, `StyleRefinement`, `ScopeStyle`, `Interactivity`, `Tokens`, `DesignToken` |
| Bind identities and interactions | `ElementRef`, `ElementId`, `IdReference`, `BuiltinCommand`, `Action`, `TransitionKey` |
| Contribute document content | `RenderContext.current()`, `document(key)`, `contributeHead(...)`, `atDocumentEnd(...)`, `registerHeading(...)` |
| Compile a page | `PageDocument`, `PageCompiler.compile(...)`, `CompiledPage`, `OutputMode` |
| Coordinate page phases | `PageCompiler.prepare(...)`, opaque `PreparedPage`, `PageCompiler.paintPrepared(...)` |
| Define and compile sites | `SiteDefinition.builder()`, `SiteRoute`, `SiteAsset`, `LinkSpeculation`, `SiteCompiler.compile(...)`, `CompiledSite` |
| Write and preview | `SiteWriter.writeToDirectory(...)`, `SitePreviewServer.start(...)` |
| Derive navigation | `navigation.SiteNavigation.searchPalette(...)`, `docPager(...)` |
| Use supplied UI | Public factories and builders in `components` |

`CompiledPage` contains HTML, an optional external stylesheet path, and CSS text. `CompiledSite` contains ordered routes and published files, shared CSS, and readers for generated artifacts. Output types are independent of the functions producing them.

## Page compilation contracts

```text
PageDocument + Tokens + shared head contributions
    |
    v
LayoutPass          expand components; allocate identities; collect styles
    |
    v
ReferenceLinker     resolve late slots and symbolic ID attributes
    |
    v
PageValidator       validate linked nodes; collect outgoing URLs
    |
    v
PreparedPage        opaque tree + metadata snapshots + privately owned CSS
    |
    +---- site validation and ordered CSS merge ----+
    |                                              |
    v                                              v
HtmlWriter <--------------------------------- selected stylesheet
    |
    v
CompiledPage
```

`PageCompiler` coordinates passes. `CompilationState` belongs to one preparation: identity bindings, counters, deferred priorities, late slots, and collected metadata. It does not escape preparation.

`LayoutPass` runs inside `RenderContext.runRoot`. Child view scopes share page state and styles. Deferred elements expand in their lexical scope and are emitted later in priority/sequence order. Late elements capture their lexical context and expand after ordinary traversal. End sections may register more end sections; head contributions may add more head contributions. Existing iterative processing and ordering are preserved.

`ReferenceLinker` substitutes slots and resolves references after identity allocation. `PageValidator` checks images, readable interactive labels, same-page fragments, and invoker target existence. It collects `href`, `src`, and `poster` URLs from both body and head nodes. Image checks run during layout while authored style information is available; tree checks run after linking.

`ResolvedNode` and its variants are package-private. Attributes and child lists are compiler-owned copies, so compilation does not modify authoring builders. Raw HTML is an explicit trusted escape hatch and remains opaque to validation.

`PreparedPage` has no public constructor, node accessors, or document-state accessor. Public metadata consists of snapshots of IDs, outgoing URLs, transition keys, and headings. Its stylesheet collector is copied and privately owned. `contributeStylesTo(target)` copies rules into a caller-owned collector; `stylesheet(tokens)` serializes the page contribution. Painting reuses prepared content without rerunning component callbacks.

`HtmlWriter` owns HTML context encoding and the document envelope. External CSS paths derive from final stylesheet content, allowing every route to reference the same merged stylesheet. Authored CSS and explicitly raw HTML remain trusted input.

## Document metadata and component state

`DocumentState` owns page-local component registries, ordered head contributions, end sections, and navigation headings. `RenderContext` exposes authoring operations; the site layer never receives this mutable state.

`DocumentHeading(targetDomId, title, level)` is component-independent metadata. `registerHeading(localId, title, level)` resolves the local ID in the current view scope and records it. The component emits the referenced element. Registration does not automatically index every HTML heading.

`ContentSection` and `Toc.DocSection` register headings while rendering. `Toc.fromContext` consumes them through a late element, so it may precede sections. Explicit TOCs accept `List<DocumentHeading>`. Section components retain supported levels 2 through 4; general metadata supports HTML heading levels 1 through 6. Headings retain registration order and scoped target IDs.

Search generation reads `PreparedPage.headings()`. Custom components participate through `registerHeading` without importing or rendering `Toc`. Other component registries remain local implementation details keyed by `DocumentKey<T>`.

## Site orchestration and I/O

```text
SiteDefinition
    |
    v
prepare routes concurrently --> route-ordered PreparedPage results
    |
    v
SiteValidator ---------------> links, fragments, transition keys
    |
    v
merge CSS in route order ----> shared stylesheet
    |
    v
paint routes concurrently ---> route-ordered CompiledPage results
    |
    v
SiteArtifacts ---------------> sitemap and search-index assets
    |
    v
CompiledSite
    +----> SiteWriter --------> directory tree
    +----> SitePreviewServer -> local HTTP responses
```

Site definitions require at least one route. Tasks use virtual threads; results are gathered in declaration order. Each page has separate compilation state. CSS merges sequentially and deterministically. Component callbacks should avoid mutating shared application state during concurrent rendering.

`SiteValidator` receives prepared metadata and asset paths. It resolves relative and root-relative URLs against each route's published document URI, validates decoded paths and fragments, and excludes query strings from target lookup. Generated `index.html` URLs are accepted alongside route paths. Absolute and protocol-relative URLs are external to this validation. It also checks transition-key appearances on at least two routes. `SiteCompiler` retains output namespace collision checks. Compilation performs no filesystem writes.

`SiteArtifacts` owns XML/JSON serialization. `SiteWriter` owns directory creation, route-to-output-path mapping, and byte writes. `SitePreviewServer` serves the same compiled routes and assets. `SiteAsset.file(...)` remains explicit input I/O; byte-backed assets are shared by compilation and output adapters.

## Breaking changes

| Previous symbol | Current symbol or operation |
| --- | --- |
| `core.FluentBuilder` | `foundation.FluentBuilder` |
| `core.Fnv1a64` | `foundation.Fnv1a64` |
| `PageCompiler.CompiledPage` | `render.CompiledPage` |
| `PageCompiler.PreparedPage` | Opaque `render.PreparedPage` |
| Public `PageCompiler.Resolved*` | Package-private implementation |
| `prepared.styles()` | `prepared.contributeStylesTo(target)` or `prepared.stylesheet(tokens)` |
| `prepared.documentState()` | `prepared.headings()` for navigation/search metadata |
| `Toc.Item` | `core.DocumentHeading` |
| `Toc.entries(state)` | `state.headings()` during rendering; `prepared.headings()` after preparation |
| `SiteCompiler.CompiledSite` | `site.CompiledSite` |
| `SiteCompiler.writeToDirectory(...)` | `SiteWriter.writeToDirectory(...)` |
| `site.SiteNavigation` | `navigation.SiteNavigation` |
| `TagElement.AnchorTarget` | Removed unused type; `anchoredTo(ElementRef, ...)` remains |

Callers, tests, and showcase code use new APIs directly, without compatibility shims.

## Verification and remaining limits

Existing tests cover scoped identities, linking failures, late TOCs, deferred footnotes and overlays, image contributions, CSS scope deduplication, merged stylesheets, site links/assets, disk output, and preview HTTP responses. The added integration case verifies that search output includes headings registered by a custom component without `Toc`. Tests exercise output rather than package shape or builder implementation details.

The showcase is also the browser acceptance suite. It compiles through public `SiteCompiler`, writes through `SiteWriter`, and serves the resulting directory with the JDK static HTTP server on an ephemeral port. Browser tests therefore cover published output, rather than substituting in-memory HTML for the generated site.

JUnit reuses Chromium across the suite and creates a fresh browser context per test. Every route is opened at desktop and narrow viewport widths, checking visible headings, applied CSS, image loading, HTTP failures, and horizontal overflow. Interaction tests exercise dialog focus and dismissal, popover light dismissal, field labels and validation, tabs, exclusive accordions, route and fragment navigation, and switches. These complement the library tests' compiler diagnostics and output assertions.

Surefire runs unit tests during `test`. Failsafe runs `*IT` tests during `integration-test` and checks results at `verify`. Browser installation is an explicit setup command (`just browsers`); CI installs the configured release's Chromium and operating-system dependencies before full verification. There is no browser-version matrix. Tests retain Playwright traces and failure screenshots under the showcase module's `target/playwright` directory.

Invoker checks establish target existence rather than command/tag compatibility. Trusted raw HTML is not parsed, and JSON escaping retains the existing serializer's behavior. These concerns have isolated owners in validators and artifact serialization.

## Shared design tokens and search

The design-token registry covers colors, font families, typography scale, spacing, corner radii, line height, and component density. Built-in components use CSS variable references, so overriding a token affects their rendered appearance without changing the component tree. `DesignToken.length()` bridges token references to length-valued authoring APIs. Spacing tokens include the density multiplier; layout widths and breakpoint definitions remain separately authored values.

`SearchPalette` always renders a functional search input and grouped result links. It contributes a bundled inline ES module once per document using the render context, so standalone pages and compiled sites carry the behavior automatically. No component-to-site dependency or manual asset registration is needed.

The module defines a customized built-in `HTMLDialogElement`. Browser upgrade and lifecycle callbacks scope behavior to each palette; `AbortController` removes listeners when an instance disconnects. Native invoker commands and dialog behavior handle opening, focus trapping, and dismissal. Selection uses native `requestClose()` and anchor navigation, while CSS `:has()` controls empty-group visibility. The module has private instance fields and caches normalized search entries per instance. It needs no document-wide initializer, index fetch, or polyfills for the target browser.

The [consumer guide](consumer-guide.md) and its runnable `GuideSite` demonstrate these public APIs together. Browser tests cover filtered navigation, empty-state recovery, custom token rendering, and encoded relative links over published files.
