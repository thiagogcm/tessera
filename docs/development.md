# Development

Build, test, verify, and contribute to Tessera. You need **JDK 25** and the repository's Maven wrapper (`./mvnw`). [Just](https://just.systems/) wraps all build and verification recipes; run `just` with no arguments to list them.

## Repository layout

```text
tessera/
|-- pom.xml                   Parent POM and shared quality configuration
|-- tessera/                  Portable authoring library, components, and shared view engine
|-- tessera-compiler/         Document authoring, static site compiler, and preview server
|-- tessera-showcase/         Static showcase examples and Playwright E2E suite
|-- tessera-wasm/             Opt-in GraalVM Web Image browser runtime (-Pwasm)
|-- tessera-wasm-showcase/    Web Image builds and real-image browser acceptance (-Pwasm)
|-- config/                   Checkstyle and SpotBugs rule configurations
|-- docs/                     User guides, architecture, and roadmap
|-- justfile                  Local and CI task definitions
`-- .github/workflows/ci.yml  GitHub Actions verification workflow
```

The default Maven reactor builds `tessera`, `tessera-compiler`, and `tessera-showcase`. Adding `-Pwasm` activates `tessera-wasm` and `tessera-wasm-showcase`; see [Browser rendering](browser-rendering.md#build-and-verify-the-showcase) for its separate toolchain prerequisites. Playwright is test-scoped in the showcases and never enters library dependencies.

## Build and verify commands

| Task | Command |
| --- | --- |
| Package jars without running tests | `just build` |
| Compile main and test sources | `just compile` |
| Run JVM unit tests (no browsers) | `just test` |
| Install Playwright Chromium and Firefox | `just browsers` |
| Install browsers and Linux OS packages | `just browsers --with-deps` |
| Run unit tests, browser E2E, coverage, and quality gates | `just verify` |
| Build static showcase site (`target/site`) | `just site` |
| Build consumer guide example (`target/guide`) | `just guide` |
| Build and serve static showcase at `http://localhost:8080/` | `just demo` |
| Print linked element tree for one showcase route | `just explain /overlays/` |
| Build Wasm showcase images and published site | `just wasm` |
| Build and verify Wasm showcase in Chromium and Firefox | `just wasm-verify` |
| Check for forbidden `@SuppressWarnings` annotations | `just no-suppress-warnings` |
| Check Java formatting | `just format-check` |
| Format Java source files in place | `just format` |
| Print local tool versions | `just doctor` |
| Remove Maven build outputs | `just clean` |

`just verify` executes `./mvnw -B -ntp clean verify`:

1. Surefire runs unit tests on the classpath during `test`, while module-boundary tests invoke `javac` on the module path to verify consumer access.
2. Failsafe runs Playwright browser integration tests (`*IT`) during `integration-test` and checks outcomes at `verify`.
3. JaCoCo records coverage reports, and Spotless, Checkstyle, and SpotBugs enforce formatting and static analysis.

Install Playwright browsers once per machine and whenever the Playwright version in [pom.xml](../pom.xml) changes.

## Quality checks

The parent [pom.xml](../pom.xml) enforces uniform checks across all five modules:

| Gate | Requirement |
| --- | --- |
| `javac` lint and doclint (`-Werror`) | Zero compiler or Javadoc warnings (missing Javadoc is allowed) |
| Error Prone and NullAway | Strict JSpecify nullability contracts (`OnlyNullMarked=true`, `JSpecifyMode=true`) |
| `@CheckReturnValue` | Rejects discarded derived values on immutable value types |
| Checkstyle | Enforces style rules, including `///` Markdown documentation comments |
| SpotBugs | Bytecode analysis at maximum effort and low threshold |
| Spotless | Google Java Format, sorted imports, and clean whitespace |
| Warning-suppression check | Zero `@SuppressWarnings` annotations anywhere under `*/src` |

Read existing tests before changing test fixtures or adding coverage. Focus tests on observable output and browser behavior rather than private internal structures.

## Browser verification

### Static showcase (`tessera-showcase`)

`PublishedSite` compiles the showcase once per JVM through `Compiler`, writes it via `SiteWriter`, and serves the directory over HTTP. Focused edge-case fixtures (such as right-to-left layouts or multiple search palettes) are served under `/fixtures/<name>/`.

Each `@BrowserTest` runs in both Chromium and Firefox unless pinned to one engine for an engine-specific feature (such as cross-document view transitions or `::scroll-button()`). Each test receives a fresh `Session` (1440x900 viewport) whose `open(path)` helper waits for custom elements to define and fails automatically on any uncaught page error, `console.error`, failed request, or same-origin HTTP error.

| Test class | Covered visitor behavior |
| --- | --- |
| `SiteSmokeIT` | Every route loads cleanly without overflow at 390px, 1440px, and 320px with WCAG text spacing |
| `AccessibilityIT` | axe-core WCAG A/AA audits in light mode, dark mode, open overlays, and forced colors |
| `NavigationIT` | Navigation, table of contents, pager, sticky header, footnotes, fragments, history, and view transitions |
| `SearchIT` | Query filtering, keyboard navigation, `Alt+K` shortcut, and independent search palettes |
| `OverlaysIT` | Modals, drawers, popovers, Pages menu, inspector, and tooltips |
| `DisclosureIT` | Tabs, accordions, and carousels, including RTL writing direction |
| `FormsIT` | Validation, switches, selects, growing textareas, and choice cards |
| `DataIT` | Scrolling and stacked card tables, code blocks, and `content-visibility` lists |
| `LayoutIT` | Grids, cards, prose, density scaling, and container queries |
| `MotionIT` | Reduced motion, scroll progress, viewport reveal animations, and spring transitions |

Inspect static test failures here:

- Failsafe reports: `tessera-showcase/target/failsafe-reports/`
- Failure traces and screenshots: `tessera-showcase/target/playwright/{chromium,firefox}/<class>/`
- CI artifact: `playwright-diagnostics`

### Wasm showcase (`tessera-wasm-showcase`)

Under `-Pwasm`, the suite publishes the built Wasm images through `PublishedWeb` and serves them with full deployment response headers (`Content-Security-Policy` and `Speculation-Rules`). Each engine launches once per JVM with a fresh page per test, failing on any undeclared page error, `console.error`, or CSP violation.

Engine-dependent behavior (typing, IME composition, focus, dialogs, event defaults, media, CSP, and bundle loading) runs in both Chromium and Firefox; pure Java runtime logic (scheduling, render rejection, effect lifecycles, ID ownership, and failure reporting) runs in Chromium. Timers use Playwright's clock instead of sleeps. Failsafe reports live under `tessera-wasm-showcase/target/failsafe-reports/`.

## Change policy

- Update implementation, examples, tests, and documentation in the same change whenever public behavior changes.
- Replace superseded APIs directly; do not leave compatibility shims, deprecated aliases, or parallel legacy paths.
- Use Conventional Commits for commit messages.

## Maintain documentation

Write documentation in plain language following [ISO 24495-1:2023](https://www.iso.org/standard/78907.html) (Relevant, Findable, Understandable, Usable):

- Give every file a clear audience and purpose, with task-oriented headings.
- Keep one focused subject per paragraph; use active voice, plain words, and concise sentences.
- Maintain a single source of truth per contract; cross-link instead of duplicating explanations.
- Balance prose with tables for exact mappings, numbered steps for ordered procedures, bullets for parallel items, and Mermaid diagrams chosen for the concept being explained.
- Keep all guides forward-looking and current; keep change history in Git, not in docs.
- Never use em-dashes or en-dashes as prose punctuation. Format Markdown files with Prettier.
- Verify all links, code examples, commands, and Mermaid syntax before committing.

Choose Mermaid diagram types to match the relationship you are illustrating:

| Question                                        | Diagram type      |
| ----------------------------------------------- | ----------------- |
| How do types, interfaces, or modules relate?    | `classDiagram`    |
| How do entities and generated artifacts map?    | `erDiagram`       |
| How do participants interact step by step?      | `sequenceDiagram` |
| What states and transitions can an owner enter? | `stateDiagram-v2` |
| How do data stages or precedence layers flow?   | `flowchart`       |
