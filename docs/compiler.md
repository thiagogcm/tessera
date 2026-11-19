# Compiler internals

The static compiler separates preparation from emission so a site can validate every route and merge styles before producing HTML. It uses the same expansion, linking, and validation layer as the [browser renderer](browser-internals.md). Public inputs and outputs are described in [Sites](sites.md); [Architecture](architecture.md) defines package boundaries.

## Prepare a page

`PageCompiler` coordinates the passes. The sequence shows the successful path:

```mermaid
sequenceDiagram
    participant Page as PageCompiler
    participant Document as DocumentPass
    participant Expand as ViewExpansion
    participant Link as ReferenceLinker
    participant Validate as ViewValidation
    participant Prepared as PreparedPage

    Page->>Document: Lay out document and contributions
    Document->>Expand: Expand components in their contexts
    Expand-->>Document: Expanded nodes and collected styles
    Document->>Expand: Link ordered body and head content
    Expand->>Link: Resolve slots and references
    Link-->>Expand: Linked nodes
    Expand-->>Document: Linked document
    Document->>Expand: Resolve collected heading references
    Document-->>Page: Linked nodes, headings, and compilation state
    Page->>Validate: Index and validate linked document
    Validate-->>Page: Metadata and diagnostics
    Page->>Page: Fail if diagnostics exist
    Page->>Prepared: Retain linked tree, styles, and metadata snapshots
```

| Owner | Responsibility |
| --- | --- |
| `DocumentPass` | Orders deferred priorities, late slots, and document contributions |
| `DocumentExpansion` | Lowers document placements and contributes scripts declared by portable widgets |
| `ViewExpansion` | Expand components/scopes, require one element host per scope, allocate identities, collect styles and outline headings, and preserve origins |
| `ViewState` | Shared reference bindings, DOM IDs, generated counters, transition identities, and expansion diagnostics |
| `ReferenceLinker` | Resolve symbolic references and late slots, check sibling keys, splice transparent wrappers, and place tooltip companions |
| `CompilationState` | Read-only identity/transition views, outgoing URLs, and accumulated diagnostics |
| `ViewValidation` / `AccessibilityRules` | Validate linked elements, commands, URLs, and accessibility semantics |
| `PreparedPage` | Own the linked tree and stylesheet; snapshot IDs, headings, URLs, transitions, and scripts |

### Placement and linking

Deferred content expands in its lexical scope and is emitted in priority/sequence order. Late callbacks resolve before document-end sections and head contributions. Contribution processing allows end sections to register more end sections and head contributions to register more head content.

A scope's host is the element its `expand` returns: a `TagElement` for a portable scope and a `DocumentElement` for a document scope. `ViewExpansion` stamps `data-scope` and `data-scope-id` on it. Expansion does not insert a `div` or replace an inner scope's host. Page bodies, document components, late callbacks, and end sections lower to `DocumentNode`s; live nodes never reach the compiler.

`ReferenceLinker` converts `ViewNode` values to `LinkedNode` values in `internal.view` (which `ViewSnapshot` hands to `tessera-wasm` as is). Component boundaries, scope boundaries, fragments, and keyed children keep their own child namespaces while emitting no wrapper markup of their own. A scope boundary links as an ordinary group whose definition is the stable `scopeName()`. Expanded keyed wrappers retain keys and locations until linking, so keys returned by late callbacks participate in the surrounding sibling namespace. Tooltip companions are placed after their invokers.

Heading metadata is read from the element during expansion and resolved by `DocumentPass` after linking. An unbound outline target reports `unbound-reference`.

Linked tags, text, raw HTML, and transparent groups reach document validation, inspection, and emission. Groups retain keys and authoring definitions for browser reconciliation; static writers flatten their children. Linked tags own copies of their attributes and child lists. `TagElement.copy` also copies descendant elements while retaining reference identities and nested component/scope instances. `Field` and `Image` use copies when adding labels, hints, errors, and anchors, so repeated compilation does not mutate the caller's retained builder.

### Validation and preparation

After references and headings resolve, `CompilationState` receives read-only identity and transition views and copies diagnostics. It does not retain allocation counters or reference bindings. Indexing records targets by ID and labelled control IDs for later checks.

Image checks run during expansion while authored style information is available. Linked-tree checks validate interactive names, fragments, invoker targets, and built-in command compatibility. Accessibility rules inspect the body tree; URL collection reads `href`, `src`, and `poster` in both body and head. Malformed URLs become page diagnostics before site validation uses their metadata.

Ambiguous bindings and unresolved attributes remain marked so dependent checks do not choose arbitrary targets or report secondary errors. All collected authoring diagnostics fail preparation together. The [diagnostics guide](diagnostics.md) defines the public reporting behavior and accessibility rule codes.

A prepared page retains its own stylesheet. `contributeStylesTo(target)` copies its rules to the site stylesheet; `stylesheet(theme)` serializes a standalone contribution. Prepared metadata snapshots keep the first location for each outgoing URL and transition key, allowing site validation without exposing the intermediate tree.

## Coordinate a site

Site compilation gates each stage before proceeding. The diagram describes authoring-error paths; application callback and internal compiler exceptions propagate directly.

```mermaid
stateDiagram-v2
    [*] --> CheckingPaths
    CheckingPaths: Check routes and registered or reserved files
    CheckingPaths --> Rejected: Path diagnostics
    CheckingPaths --> Preparing: Paths are distinct
    Preparing: Prepare routes concurrently
    Preparing --> Rejected: Page diagnostics
    Preparing --> Validating: Every page prepared
    Validating: Check links, transitions, and generated asset paths
    Validating --> Rejected: Site diagnostics
    Validating --> Emitting: Site is valid
    Emitting: Emit prepared pages with merged CSS
    Emitting --> Compiled: Assemble pages and artifacts
    Rejected: Throw CompilationException
    Rejected --> [*]
    Compiled: Return CompiledSite
    Compiled --> [*]
```

Route preparation and emission run on virtual threads. The shared parallel mapper owns scheduling and ordered results; the coordinator owns diagnostic aggregation. Page failures, output pages, and stylesheet contributions retain route declaration order. Each page has separate compilation state; shared application state must be safe for concurrent callbacks.

`SiteValidator` consumes prepared metadata and registered asset paths. It resolves local URLs from each route's document URI, decodes paths and fragments, ignores queries for target lookup, and accepts generated `index.html` aliases. It also checks that transition keys appear on at least two routes. [Sites](sites.md#write-links-as-browser-urls) gives URL examples.

CSS merges sequentially in route order. The coordinator checks generated stylesheet and module paths for collisions with routes or registered files before emitting. `SiteArtifacts` then generates the search index and optional sitemap from prepared metadata.

## Emit resources

`HtmlWriter` encodes the linked tree and document envelope using the selected output mode. Its `Resources` object hashes and encodes the final stylesheet once and each distinct page module once per compilation. Every page references those selected resources. Resource paths and returned asset collections follow the [output-mode contract](sites.md#choose-an-output-mode).

Diagnostics and locations live in `core` so both backends use the same model. `CompilationException` in `compiler` reports static failures; `ViewException` in `view` reports invalid mounted candidates.

`PreparedPage` is opaque to callers. Emission reads its prepared content without invoking application callbacks again. Trusted raw HTML bypasses parsing and encoding; typed text and attribute output use context-sensitive encoding.

## Preserve source and diagnostic context

`Location` records optional route, page, component path, element tag, DOM ID, and source. Contribution records pair head, end-section, and heading content with the registering location. ViewExpansion and linking carry that location through deferred and late content.

`SourceProvenance` captures locations through the `SourceCapture` contract in `internal.authoring`. The JVM implementation, `SourceTracking`, lives in `compiler` and uses stack walking. Code-source classification distinguishes platform/library frames from application frames, including applications using Tessera-like package names. Elements built during component expansion inherit the component placement origin.

Tracking is off by default and is propagated to site virtual threads when enabled. Public usage and inspection commands are in [Diagnostics](diagnostics.md#trace-elements-to-java-source).
