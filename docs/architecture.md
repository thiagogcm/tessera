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
| `showcase` | `components`, `core`, `css`, `render`, `site` | Example application and CLI |

The component catalog and page compiler are sibling consumers of the element/CSS model. Neither imports the other. Site compilation consumes page metadata without knowing which components produced it. Navigation integrates site models and UI.

This remains one Maven artifact. Package-private access protects compiler details; the graph is a source-level layering convention rather than separate JPMS modules. There is no current independent deployment requirement to justify multiple artifacts.

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

`SiteValidator` receives prepared metadata and asset paths. It verifies internal root-relative links, cross-page fragments, and transition-key appearances on at least two routes. `SiteCompiler` retains collision checks because it assembles the output namespace. Compilation performs no filesystem writes.

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

This refactor preserves the scope of existing validation. Invoker checks establish target existence rather than command/tag compatibility. URL validation retains its existing root-relative path handling. Trusted raw HTML is not parsed, and JSON escaping retains the existing serializer's behavior. These behavioral concerns now have isolated owners in validators and artifact serialization.
