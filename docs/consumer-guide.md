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

Everything for compiling and publishing lives in `dev.nthings.tessera.compiler`, with `Compiler` as the single entry point. The example assembles `SiteRoute` values into a `SiteDefinition`:

```java
var site = SiteDefinition.builder()
    .route(new SiteRoute("/", new PageDocument("Home", Elements.main().child(h1().text("Home")))))
    .route(new SiteRoute("/guide", new PageDocument("Guide", Elements.main().text("Start here"))))
    .build();

var compiled = Compiler.compile(site);
SiteWriter.writeToDirectory(compiled, Path.of("target/site"));
```

`PageDocument` accepts an optional language before the title; the two-argument constructor uses English. The default site output shares a content-hashed external stylesheet across all pages. `CompiledSite` holds each route's HTML in `pages()`, the shared stylesheet text in `css()`, and every published file in `files()`. `site.route("/guide")` looks a route up by path.

A single page compiles without a site:

```java
CompiledPage page = Compiler.compile(new PageDocument("Hello", Elements.main().text("Hi")));
page.html();   // the document, with its CSS in a <style> element
page.css();    // the stylesheet text
page.assets(); // empty under inline output
```

`Compiler.compile(document, tokens, OutputMode.external("/assets"))` links a content-hashed stylesheet instead and lists it, followed by any client behavior modules, in `assets()`. Publish every `Asset` at its `path()`. The base must be an absolute path such as `/assets`; `OutputMode.external("assets")` throws `IllegalArgumentException`.

| Route         | Public document URL | Generated file     |
| ------------- | ------------------- | ------------------ |
| `/`           | `/`                 | `index.html`       |
| `/guide`      | `/guide/`           | `guide/index.html` |
| `/about.html` | `/about.html`       | `about.html`       |

Use `route.documentUri()` when you need the canonical public URL for a route. Preview serving redirects directory routes to their trailing-slash URL, preserving the query string, so relative links resolve the same way as on a static host.

## Compose reusable components

Use `Component` for composition without adding a scope:

```java
Component notice = context -> div().child(p().text("Build completed"));
```

`render(context)` returns any `IntoElement`, so a component may return an element, a fragment, or another component without calling `.intoElement()`.

Use `View` for a reusable component with an identity namespace and isolated CSS. The runnable example includes this component:

```java
public record MetricCard(String id, String title, String value) implements View {
  @Override
  public String viewId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope.root(s -> s
        .p(Tokens.SPACE_5)
        .rounded(Tokens.RADIUS_XL)
        .bg(Tokens.SURFACE_CARD)
        .border(Border.hairline(Tokens.BORDER_SUBTLE)));
  }

  @Override
  public IntoElement render(RenderContext context) {
    return div().children(h2().text(title), p().text(value));
  }
}
```

Pass it to `.child(...)` or `.children(...)` like any other component. The compiler establishes the view's namespace before calling its render method. Repeated instances need distinct instance IDs within the same enclosing scope. Omitting `viewId()` (it defaults to `null`) lets the compiler assign deterministic IDs.

`viewType()` names the view in `data-view` and its `@scope` block; it defaults to the kebab-cased class name (`MetricCard` gives `metric-card`). Anonymous classes and lambdas have no stable class name, so the compiler reports `unstable-view-type` for them unless they override `viewType()`.

The compiler marks the view's root element with `data-view` and `data-view-id`, which scope its styles. A view whose `render(context)` returns a fragment or text has no single root to mark, so the compiler wraps it in a `<div>`. Return one element when the parent's content model matters.

Both callbacks receive an explicit `RenderContext` for that turn. Do not retain it for later async work. Keep render callbacks free of shared mutable application state: site compilation expands different pages concurrently.

A component that finds a wiring mistake reports it instead of throwing, so it is collected with the compiler's own diagnostics and located at the component:

```java
context.report("duplicate-hero-image", "Only one hero image per page", "Remove .hero() from one image.");
```

## Use the built-in components

The components in `dev.nthings.tessera.components` share one shape. Each is a final class created by a static factory that takes its required arguments, usually `X.of(...)`; options are fluent setters; and it is placed with `.child(...)` like any element. Named factories exist only where the required arguments genuinely differ, such as `Grid.autoFit(minWidth, ...)` versus `Grid.columns(count, ...)` and `CodeBlock.of(source)` versus `CodeBlock.ofLines(...)`. Their render callbacks are internal, so autocomplete shows only authoring options.

```java
var modal = Modal.of("Publish release?").id("publish").bodyText("Ship it to every region.");
main().children(
    Section.of("install", "Installation").description("Two steps.").child(p().text("Run it.")),
    Card.of("Team plan").description("Five seats").radio("plan", "team", true),
    Badge.of("Live").tone(Tone.SUCCESS).pill().dot(),
    modal.triggerButton("Publish"),
    Elements.deferred(modal, 10));
```

A scoped component takes an optional `id(String)` that names the instance in the enclosing view scope; the DOM ids it binds are prefixed with it, so the modal above emits `publish--dialog`. Without one the compiler assigns `<view type>-<n>` per scope (`modal-1--dialog`), which is fine unless other code links to those ids. Buttons bind their own element with `id(ElementRef)` instead; a button opens no scope of its own, so `ElementRef.scoped("save")` on a button inside a view with id `toolbar` emits `toolbar--save`. A `Section` id is different: it is a page-level fragment, so `/guide/#install` links to it from any page.

The vocabulary is shared: `Tone` for semantic color, `Size` (`SM`, `MD`, `LG`) for buttons and badges, and `Link(href, label)` for `Breadcrumbs`, `NavTabs` and `DocPager`. A `label` is an accessible name, a `title` a visible heading, a `description` muted secondary text, and `actions(...)` footer actions. `Forms` (styled controls such as `Forms.email(name)`, wrapped by `Field.of(label, control)`) and `Formatting` (inline `mark`, `kbd`, `code`, ...) are plain functions that return elements.

## Group siblings and name custom elements

Use `Elements.fragment()` when a component owns several roots or when a wrapper would change the parent content model. The fragment emits no markup:

```java
Elements.ul().child(
    Elements.fragment().children(li().text("One"), li().text("Two")));
```

`HtmlTag` covers the built-in vocabulary, including `video`, `audio`, `iframe`, `canvas`, `template`, `slot`, `picture`, `source`, and `track`. `Elements.custom("my-widget")` emits an autonomous custom element. Custom element names must be lowercase and contain a hyphen; follow the HTML standard's other naming rules yourself, since Tessera does not check them. Pair the element with a `ClientBehavior` so the browser upgrades it.

## Contribute a client behavior

A behavior is an ES module with a stable id. Components contribute it through the render context:

```java
var behavior = new ClientBehavior("my-widget", moduleSource);
context.contributeBehavior(behavior);
```

The compiler emits each behavior once per page. `OutputMode.inline()` inlines a `<script type="module">`. `OutputMode.external("/assets")` publishes a content-hashed module, records it in `CompiledPage.assets()`, and site compilation merges it into `CompiledSite.files()` so `SiteWriter` writes it. Behaviors keep browser code out of the component tree and avoid a component-to-site dependency. `SearchPalette` and `ContentTabs` use this path for their bundled modules.

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

`inlineRow` and `inlineColumn` use `inline-flex`, so the container stays on the surrounding line. A layout sets `display`, `flex-direction`, `flex-wrap`, `align-items`, and `gap`; a later call that sets one of those, such as `gap(...)`, replaces that one declaration on the element. The shared `FlexLayout` value is unchanged. Lengths accept `CssLength` values, including `DesignToken.Length` tokens.

A scrolling region inside a fixed-height column is a property of the inner panel: give the column a block size, and give the panel `flexGrow(1)`, `flexShrink(1)`, `minBlockSize(CssLength.px(0))`, and `overflowY(Overflow.AUTO)`.

## Link elements with identity references

An `ElementRef` is the only way to give an element an id. Bind it where the element is emitted with `.id(ref)` and use the same reference at its invoker, label, or link. The compiler resolves both:

```java
var target = ElementRef.scoped("help");
var trigger = button().command(target, BuiltinCommand.SHOW_MODAL).text("Help");
var dialog = Elements.dialog().id(target).ariaLabel("Help").text("Contact support");
var body = Elements.main().children(trigger, dialog);
```

| Factory | DOM id |
| --- | --- |
| `ElementRef.scoped("save")` | `save` prefixed by the binding's view scope: `toolbar--save` inside a view with id `toolbar` |
| `ElementRef.page("installation")` | `installation` exactly, wherever it is bound; use it for public fragments such as `/guide/#installation` |
| `ElementRef.anonymous()` or `element.ref()` | Assigned by the compiler as `<tag>-<n>` within the view scope |

The reference is an identity token; another `ElementRef.scoped("help")` is a different token. An unbound reference fails compilation with `unbound-reference`. `element.ref()` returns a ref to an element you hold, binding an anonymous one if it has none, so `button().command(dialog.ref(), BuiltinCommand.SHOW_MODAL)` works too. Several refs may name one element: they all resolve to its single DOM id, taken from the named ref. Two named refs that ask for different ids on one element report `conflicting-element-id`. `attr("id", ...)` is refused, like `role` and `aria-*`. Higher-level components such as `Modal` expose their own trigger helpers to manage these references.

For document navigation, the example uses `Section.of(id, title)`. It registers heading metadata and binds the section to a page-level id, and `TableOfContents.of(title)` may appear before the sections it lists. `DocLayout.of(sections...)` places them beside a sticky table of contents. Custom components call `context.registerHeading(target, title, level)` inside their render callback and bind `target` on the heading element; a target that is never bound reports `unbound-reference`.

## Add a button

`Button` and `IconButton` emit a native `<button>`. Tone, surface, and size are separate from what the button does. An ordinary button is `type="button"`. `submit()` is `type="submit"`. `command(target, command)` runs a `Command` on the element bound to `target`, including one emitted later in the document: `Modal.dialogRef()`, `Drawer.dialogRef()` and `Dropdown.panelRef()` expose those targets. A `BuiltinCommand` such as `SHOW_MODAL` or `TOGGLE_POPOVER` is checked against its target; `Command.custom("cart", "add", Map.of("data-sku", sku))` emits `command="--cart-add"` plus the payload attributes for the application's own script, and accepts any target. Plain elements take the same `command(target, command)`. Navigation stays on anchors.

```java
var confirm = Modal.of("Confirm");
var open = Button.of("Review").command(confirm.dialogRef(), BuiltinCommand.SHOW_MODAL);
var save = Button.of("Save").submit().variant(Button.Variant.OUTLINE);
var close = IconButton.of("Close", Elements.span().text("×"))
    .command(confirm.dialogRef(), BuiltinCommand.CLOSE);
```

`IconButton` takes the accessible name beside the icon and marks the icon as decorative. `Button.of(label, content...)` keeps `label` as the accessible name when the visible content differs. `disabled()` and `busy()` combine freely. The name remains, and keyboard focus uses `:focus-visible` outside reduced-motion styling.

`Button.interestTooltip(...)` adds a hint popover on hover or focus. Native interest invokers drive it where available; a bundled `TooltipBehavior` mirrors hover and focus handling in Firefox, which does not ship the API yet. `Modal.of(...).opened()` renders a dialog visible in the non-modal `open` state.

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

Every `Compiler` method throws `CompilationException` with a `diagnostics()` list when authoring errors prevent compilation. Each `Diagnostic` has a `rule` code, a `message`, a `fix`, and a `Location`. The location is flat: `route` (null for a standalone page), `page` title (route and page are both null for a file conflict that belongs to no page), enclosing `components`, element `tag`, nullable `domId`, and nullable `source`. `Diagnostic.format()` presents a diagnostic as the exception prints it. An exception thrown by a component's render or style callback, or by a late or document-end callback, propagates unchanged, with a suppressed `CompilationException` naming the component path.

```java
try {
  Compiler.compile(site);
} catch (CompilationException failure) {
  for (var diagnostic : failure.diagnostics()) {
    System.err.println(diagnostic.rule() + ": " + diagnostic.message());
    System.err.println(diagnostic.location().describe());
    System.err.println(diagnostic.fix());
  }
}
```

With source tracking on (see below), the location line also names the element's Java source line, for example `<button id="toolbar--save"> (Settings.java:42)`. The exception message also presents the full report for command-line builds:

```text
Compilation failed with 1 error:
[interactive-label] Route "/settings", page "Settings" > toolbar[toolbar] > <button id="toolbar--save">: Interactive <button> element must have text content or an aria-label / aria-labelledby / title attribute
  Fix: Add visible label text, .ariaLabel(...), or .ariaLabelledBy(...) referencing a label.
```

Independent image, identity, reference, URL syntax, and markup errors are collected before a page fails. Late and deferred elements retain their enclosing component path. Site compilation first checks paths: two routes with one path report `duplicate-route`, and a registered file at the path of a route, another file, or a generated file (`/search-index.json`, `/sitemap.xml`) reports `file-path-collision`. These stop compilation before any page renders. It then collects page errors in route declaration order; once all pages prepare successfully, it collects cross-route link and transition-key errors together, along with any generated stylesheet or module path that collides with a route or file. Cross-route diagnostics identify the first use of each outgoing URL on a page. Head and end-section contributions retain the component path from their registration, including when they expand further components.

A native dialog's dismissal is set with `closedBy(ClosedBy.ANY)`, `CLOSE_REQUEST`, or `NONE`. Native dialog commands (`show-modal`, `close`, `request-close`) require a `<dialog>` target. Popover commands (`toggle-popover`, `show-popover`, `hide-popover`) require a target with a `popover` attribute, following the [HTML command rules](https://html.spec.whatwg.org/multipage/form-elements.html#attr-button-command). Custom commands continue to use any existing target.

Duplicate bindings are reported as ambiguous; target-dependent command checks are skipped for ambiguous targets. Malformed URLs produce an `invalid-url` diagnostic alongside other page errors. The compiler reports authoring errors without emitting HTML. Exceptions thrown by application render callbacks still propagate normally, and trusted raw HTML remains outside validation.

## Customize the design system

Components reference shared CSS custom properties, the `DesignToken` constants in `Tokens`. A `Theme` holds their values: it is immutable, so start from `Theme.defaults()` and override tokens with `with(token, value)`, which returns a new theme. `DesignToken<V>` is typed by category, so `with` only accepts a value of the token's category and a length cannot replace a color:

```java
var theme = Theme.defaults()
    .with(Tokens.FONT_BODY, FontStack.of(FontStack.named("Georgia"), FontStack.Generic.SERIF))
    .with(Tokens.DENSITY, CssNumber.of(0.8))
    .with(Tokens.RADIUS_XL, CssLength.rem(1))
    .with(Tokens.CONTROL_FG, CssColor.lightDark(CssColor.oklch(20, 0.02, 150), CssColor.oklch(96, 0.01, 150)))
    .with(Tokens.CONTROL_FONT_SIZE, CssLength.rem(1));

var site = SiteDefinition.builder().theme(theme).routes(routes).build();
var page = Compiler.compile(document, theme, OutputMode.inline());
```

Your own tokens are records of a category, such as `new DesignToken.Color("brand", CssColor.oklch(60, 0.2, 300))`; `theme.with(brand, value)` adds a token the theme does not have yet. Token names are validated (`[A-Za-z0-9_-]`, no leading `-`) and an invalid name throws instead of being rewritten. A token is a value of its category, so another token can be a token's value: `with(Tokens.CONTROL_FG, Tokens.ACCENT_TEXT)` makes it follow the accent.

`Typography` sets family, size, weight, line height, and tracking together. `Roles.LABEL` is the control label. `Tokens.CONTROL_FG` is the control text color, defined as `var(--text-primary)` until a theme replaces `--control-fg`. `Transition.controlFeedback()`, `Transition.overlayEntry()`, and `Transition.disclosure()` set duration, easing, and the transitioned properties. Apply a transition that moves an element inside `on(Media.MOTION_SAFE, ...)`, and leave its visible end state outside that query.

`Tone` (`NEUTRAL`, `ACCENT`, `INFO`, `SUCCESS`, `WARNING`, `DANGER`) is the one semantic color vocabulary: `Button.tone(...)`, `Badge.tone(...)` and `highlight(...)` take it, and each component decides how a tone paints. `Callout.Kind` (note, tip, important, warning, caution) picks a callout's icon, title and tone.

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

Density scales spacing tokens, including control padding. It does not scale the type scale, tracking, or motion durations. Each token is also a value of its category (`CssLength`, `CssColor`, `CssDuration`, `CssNumber`, or a `FontStack` entry), so it works in any method that takes that category and serializes as `var(--name)`.

`Roles.LABEL` and `Tokens.CONTROL_FG` are what field labels and callout titles consume. Replacing `CONTROL_FONT_SIZE` or `CONTROL_FG` changes both. Replacing `FONT_SIZE_SM` changes the label size only while `CONTROL_FONT_SIZE` still points at it.

## Write styles with typed values

Style methods take values of the type their property accepts; no style method takes raw declaration text. `ScopeStyle.select(...)` selectors are the one raw string: they are emitted as written.

```java
div()
    .p(Tokens.SPACE_4)
    .bg(Tokens.SURFACE_CARD)
    .border(Border.hairline(Tokens.BORDER_SUBTLE))
    .rounded(Tokens.RADIUS_XL)
    .shadow(Shadow.drop(CssLength.px(12), CssLength.px(32), CssLength.px(-4), CssColor.oklch(0, 0, 0, 0.18)))
    .gridColumns(TrackList.autoFit(CssLength.rem(16)))
    .inlineSize(CssLength.pct(100).minus(CssLength.rem(2)))
    .transition(Transition.of(CssDuration.ms(150), Easing.easeOut(), Animatable.OPACITY, Animatable.TRANSLATE));
```

Lengths, colors, durations, and numbers also accept design tokens, so a theme override reaches them. `CssLength` supports `plus`, `minus`, `times`, `dividedBy`, `min`, `max`, and `anchor(...)`; `CssColor` supports `mix`, `mixTransparent`, `sameHue`, `lightDark`, and `contrast`. Keyword properties with several useful values take one enum (`overflow(Overflow.AUTO)`, `textWrap(TextWrap.BALANCE)`, `cursor(Cursor.POINTER)`, `appearance(Appearance.NONE)`, `resize(Resize.VERTICAL)`, ...); a property with a single useful value has a named toggle (`listStyleNone()`, `tabularNums()`). Names are small validated handles: `Container`, `Keyframes`, `Anchor`, `Counter`, and `CustomProperty` for a local custom property. Every style method is on `Styled`, which elements, components such as `Image`, and `StyleRefinement` implement.

The last call wins. Writing a property again replaces it, and writing a shorthand removes the longhands it covers: `p(...)` replaces an earlier `pt(...)`/`pb(...)`/`ps(...)`/`pe(...)`, `border(...)` replaces earlier side borders and `borderColor(...)`, and `gap(...)` replaces `rowGap(...)`/`colGap(...)`. A longhand written after its shorthand refines it, so `p(Tokens.SPACE_4).pt(CssLength.zero())` keeps both.

Share a style as a `Consumer<StyleRefinement>` and apply it with `apply(...)`. Conditional styles and `ScopeStyle` rules take the same type:

```java
Consumer<StyleRefinement> card = s -> s.p(Tokens.SPACE_4).rounded(Tokens.RADIUS_XL).bg(Tokens.SURFACE_CARD);

div().apply(card).on(State.HOVER, s -> s.bg(Tokens.SURFACE_SUBTLE));
scope.select(":scope > article", card);
```

### Conditional styles

`on(condition, styles)` applies `styles` only while the condition holds. A `Condition` is a `State` (`HOVER` and `ACTIVE`, which skip disabled elements, `FOCUS_VISIBLE`, `CHECKED`, `OPEN`, `USER_INVALID`, `USER_VALID`, `DISABLED`), a `Media` feature (`MOTION_SAFE`, no reduced-motion preference), or a container query. States are emitted in that order whatever the call order, so `ACTIVE` beats `HOVER` and `DISABLED` beats both. Conditions nest: `on(Media.MOTION_SAFE, m -> m.on(State.HOVER, ...))`.

On an element, state and media rules go to the `interactive` cascade layer and container queries to the `container` layer, both above the element's base styles. Inside a `ScopeStyle` rule they nest in that rule (`&:hover`, `@media`, `@container`), so a scope can style its links' hover state with `select(":scope a", s -> s.on(State.HOVER, ...))`, which is exactly as specific as `:scope a:hover` (the disabled guard is wrapped in `:where()`). `ScopeStyle.on(query, rules)` wraps whole rules in a media feature or container query. `startingStyle(...)` writes the `@starting-style` values an entry transition starts from:

```java
scope.select(":scope:open", s -> s.opacity(1).startingStyle(start -> start.opacity(0)));
```

A `Container` is declared once and queried by handle. A named container is matched by name; an unnamed one is the nearest ancestor container of the right type:

```java
var cards = Container.named("cards", Container.Type.INLINE_SIZE);
div().container(cards).child(
    article().on(cards.atLeast(CssLength.rem(30)), s -> s.flexRow()));

var header = Container.named("site-header", Container.Type.SCROLL_STATE);
scope.root(s -> s.sticky().container(header))
    .on(header.stuck(Container.Edge.TOP), c -> c.root(s -> s.shadow(...)));
```

Size queries are `atLeast(length)` and `below(length)`; scroll-state queries are `stuck(edge)` and `snapped(axis)`. `containerNone()` undoes a container a component declares.

### Animations

`Keyframes` are typed values, and an `Animation` plays them. Applying an animation registers its keyframes in the stylesheet and wraps it in `prefers-reduced-motion: no-preference`:

```java
var pulse = Keyframes.named("pulse").from(s -> s.opacity(1)).at(50, s -> s.opacity(0.5)).to(s -> s.opacity(1));
div().animation(Animation.of(pulse, CssDuration.ms(800), Easing.ease()));

div().animation(Animation.reveal(Keyframes.FADE_UP));                 // scrubbed by the view timeline
chip.animation(Animation.revealOnce(Keyframes.FADE_UP).staggerBySiblingIndex(60));
bar.animation(Animation.scrollProgress(grow));                        // follows the page scroll
```

`Animation.exit(...)` follows the element out of view, and `along(TimelineAxis.INLINE)` switches a scroll-driven animation to the inline axis. The presets `FADE_IN`, `FADE_UP`, `SCALE_IN`, `SLIDE_INLINE` and `FADE_OUT` animate `opacity` and `transform`.

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

Inside `SourceTracking.enabled(() -> ...)` (`dev.nthings.tessera.compiler`), every element records where it was built. Rendered HTML carries `data-src="Page.java:42"`, diagnostics print the line (`<button> (Page.java:42)`), and `Compiler.explain(document)` returns a `PageTree` of tags, ids, components, CSS scopes, and sources. Elements a Tessera component builds are attributed to the line that placed the component. Tracking walks the stack per element, so leave it off in production.

## Publish assets

Register files at absolute logical paths. The writer publishes their bytes and site validation recognizes them as link and media targets:

```java
builder.asset(Asset.file("/assets/logo.svg", Path.of("assets/logo.svg")));
builder.asset(Asset.text("/assets/robots.txt", "User-agent: *\nAllow: /\n"));
```

Use `Image.of("/assets/logo.svg", "Site logo").dimensions(64, 64)` for an image with an accessible description and reserved layout space. Decorative images need the appropriate presentation or hidden semantics. The compiler reports missing alt text and missing dimensions/aspect ratios. `hero()` marks the page's main image: it loads eagerly with a `<head>` preload, and a second hero on one page reports `duplicate-hero-image`.

## Add static-site search

Rendering a `SearchPalette` includes its search input, result links, and JavaScript module automatically:

```java
var search = SearchPalette.of()
    .placeholder("Search pages and sections...")
    .item("Guide", "Installation", "/guide/#installation")
    .item("Guide", "Publishing", "/guide/#publishing");

Elements.main().children(search.triggerButton(), Elements.deferred(search, 10));
```

The palette contributes one client behavior per document, shared by all palettes on the page. Inline output embeds the module in `<script type="module">`; external output publishes a content-hashed asset through `CompiledPage.assets()` or `CompiledSite.files()`. There is no enable flag, separate asset registration, or search endpoint. This also works with standalone `Compiler.compile(...)` output. Pages without palettes receive no search script.

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
