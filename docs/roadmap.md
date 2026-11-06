# Roadmap

Improve the Java authoring experience while compiling to semantic HTML and modern CSS. Work follows the package boundaries and direct API evolution policy in the [architecture guide](architecture.md). The APIs described here are proposed capabilities, not settled public identifiers.

## Implementation sequence

| Order | Item | Priority | Prerequisites |
| --- | --- | --- | --- |
| 1 | Reusable component stories | Medium | `Button`, `IconButton`, and theme variants |
| 2 | Compilation inspection | Medium | Existing compiler passes and component origins |

Compilation inspection can proceed independently; the other items follow the sequence above. Row and column layout is `FlexLayout`, applied with `Styled.layout`. Typography, motion, and semantic roles are `Typography`, `Motion`, and `Roles`. Native buttons are `Button` and `IconButton`.

## 1. Reusable component stories

Evolve the showcase's `Demo` model into named component cases with variant parameters and a fresh content factory. A shared catalog drives both the rendered documentation and browser fixtures. Each case identifies the component state being demonstrated, such as a disabled action, an open overlay, or a compact control.

Keep the catalog and browser integration in `tessera-showcase`. Reuse component factories instead of duplicating markup in tests, and create fresh element builders for each case compilation. Named cases can be opened directly at a selected viewport, appearance, and density. Extend the existing Playwright interactions to use these cases and select combinations that exercise distinct behavior.

**Verification criteria**

- A named case and variant opened directly show the same component setup used by its browser interaction test.
- Opening a dialog, changing a control, and then opening another case starts that case in its declared state. Reopening the original case starts it fresh.
- Representative narrow and wide cases remain usable, including keyboard access to overflowed content and text wrapping without unintended horizontal document scrolling.

## 2. Compilation inspection

Provide an opt-in, read-only report for successful page and site compilation. Connect component paths and scoped DOM IDs to generated classes, declared CSS rules, cascade layers, and scope boundaries. Report UTF-8 output sizes so consumers can identify large documents and stylesheets.

The existing `render` passes collect provenance when inspection is requested and publish separate report values. Several component origins can reference one interned style rule. `site` adds route attribution and identifies the final shared stylesheet; output adapters present the report as JSON or readable text. The intermediate tree and mutable compilation state remain private. Computed layout and the effective browser cascade are inspected through browser tooling.

**Verification criteria**

- Two scoped component instances sharing a style rule appear with distinct origins and DOM identities, and both report the class and rule present in the published HTML and stylesheet.
- Content registered for the head, emitted late, or deferred to the end of the body reports its originating component path and the route where it appears.
- A site report points to the published shared stylesheet and identifies the relevant scope and declarations for a component whose appearance is being investigated.
- Output sizes match the written UTF-8 artifacts for a page containing non-ASCII text, so character counts cannot understate the reported byte sizes.
