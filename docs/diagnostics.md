# Diagnostics

Use compiler diagnostics to find invalid markup, broken references, or site conflicts before publishing. `Compiler` methods throw `CompilationException` when authoring errors prevent output. Each diagnostic includes a rule, message, suggested fix, and location.

## Read a diagnostic

```java
try {
  Compiler.compile(site);
} catch (CompilationException failure) {
  for (var diagnostic : failure.diagnostics()) {
    System.err.println(diagnostic.format());
  }
}
```

The exception message also contains the full report:

```text
Compilation failed with 1 error:
[interactive-label] Route "/settings", page "Settings" > toolbar[toolbar] > <button id="toolbar--save">: Interactive <button> element must have text content or an aria-label / aria-labelledby / title attribute
  Fix: Add visible label text, .ariaLabel(...), or .ariaLabelledBy(...) referencing a label.
```

| Field | Meaning |
| --- | --- |
| `rule()` | Code identifying the failed check |
| `message()` | What is wrong |
| `fix()` | How to correct it |
| `location()` | Route, page title, component path, element tag, optional DOM ID, and optional source line |

A standalone page has no route in its location. A site-level file conflict can have neither route nor page. Deferred content, head contributions, and document-end contributions retain the component path that registered them.

## Understand which errors are collected

The compiler collects independent authoring errors within each validation stage. It returns no HTML when a stage fails.

| Stage | Checks and ordering |
| --- | --- |
| Site paths | Duplicate routes and registered/reserved file collisions; failure stops page preparation |
| Page preparation | Identity, references, images, markup, accessibility, and URL syntax; site page failures are collected in route declaration order |
| Site validation | Cross-route targets and fragments, transition keys, and generated stylesheet/module path collisions; runs after every page prepares successfully |

Markup checks include `svg-namespace`: an SVG shape must sit inside an `svg` element, and HTML elements cannot sit inside one, because an HTML parser and a browser root would otherwise place the element in different namespaces. Deferred content is checked against the end of the body, where it is placed. `script-url` reports a `javascript:` URL in `href`, `xlink:href`, `src`, `action`, `formaction`, `poster`, or `data`, and an iframe `src` holding an inline `data:` document: portable markup runs no script of its own. HTML attribute names are stored in lowercase, so every check sees `HREF` as `href`. Browser roots apply the same shared checks before publishing a render.

Ambiguous duplicate bindings and unresolved references are excluded from target-dependent checks to avoid misleading secondary errors. Cross-route diagnostics retain the first location where an outgoing URL appears on a page. See [compiler internals](compiler.md) for the validation flow.

Exceptions thrown by application expansion, styling, late, or document-end callbacks propagate unchanged. A suppressed `CompilationException` adds component context. Trusted raw HTML remains opaque to validation.

## Accessibility rules

Use [typed roles and states](consumer-guide.md#declare-roles-and-states) to declare semantics. Role-specific checks apply to explicitly declared roles; native semantics are checked where the compiler recognizes them.

| Rule | Reported when |
| --- | --- |
| `role-name-required` | A role that needs an accessible name, or a native dialog, has none from its supported naming sources |
| `role-required-children` | A role such as `tablist`, `list`, `listbox`, `menu`, or `tree` lacks required owned roles |
| `role-required-attribute` | A declared role lacks required state, such as `aria-checked`, `aria-level`, `aria-valuenow`, or `aria-selected`; native hosts exposing that state are exempt |
| `aria-attribute-not-allowed` | An ARIA attribute is neither global nor supported by the declared role |
| `landmark-name-unique` | Repeated landmarks of one kind lack distinct names |
| `multiple-main` | A page contains more than one main landmark |
| `heading-level-skip` | Heading levels jump by more than one in document order |
| `empty-heading` | A native heading has no text |
| `tabindex-positive` | A positive tabindex overrides natural tab order |
| `label-target` | A label targets an element that cannot be labelled that way |
| `aria-hidden-focusable` | An ARIA-hidden subtree contains a control in the tab order |

Accessible names can come from ARIA labels, title attributes, native labels, or readable content, as allowed by the role. Declared heading roles use `role-name-required`; declared roles on links and buttons also use it in place of `interactive-label`.

Closed dialogs, popovers, and ARIA-hidden subtrees are excluded from landmark and heading structure checks. Compiler checks complement [browser accessibility verification](development.md#browser-verification).

## Trace elements to Java source

Enable `SourceTracking` around authoring and compilation to capture source locations:

```java
var page = SourceTracking.enabled(() -> Compiler.compile(
    new PageDocument("Home", Elements.main().child(Elements.h1().text("Home")))));
```

Tracked HTML carries attributes such as `data-src="Page.java:42"`; diagnostics print the source line too. Elements created inside a built-in component are attributed to the line that placed the component. Site compilation carries tracking onto its virtual threads.

`Compiler.explain(document)` returns a `PageTree` containing tags, IDs, attributes, enclosing components, CSS scopes, and sources. `toText()` renders that tree. For the showcase:

```sh
just demo
just explain /overlays/
```

`just demo` enables tracking for the preview. Stack capture adds work per element, so leave tracking disabled in production. `SourceTracking.enabledIf(flag, callback)` supports a development flag.
