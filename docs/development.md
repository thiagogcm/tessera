# Development

Use JDK 25 and the Maven wrapper. [Just](https://just.systems/) lists and runs the repository tasks; run `just` without arguments to see them all.

## Repository layout

```text
tessera/
|-- pom.xml                   Parent and shared build configuration
|-- tessera/                  Library and unit tests
|-- tessera-showcase/         Public API examples and browser tests
|-- tessera-wasm/             Opt-in Java browser runtime
|-- tessera-wasm-showcase/    Web Image builds and browser acceptance
|-- config/                   Checkstyle and SpotBugs policies
|-- docs/                     Guides, architecture, and planned work
|-- justfile                  Local and CI commands
`-- .github/workflows/ci.yml  Verification workflow
```

The default reactor builds the library and static showcase. The `wasm` profile adds the browser runtime and its showcase; [Browser rendering](browser-rendering.md#build-and-verify) lists the separate prerequisites and commands. The static showcase depends on the library's public API. Playwright is test-scoped there and is excluded from library dependencies. Package responsibilities are defined in [Architecture](architecture.md#package-boundaries).

## Build and verify

| Task | Command |
| --- | --- |
| Package without running tests | `just build` |
| Compile main and test sources | `just compile` |
| Run unit tests | `just test` |
| Install both browser engines | `just browsers` |
| Install browsers and Linux system dependencies | `just browsers --with-deps` |
| Run unit tests, browser tests, and quality checks | `just verify` |
| Check for forbidden warning suppressions | `just no-suppress-warnings` |
| Check Java formatting | `just format-check` |
| Apply Java formatting | `just format` |
| Print tool versions | `just doctor` |
| Remove build output | `just clean` |

`just verify` runs `./mvnw -B -ntp clean verify`. Surefire runs unit tests on the classpath during `test`. The placement tests invoke javac on the module path to verify consumer access. Failsafe runs browser integration tests during `integration-test` and checks their results at `verify`. Install browsers once and after Playwright upgrades.

CI also checks warning suppressions and builds both example sites. Run `just site` for the showcase and `just guide` for the smaller example. [Getting started](getting-started.md) covers previewing their files.

## Quality checks

The parent POM configures the checks for both modules. Use it as the source for tool versions and compiler flags.

| Check | Purpose |
| --- | --- |
| javac lint and doclint with warnings as errors | Reject compiler and documentation warnings, except missing documentation |
| Error Prone and NullAway | Check Java usage and JSpecify nullability contracts |
| `@CheckReturnValue` | Catch discarded derived values from immutable API types |
| Checkstyle | Enforce source conventions, including `///` Markdown documentation comments |
| SpotBugs | Analyze bytecode at maximum effort and low threshold |
| Spotless | Check Java formatting and imports |
| Warning-suppression scan | Reject `@SuppressWarnings` annotations in source |

Read related tests before changing test setup or adding coverage. Prefer scenarios that expose incorrect output or behavior. Keep shared setup and checks focused so the suite remains useful and efficient.

## Browser verification

The showcase suite compiles through `Compiler`, writes through `SiteWriter`, and serves the resulting files over HTTP. It reuses a browser per test class and creates a fresh context per test.

| Coverage | Checks |
| --- | --- |
| Documentation routes and component cases | Desktop/narrow layouts, headings, CSS, images, HTTP failures, horizontal overflow |
| Interactive components | Keyboard flows, focus, and native control behavior |
| Accessibility | axe-core WCAG A/AA audits, forced colors, reduced motion, 200% zoom, and text spacing |

Component cases use `/stories/{case}/{variant}/{appearance}/{density}/`. Appearance is `light` or `dark`; density is `regular` or `compact`. For example, `/stories/modal/open/dark/compact/` shows an open modal with compact spacing. Window width sets the viewport; the compiled case declares its initial state.

Inspect failures in these locations:

| Output | Path |
| --- | --- |
| Failsafe reports | `tessera-showcase/target/failsafe-reports/` |
| Traces and failure screenshots | `tessera-showcase/target/playwright/{chromium,firefox}/` |
| CI diagnostics artifact | `playwright-diagnostics` |

See [Browser support](browser-support.md) for the supported engines and the distinction between the support target and pinned test builds.

## Change the API

During development, update the implementation, examples, tests, and docs together when changing contracts. Keep proposed APIs in the [browser architecture proposal](architecture-redesign.md), with their acceptance criteria in the [roadmap](roadmap.md). Current usage guides should show APIs present in the checkout and identify their backend and profile requirements.

Use Conventional Commits for commit messages.

## Maintain the documentation

Organize documentation around what a reader needs to accomplish. Apply the relevance, findability, understanding, and usability principles in [ISO 24495-1:2023](https://www.iso.org/standard/78907.html).

- Give each file a clear audience and purpose. Keep its introduction and headings specific to the task.
- Explain one subject per paragraph. Prefer familiar words and direct instructions; define necessary technical terms.
- Keep each contract in one primary location and link to it from related guides.
- Use tables for exact mappings, numbered steps for ordered tasks, and bullets for parallel items.
- Use diagrams when relationships, interactions, or states are easier to understand visually. Keep the surrounding text useful without the diagram.
- Describe current behavior or intended work directly. Keep change history out of guides and proposals.
- Avoid em-dashes. Format edited Markdown files with Prettier.
- Check links, examples, commands, and diagram syntax. Ask representative readers to complete the documented tasks and use their feedback to revise the text.

Choose Mermaid types by the question being explained:

| Question | Diagram type and reference |
| --- | --- |
| Which types implement or depend on each other? | [Class diagram](https://mermaid.js.org/syntax/classDiagram.html) |
| Which participants interact, and in what order? | [Sequence diagram](https://mermaid.js.org/syntax/sequenceDiagram.html) |
| Which states and rejection paths are possible? | [State diagram](https://mermaid.js.org/syntax/stateDiagram.html) |
| Which processing steps depend on one another? | [Flowchart](https://mermaid.js.org/syntax/flowchart.html) |

Use syntax supported by the documentation renderer. Mermaid's [syntax reference](https://mermaid.js.org/intro/syntax-reference) describes additional types; diagram variety should serve the explanation.
