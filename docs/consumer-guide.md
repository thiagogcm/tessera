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
RenderOnce notice = () -> div().child(p().text("Build completed"));
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
        .p(Tokens.SPACE_5.length())
        .rounded(Tokens.RADIUS_XL.length())
        .bg(Tokens.SURFACE_CARD)
        .borderSubtle(Tokens.BORDER_SUBTLE));
  }

  @Override
  public Element render() {
    return div().children(h2().text(title), p().text(value));
  }
}
```

Pass it to `.child(...)` or `.children(...)` like any other component. The compiler establishes the view's namespace before calling its render method. Repeated instances need distinct instance IDs within the same enclosing scope. Omitting `entityId()` lets the compiler assign deterministic IDs.

Keep render callbacks free of shared mutable application state: site compilation expands different pages concurrently.

## Link elements with identity references

Bind an `ElementRef` where an element is emitted and use the same reference at its invoker. The compiler resolves both under the binding's view scope:

```java
var target = ElementRef.named("help");
var trigger = button().command(target, BuiltinCommand.SHOW_MODAL).text("Help");
var dialog = Elements.dialog().id(target).ariaLabel("Help").text("Contact support");
var body = Elements.main().children(trigger, dialog);
```

The reference is an identity token; another `ElementRef.named("help")` is a different token. An unbound reference fails compilation. Higher-level components such as `Overlays.modal(...)` expose their own trigger helpers to manage these references.

For document navigation, the example uses `Toc.section(...)`. It registers heading metadata, emits a target ID, and allows `Toc.fromContext(...)` to appear before the sections it lists. Custom components can call `RenderContext.current().registerHeading(localId, title, level)` and emit the returned `targetDomId` themselves.

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

`PageCompiler.compile(...)`, `PageCompiler.prepare(...)`, and `SiteCompiler.compile(...)` throw `CompilationException` with a `diagnostics()` list when authoring errors prevent compilation. Each `CompilationDiagnostic` contains a rule code, a `CompilationLocation`, an error message, and a suggested correction. The location identifies the page title, route (for sites), enclosing component path, and separate `tagName` and nullable `domId` fields. The exception formats those fields for display.

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

The exception message also presents the full report for command-line builds:

```text
Compilation failed with 1 error:
[interactive-label] Route "/settings", page "Settings" > toolbar[toolbar] > <button id="toolbar--save">: Interactive <button> element must have text content or an aria-label / aria-labelledby / title attribute
  Fix: Add visible label text, .ariaLabel(...), or .ariaLabelledBy(...) referencing a label.
```

Independent image, identity, reference, URL syntax, and markup errors are collected before a page fails. Late and deferred elements retain their enclosing component path. Site compilation collects page errors in route declaration order; once all pages prepare successfully, it collects cross-route link and transition-key errors together. Cross-route diagnostics identify the first use of each outgoing URL on a page. Head and end-section contributions retain the component path from their registration, including when they expand further components.

Native dialog commands (`show-modal`, `close`, `request-close`) require a `<dialog>` target. Popover commands (`toggle-popover`, `show-popover`, `hide-popover`) require a target with a `popover` attribute, following the [HTML command rules](https://html.spec.whatwg.org/multipage/form-elements.html#attr-button-command). Custom commands continue to use any existing target.

Duplicate bindings are reported as ambiguous; target-dependent command checks are skipped for ambiguous targets. Malformed URLs produce an `invalid-url` diagnostic alongside other page errors. The compiler reports authoring errors without emitting HTML. Exceptions thrown by application render callbacks still propagate normally, and trusted raw HTML remains outside validation.

## Customize the design system

Components reference shared CSS custom properties. Replace a token by registering a new value under its existing name:

```java
var tokens = new Tokens()
    .register(DesignToken.value(Tokens.FONT_BODY.name(), "Georgia,serif"))
    .register(DesignToken.value(Tokens.DENSITY.name(), "0.8"))
    .register(DesignToken.value(Tokens.RADIUS_XL.name(), "1rem"));

var site = SiteDefinition.builder().tokens(tokens).routes(routes).build();
```

| Family | Public tokens | Defaults |
| --- | --- | --- |
| Fonts | `FONT_BODY`, `FONT_MONO` | System UI and system monospace stacks |
| Type scale | `FONT_SIZE_XS`, `FONT_SIZE_SM`, `FONT_SIZE_MD`, `FONT_SIZE_LG`, `FONT_SIZE_XL`, `FONT_SIZE_2XL` | 0.75, 0.875, 1, 1.125, 1.25, 1.5 rem |
| Line height | `LINE_HEIGHT_BODY` | 1.5 |
| Spacing | `SPACE_1` through `SPACE_6`, `SPACE_8`, `SPACE_10`, `SPACE_12`; half steps `SPACE_0_5`, `SPACE_1_5`, `SPACE_2_5`, `SPACE_3_5` | Quarter-rem steps multiplied by density |
| Density | `DENSITY` | 1 |
| Controls | `CONTROL_PADDING_INLINE` | 0.875 rem multiplied by density |
| Corners | `RADIUS_SM`, `RADIUS_MD`, `RADIUS_LG`, `RADIUS_XL`, `RADIUS_FULL` | 0.25, 0.375, 0.5, 0.75 rem; pill radius |
| Colors | `SURFACE_*`, `TEXT_*`, `ACCENT_PRIMARY`, `BORDER_SUBTLE`, `STATUS_*` | Adaptive light/dark OKLCH colors |

Density scales component padding, gaps, and margins. Custom spacing definitions can include `var(--density)` to follow it. Use `.length()` when passing a token to length-valued authoring methods, and `.varReference()` for string-valued methods such as `fontFamily(...)`.

Existing components consume the shared scale. Explicit layout dimensions and special component styles remain individually configurable through authoring APIs.

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

The palette contributes one inline ES module per document, shared by all palettes on the page. There is no enable flag, separate asset registration, or search endpoint. This also works with standalone `PageCompiler.compile(...)` output. Pages without palettes receive no search script.

The module registers a customized native `<dialog is="tessera-search">` element. The browser upgrades each instance and owns modal opening, focus trapping, and light dismissal. Escape requests native dialog dismissal through `requestClose()`. Component lifecycle callbacks install event listeners with an `AbortSignal` and release them on disconnection. CSS `:has()` hides empty result groups; JavaScript handles filtering and result selection.

Typing filters the rendered links by group, title, and description. Matching is case- and accent-insensitive and accepts multiple words. Empty results show a message; clearing the query restores the directory. Arrow keys move focus through matching links. Enter on the input opens the first result, and Enter on a focused link uses native navigation. Selecting a result requests native dialog dismissal before the link navigates. Escape dismisses the dialog.

The runtime reads the palette's rendered links. The generated `search-index.json` is a separate artifact for other consumers. Add the routes and sections you want to expose with `.item(...)`, or initialize a palette from routes using `SiteNavigation.searchPalette(site)`.

## Verify and deploy

```sh
just test       # fast compiler, component, and site tests
just browsers   # install the configured Chromium build
just verify     # unit tests, browser tests, and quality gates
just guide     # generate the runnable example
```

Upload the entire `target/guide` directory, including its assets. This example publishes at the origin root. Configure your host to serve directory indexes and redirect `/guide` to `/guide/`.

For failures in the showcase's browser suite, inspect the reports under `tessera-showcase/target/failsafe-reports` and traces/screenshots under `tessera-showcase/target/playwright`.
