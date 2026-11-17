# Architecture

Tessera uses shared expansion, reference linking, validation, and CSS collection for two outputs: static HTML documents and experimental live browser roots. Applications author mutable elements and components; each expansion captures its own tree before output or DOM reconciliation.

This document helps maintainers locate responsibilities in the current implementation. Read [compiler internals](compiler.md) for static pass ownership and [browser internals](browser-internals.md) for runtime behavior. The [proposal](architecture-redesign.md) defines target contracts; the [roadmap](roadmap.md) separates prerequisite repairs from the refactor and later features.

## Rendering paths

Static compilation produces complete documents and assets. Browser rendering produces a linked snapshot for one mounted root, then reconciles it with native DOM. Both paths use the same authoring model, but currently reject unsupported capabilities during expansion.

```mermaid
flowchart LR
    accTitle: Shared expansion with two output backends
    accDescr: Authoring passes through shared expansion and validation. Document orchestration emits static files; a browser root reconciles linked snapshots into DOM.
    Author["Elements and components"] --> Shared["Shared expansion and linking"]
    Document["Document assembly"] --> Shared
    Shared --> Validate["Shared semantic validation"]
    Validate --> Page["Document checks and prepared page"]
    Page --> HTML["HTML and assets"]
    Validate --> Snapshot["Live view snapshot"]
    Snapshot --> DOM["DOM reconciliation"]
```

## Package boundaries

All shared library packages belong to `dev.nthings:tessera`. The separate `tessera-showcase` module consumes its public API. The opt-in `tessera-wasm` runtime consumes shared view operations, and `tessera-wasm-showcase` builds its images and acceptance application.

| Package or module | Direct project dependencies | Responsibility |
| --- | --- | --- |
| `foundation` | None | Fluent composition, Kotlin callback marker, deterministic hashing and identifiers |
| `css` | `foundation` | Typed values, tokens, style authoring, collection, and serialization |
| `core` | `foundation`, `css` | Elements, component contracts, identities, document contributions, and current event/effect/control types |
| `view` | `foundation`, `css`, `core` | Shared expansion, linking, validation, snapshots, and diagnostic model |
| `compiler` | `foundation`, `css`, `core`, `view` | Page/site compilation, diagnostics, resources, filesystem and preview adapters |
| `components` | `foundation`, `css`, `core` | Reusable UI |
| `navigation` | `components`, `compiler` | Route-derived search palettes and pagers |
| `tessera-wasm` | `core`, `css`, `view`, Web Image interoperability SDK | Browser roots, reconciliation, events, lifetimes, and host services |
| `tessera-showcase` | `components`, `compiler`, `core`, `css`, `navigation` | Static examples and browser verification |
| `tessera-wasm-showcase` | `tessera-wasm`, shared library | Image building, browser examples, and real-image acceptance |

Components and the shared view layer are sibling consumers of the element/CSS model. The compiler and browser runtime consume shared view operations. Neither imports the other. Navigation connects the site model to UI. Page and site compilation share one package so their intermediate types and coordination APIs can remain package-private.

The parent POM owns common build policies. Runtime dependencies belong to their modules; Playwright stays test-scoped in the showcases. The `wasm` profile adds the browser modules without invoking their toolchain in the default build. See the [repository layout](development.md#repository-layout).

These are package and Maven artifact boundaries. There are no Java Platform Module System (JPMS) descriptors yet. Backend-facing types are public where cross-package or cross-artifact access requires it; they are not protected by qualified exports.

## Authoring model

`IntoElement` is the placement contract. Components convert to wrappers that the compiler expands; the sealed `Element` hierarchy supports exhaustive dispatch. The diagram shows the relevant subset:

```mermaid
classDiagram
    accTitle: Current authoring and component wrappers
    accDescr: Elements, components, and scopes share IntoElement. Component and scope records defer expansion.
    direction TB
    class IntoElement {
        <<interface>>
        +intoElement() Element
    }
    class Element {
        <<interface>>
    }
    class Component {
        <<interface>>
        +expand(DocumentContext) IntoElement
    }
    class Scope {
        <<interface>>
        +expand(DocumentContext) IntoElement
        +scopeId() String
        +configureScope(ScopeStyle) void
    }
    class TagElement
    class ComponentElement
    class ScopeElement

    IntoElement <|-- Element
    IntoElement <|-- Component
    IntoElement <|-- Scope
    Element <|.. TagElement
    Element <|.. ComponentElement
    Element <|.. ScopeElement
    ComponentElement --> Component : wraps
    ScopeElement --> Scope : wraps
```

`TagElement` holds private attributes, references, children, and declarations. Compiler-facing getters expose read-only views; typed methods enforce identity and ARIA guards. `FragmentElement` groups siblings without markup. Other element variants represent text, trusted raw HTML, deferred placement, late callbacks, keyed content, and effects. Event handlers and live control properties are currently stored on `TagElement`.

These wrapper records are public for backend access across packages. They are backend-facing representations; application composition uses the [component contracts](consumer-guide.md#choose-a-component-contract).

A scope root must be one element host. Expansion stamps `data-scope` on that host. It does not insert a `div`, and it does not retarget an inner scope's host. `Scope.expand` still returns `IntoElement`; the host check is runtime validation. A compile-time host API remains the target in [explicit scope roots](design/authoring.md#require-explicit-scope-roots).

`Styled`, `ParentElement`, and `FluentBuilder` share authoring operations without unchecked self casts. `StyleBlock`, `ScopeBlock`, and `ElementBlock` carry the `KotlinReceiver` marker for optional [Kotlin receiver lambdas](kotlin.md#enable-receiver-lambdas).

## Built-in components

Public built-in types expose factories and fluent configuration. Their expansion methods remain internal through two package-private bases:

| Base | Expansion boundary |
| --- | --- |
| `BuiltinComponent` | Wraps a component without opening a new identity scope; uses its class name in diagnostics |
| `BuiltinScope<B>` | Uses a private `Scope` adapter for the instance ID, stable scope name, styles, and expansion |

`Modal` and `Drawer` share a `DialogView` base. `Button` and `IconButton` are scope-free: their references resolve in the caller's namespace. Components that own a tag, such as `Image`, expose an `ElementBlock` configuration callback.

`TagName` represents built-in `HtmlTag` values and authored `CustomElement` names. `Command` represents checked built-in commands and custom commands with application-supplied payloads.

## Identity model

`ElementRef` carries symbolic identity until linking. The first named reference on an element chooses its DOM ID; anonymous references alias it. A conflicting second named reference becomes a diagnostic. `IdReference` represents attributes that target references.

Package-private `view.ViewScope` values qualify scoped names. Generated counters are local to a scope. Keyed content extends the scope with the key's hexadecimal UTF-16 units and, when present, its nearest named owner. The hexadecimal encoding has no separator ambiguity. A keyed component attaches the key to its component-path entry without adding an extra component wrapper.

`ViewScope` preserves whether an ancestor prefix was generated through both ordinary and keyed extensions. An explicit child scope does not make a generated parent stable. Expansion reports keyed content within any such prefix. See [identity references](consumer-guide.md#link-elements-with-identity-references) and [list keys](consumer-guide.md#key-list-items-that-move) for the authoring rules and fixes.

## Document state

`DocumentContext` exposes authoring capabilities while package-private `view.ViewContext` carries the current identity scope, component path, stylesheet, and origin. Derived contexts share page state without changing their parent's location or namespace.

`view.DocumentRegistry` owns component state and headings for one page compilation or one live render. `compiler.DocumentState` owns head contributions, document-end sections, and scripts for static output. `DocumentKey<T>` retains state without exposing its concrete type to the backend. `Image.hero()` uses this mechanism to detect a second hero image.

`DocumentHeading` is component-independent metadata collected from an element during expansion, not a context side effect. `Heading` carries an optional `DocumentHeading`. `ViewExpansion` records it, and `DocumentContext` does not register headings. Linking resolves the reference to a DOM ID for navigation and search. Search generation reads prepared heading metadata; it does not depend on a table-of-contents component. Other component registries remain local to their owners. Public contribution behavior is described in the [component guide](consumer-guide.md#contribute-document-content).

Browser callbacks currently receive `DocumentContext` too. Their state and headings last one attempted render, while head contributions, end sections, scripts, raw HTML, and late/deferred placement produce diagnostics. Static compilation rejects live event handlers, effects, and controlled properties. The proposed `ViewContext` is a public capability contract; it is distinct from today's package-private implementation class with that name.

## Stylesheet assembly

`Styled` is the public style trait. Package-private traits group its methods by concern. `CssValue` types represent lengths, colors, numbers, durations, borders, shadows, grids, transforms, and keyword values. Implementation records and property plumbing remain package-private.

`Declarations` accepts property writes from the CSS package and applies consistent shorthand/longhand replacement. It also retains conditional groups, starting styles, and referenced keyframes. [Styling](styling.md) defines the author-facing operations and precedence rules.

`StyleSheet` accepts typed declarations, scope callbacks, and validated transition names. It deduplicates rules and returns class names or scope keys to the compiler. The cascade layer order is:

```css
@layer reset, base, theme, components, utilities, interactive, container;
```

| Contribution | Generated representation |
| --- | --- |
| Element declarations | `u-` classes in `utilities` |
| Element state/media/starting styles | `i-` classes in `interactive` |
| Element container queries | `cq-` classes in `container` |
| Scoped component rules | Deduplicated scope blocks |
| Referenced keyframes | Emitted once in `base` |
| Theme values | CSS custom properties used by component rules |

Design tokens are sealed by category. `Theme.with` preserves those categories, and components use variable references so theme changes do not require a different component tree. Layout widths and breakpoint values remain explicitly authored.

## Compilation and output boundaries

`Compiler` is the public entry point. Inputs include `PageDocument`, `SiteDefinition`, `SiteRoute`, `Asset`, and `OutputMode`. Results are `CompiledPage`, `CompiledSite`, and the inspection result `PageTree`. [Sites](sites.md) defines their publication contracts.

`PageCompiler` coordinates document placement and output through the shared `ViewExpansion`, `ReferenceLinker`, and `ViewValidation`. Backend-facing representations are public for cross-package access; document coordination stays package-private. `PreparedPage` exposes metadata snapshots while retaining ownership of its stylesheet and linked tree. Emission uses prepared content without rerunning callbacks. See [compiler internals](compiler.md).

`ViewRenderer` produces a linked `ViewSnapshot` for one mounted render. The browser runtime reconciles its groups, events, controls, and effects against native DOM. See [browser internals](browser-internals.md) for identity, lifetimes, failure handling, and scheduling.

`HtmlWriter` owns context-sensitive encoding and the HTML envelope. `SiteArtifacts` serializes search JSON and sitemap XML. `SiteWriter` owns output paths and file writes; `SitePreviewServer` serves compiled results. Compilation itself writes no files. `Asset.file(...)` is explicit input I/O.

Authored raw HTML, selectors, and script source remain trusted inputs. [Diagnostics](diagnostics.md) describes validation coverage and [Browser support](browser-support.md) describes the generated client behavior.

## Browser ownership

`BrowserRoot` owns the view callback, committed references and IDs, scheduling, root lifetime, and styles. `DomReconciler` retains native nodes, logical ranges, listeners, and effect lifetimes. `Host` binds asynchronous work to a root or effect lifetime. The package-private `Dom` bridge contains Web Image interoperability code.

Each render prepares and validates a candidate before DOM mutation. The current commit path installs candidate references, reconciles nodes, runs removed-effect cleanup during reconciliation, starts new effects, and then updates committed IDs. Resource ownership is therefore not published at one boundary. The [runtime design](design/runtime.md#own-resources-before-exposing-callbacks) defines the replacement ordering.

Styles are shared by roots within one compiled entry. `StyleSink` manages the constructed stylesheet; `StyleLedger` counts rule ownership and bounds unused-rule retention. Independent entries have separate runtimes and ledgers. Frequently changing style values use stable custom-property rules. Detailed event, control, and scheduling behavior belongs in [browser internals](browser-internals.md).

## Current limits

These constraints affect current use and define the repairs required before broader API migration. They are not guarantees of the proposed design.

| Boundary | Current limitation | Required work |
| --- | --- | --- |
| Direct scope delegation | An outer scope that returns another scope reports `scope-root` and preserves the inner scope boundary. `Scope.expand` still returns `IntoElement` | [Compile-time scope hosts](design/authoring.md#require-explicit-scope-roots) |
| Non-element scope roots | A fragment, text, raw HTML, late or deferred content, or an empty root reports `scope-root`. Expansion inserts no host | [Compile-time scope hosts](design/authoring.md#require-explicit-scope-roots) |
| Closure during reconciliation | A removed effect's cleanup can close the root before new resources become reachable through its owned tree, leaving detached handlers active | [P2: Commit ownership](roadmap.md#p2-correct-commit-ownership-and-id-reservations) |
| Portable placement | One `IntoElement` surface accepts features that only one backend can execute | [Placement families](design/authoring.md#separate-capabilities-from-placement) |
| Public internals | Wrappers, inspection getters, stylesheet collection, and shared backend types are accessible to consumers | [Internal access](architecture-redesign.md#package-and-module-boundaries) |
| Deployment evidence | Existing CSP acceptance exercises `style-src 'self'`; full policy and performance budgets need a dedicated gate | [P3: Browser feasibility](roadmap.md#p3-establish-browser-feasibility) |

For current applications, return one authored element from a scope. Runtime expansion reports `scope-root` when that host is missing; it does not insert a wrapper or replace an inner scope. Each mount host's ID is reserved: a child claiming it is rejected before publication. Keep root closure outside effect cleanup during reconciliation; that ownership defect belongs to the experimental implementation, and [pre-refactoring work](roadmap.md#pre-refactoring-work) removes it. The remaining scope-host gap is the compile-time API in [explicit scope roots](design/authoring.md#require-explicit-scope-roots).
