# Architecture and separation of concerns

Tessera is a declarative Java 25 HTML/CSS compiler with a component library and static-site tooling. Authors construct mutable element builders and components. Compilation expands them into a separate linked tree, validates that tree, and emits HTML with inline or external CSS. Site compilation coordinates this process across routes and produces a shared stylesheet and generated artifacts.

## API evolution

Tessera has no external consumers. Evolve APIs directly with breaking changes and zero compatibility shims. Update the implementation, showcase, tests, and documentation together; do not add deprecation wrappers or migration guides.

Documentation diagrams use [Mermaid](https://mermaid.js.org/syntax/all-examples). Select the diagram type that best expresses the structure, relationships, states, or interactions being documented. Exact mappings remain Markdown tables.

## Core abstractions

- `IntoElement` is the authoring conversion contract. The sealed `Element` hierarchy allows exhaustive compiler dispatch.
- `RenderOnce.render(context)` expands without adding a scope. `RenderView.render(context)` establishes an identity namespace and optional CSS scope before expansion. `configureScope(scope)` declares the scoped CSS.
- `FragmentElement` groups sibling elements without emitting a wrapper, preserving direct-child content models.
- `TagName` is the sealed element-name family: the built-in `HtmlTag` enum or an author-defined `CustomElement`.
- `ElementRef` is an identity token, `ElementId` a local identifier, and `IdReference` an attribute reference resolved after expansion.
- `ClientBehavior` is a deduplicated ES module contribution. It emits inline for standalone pages or as a content-hashed asset when output publishes external files.
- `Styled`, `InteractiveElement`, `ParentElement`, and `FluentBuilder` share authoring operations without unchecked self casts.
- `Styled` composes six traits by concern: `LayoutStyled`, `BoxStyled`, `TextStyled`, `EffectStyled`, `ScrollStyled`, and `AnchorStyled`. Every style method takes a typed value of the category its property accepts, so a color cannot be passed where a length belongs.
- `CssValue` is the sealed family of those values: `CssLength` (dimensions, `calc`, `min`/`max`, `anchor()`), `CssColor` (OKLCH, `color-mix`, `light-dark`, relative colors), `CssNumber`, `CssDuration`, `Easing`, `Border`, `Shadow`, `Background`, `TrackList`, `GridPlacement`, `Transform`, `AspectRatio`, `CssIdent`, and enums for keyword sets (`CssKeyword`). `StyleRefinement` accepts declarations only from the `css` package, so no declaration is built from raw text.
- `Accessible` writes the element's `Role` and typed `aria-*` states. The generic `attr`, `boolAttr`, and `idRef` methods refuse `role` and `aria-*`.
- `SourceProvenance` lets elements record their Java call site as a `SourceLocation` through a scoped `SourceCapture`; `render.SourceTracking` supplies the JVM stack-walking capture, so `core` has no stack-walker dependency.
- `DocumentKey<T>` supports per-compilation component state. Footnotes and image coordination retain their own registries without site-layer knowledge.
- `OutputMode` describes CSS publication independently of filesystem I/O.

The authoring contracts convert components into element wrappers before the layout pass expands them. The class diagram shows the relevant subset of the sealed element hierarchy:

```mermaid
classDiagram
    direction TB
    class IntoElement {
        <<interface>>
        +intoElement() Element
    }
    class Element {
        <<interface>>
    }
    class RenderOnce {
        <<interface>>
        +render(RenderContext) Element
    }
    class RenderView {
        <<interface>>
        +render(RenderContext) Element
        +configureScope(ScopeStyle) void
    }
    class TagElement
    class RenderOnceElement {
        <<record>>
    }
    class RenderViewElement {
        <<record>>
    }

    IntoElement <|-- Element
    IntoElement <|-- RenderOnce
    IntoElement <|-- RenderView
    Element <|.. TagElement
    Element <|.. RenderOnceElement
    Element <|.. RenderViewElement
    RenderOnceElement --> RenderOnce : wraps
    RenderViewElement --> RenderView : wraps
```

## Dependency graph

Arrows point from consumers to providers. The diagram omits transitive edges for readability; the table lists every direct project dependency. The graph has no package cycles.

```mermaid
flowchart TB
    showcase --> navigation
    navigation --> site
    navigation --> components
    site --> render
    render --> core
    components --> core
    core --> css
    css --> foundation
```

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

```mermaid
block
    columns 3
    parent["tessera-parent<br/>Shared compiler and quality configuration"]:3
    showcase["tessera-showcase<br/>Runnable examples and Playwright tests"]
    space
    library["tessera<br/>Published library and unit tests"]
    showcase --> library
```

`dev.nthings:tessera` contains every library package. `tessera-showcase` compiles as a separate module using the public API, with no dependency on library test helpers. Playwright is test-scoped in the showcase module. Shared compiler and quality configuration belongs to the parent; runtime dependencies belong to their modules. Package-private access still protects internal compiler details within the library.

## User-facing APIs

| Task | Entry points and contracts |
| --- | --- |
| Author HTML | `Elements`, `TagElement`, `TagName` (`HtmlTag`, `CustomElement`), `FragmentElement`, `IntoElement`, sealed `Element` variants |
| Build components | `RenderOnce`, `RenderView`, `ParentElement`, `InteractiveElement`, `Styled` |
| Add client behavior | `ClientBehavior`, `RenderContext.contributeBehavior(...)`, `CompiledPage.assets()` |
| Author styles | `Styled` and its six traits, the `CssValue` family (`CssLength`, `CssColor`, `Border`, `Shadow`, `TrackList`, ...), `StyleRefinement`, `FlexLayout`, `Typography`, `Motion`, `ScopeStyle`, `Interactivity`, `Tokens`, sealed `DesignToken` |
| Declare accessibility semantics | `Accessible`, `Role` (name, children, state, and `aria-*` requirements) |
| Bind identities and interactions | `ElementRef`, `ElementId`, `IdReference`, `BuiltinCommand`, `Action`, `TransitionKey` |
| Trace elements to source | `SourceTracking`, `SourceLocation`, `PageCompiler.explain(...)`, `PreparedPage.tree()`, `PageTree` |
| Contribute document content | The callback's `RenderContext`: `document(key)`, `contributeHead(...)`, `atDocumentEnd(...)`, `registerHeading(...)` |
| Compile a page | `PageDocument`, `PageCompiler.compile(...)`, `CompiledPage`, `OutputMode` |
| Coordinate page phases | `PageCompiler.prepare(...)`, opaque `PreparedPage`, `PageCompiler.paintPrepared(...)` |
| Define and compile sites | `SiteDefinition.builder()`, `SiteRoute`, `SiteAsset`, `LinkSpeculation`, `SiteCompiler.compile(...)`, `CompiledSite` |
| Write and preview | `SiteWriter.writeToDirectory(...)`, `SitePreviewServer.start(...)` |
| Derive navigation | `navigation.SiteNavigation.searchPalette(...)`, `docPager(...)` |
| Use supplied UI | Public factories and builders in `components` |

`CompiledPage` contains HTML, an optional external stylesheet path, CSS text, and generated assets such as external client behavior modules. `CompiledSite` contains ordered routes and published files, shared CSS, and readers for generated artifacts. Output types are independent of the functions producing them.

## Page compilation contracts

```mermaid
flowchart TD
    Input["PageDocument, Tokens, and shared head contributions"]
    Layout["LayoutPass and ViewExpansion<br/>Expand components, bind identities, collect styles"]
    Linker["ReferenceLinker<br/>Resolve late slots, fragments, tooltip companions and symbolic ID attributes"]
    Validator["PageValidator<br/>Check linked nodes and collect valid URLs"]
    Prepared["PreparedPage<br/>Opaque tree, metadata snapshots, and private CSS"]
    Selection["Standalone stylesheet or site validation<br/>and ordered stylesheet merge"]
    Writer["HtmlWriter<br/>Encode the prepared tree with selected CSS"]
    Output["CompiledPage"]

    Input --> Layout --> Linker --> Validator
    Validator -->|No authoring errors| Prepared
    Prepared -->|Prepared content| Writer
    Prepared -->|Styles and metadata| Selection
    Selection -->|Selected stylesheet| Writer
    Writer --> Output
```

`PageCompiler` coordinates passes. `CompilationState` belongs to one preparation: identity bindings, counters, deferred priorities, late slots, and collected metadata. It does not escape preparation.

`LayoutPass` orders the document and `ViewExpansion` expands components into `ViewNode`s, binding element references and numbering generated ids within each identity scope, so content added in one view never renumbers another. A render or style callback failure propagates unchanged, with a suppressed `CompilationException` naming the component path. `LayoutPass` creates an explicit document context. Derived child contexts share that document's state and styles without changing their parent's scope or component path. Deferred elements expand in their lexical scope and are emitted later in priority/sequence order. Late and document-end callbacks receive their explicit contribution context. End sections may register more end sections; head contributions may add more head contributions. Existing iterative processing and ordering are preserved.

`ReferenceLinker` turns `ViewNode`s into `ResolvedNode`s after identity allocation: it substitutes late slots, splices fragments, places each tooltip companion after its invoker and resolves references. `PageValidator` checks images, readable interactive labels, same-page fragments, invoker target existence, and compatibility of built-in commands with dialog and popover targets. `AccessibilityRules` checks the linked body tree (see below). It collects `href`, `src`, and `poster` URLs from both body and head nodes. Image checks run during layout while authored style information is available; tree checks run after linking.

`ViewNode` and `ResolvedNode` are package-private. Only linked `ResolvedNode`s (tags, text and raw HTML) reach validation, inspection and painting. Attributes and child lists are compiler-owned copies, so compilation does not modify authoring builders. Raw HTML is an explicit trusted escape hatch and remains opaque to validation.

`PreparedPage` has no public constructor, node accessors, or document-state accessor. Public metadata consists of snapshots of IDs, outgoing URLs, transition keys, headings, and client behaviors. Its stylesheet collector is copied and privately owned. `contributeStylesTo(target)` copies rules into a caller-owned collector; `stylesheet(tokens)` serializes the page contribution. Painting reuses prepared content without rerunning component callbacks.

Field rendering copies the retained control's authoring configuration before wiring labels, hints, errors and anchors. The copy retains reference identities and child definitions but owns its attribute and reference maps. Setting `hint(null)` or `error(null)` removes that contribution on the next render without leaving stale references on the retained builder.

`HtmlWriter` owns HTML context encoding and the document envelope. External CSS paths derive from final stylesheet content, allowing every route to reference the same merged stylesheet. Client behaviors are emitted once per page: inline module scripts for `OutputMode.InlineStyle`, or content-hashed module assets under the same external asset base for `OutputMode.ExternalAsset`. Authored CSS and explicitly raw HTML remain trusted input.

## Document metadata and component state

`DocumentState` owns page-local component registries, ordered head contributions, end sections, and navigation headings. `RenderContext` exposes authoring operations; the site layer never receives this mutable state.

`DocumentHeading(targetDomId, title, level)` is component-independent metadata. `registerHeading(localId, title, level)` resolves the local ID in the current view scope and records it. The component emits the referenced element. Registration does not automatically index every HTML heading.

`ContentSection` and `Toc.DocSection` register headings while rendering. `Toc.fromContext` consumes them through a late element, so it may precede sections. Explicit TOCs accept `List<DocumentHeading>`. Section components retain supported levels 2 through 4; general metadata supports HTML heading levels 1 through 6. Headings retain registration order and scoped target IDs.

Search generation reads `PreparedPage.headings()`. Custom components participate through `registerHeading` without importing or rendering `Toc`. Other component registries remain local implementation details keyed by `DocumentKey<T>`.

## Accessibility rules

`AccessibilityRules` runs on the linked body after `PageValidator` and reports through the same diagnostics. Rules that depend on a role apply to elements that declare one with `Accessible.role(Role)`; native elements are checked where their semantics are unambiguous (`<dialog>`, headings, landmarks, `<label for>`).

| Rule | Reported when |
| --- | --- |
| `role-name-required` | A role that requires an accessible name (or a native `<dialog>`) has no `aria-label`, `aria-labelledby`, `title`, native label, or, for roles named from content, readable content |
| `role-required-children` | A role such as `tablist`, `list`, `listbox`, `menu`, or `tree` owns none of the roles it requires |
| `role-required-attribute` | A custom element lacks a state its role needs (`aria-checked`, `aria-level`, `aria-valuenow`, `aria-selected`); native hosts that expose the state are exempt |
| `aria-attribute-not-allowed` | An `aria-*` attribute is neither global nor supported by the declared role |
| `landmark-name-unique` | Several landmarks of one kind lack distinct accessible names |
| `multiple-main` | A page has more than one main landmark |
| `heading-level-skip`, `empty-heading` | Heading levels jump by more than one in document order, or a heading has no text |
| `tabindex-positive` | A positive `tabindex` overrides the natural tab order |
| `label-target` | `<label for>` points at an element a label cannot name |
| `aria-hidden-focusable` | An `aria-hidden` subtree contains a control that stays in the tab order |

Closed dialogs, popovers, and `aria-hidden` subtrees are excluded from landmark and heading structure because they have no accessibility tree. `Role` carries each role's requirements as data, so the rules and the authoring API share one source.

## Source provenance

`SourceTracking` is off by default. While it is active (inside `SourceTracking.enabled(...)` or `SourceTracking.with(true, ...)`), each `TagElement` and component wrapper records the first stack frame that is neither Tessera's nor the platform's. Frames are classified by code source, so a library user's own code is never mistaken for Tessera's, even in the same package.

- An element a Tessera component builds during compilation is attributed to the line that placed the component, through the origin carried by `RenderContext` and by head and end-section contributions.
- `CompilationLocation` carries the `SourceLocation`; `CompilationException` prints it as `<button> (Page.java:42)`.
- `HtmlWriter` emits `data-src="Page.java:42"` for tracked elements, so a rendered element leads back to the line that wrote it.
- `SiteCompiler` carries the caller's setting onto its virtual threads.
- `PreparedPage.tree()` snapshots the linked tree as `PageTree`: tag, DOM id, attributes, enclosing components, CSS scope, and source. `PageTree.toText()` prints it; `just explain <route>` prints a showcase route.

## Compilation diagnostics

`CompilationLocation` records the page title, optional route and a `ViewLocation`: enclosing component path, element tag name, optional DOM ID and source. `CompilationDiagnostic.format()` presents it; a component-level location omits the element.

`RenderContext` carries a component path independently of the identity scope. `DocumentState.Contribution<T>` pairs registered head or end-section content with that path, allowing layout to expand it with its originating component context. These authoring types have no dependency on `render`.

Layout attaches diagnostic locations to resolved tags and retains them when expanding late or deferred content. Linking preserves the location when replacing symbolic attributes. `PreparedPage` snapshots the first location of each outgoing URL and each transition key so site validation can report errors without exposing the intermediate tree.

`CompilationState` collects `CompilationDiagnostic` records containing a rule code, location, message, and suggested fix. Recoverable authoring errors accumulate through layout, linking, and validation. Duplicate reference bindings and DOM IDs are marked ambiguous, so target-dependent checks do not select an arbitrary element. Unresolved symbolic attributes remain marked as unresolved and are excluded from dependent checks, avoiding secondary errors such as a missing-label report caused by an unbound label reference. The compiler throws one `CompilationException` with the collected diagnostics before returning a prepared page or emitting HTML.

The preparation coordinator maps each route to either a prepared page or collected diagnostics, annotates failures with the route, and gathers diagnostics in route declaration order despite concurrent execution. The shared parallel mapper owns scheduling and result ordering; it has no diagnostic aggregation policy. URL syntax is validated once during page validation: malformed URLs become `invalid-url` diagnostics. Site validation consumes the syntactically valid URL metadata. Cross-route link and transition validation run only after every page prepares successfully and share one diagnostic report. Application callback failures and internal compiler failures continue to propagate rather than being converted to authoring diagnostics.

For site compilation, validation gates the transition from prepared pages to painted output:

```mermaid
stateDiagram-v2
    [*] --> Preparing
    Preparing: Prepare all routes and collect page diagnostics
    Preparing --> Rejected: Page authoring errors
    Preparing --> Prepared: Every page prepared successfully
    Prepared --> ValidatingSite
    ValidatingSite: Check cross-route links and transition keys
    ValidatingSite --> Rejected: Site diagnostics
    ValidatingSite --> Painting: Site is valid
    Painting: Merge CSS and paint prepared pages
    Painting --> Compiled: Assemble pages and generated assets
    Rejected: Throw CompilationException
    Rejected --> [*]
    Compiled: Return CompiledSite
    Compiled --> [*]
```

## Site orchestration and I/O

The sequence shows a successful multi-route compilation; the state diagram above describes rejection paths. Each parallel block represents the route batch.

```mermaid
sequenceDiagram
    actor App as Application
    participant Site as SiteCompiler
    participant Pages as PageCompiler
    participant Checks as SiteValidator
    participant Assets as SiteArtifacts

    App->>Site: compile(definition)
    par First route on a virtual thread
        Site->>Pages: prepare(...)
        Pages-->>Site: PreparedPage
    and Other routes on virtual threads
        Site->>Pages: prepare(...)
        Pages-->>Site: PreparedPage
    end
    Site->>Site: Gather results in route order
    Site->>Checks: Validate links and transition keys
    Checks-->>Site: No site diagnostics
    Site->>Site: Merge CSS in route order
    par First prepared page on a virtual thread
        Site->>Pages: paintPrepared(...)
        Pages-->>Site: CompiledPage
    and Other prepared pages on virtual threads
        Site->>Pages: paintPrepared(...)
        Pages-->>Site: CompiledPage
    end
    Site->>Site: Assemble pages in route order
    Site->>Assets: Generate search index and optional sitemap
    Assets-->>Site: Generated assets
    Site-->>App: CompiledSite
```

Site definitions require at least one route. Tasks use virtual threads; results are gathered in declaration order. Each page has separate compilation state. CSS merges sequentially and deterministically. Component callbacks should avoid mutating shared application state during concurrent rendering.

`SiteValidator` receives prepared metadata and asset paths. It resolves relative and root-relative URLs against each route's published document URI, validates decoded paths and fragments, and excludes query strings from target lookup. Generated `index.html` URLs are accepted alongside route paths. Absolute and protocol-relative URLs are external to this validation. It also checks transition-key appearances on at least two routes. `SiteCompiler` retains output namespace collision checks. Compilation performs no filesystem writes.

`SiteArtifacts` owns XML/JSON serialization. `SiteWriter` owns directory creation, route-to-output-path mapping, and byte writes. `SitePreviewServer` serves the same compiled routes and assets. `SiteAsset.file(...)` remains explicit input I/O; byte-backed assets are shared by compilation and output adapters.

## Verification and remaining limits

Existing tests cover scoped identities, linking failures, late TOCs, deferred footnotes and overlays, image contributions, merged stylesheets, site links/assets, disk output, and preview HTTP responses. Search integration tests verify that output includes headings registered by a custom component without `Toc`. Tests exercise output rather than package shape or builder implementation details.

The showcase is also the browser acceptance suite. It compiles through public `SiteCompiler`, writes through `SiteWriter`, and serves the resulting directory with the JDK static HTTP server on an ephemeral port. Browser tests therefore cover published output, rather than substituting in-memory HTML for the generated site.

JUnit runs the same browser suite on the latest Playwright Chromium and Firefox, reusing each browser across its class and creating a fresh browser context per test. Documentation routes are opened at desktop and narrow viewport widths, checking visible headings, applied CSS, image loading, HTTP failures, and horizontal overflow. Named cases are routes under `/stories/{case}/{variant}/{appearance}/{density}/`. Documentation sections and browser tests call the same factories, and each case page contributes the chosen `color-scheme` and density in its document head. Those cases are opened at the same widths for the overflow check. Documentation routes additionally run axe-core WCAG A/AA audits, keyboard flows for every interactive widget, forced-colors, reduced-motion, 200% zoom, and text-spacing checks. These complement the library tests' compiler diagnostics and output assertions.

Surefire runs unit tests during `test`. Failsafe runs `*IT` tests during `integration-test` and checks results at `verify`. Browser installation is an explicit setup command (`just browsers`); CI installs the configured release's Chromium, Firefox, and operating-system dependencies before full verification. There is no browser-version matrix: the contract is the latest stable release of each supported engine. Tests retain Playwright traces and failure screenshots under the showcase module's `target/playwright/{chromium,firefox}` directory.

Invoker checks establish target existence and require the appropriate dialog or popover target for Tessera's built-in commands. Custom commands remain valid for arbitrary existing targets. Trusted raw HTML is not parsed, and JSON escaping retains the existing serializer's behavior. These concerns have isolated owners in validators and artifact serialization.

## Shared design tokens and search

The design-token registry covers colors, font families, the type scale, tracking, spacing, corner radii, line height, motion durations, and component density. `DesignToken` is sealed by category: `Color`, `Length`, `Duration`, `FontFamily`, and `Number`. `Length` tokens are `CssLength` values, so they work in length-valued authoring directly. `withValue` overrides a token without changing its category. Spacing tokens include the density multiplier; font sizes, tracking, and durations do not. `Typography` and `Motion` expand into the same declaration pipeline as other style values. Component roles in `Roles` alias registry tokens, so a theme replaces the role or the underlying scale through `Tokens.register`. Built-in components use CSS variable references, so an override changes rendered appearance without changing the component tree. Layout widths and breakpoint definitions remain separately authored values. `Button` and `IconButton` emit native buttons. Tone, surface, and size are separate from submit and invoker behavior.

`SearchPalette` always renders a functional search input and grouped result links. It contributes a `ClientBehavior` once per document using the render context, so standalone pages and compiled sites carry the behavior automatically. The behavior is inlined for standalone output and published as a content-hashed module asset when the page uses external assets. No component-to-site dependency or manual asset registration is needed. `ContentTabs` uses the same path for ARIA tab selection and roving tabindex. `TooltipBehavior` is contributed when an `interestTooltip` is resolved; it supplies hover and focus handling where native interest invokers are absent.

The search module defines a customized built-in `HTMLDialogElement`. Browser upgrade and lifecycle callbacks scope behavior to each palette; `AbortController` removes listeners when an instance disconnects. Native invoker commands and dialog behavior handle opening, focus trapping, and dismissal. Selection uses native `requestClose()` and anchor navigation, while CSS `:has()` controls empty-group visibility. The module has private instance fields and caches normalized search entries per instance. It needs no document-wide initializer, index fetch, or polyfills for the target browsers. The tabs module defines an autonomous `tessera-tabs` element, selects panels through `aria-controls`, and moves focus with Arrow Left, Arrow Right, Home, and End.

The [consumer guide](consumer-guide.md) and its runnable `GuideSite` demonstrate these public APIs together. Browser tests cover filtered navigation, empty-state recovery, custom token rendering, and encoded relative links over published files.
