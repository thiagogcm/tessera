# Building a site with Tessera

Tessera compiles Java components into HTML, CSS, and static assets. This guide builds a two-page site, customizes its appearance, adds a scoped component, and publishes it with interactive search.

The complete runnable example is [GuideSite.java](../tessera-showcase/src/main/java/dev/nthings/tessera/showcase/guide/GuideSite.java). It lives in the showcase module and uses only public library APIs.

## Run the example

Install JDK 25, then run these commands from the repository root:

```sh
just guide
jwebserver -d target/guide -p 8081
```

Open `http://localhost:8081/`. Follow the installation link, then open the search palette and type `publishing`. Enter opens the matching section. The generated files can be uploaded to a static host.

Without Just, the generation command is:

```sh
./mvnw compile exec:java \
  -Dexec.mainClass=dev.nthings.tessera.showcase.guide.GuideSite \
  -Dexec.args="$PWD/target/guide"
```

For a separate Maven application, first install the library and parent locally:

```sh
./mvnw -pl tessera -am install
```

Add the following dependency to your application's POM and set `maven.compiler.release` to `25`:

```xml
<dependency>
    <groupId>dev.nthings</groupId>
    <artifactId>tessera</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Define pages and publish files

The example assembles `SiteRoute` values into a `SiteDefinition`:

```java
var site = SiteDefinition.builder()
    .route(new SiteRoute("/", new PageDocument("Home", Elements.main().child(h1().text("Home")))))
    .route(new SiteRoute("/guide", new PageDocument("Guide", Elements.main().text("Start here"))))
    .build();

var compiled = SiteCompiler.compile(site);
SiteWriter.writeToDirectory(compiled, Path.of("target/site"));
```

`PageDocument` accepts an optional language before the title; the two-argument constructor uses English. The default site output shares a content-hashed external stylesheet across all pages.

| Route         | Public document URL | Generated file     |
| ------------- | ------------------- | ------------------ |
| `/`           | `/`                 | `index.html`       |
| `/guide`      | `/guide/`           | `guide/index.html` |
| `/about.html` | `/about.html`       | `about.html`       |

Use `route.documentUri()` when you need the canonical public URL for a route. Preview serving redirects directory routes to their trailing-slash URL, preserving the query string, so relative links resolve the same way as on a static host.

## Compose reusable components

Use `RenderOnce` for composition without adding a scope:

```java
RenderOnce notice = context -> div().child(p().text("Build completed"));
```

Use `RenderView` for a reusable component with an identity namespace and isolated CSS. The runnable example includes this component:

```java
public record MetricCard(String id, String title, String value) implements RenderView {
  @Override
  public ElementId entityId() {
    return ElementId.of(id);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope.root(s -> s
        .p(Tokens.SPACE_5)
        .rounded(Tokens.RADIUS_XL)
        .bg(Tokens.SURFACE_CARD)
        .borderSubtle(Tokens.BORDER_SUBTLE));
  }

  @Override
  public Element render(RenderContext context) {
    return div().children(h2().text(title), p().text(value));
  }
}
```

Pass it to `.child(...)` or `.children(...)` like any other component. The compiler establishes the view's namespace before calling its render method. Repeated instances need distinct instance IDs within the same enclosing scope. Omitting `entityId()` lets the compiler assign deterministic IDs.

The compiler marks the view's root element with `data-view` and `data-view-id`, which scope its styles. A view whose `render(context)` returns a fragment or text has no single root to mark, so the compiler wraps it in a `<div>`. Return one element when the parent's content model matters.

Both callbacks receive an explicit `RenderContext` for that turn. Do not retain it for later async work. Keep render callbacks free of shared mutable application state: site compilation expands different pages concurrently.

## Group siblings and name custom elements

Use `Elements.fragment()` when a component owns several roots or when a wrapper would change the parent content model. The fragment emits no markup:

```java
Elements.ul().child(
    Elements.fragment().children(li().text("One"), li().text("Two")));
```

`HtmlTag` covers the built-in vocabulary, including `video`, `audio`, `iframe`, `canvas`, `template`, `slot`, `picture`, `source`, and `track`. `Elements.custom("my-widget")` emits an autonomous custom element. Custom element names must start with a lowercase letter, contain a hyphen, and avoid the names reserved by the HTML standard. Pair the element with a `ClientBehavior` so the browser upgrades it.

## Contribute a client behavior

A behavior is an ES module with a stable id. Components contribute it through the render context:

```java
var behavior = new ClientBehavior("my-widget", moduleSource);
context.contributeBehavior(behavior);
```

The compiler emits each behavior once per page. `OutputMode.inlineStyle()` inlines a `<script type="module">`. `OutputMode.externalAsset("/assets")` publishes a content-hashed module, records it in `CompiledPage.assets()`, and `SiteCompiler` merges it into `CompiledSite.files()` so `SiteWriter` writes it. Behaviors keep browser code out of the component tree and avoid a component-to-site dependency. `SearchPalette` and `Tabs.ContentTabs` use this path for their bundled modules.

## Lay out a row or column

`FlexLayout` sets display, direction, gap, alignment, and wrapping on the element that should be the flex container. The element keeps its own tag.

```java
var links = FlexLayout.row(Tokens.SPACE_4, FlexLayout.Align.CENTER, FlexLayout.Wrap.WRAP);
var stack = FlexLayout.column(Tokens.SPACE_4, FlexLayout.Align.STRETCH, FlexLayout.Wrap.NOWRAP);

Elements.section().layout(stack).children(
    Elements.nav().layout(links).ariaLabel("Site navigation")
        .children(a("/").text("Home"), a("/guide/").text("Guide")),
    p().text("Introduction"));
```

`inlineRow` and `inlineColumn` use `inline-flex`, so the container stays on the surrounding line. A later `gap(...)`, `blockSize(...)`, `minBlockSize(...)`, or `flexShrink(...)` replaces that one declaration on the element. The shared `FlexLayout` value is unchanged. Lengths accept `CssLength` values, including `DesignToken.Length` tokens.

A scrolling region inside a fixed-height column is a property of the inner panel: give the column a block size, and give the panel `flexGrow(1)`, `flexShrink(1)`, `minBlockSize(CssLength.px(0))`, and `overflowYAuto()`.

## Link elements with identity references

Bind an `ElementRef` where an element is emitted and use the same reference at its invoker. The compiler resolves both under the binding's view scope:

```java
var target = ElementRef.named("help");
var trigger = button().command(target, BuiltinCommand.SHOW_MODAL).text("Help");
var dialog = Elements.dialog().id(target).ariaLabel("Help").text("Contact support");
var body = Elements.main().children(trigger, dialog);
```

The reference is an identity token; another `ElementRef.named("help")` is a different token. An unbound reference fails compilation. Higher-level components such as `Overlays.modal(...)` expose their own trigger helpers to manage these references.

For document navigation, the example uses `Toc.section(...)`. It registers heading metadata, emits a target ID, and allows `Toc.fromContext(...)` to appear before the sections it lists. Custom components can call `context.registerHeading(localId, title, level)` inside their render callback and emit the returned `targetDomId` themselves.

## Add a button

`Button` and `IconButton` emit a native `<button>`. Tone, surface, and size are separate from what the button does. An ordinary button is `type="button"`. `submit()` is `type="submit"`. `command(...)` targets a dialog or popover, including one emitted later in the document: `Overlays.Modal.dialogRef()`, `Overlays.Drawer.dialogRef()` and `Dropdown.panelRef()` expose those targets. Navigation stays on anchors.

```java
var confirm = Overlays.modal("confirm", "Confirm");
var open = Button.of("Review").command(confirm.dialogRef(), BuiltinCommand.SHOW_MODAL);
var save = Button.of("Save").submit().variant(Button.Variant.OUTLINE);
var close = IconButton.of("Close", Elements.span().text("×"))
    .command(confirm.dialogRef(), BuiltinCommand.CLOSE);
```

`IconButton` takes the accessible name beside the icon and marks the icon as decorative. `Button.of(name, content)` keeps `name` as the accessible name when the visible content differs. `disabled()` and `busy()` combine freely. The name remains, and keyboard focus uses `:focus-visible` outside reduced-motion styling.

`Button.interestTooltip(...)` adds a hint popover on hover or focus. Native interest invokers drive it where available; a bundled `TooltipBehavior` mirrors hover and focus handling in Firefox, which does not ship the API yet. `Overlays.modal(...).opened()` renders a dialog visible in the non-modal `open` state.

## Open a component case

The showcase publishes each component case as its own page:

`/stories/{case}/{variant}/{appearance}/{density}/`

Appearance is `light` or `dark`. Density is `regular` or `compact` (`--density: 0.8`). The viewport is the browser window, so a narrow or wide window shows the same compiled page. The page builds the case when it renders, so opening it again starts from the declared state.

`/stories/modal/closed/light/regular/` is the closed publish dialog used by the overlays page. `/stories/modal/open/dark/compact/` shows that dialog open, in the dark scheme, at compact density. `/stories/actions/disabled/light/regular/` is the disabled action.

## Write links as browser URLs

Site compilation resolves local links from each page's published document URL. It validates relative and root-relative `href`, `src`, and `poster` targets. Queries are preserved in output and excluded from route/asset lookup. Percent-encoded paths and fragments are decoded for comparison with registered paths and DOM IDs. A `+` in a fragment remains a plus sign.

| Source route | Link | Validated destination |
| --- | --- | --- |
| `/guides/start` | `../install?mode=full#requirements` | `/guides/install`, ID `requirements` |
| `/guides/start.html` | `?mode=full#requirements` | Same HTML file, ID `requirements` |
| `/` | `/guides/caf%C3%A9/#caf%C3%A9%2Bintro` | `/guides/café`, ID `café+intro` |
| `/guide` | `../assets/manual%20v1.pdf?download=1` | Asset `/assets/manual v1.pdf` |

Route and asset definitions use their logical paths, such as `/guides/café`. Links use URL escaping, such as `/guides/caf%C3%A9/`. Explicit `index.html` links are accepted as aliases of generated directory pages. Absolute and protocol-relative URLs are external to local validation.

## Read compilation diagnostics

`PageCompiler.compile(...)`, `PageCompiler.prepare(...)`, and `SiteCompiler.compile(...)` throw `CompilationException` with a `diagnostics()` list when authoring errors prevent compilation. Each `CompilationDiagnostic` contains a rule code, a `CompilationLocation`, an error message, and a suggested correction. The location identifies the page title and route (for sites); its `view()` holds the enclosing component path and separate `tagName` and nullable `domId` fields. `CompilationDiagnostic.format()` presents a diagnostic as the exception prints it. An exception thrown by a component's render or style callback, or by a late or document-end callback, propagates unchanged, with a suppressed `CompilationException` naming the component path.

```java
try {
  SiteCompiler.compile(site);
} catch (CompilationException failure) {
  for (var diagnostic : failure.diagnostics()) {
    System.err.println(diagnostic.rule() + ": " + diagnostic.message());
    System.err.println(diagnostic.location());
    System.err.println(diagnostic.suggestion());
  }
}
```

With source tracking on (see below), the location line also names the element's Java source line, for example `<button id="toolbar--save"> (Settings.java:42)`. The exception message also presents the full report for command-line builds:

```text
Compilation failed with 1 error:
[interactive-label] Route "/settings", page "Settings" > toolbar[toolbar] > <button id="toolbar--save">: Interactive <button> element must have text content or an aria-label / aria-labelledby / title attribute
  Fix: Add visible label text, .ariaLabel(...), or .ariaLabelledBy(...) referencing a label.
```

Independent image, identity, reference, URL syntax, and markup errors are collected before a page fails. Late and deferred elements retain their enclosing component path. Site compilation collects page errors in route declaration order; once all pages prepare successfully, it collects cross-route link and transition-key errors together. Cross-route diagnostics identify the first use of each outgoing URL on a page. Head and end-section contributions retain the component path from their registration, including when they expand further components.

Native dialog commands (`show-modal`, `close`, `request-close`) require a `<dialog>` target. Popover commands (`toggle-popover`, `show-popover`, `hide-popover`) require a target with a `popover` attribute, following the [HTML command rules](https://html.spec.whatwg.org/multipage/form-elements.html#attr-button-command). Custom commands continue to use any existing target.

Duplicate bindings are reported as ambiguous; target-dependent command checks are skipped for ambiguous targets. Malformed URLs produce an `invalid-url` diagnostic alongside other page errors. The compiler reports authoring errors without emitting HTML. Exceptions thrown by application render callbacks still propagate normally, and trusted raw HTML remains outside validation.

## Customize the design system

Components reference shared CSS custom properties. Replace a token by registering a new value for it. `withValue` and `withLightDark` keep the token's name and category and take a value of that category, so a length cannot replace a color:

```java
var tokens = new Tokens()
    .register(Tokens.FONT_BODY.withValue(FontStack.of(FontStack.named("Georgia"), FontStack.Generic.SERIF)))
    .register(Tokens.DENSITY.withValue(0.8))
    .register(Tokens.RADIUS_XL.withValue(CssLength.rem(1)))
    .register(Tokens.CONTROL_FG.withLightDark(CssColor.oklch(20, 0.02, 150), CssColor.oklch(96, 0.01, 150)))
    .register(Tokens.CONTROL_FONT_SIZE.withValue(CssLength.rem(1)));

var site = SiteDefinition.builder().tokens(tokens).routes(routes).build();
```

`Typography` sets family, size, weight, line height, and tracking together. `Roles.LABEL` is the control label. `Tokens.CONTROL_FG` is the control text color, defined as `var(--text-primary)` until a theme replaces `--control-fg`. `Motion.controlFeedback()`, `Motion.overlayEntry()`, and `Motion.disclosure()` set duration, easing, and the transitioned properties. Apply a preset that moves an element inside `motionSafe`, and leave its visible end state outside that query.

| Family | Public tokens | Defaults |
| --- | --- | --- |
| Fonts | `FONT_BODY`, `FONT_MONO` | System UI and system monospace stacks |
| Type scale | `FONT_SIZE_XS`, `FONT_SIZE_SM`, `FONT_SIZE_MD`, `FONT_SIZE_LG`, `FONT_SIZE_XL`, `FONT_SIZE_2XL` | 0.75, 0.875, 1, 1.125, 1.25, 1.5 rem |
| Line height | `LINE_HEIGHT_BODY` | 1.5 |
| Tracking | `TRACKING_NORMAL` | 0 |
| Spacing | `SPACE_1` through `SPACE_6`, `SPACE_8`, `SPACE_10`, `SPACE_12`; half steps `SPACE_0_5`, `SPACE_1_5`, `SPACE_2_5`, `SPACE_3_5` | Quarter-rem steps multiplied by density |
| Density | `DENSITY` | 1 |
| Controls | `CONTROL_PADDING_INLINE`, `CONTROL_FAMILY`, `CONTROL_FONT_SIZE`, `CONTROL_LINE_HEIGHT`, `CONTROL_TRACKING`, `CONTROL_FG` | Padding follows density. The label role aliases the body family, small type size, body line height, normal tracking, and primary text |
| Motion | `CONTROL_FEEDBACK_DURATION`, `OVERLAY_DURATION`, `DISCLOSURE_DURATION` | 150ms, 200ms, 250ms |
| Corners | `RADIUS_SM`, `RADIUS_MD`, `RADIUS_LG`, `RADIUS_XL`, `RADIUS_FULL` | 0.25, 0.375, 0.5, 0.75 rem; pill radius |
| Colors | `SURFACE_*`, `TEXT_*`, `ACCENT_PRIMARY`, `BORDER_SUBTLE`, `STATUS_*` | Adaptive light/dark OKLCH colors |

Density scales spacing tokens, including control padding. It does not scale the type scale, tracking, or motion durations. Length tokens are `CssLength` values and work in any length-valued authoring method. Color, duration, family, and number tokens expose `varReference()` instead.

`Roles.LABEL` and `Tokens.CONTROL_FG` are what field labels and callout titles consume. Replacing `CONTROL_FONT_SIZE` or `CONTROL_FG` changes both. Replacing `FONT_SIZE_SM` changes the label size only while `CONTROL_FONT_SIZE` still points at it.

## Write styles with typed values

Style methods take values of the type their property accepts. There is no method that takes raw CSS text.

```java
div()
    .p(Tokens.SPACE_4)
    .bg(Tokens.SURFACE_CARD)
    .border(Border.hairline(Tokens.BORDER_SUBTLE))
    .rounded(Tokens.RADIUS_XL)
    .shadow(Shadow.drop(CssLength.px(12), CssLength.px(32), CssLength.px(-4), CssColor.oklch(0, 0, 0, 0.18)))
    .gridColumns(TrackList.autoFit(CssLength.rem(16)))
    .inlineSize(CssLength.pct(100).minus(CssLength.rem(2)))
    .motion(Motion.of(CssDuration.ms(150), Easing.easeOut(), Animatable.OPACITY, Animatable.TRANSLATE));
```

Lengths, colors, durations, and numbers also accept design tokens, so a theme override reaches them. `CssLength` supports `plus`, `minus`, `times`, `dividedBy`, `min`, `max`, and `anchor(...)`; `CssColor` supports `mix`, `mixTransparent`, `sameHue`, `lightDark`, and `contrast`. Keyword properties take enums (`TextAlign`, `WhiteSpace`, `Cursor`, `Resize`, ...), and identifiers such as container, anchor, and counter names are validated `CssIdent` values.

## Declare roles and states

Roles are `Role` constants and each `aria-*` attribute has a method taking the type it accepts. The generic `attr(...)` method refuses `role` and `aria-*`.

```java
var tabs = div().role(Role.TABLIST).ariaLabel("Sections")
    .child(div().role(Role.TAB).ariaSelected(true).ariaControls(panel).text("Overview"));
```

The compiler checks what each declared role requires: an accessible name, owned children, state attributes, and supported `aria-*` attributes. It also reports landmarks that need distinct names, a second `<main>`, heading levels that skip, empty headings, positive `tabindex` values, `<label for>` targets a label cannot name, and focusable controls inside `aria-hidden`. Every violation names a rule code and a fix; the rule table is in the [architecture notes](architecture.md#accessibility-rules).

Components with a landmark take its name where a page may hold several: `Breadcrumbs.label(...)` and `DocPager.label(...)`. `Card.headingLevel(...)` sets the title's level so the outline has no gaps.

## Trace elements to their source

Turn on source tracking to find the Java line behind an element:

```sh
just demo                 # serves the showcase with tracking on
just explain /overlays/   # prints one route's element tree with source lines
```

Inside `SourceTracking.enabled(() -> ...)` (`dev.nthings.tessera.render`), every element records where it was built. Rendered HTML carries `data-src="Page.java:42"`, diagnostics print the line (`<button> (Page.java:42)`), and `PageCompiler.explain(document)` returns a `PageTree` of tags, ids, components, CSS scopes, and sources. Elements a Tessera component builds are attributed to the line that placed the component. Tracking walks the stack per element, so leave it off in production.

## Publish assets

Register files at absolute logical paths. The writer publishes their bytes and site validation recognizes them as link and media targets:

```java
builder.asset(SiteAsset.file("/assets/logo.svg", Path.of("assets/logo.svg")));
builder.asset(SiteAsset.text("/assets/robots.txt", "User-agent: *\nAllow: /\n"));
```

Use `Image.of("/assets/logo.svg", "Site logo").dimensions(64, 64)` for an image with an accessible description and reserved layout space. Decorative images need the appropriate presentation or hidden semantics. The compiler reports missing alt text and missing dimensions/aspect ratios.

## Add static-site search

Rendering a `SearchPalette` includes its search input, result links, and JavaScript module automatically:

```java
var search = SearchPalette.create("search", "Search pages and sections...")
    .item("Guide", "Installation", "/guide/#installation")
    .item("Guide", "Publishing", "/guide/#publishing");

Elements.main().children(search.triggerButton(), search.asDeferred(10));
```

The palette contributes one client behavior per document, shared by all palettes on the page. Inline output embeds the module in `<script type="module">`; external output publishes a content-hashed asset through `CompiledPage.assets()` or `CompiledSite.files()`. There is no enable flag, separate asset registration, or search endpoint. This also works with standalone `PageCompiler.compile(...)` output. Pages without palettes receive no search script.

The module registers a customized native `<dialog is="tessera-search">` element. The browser upgrades each instance and owns modal opening, focus trapping, and light dismissal. Escape requests native dialog dismissal through `requestClose()`. Component lifecycle callbacks install event listeners with an `AbortSignal` and release them on disconnection. CSS `:has()` hides empty result groups; JavaScript handles filtering and result selection.

Typing filters the rendered links by group, title, and description. Matching is case- and accent-insensitive and accepts multiple words. Empty results show a message; clearing the query restores the directory. Arrow keys move focus through matching links. Enter on the input opens the first result, and Enter on a focused link uses native navigation. Selecting a result requests native dialog dismissal before the link navigates. Escape dismisses the dialog.

The runtime reads the palette's rendered links. The generated `search-index.json` is a separate artifact for other consumers. Add the routes and sections you want to expose with `.item(...)`, or initialize a palette from routes using `SiteNavigation.searchPalette(site)`.

## Verify and deploy

```sh
just test       # fast compiler, component, and site tests
just browsers   # install the latest Chromium and Firefox builds
just verify     # unit tests, both browser suites, audits, and quality gates
just guide      # generate the runnable example
```

Upload the entire `target/guide` directory, including its assets. This example publishes at the origin root. Configure your host to serve directory indexes and redirect `/guide` to `/guide/`.

The browser suite runs on latest Chromium and latest Firefox. It includes axe-core WCAG A/AA audits, keyboard flows for interactive widgets, forced-colors, reduced-motion, 200% zoom, and text-spacing checks.

For failures in the showcase's browser suite, inspect the reports under `tessera-showcase/target/failsafe-reports` and traces/screenshots under `tessera-showcase/target/playwright/{chromium,firefox}`.
