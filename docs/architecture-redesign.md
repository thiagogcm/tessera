# Dual-backend architecture proposal

Use one portable component model to produce static documents and live browser views. Keep document assembly and browser execution explicit, and make unsupported placement a type error. This proposal defines the target design; [Architecture](architecture.md) describes the implementation available now.

The intended readers are maintainers changing authoring APIs, compiler passes, and the browser runtime. The [roadmap](roadmap.md) defines delivery order and acceptance evidence. Proposed names below describe responsibilities; they are not available APIs.

## Read the design by responsibility

| Decision | Primary specification |
| --- | --- |
| Placement types, contexts, scope roots, controls, and widgets | [Authoring design](design/authoring.md) |
| Commit ownership, disposal, identity, and browser publication | [Runtime design](design/runtime.md) |
| Packages, module access, and change policy | This document |
| Work sequence and completion gates | [Roadmap](roadmap.md) |

## Design commitments

Preserve shared expansion, reference linking, semantic validation, and CSS generation. The static compiler and browser renderer remain separate consumers of those operations. Neither backend depends on the other backend's implementation.

| Concern | Decision |
| --- | --- |
| Portability | Portable output cannot contain document-only or live-only children, including children received from callers |
| Static authoring | Document composition adds assembly capabilities without importing browser services |
| Live authoring | Browser events, effects, and services belong to the browser module |
| Scope composition | Every styled scope has an explicit authored host; expansion never invents a host or replaces another scope's markers |
| Control state | Portable declarations describe initial state; live bindings separately declare application ownership |
| Runtime ownership | Every acquired resource has an owner before callbacks can request disposal |
| Validation | Types exclude unsupported capabilities; shared validation checks authored values and relationships |
| Change policy | Replace obsolete APIs and migrate all callers together; do not add compatibility aliases or alternate legacy paths |

The browser target is main-thread WebAssembly with garbage collection (WasmGC), built through GraalVM Web Image. Invalidation stays explicit. Hydration, workers, WASI, implicit observable stores, and serialization of JVM closures are outside this proposal.

## Package and module boundaries

Keep two runtime artifacts: `tessera` for portable authoring and static compilation, and `tessera-wasm` for live authoring and browser execution. Keep each showcase separate. Do not split the shared library into more artifacts solely to hide individual classes.

The Java Platform Module System (JPMS) controls exports on the module path. The public API must remain usable there, and the Web Image build must consume the same implementation. Classpath execution does not enforce qualified exports; document that limitation rather than claiming universal access protection.

| Package or module | Contents | Access |
| --- | --- | --- |
| `foundation` | Fluent composition and deterministic identifiers | Public authoring support |
| `css` | Typed CSS values, declarations, style callbacks, and package-private serialization | Exported public authoring package |
| `core` | Portable elements, component contracts, references, render context, and public diagnostics | Exported public authoring package |
| `document` and `document.components` | Document content, builders, assembly context, and document-only components | Exported from `tessera`; no browser dependencies |
| `components` | Portable widget structure and configuration | Exported; depends on portable authoring |
| `compiler` | Static orchestration, page/site inputs, output, filesystem and preview adapters | Exported entry points; package-private coordination |
| `navigation` | Site-derived document UI | Exported; depends on document components and compiler |
| `internal.authoring` | Closed node data and controlled access from public builders | Not exported |
| `internal.css` | CSS collection entry point and immutable collected rule data | Not exported |
| `view` | Shared backend entry points and linked snapshots | Qualified export to `dev.nthings.tessera.wasm` |
| `dev.nthings.tessera.wasm` | Live content, builders, events, effects, root ownership, and host services | Public live API; DOM bridge and reconciliation stay internal |

The linked backend tree carries backend-owned metadata through one internal type parameter. The browser owns the concrete handler and effect payloads; shared traversal carries them without interpreting them or importing browser classes. Portable authoring types remain free of that parameter. Document and live lowering feed the same structural expansion and validation operations through their respective entry points.

Shared backend interfaces carry only the capabilities each backend implements. Remove hooks whose only role is to reject a node that the placement types already exclude. Do not replace them with a general plugin registry, service locator, or backend-mode flags on portable builders.

### Access authoring data without public inspection APIs

Store mutable node data in the internal authoring package. Public builders delegate writes to that data and retain their value checks. Public element interfaces are sealed where needed to keep the portable node set closed; user components remain extensible through `Component`.

An internal accessor registered by the owning public package reads each builder's data. Expansion captures the data it needs into its own representation. This narrowly scoped accessor is the cross-package bridge; it is not a public escape hatch, reflective lookup, or adapter supplied by applications.

Remove public wrapper constructors and backend inspection getters after compiler and browser callers use the bridge. Exported authoring signatures must not expose internal node data or linked snapshots. The qualified backend API may expose its own linked types. A client using supported APIs should not need `--add-exports` or reflective access.

### Keep CSS authoring and collection separate

Move stylesheet aggregation behind `internal.css`, while keeping property ordering, serialization, and `ScopeStyle` construction with their package-private CSS collaborators. A CSS-owned internal accessor exposes collection operations and immutable rule data to the aggregator. This avoids making serialization helpers public merely because `StyleSheet` moves packages.

The shared `view` boundary returns collected rules needed by the browser. Browser consumers use public CSS authoring APIs but do not reach into CSS serialization or `internal.css`. Static and embedded output continue to derive from one rule representation, including live-value shapes, rather than two serializers that can drift.

### Keep browser behavior out of portable widgets

Portable widgets expose markup, configuration, and initial state. Static widget behavior contributes its module during document lowering. Browser adapters in `tessera-wasm` attach Java bindings and lifecycle work to portable structure through the qualified backend interface.

An adapter returns live content. It cannot be placed back into portable or document content. The [widget state rules](design/authoring.md#adaptive-widgets) define which layer owns state and how native actions reach the application.

## Preserve the compiler's useful boundaries

Retain preparation before emission, ordered document contributions, reference linking before validation, and deterministic site stylesheet merging. Prepared pages own their linked trees and expose metadata snapshots. Emission does not rerun author callbacks.

Retain the separation between compilation and publication. The compiler returns pages and assets; filesystem and HTTP adapters publish them. Browser bundle support extends that resource boundary without teaching the compiler to invoke the ahead-of-time toolchain.

Keep diagnostics precise about authoring locations and recoverable failures. Checks for duplicate IDs, invalid control hosts, unresolved references, malformed URLs, and accessibility relationships remain useful even when placement is type-safe.

## Verify the design

Use the roadmap's [acceptance rules](roadmap.md#acceptance-rules) and stage-specific scenarios. Keep behavioral compiler and browser coverage through the migration; revise representation-pinning assertions with the code they constrain.
