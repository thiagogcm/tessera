# Browser runtime internals

This document describes the experimental `tessera-wasm` runtime. Its [current limits](architecture.md#current-limits) qualify the ownership and identity behavior below; the [runtime proposal](design/runtime.md) specifies the target guarantees. See [Browser rendering](browser-rendering.md) for authoring and setup, and [Architecture](architecture.md) for shared package boundaries.

`ViewRenderer.render(content, scope)` runs one turn of a mounted view through the same `ViewExpansion` with a mounted-view backend and returns the linked `ViewSnapshot` without document placement or whole-document validation. Shared semantic validation still runs.

Per-page state (`DocumentContext.document`) and headings last one turn there.

Head contributions, document-end sections, page scripts (including the interest-tooltip script), late or deferred placement and raw HTML are reported as `native-head-contribution`, `native-document-end`, `native-page-script`, `native-document-placement` and `native-raw-html`, which reject the candidate like any other `ViewException`.

Event handlers travel through shared expansion on the linked tags; the document backend rejects them because Java closures cannot be serialized into static HTML. `tessera-wasm` builds the browser backend on this entry point.

## Native identity and reconciliation

Linking keeps components, scopes, fragments and keyed children as `ViewSnapshot.Group`s carrying an optional sibling key and a definition: the authoring type of a component (`ComponentElement.type()`) or of other keyed content, the `scopeName()` of a scope, or `FragmentElement` for a fragment. A scope group's definition is that name, not a scope instance created for the render, so reconciliation keeps the same group across turns. A keyed component, fragment, or scope carries its key on its own group. The DOM reconciler anchors each group with two comment nodes and retains the complete range between them.

Expansion stamps `data-scope` on the one element a scope resolves to, looking through component and keyed groups. A keyed element is a valid host. A fragment, text, raw HTML, another scope, or nothing reports `scope-root` and inserts no element. An enclosing scope does not replace an inner scope's host. `generated-scope-id` on a keyed host does not also make that host a `scope-root`.

Shared expansion allocates each keyed child's scope from the enclosing id scope, the nearest named ref owner and a lossless encoding of the key, so reordering a keyed child, or inserting and removing other content, does not renumber the surviving child's ids. Duplicate keys are diagnosed within each logical sibling list before native mutation. A generated enclosing scope id still reports `generated-scope-id`.

| Candidate child | Matching rule |
| --- | --- |
| Keyed group | Same parent, exact key and same authoring type |
| Unkeyed group | Earliest unmatched sibling group with the same authoring type |
| Element | Earliest unmatched sibling element with the same tag name |
| Text | Earliest unmatched sibling text node |
| Incompatible child | Create the replacement and dispose the obsolete subtree |

The reconciler patches changed text and attributes, removes obsolete attributes, updates event registrations and recursively reconciles children. Retained ranges on the longest run already in sibling order stay in place; only the others move, and new nodes are inserted directly at their final position, so ordinary insertion or removal does not move unchanged sibling ranges. Connected moves use the browser's state-preserving `moveBefore` operation when available.

Unchanged attributes are not reapplied, so unrelated updates do not overwrite dirty native input values. Matching unkeyed children by kind keeps a field's control when its hint or error appears or disappears beside it; inserting a sibling of the same kind before it still shifts the match, so such lists need keys.

Ordinary subtree removal releases its listeners, including callbacks on detached native nodes. The [closure-during-reconciliation limit](architecture.md#current-limits) is an exception.

## Event contract

`TagElement.onEvent(type, options, handler)` binds an `EventListener`. `EventOptions` selects `capture`, `passive` and `once`; a listener's identity on its element is its type and phase, as with `addEventListener`. Each element keeps one native listener per identity. A re-render with the same options swaps the Java handler the native listener calls, so the previous callback is no longer referenced; different options re-register it, and removing the listener releases it. A dispatched `once` listener stays spent while later renders keep binding it, and is re-armed only after a render omits it.

Handlers run synchronously on the native dispatch, within the event's user activation, so `preventDefault()`, `stopPropagation()` and `stopImmediatePropagation()` take effect before the browser continues and gesture-gated operations such as `showModal()` succeed. A passive listener's `preventDefault()` is ignored by the browser, which `defaultPrevented()` reports. `BrowserEvent` copies `control()`, `keyboard()` and `pointer()` data; asking for data of the wrong event kind fails, and the failure is reported like any handler failure.

## Controlled native properties

`ControlProperties` carries optional desired `value`, `checked` and `selectedValues`. An absent field leaves that property browser-managed, so initial HTML attributes and live bindings stay independent. Shared expansion rejects bindings on incompatible hosts (`control-value`, `control-checked`, `control-selection`), and the document backend reports `native-control-properties` rather than emitting live state as an attribute.

The reconciler applies controls after attributes and children, comparing each desired property with the actual DOM state rather than only with the previous snapshot. Equal values are not reassigned, which preserves caret, selection and focus during unrelated updates. Different values are written even when the Java value did not change, which restores edits the application rejected. `uncontrolled()` removes the bindings and leaves subsequent browser state intact. Retained `input` and `textarea` nodes track IME composition per node: while composing, value writes are deferred so the browser owns provisional text, and on a controlled field `compositionend` requests a fresh render.

## Lifetime and effects

`Elements.effect(key, effect)` wraps an `EffectElement` in a `KeyedElement`, so an effect reaches the linked tree as a `ViewSnapshot.Effect` alone inside a keyed `ViewSnapshot.Group` and takes its identity from the keyed-group machinery: effect keys share the sibling namespace with keyed children, so an effect and a keyed sibling with the same key are diagnosed as `duplicate-key`. The reconciler anchors the effect with a comment node inside the group's two markers and starts it after the tree is committed. Its `Lifetime` is disposed when a render removes it, when its key changes (the group is replaced and the new effect then starts) or when its root closes; cleanups run newest first and every one runs even if another fails.

`BrowserRoot.lifetime()` scopes cleanup to the root. Requests started through `Host` have individual abort controllers whose cancellation belongs to their lifetime and root. A cleanup registered after disposal runs immediately, and `Lifetime.guard(...)` and `BrowserRoot.update(...)` ignore completions that arrive after disposal.

## Entry points and cross-root identity

A page declares each mount on its host element: `data-tessera-entry` names the launcher of the compiled entry and `data-tessera-view` the view to mount (the host id by default).

`bootstrap.js` loads each distinct launcher once, one at a time. Every Web Image launcher assigns the same `GraalVM` global, so the bootstrap captures each launcher's object as soon as its script runs and restores the previous value; each entry then runs with its own `Config`, with its hosts passed to `main` as `host=view` arguments. `BrowserEntry.start(args, views)` mounts each declared host through the mount function its view names. An undeclared view or a failed mount is reported and leaves that host's fallback content; other hosts and entries still mount. Each entry keeps its own shared stylesheet.

Identity checks read the document, so they cover roots of every compiled entry. A root refuses a host that already holds a root, lies inside one or contains one; closing removes the host's root marker, so the host can be mounted again.

Generated and scoped ids are prefixed with the host id. Before each commit, every DOM id the candidate introduces (an id in `ViewSnapshot.ids()` that the root's last commit did not render) is checked against the document: an id that an element outside the root already uses, in another root or in the host page, rejects the candidate with a `cross-root-id` error naming the owner. The host's own id is reserved, so an id the candidate renders that names the mount host rejects the candidate the same way; a child never duplicates the host's identity. A newly mounting root therefore checks all its ids.

Ids a root has committed are its own and are not checked again, so markup the host page adds later with one of them does not block the root's renders; an id the root stops rendering is released, and rendering it again checks it again.

Sibling matching is separate from this DOM-ID check: a logical group's key and definition identify its retained range.

## Host services

`BrowserRoot.host()` returns a `Host` bound to the root lifetime; `host.within(lifetime)` scopes the same services to an effect, returning one cached `Host` per lifetime so repeated calls register nothing more with it. It covers timeouts, intervals, animation frames and `afterRender` tasks, text requests, focus and focus return, text selection, dialogs, popovers and media playback. Scheduled callbacks and request completions run only while their lifetime and root are active, report failures like event handlers and then invalidate the root; `afterRender` tasks do not invalidate it again. Disposing the lifetime cancels pending callbacks and aborts its requests; a disposed request reports nothing. `fetchText` checks the HTTP status and separates HTTP from network failures.

Element operations take an `ElementRef` and never return native objects. Each commit records the refs its tree binds (`ViewSnapshot.references()`), and an operation resolves its ref to the committed element by DOM id within the root's host, so it cannot reach another root's elements. An unbound ref fails with an `IllegalStateException` explaining the rule below. `showModal` is idempotent for a modal dialog and rejects a dialog already open without a modal; `FocusReturn` restores the remembered element, or a fallback ref when a render replaced it.

Refs resolve against the latest committed render. A handler bound in a render may use refs that render creates, because the handler is replaced with its render. Work that outlives a render (effects, timers, request callbacks, `afterRender` tasks) must use a ref held in application state and passed to the component on every render. Components that own a dialog or popover accept one: `Modal.dialogRef(ref)`, `Drawer.dialogRef(ref)` and `Dropdown.panelRef(ref)`.

## Failure containment

Each turn expands a candidate before touching the DOM. Failures become readable browser `Error` reports naming the root rather than opaque Java proxies:

| Failure | Effect |
| --- | --- |
| Diagnostics or a throwing render/style callback | Candidate rejected; the committed tree is unchanged and a later invalidation retries. Callback failures carry the component and key path. |
| Initial preparation failure | The host keeps its fallback content and later roots still mount. |
| Unexpected DOM commit failure | Closes the root and reports the error, releasing what the commit acquired. A first commit builds off the host, so a failure before publication keeps the fallback content. |
| Java event handler | Reported with the event type and root; the root and its tree stay interactive. |
| Application cleanup while closing | Remaining application cleanups still run; failures are reported together once. |

Closing a root is idempotent. It cancels scheduled work, releases listeners, disposes effects innermost first, closes open dialogs and popovers, empties the owned host, disposes the root lifetime, and releases styles. It also drops its view callback.

A commit owns what it acquires. It registers each instance it creates before that instance acquires listeners or an effect lifetime, detaches replaced ranges, publishes the tree, references and IDs at one boundary, retires the replaced resources with their application cleanups, and starts the new effects. No application callback runs while the commit mutates the DOM, so a closure request settles at a phase boundary: closing during expansion discards the candidate, closing during mutation aborts and releases what the commit acquired, and closing from a retirement cleanup, an effect start or an after-render task joins teardown and stops the rest of the batch. A root that has started closing dispatches no handler, so a detached node cannot run application code after teardown.

## Embedded styles

`StyleSheet.embeddedBaseRules(theme)` emits the reset and base rules under the `data-tessera-root` host and the token values under a content-addressed `data-tessera-theme` key (`StyleSheet.themeKey(theme)`); a root acquires them once at mount, after `StyleSheet.LAYER_ORDER`. Each render's `embeddedRules()` emits one self-contained rule per entry: the same component, utility, interactive and container rules as static output, so only those are diffed between commits. Static and embedded output are written from one source of registered rules, and the reset and base rules take the root they apply beneath. Each embedded rule carries a short key (its `u-`, `i-` or `cq-` class, its `data-scope` selector, or its property name), so a root keeps the rule text it built per key and compares renders by key, wrapping and hashing only rules it does not hold yet. Mounting never restyles `:root`, `body` or the rest of the host page, and document navigation transitions are omitted.

Roots within one compiled entry share a constructed stylesheet adopted into the document. The stylesheet needs no inline `<style>` element and is exercised under `style-src 'self'`. This establishes stylesheet behavior, not the complete proposed deployment policy.

`StyleSink.Root` owns each root's base rules from mount to close and its render rules by key, acquires new rules before native mutation, and releases superseded rules after a successful commit. `StyleLedger` reference-counts rules across roots. Up to 512 unused rules stay inserted for later renders and mounts; the least recently released beyond that are deleted. The layer-order statement is pinned first so reinserted rules never change the cascade order.

Every distinct style value is a content-addressed rule, so a value that changes on every render would churn rules through that pool. `TagElement.live(...)` avoids that: `StyleSheet.registerLive` compiles each live declaration into the element's stable rule as `property: var(--tl-property)`, and the reconciler sets `--tl-property` on the element through CSSOM `style.setProperty`, which a `style-src 'self'` policy allows. Values stay in the element's cascade layer, so state and container rules still apply, and each `--tl-*` name is registered once with `@property … inherits: false`, so a changing value restyles only its element.

## Scheduling

Each root schedules invalidations into a browser task. Repeated calls before that task runs coalesce into one render, and a render that calls `invalidate()` schedules a later turn instead of recursing. `BrowserRoot.Scheduling` bounds the render turns applied in one flush and the consecutive turns that may request another render. After the work budget, the root waits for an animation frame before the next batch, so the browser can paint.

| Limit | Default | Effect |
| --- | --- | --- |
| `workBudget` | 8 | Render turns applied in one browser task before a paint opportunity |
| `runawayLimit` | 32 | Consecutive self-invalidating turns before the root reports and stops |

A root that reaches its runaway limit reports one browser `Error`, leaves the last committed tree in place and stops scheduling until an external invalidation. `BrowserRoot.update(change)` wraps a change so that running it applies the change and invalidates the root; after `close()` the wrapper does nothing.

The real image is verified in Chromium and Firefox by `WebImageIT`, which serves the compiled launchers and host page and drives every demo in the `tessera-wasm-showcase` example package.
