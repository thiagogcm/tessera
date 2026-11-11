# Browser rendering roadmap

Deliver a Java-to-WebAssembly backend that mounts portable Tessera components into the browser DOM. The [architecture proposal](architecture-redesign.md) defines the target contracts; this roadmap defines implementation order and acceptance evidence.

The current reactor provides the JVM library and showcase. Treat `tessera-wasm`, browser roots, bundle descriptors, and the commands needed to build them as planned work. Use the [current guides](../README.md#documentation) for supported application development.

## Acceptance approach

Complete each phase before starting work that depends on it. A gate passes when its scenario works in an HTTP-served browser distribution in both Chromium and Firefox. JVM unit tests and ahead-of-time compilation support that evidence but cannot replace it.

Keep static compilation and its showcase verification passing throughout. Update implementation, examples, tests, and documentation together when changing shared APIs.

## Phase 1: Context and module boundaries

Implement the [context hierarchy](architecture-redesign.md#context-hierarchy-and-placement) and [module boundaries](architecture-redesign.md#module-and-package-boundaries).

| Deliverable | Acceptance evidence |
| --- | --- |
| Portable `ViewContext` and document-only assembly | Portable components compile without document capabilities; document placement is unavailable to browser views |
| Shared expansion and validation boundary | Both compiler and browser backend use shared operations without exposing backend types in authoring APIs |
| Module descriptors and internal access contract | API inspection and showcase compilation confirm that wrappers, inspection getters, and stylesheet collection respect the intended boundary |

## Phase 2: Controls and adaptive widgets

Implement the [shared control model](architecture-redesign.md#form-controls-and-adaptive-widgets).

| Deliverable | Acceptance evidence |
| --- | --- |
| Portable control state | Static/live parity for input, textarea, and select; live updates preserve caret, selection, focus, and IME composition |
| Tabs and field errors | Add/remove tabs while selected; verify roving focus and ARIA state; apply and clear explicit field errors |
| Dialogs, drawers, and tooltips | Open/dismiss in place, restore focus, and verify live tooltips without document script contributions |

## Phase 3: Resources, policy, and fallback

Implement [compiler-managed browser mounts](architecture-redesign.md#browser-mounts-and-resource-publication).

| Deliverable | Acceptance evidence |
| --- | --- |
| Immutable resource graph | Publish root and nested asset bases with correct MIME types, content hashes, and manifest integrity |
| Strict Content Security Policy | Browser acceptance runs under the proposed policy with no violations or inline script/style exceptions |
| Safe mount configuration | Escaping tests cover serialized mount attributes and entry-point configuration |
| Failure recovery | Missing launcher/binary assets, unsupported capabilities, and failing initial renders retain readable fallback content with an actionable error |

## Phase 4: Runtime resilience and release evidence

Verify the packaged application, including its dynamic behavior and resource lifecycle.

| Deliverable | Acceptance evidence |
| --- | --- |
| Streaming Fetch | Incremental DOM updates, cancellation during a stream, and ignored completion after root disposal |
| Dynamic accessibility | Keyboard, accessibility-tree, and axe-core WCAG A/AA checks after DOM mutations in both engines; reduced-motion and forced-colors coverage |
| Performance budgets | Define thresholds and report compressed size, boot-to-interactive time, render latency, and retained handles over repeated mount/close cycles |
| Complete distribution | Resource graph excludes compiler intermediates and remote dependencies |
| Acceptance application | A form and filterable list built with Tessera and the browser backend pass deployed browser verification |

## Scope limits

| Capability | Boundary |
| --- | --- |
| Server-rendered DOM hydration/adoption | Mounts replace validated fallback after a successful initial render |
| Implicit observable stores | Invalidation remains explicit through the browser root |
| Worker rendering or WASI | Target main-thread browser WasmGC through GraalVM Web Image |
| JVM closure/service transfer | Compile browser entry points ahead of time |

## Toolchain and command design

Choose and pin a compatible GraalVM Web Image toolchain, JavaScript interoperability API, and Binaryen version when implementing the backend. Keep that toolchain outside the default JVM build.

Add build and verification recipes only with runnable modules. They must assemble a self-contained browser host, serve it over HTTP, and exercise the packaged output in both engines. Record exact prerequisites beside those recipes so users can reproduce the distribution.

Reference the [Web Image manual](https://www.graalvm.org/latest/reference-manual/web-image/) and its [JavaScript interoperability API](https://www.graalvm.org/latest/reference-manual/web-image/api-guide/) when selecting the toolchain.
