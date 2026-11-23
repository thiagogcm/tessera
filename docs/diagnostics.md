# Diagnostics

Tessera validates theme contrast, markup, references, form controls, accessibility semantics, and site links before returning output. Static `Compiler` methods throw `CompilationException` on authoring errors; live browser renders reject invalid candidates with `ViewException` and keep the last valid DOM.

## Read a diagnostic

Inspect collected errors without exceptions using `tryCompile(...)` (returning a sealed `CompilationResult`), or catch `CompilationException` from `compile(...)`:

```java
switch (Compiler.forSite().tryCompile(site)) {
  case CompilationResult.Compiled(var compiled) -> SiteWriter.writeToDirectory(compiled, out);
  case CompilationResult.Failed(var diagnostics) ->
      diagnostics.forEach(d -> System.err.println(d.format()));
}
```

The exception message prints the complete formatted report:

```text
Compilation failed with 1 error:
[interactive-label] Route "/settings", page "Settings" > toolbar[toolbar] > <button id="toolbar--save">: Interactive <button> element must have text content or an aria-label / aria-labelledby / title attribute
  Fix: Add visible label text, .ariaLabel(...), or .ariaLabelledBy(...) referencing a label.
```

| Accessor | Meaning |
| --- | --- |
| `rule()` | Stable rule code identifying the check |
| `message()` | What failed validation |
| `fix()` | Actionable correction |
| `location()` | Optional route, page title, component path, element tag, DOM ID, and Java source line |

Standalone pages omit routes from locations, and site-level file collisions and theme diagnostics can omit both route and page. Deferred, head, and document-end content retains the component path that registered it.

## Understand validation stages

The compiler collects all independent diagnostics within the failing stage and emits no HTML when a stage fails:

| Stage | Scope and ordering |
| --- | --- |
| 1. Theme and site paths | Checks [theme contrast](#theme-contrast), then duplicate routes and registered or reserved file collisions, before preparing pages; a standalone page checks its theme only |
| 2. Page preparation | Checks identity, references, images, markup, controls, widgets, accessibility, and URL syntax; site failures are ordered by route declaration |
| 3. Site validation | Runs after all pages prepare; checks cross-route links, fragments, shared transition keys, and generated asset collisions |

Ambiguous duplicate bindings and unresolved references are excluded from target-dependent checks to prevent cascading noise. Cross-route link diagnostics report the first location where a broken URL appears on a page. See [Compiler internals](compiler.md#coordinate-a-site) for the complete stage machine.

Exceptions thrown inside application callbacks (`expand`, `configureScope`, `late`, `atDocumentEnd`) propagate directly with a suppressed `CompilationException` adding component context. Trusted [`Documents.rawHtml(...)`](documents.md#mix-portable-and-document-content) bypasses validation.

## Rule reference

### Identity, markup, and security

| Rule | Reported when |
| --- | --- |
| `duplicate-id` | Two elements on the same page or root bind the same DOM ID |
| `conflicting-element-id` | Multiple named `ElementRef` bindings on one element request different DOM IDs |
| `duplicate-reference-binding` | One `ElementRef` is bound to more than one element |
| `unbound-reference` | An attribute, command, or outline targets an `ElementRef` that is never bound |
| `duplicate-key` | Two sibling elements, components, or effects in the same parent share a key |
| `generated-scope-id` | Keyed content sits inside a scope whose `scopeId()` was generated from encounter order |
| `unstable-scope-name` | An anonymous `Scope` omits `scopeName()` |
| `cross-root-id` | A browser root introduces a DOM ID already owned by the host element or external document markup |
| `svg-namespace` | An SVG shape sits outside `<svg>`, or an HTML element sits inside `<svg>` |
| `script-url` | A `javascript:` URL appears in `href`, `xlink:href`, `src`, `action`, `formaction`, `poster`, or `data`, or an `<iframe>` `src` holds a `data:` URL (checked case-insensitively) |
| `invalid-url` | A local URL cannot be parsed |
| `missing-fragment` | A same-page `#fragment` link does not match any DOM ID on the page |
| `missing-command-target`, `incompatible-command-target` | A `commandfor` target is missing, or a built-in dialog/popover command targets an incompatible element |
| `missing-interest-target` | An `interestTooltip` trigger targets a missing ID |
| `image-alt`, `decorative-image`, `image-dimensions` | An image lacks `alt` text, mixes empty `alt` without decorative ARIA, or lacks dimensions, an aspect ratio, or `.unknownDimensions()` |
| `duplicate-hero-image` | More than one `HeroImage` is placed on the same page |
| `callback-failure` | An expansion or styling callback throws during live rendering |

### Form controls

| Rule | Reported when |
| --- | --- |
| `initial-value` | `initialValue` is set on an unsupported host, such as a file input or multiple select |
| `initial-checked` | `initialChecked` is set outside a checkbox or radio input |
| `initial-selection` | `initialSelectedValues` is set outside a multiple select, or raw HTML prevents option matching |
| `initial-control-conflict` | A typed default conflicts with a generic attribute, authored textarea text, or an option's `selected` attribute |
| `control-value`, `control-checked`, `control-selection` | A live binding or typed change callback is attached to an incompatible host |

See [Set form defaults](components.md#set-form-defaults) and [Bind form values](browser-rendering.md#bind-form-values).

### Widgets, mounts, and sites

| Rule | Reported when |
| --- | --- |
| `empty-content-tabs` | `ContentTabs` has no tabs |
| `duplicate-tab` | Two tabs in one `ContentTabs` share a key |
| `tab-selection` | A static or live tab selection names an absent or disabled tab, or a live binding is empty while an enabled tab exists |
| `wasm-mount-placement` | A `Documents.wasmMount(...)` sits in `<head>`, inside another mount, or inside a non-host container |
| `duplicate-route` | Two `SiteRoute` entries declare the same normalized path |
| `file-path-collision` | Two routes, registered assets, or generated files map to the same URL or output path |
| `missing-route-or-asset`, `missing-route-fragment` | A local site link targets a missing route, asset, or target-page fragment |
| `duplicate-transition-key`, `unshared-transition-key` | A `TransitionKey` appears twice on one page or on fewer than two site routes |

### Theme contrast

The compiler checks the `Theme` before it prepares any page. Each failing pair produces one `theme-contrast` diagnostic per color scheme:

```text
[theme-contrast] Light scheme: the solid accent label color (#ffffff) on --accent-primary (#008c2f) has a contrast ratio of 4.36:1, below the WCAG AA minimum of 4.5:1 for text in accent solid buttons and badges
  Fix: Darken --accent-primary in the light scheme until the solid accent label color on it reaches 4.5:1. Use CssColor.lightDark(light, dark) to give a token separate light and dark values.
```

| Rule | Reported when |
| --- | --- |
| `theme-contrast` | A text color that built-in components draw falls below 4.5:1 (WCAG 2.2 SC 1.4.3, level AA) on its surface in the light or dark scheme |

The check covers the text and surface pairs built-in components draw from color tokens:

| Text | Surfaces |
| --- | --- |
| `TEXT_PRIMARY`, `TEXT_MUTED`, `CONTROL_FG` | `SURFACE_CANVAS` and `SURFACE_CARD`; `SURFACE_SUBTLE` for primary and muted text |
| `ACCENT_PRIMARY` (active tabs, pager links, eyebrows) | `SURFACE_CANVAS`, `SURFACE_CARD`, and `SURFACE_SUBTLE` |
| Each `Tone.textColor()` (`ACCENT_TEXT`, `STATUS_*_TEXT`) | `SURFACE_CANVAS`, `SURFACE_CARD`, and the tone's 14% tint used by tinted badges |
| Each `Tone.onSolid()` label color | The tone's solid fill (`ACCENT_PRIMARY`, `STATUS_*`) |
| `SURFACE_CARD` | Solid neutral fills of `TEXT_PRIMARY` (buttons) and `TEXT_MUTED` (badges) |
| `CONTROL_FG`, `TEXT_PRIMARY`, `TEXT_MUTED`, `ACCENT_PRIMARY` | Callout tints, text highlights, invalid fields, and highlight sections, as each component uses them |
| `STATUS_SUCCESS_TEXT`, `STATUS_DANGER_TEXT` | Inserted and deleted code line tints |

Non-text indicators such as focus rings and switch tracks use `ACCENT_PRIMARY`, which its text pairs already hold to the stricter 4.5:1.

Colors resolve the way the browser computes them: token aliases follow the theme, `light-dark()` picks the scheme's branch, and `sameHue`, `color-mix()`, and `contrast-color()` are evaluated in OKLab, OKLCH, and sRGB. Translucent colors composite over the layers beneath them, and colors outside the sRGB gamut clip per channel. Ratios use WCAG 2.x relative luminance and print truncated to two decimals, so a failing pair never prints as `4.50:1`. The check skips a pair, rather than guessing, when a color depends on the page: `currentColor`, a token the theme does not define, a cycle of token aliases, or a `color-mix()` weight that is not a plain percentage.

Fix a failure by adjusting the named token for the reported scheme. Use `CssColor.lightDark(light, dark)` for a token whose light and dark values need different lightness, because a single value rarely reads on both a near-white and a near-black surface. Call `theme.contrastDiagnostics()` to inspect a theme without compiling, such as in a unit test.

### Accessibility rules

Use [typed roles and states](components.md#declare-roles-and-accessibility-states) to declare semantics. Role-specific checks apply to explicit roles and recognized native elements. Closed dialogs, popovers, and `aria-hidden` subtrees are excluded from landmark and heading outline checks.

| Rule | Reported when |
| --- | --- |
| `interactive-label` | A native interactive control without an explicit role lacks an accessible name |
| `role-name-required` | A role requiring an accessible name, a native `<dialog>`, or a heading/link/button with an explicit role has no accessible name |
| `role-required-children` | A role such as `tablist`, `list`, `listbox`, `menu`, or `tree` lacks required owned roles |
| `role-required-attribute` | A declared role lacks a required state (`aria-checked`, `aria-level`, `aria-valuenow`, `aria-selected`) not provided natively |
| `aria-attribute-not-allowed` | An ARIA attribute is neither global nor valid on the declared role |
| `landmark-name-unique` | Repeated landmarks of the same kind lack distinct accessible names |
| `multiple-main` | A page contains more than one `main` landmark |
| `heading-level-skip` | Heading levels jump by more than one in document order |
| `empty-heading` | A native heading contains no text |
| `tabindex-positive` | A positive `tabindex` disrupts natural keyboard focus order |
| `label-target` | A `<label>` targets a non-labellable element |
| `aria-hidden-focusable` | An `aria-hidden="true"` subtree contains a focusable control in the tab order |

## Trace elements to Java source

Wrap authoring and compilation in `SourceTracking.enabled(...)` to record Java file and line origins:

```java
var page =
    SourceTracking.enabled(
        () -> Compiler.forPage().compile(PageDocument.of("Home", Elements.main(Elements.h1("Home")))));
```

When enabled, emitted HTML elements carry `data-src="Page.java:42"` attributes and diagnostics include the exact Java source line. Elements constructed inside built-in components attribute themselves to the caller line that placed the component. Site compilation propagates tracking automatically to its virtual threads.

Inspect a page's linked structure without reading HTML using `Compiler.explain(document)`, which returns a `PageTree` of tags, IDs, attributes, component paths, CSS scopes, and source lines (`toText()` formats it). On the showcase:

```sh
just demo
just explain /overlays/
```

`just demo` enables source tracking automatically. Stack walking adds per-element overhead, so keep tracking off in production or gate it with `SourceTracking.enabledIf(flag, callback)`.
