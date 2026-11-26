# Browser support

Tessera requires **JDK 25** and targets current stable **Chromium and Firefox**. Generated pages use modern HTML and CSS features; compatibility with older engines or other browser families is outside the support contract.

## Platform features

Built-in components rely on native browser capabilities for dialogs, popovers, focus management, layout, and navigation, including invoker commands, light-dismiss dialogs, CSS cascade layers, `@scope`, OKLCH colors, subgrid, anchor positioning, and cross-document view transitions.

Where an enhancement is not yet available in an engine, output degrades to usable native behavior:

| Feature | Fallback when unsupported |
| --- | --- |
| Carousel scroll buttons and markers | Native horizontal scrolling remains usable |
| `appearance: base-select` | Native `<select>` presentation applies |
| Scroll-state container queries | Rules inside unsupported queries are skipped; sticky headers keep their default surface |
| `:target-current` | Table of contents remains navigable without active-section highlighting |
| Cross-document view transitions | Navigation succeeds without cross-fade or morph animation |
| Scroll-triggered animations (`animation-trigger`) | Reveal-once tiles render immediately without entry motion |
| Interest-invoker tooltips | Bundled tooltip script shows and anchors tooltips on hover and focus |

At the Playwright browser builds pinned in [pom.xml](../pom.xml), Firefox lacks the first six features above, and both Chromium and Firefox lack native interest invokers. Scroll-driven animations, `field-sizing`, `sibling-index()`, anchor positioning, `@starting-style`, invoker commands, `linear()` easings (compiled springs), and `content-visibility: auto` work in both engines.

Verify pages that rely on progressive enhancements in both Chromium and Firefox using the [browser verification suite](development.md#browser-verification).

## Component scripts

Static pages load JavaScript only when a page includes a component that requires a script. Built-in portable widgets declare their bundled ES modules automatically during static compilation; custom components can register additional scripts via [`DocumentContext.contributeScript`](documents.md#contribute-a-page-script).

Modules are deduplicated once per page and packaged according to the page's [output mode](sites.md#choose-an-output-mode). Browser roots do not load static component modules; use [adaptive widget adapters](browser-rendering.md#adapt-portable-widgets) inside live views instead.

| Component | Bundled ES module | Responsibility |
| --- | --- | --- |
| `ContentTabs` | `tessera-tabs.js` (`<tessera-tabs>`) | Sync panel selection through `aria-controls`; manage Arrow Left, Arrow Right, Home, and End focus |
| `SearchPalette` | `search-palette.js` (customized `<dialog>`) | Filter rendered links, move result focus, and open the first palette whose trigger advertises the pressed shortcut |
| `ColorSchemeToggle` | `color-scheme.js` (`<tessera-color-scheme>`) | Cycle the root's `data-color-scheme` through system, light, and dark; save the choice in `localStorage` and follow other tabs |
| `Dropdown` with command items | `tessera-menu.js` (`<tessera-menu>`) | Return focus to the menu button after a dialog opened from the menu closes |
| Elements with `interestTooltip` | `interest-tooltip.js` | Show and anchor tooltips on hover and focus where native interest invokers are absent |

Host-bound modules (tabs, menu, search, and color scheme) attach their listeners with an `AbortSignal` and clean up when their host disconnects. The tooltip module listens on the document for the page's lifetime. Native `<dialog>` behavior handles opening, focus trapping, and dismissal.

## Verification boundary

CI runs against the Chromium and Firefox builds bundled with the Playwright version pinned in [pom.xml](../pom.xml). Updating Playwright and re-checking stable browser releases is part of maintaining the support contract; a green run verifies the pinned builds rather than every installed browser version.

The opt-in `wasm` Maven profile builds and tests the [experimental browser rendering backend](browser-rendering.md) separately from the default JVM CI workflow.
