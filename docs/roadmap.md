# Roadmap

Improve the Java authoring experience while compiling to semantic HTML and modern CSS. Work follows the package boundaries and direct API evolution policy in the [architecture guide](architecture.md). The APIs described here are proposed capabilities, not settled public identifiers.

## Implementation sequence

| Order | Item | Priority | Prerequisites |
| --- | --- | --- | --- |
| 1 | Complete layout recipes | High | Existing styling and CSS value APIs |
| 2 | Typed design values and semantic themes | High | Existing token registry and stylesheet collection |
| 3 | Native button components | High | Layout recipes and typed design values |
| 4 | Reusable component stories | Medium | Native button components and theme variants |
| 5 | Compilation inspection | Medium | Existing compiler passes and component origins |

Compilation inspection can proceed independently; the other items follow the sequence above.

## 1. Complete layout recipes

Provide composable layout values that configure a working row or column in one operation, including flex display, direction, and the chosen gap, alignment, and wrapping. Block and inline flex layouts are explicit choices. A recipe applies to an authored semantic element, so choosing a layout does not require replacing a section, navigation landmark, or form with a generic container.

Layout values belong in `css` and contribute declarations through `Styled` and `StyleRefinement`. They retain `CssLength` expressions, logical dimensions, and CSS custom property references. Applying a recipe and refining individual declarations use the existing cascade and declaration override rules. Explicit sizing and shrink choices remain available for nested scrolling layouts.

**Verification criteria**

- A section containing two children renders as the requested row or column with visible spacing, using only the complete layout operation; the section retains its HTML semantics.
- An inline recipe stays alongside neighboring inline content. A nested, explicitly height-constrained column lets its inner panel scroll instead of expanding past its container.
- Refining a recipe's gap for one element changes that element's spacing while other elements using the same recipe retain their original layout.

## 2. Typed design values and semantic themes

Introduce immutable Java values for typography and motion. Typography combines font family, size, weight, line height, and tracking. Motion combines duration, easing, and a reduced-motion policy for a named purpose such as control feedback, overlay entry, or disclosure. These values apply to elements and scoped rules through the same declaration pipeline.

Give tokens typed categories for the values consumed by these APIs, preserving CSS variable references through serialization. Generic value types and token emission belong in `css`; component-specific semantic roles belong in `components`. Theme data supplies the values for those roles through the token registry. Typography, color, spacing, and motion choices stay independently configurable, including light/dark appearance and density.

**Verification criteria**

- Two independently authored components using the same semantic typography and color roles reflect a theme override in both light and dark appearance, with the intended font metrics and readable content.
- A compact density changes control spacing without changing the typography scale unless the theme explicitly changes it.
- An overlay or control using a motion preset reaches its intended visible state under reduced motion, with spatial travel reduced and keyboard focus still apparent.

## 3. Native button components

Add `Button` and `IconButton` as ordinary components accepted by `IntoElement` composition. Both emit native buttons. Semantic tone, surface variant, control size, disabled state, and busy feedback are separate choices. Form submission and invoker commands describe behavior independently of appearance; navigation continues to use anchors.

An icon button requires an accessible name alongside its visual content. Custom button content retains an intentional accessible name. Components coordinate native attributes, focus styling, semantic tokens, and `ElementRef` command targets. CSS pseudo-classes and browser controls provide interaction behavior, with Java callbacks used for authoring-time composition.

**Verification criteria**

- Pointer and keyboard activation open a referenced native dialog or popover, including when the target is emitted as deferred content. Disabled buttons cannot activate it.
- A submit button participates in native form submission, while an ordinary action button inside the same form does not submit it. Changing the visual variant does not change either behavior.
- An icon-only button has the supplied accessible name and a visible keyboard focus indicator. Decorative visual content does not repeat that name to assistive technology.
- Busy feedback is conveyed to assistive technology and can be combined with an explicit disabled choice without losing the button's name.

## 4. Reusable component stories

Evolve the showcase's `Demo` model into named component cases with variant parameters and a fresh content factory. A shared catalog drives both the rendered documentation and browser fixtures. Each case identifies the component state being demonstrated, such as a disabled action, an open overlay, or a compact control.

Keep the catalog and browser integration in `tessera-showcase`. Reuse component factories instead of duplicating markup in tests, and create fresh element builders for each case compilation. Named cases can be opened directly at a selected viewport, appearance, and density. Extend the existing Playwright interactions to use these cases and select combinations that exercise distinct behavior.

**Verification criteria**

- A named case and variant opened directly show the same component setup used by its browser interaction test.
- Opening a dialog, changing a control, and then opening another case starts that case in its declared state. Reopening the original case starts it fresh.
- Representative narrow and wide cases remain usable, including keyboard access to overflowed content and text wrapping without unintended horizontal document scrolling.

## 5. Compilation inspection

Provide an opt-in, read-only report for successful page and site compilation. Connect component paths and scoped DOM IDs to generated classes, declared CSS rules, cascade layers, and scope boundaries. Report UTF-8 output sizes so consumers can identify large documents and stylesheets.

The existing `render` passes collect provenance when inspection is requested and publish separate report values. Several component origins can reference one interned style rule. `site` adds route attribution and identifies the final shared stylesheet; output adapters present the report as JSON or readable text. The intermediate tree and mutable compilation state remain private. Computed layout and the effective browser cascade are inspected through browser tooling.

**Verification criteria**

- Two scoped component instances sharing a style rule appear with distinct origins and DOM identities, and both report the class and rule present in the published HTML and stylesheet.
- Content registered for the head, emitted late, or deferred to the end of the body reports its originating component path and the route where it appears.
- A site report points to the published shared stylesheet and identifies the relevant scope and declarations for a component whose appearance is being investigated.
- Output sizes match the written UTF-8 artifacts for a page containing non-ASCII text, so character counts cannot understate the reported byte sizes.
