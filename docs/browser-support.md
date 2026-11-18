# Browser support

Tessera targets current stable Chromium and Firefox. Use JDK 25 to compile and run the Java library. The generated site uses modern HTML and CSS; compatibility with older engines and other browser families is outside the support contract.

## Platform features

Native browser behavior provides dialogs, popovers, focus handling, layout, and navigation. Tessera emits features such as invoker commands, light-dismiss dialogs, cascade layers, scoped CSS, OKLCH colors, subgrid, anchor positioning, and cross-document transitions.

Some visual enhancements depend on engine support:

| Feature | Behavior when the enhancement is unavailable |
| --- | --- |
| Carousel scroll buttons and markers | Browser-native scrolling remains available |
| `appearance: base-select` | The browser's native select presentation applies |
| Scroll-state container queries | Rules within unsupported queries do not apply |
| Interest-invoker tooltips | The bundled tooltip module supplies Firefox behavior |

Check the rendered result in both supported engines when relying on these features. The [verification suite](development.md#browser-verification) exercises generated pages, responsive layout, interactions, and accessibility.

## Component scripts

Built-in portable widgets declare their bundled module, and static output contributes it; document components contribute other modules through `DocumentContext`. A page without script contributions receives no component script. Modules are deduplicated per page and packaged according to the [output mode](sites.md#choose-an-output-mode). Browser roots load no component modules. The [adaptive widget adapters](browser-rendering.md#adapt-portable-widgets) provide interactions in browser roots while sharing the portable markup and styles.

| Component | Module responsibility |
| --- | --- |
| `ContentTabs` | Select panels through `aria-controls`; move focus with Arrow Left, Arrow Right, Home, and End |
| `SearchPalette` | Filter rendered result links and manage result selection |
| Interest tooltip | Supply hover and focus handling in Firefox; Chromium uses the native interest invoker |

The search module defines a customized native dialog. The tabs module defines an autonomous `tessera-tabs` element. Search listeners use an `AbortSignal` and are released when the component disconnects. Native dialog controls handle opening, focus trapping, and dismissal.

Custom components can [contribute their own scripts](consumer-guide.md#contribute-a-page-script).

## Verification boundary

CI installs the Chromium and Firefox builds bundled with the Playwright version pinned in [pom.xml](../pom.xml). These are the tested builds for that dependency version. Keeping Playwright current and checking stable-browser releases is part of maintaining the support contract; a green run does not establish compatibility with every installed browser version.

The opt-in `wasm` profile adds the experimental [Java browser backend](browser-rendering.md) and its acceptance showcase. Its packaged-image suite runs separately from the default JVM CI workflow. The [architecture proposal](architecture-redesign.md) describes the next capability and module boundaries.
