# Components

Build portable UI from typed element factories, reusable components, and styled scopes. Everything in this guide belongs to `dev.nthings.tessera.core` and `dev.nthings.tessera.components` and renders identically in static pages and [browser roots](browser-rendering.md).

For static document assembly such as raw HTML, deferred content, outlines, and footnotes, see [Documents](documents.md). For multi-axis data tables, see [Tables](tables.md).

## Compose elements

`Elements` provides typed factories such as `div(...)`, `h1(...)`, `p(...)`, `blockquote(...)`, `a(href, text)`, and `input(InputType)`. Zero-arg factories return mutable `TagElement` builders for chaining attributes and styles; container and text overloads build common markup concisely, and text is always HTML-escaped.

```java
var content =
    Elements.main(Elements.h1("Build status"), Elements.p("All checks passed."));
```

Element builders, portable components, and scopes all implement `IntoElement`, so you can pass them directly to any parent. Use `Elements.fragment(...)` to group siblings without emitting a wrapper tag:

```java
Elements.ul(Elements.fragment(Elements.li("One"), Elements.li("Two")));
```

Special tags and namespaces follow strict validation rules:

- **Other HTML tags**: Use `Elements.el(HtmlTag.TIME)` for standard tags without a dedicated factory.
- **Custom elements**: Use `Elements.custom("my-widget")`. Names must begin with a lowercase ASCII letter, include a hyphen, and use only lowercase ASCII letters, digits, `.`, `_`, and `-`. Check additional reserved names against the HTML specification. Contribute its ES module from a document component as described in [Documents](documents.md#contribute-a-page-script).
- **Generic attributes**: Attribute names allow only ASCII letters, digits, `-`, `_`, `.`, and `:`.
- **Inline SVG**: Use `Elements.svgIcon(width, height)` and `Elements.svg(SvgTag.PATH)` to build structured SVG nodes that work in both static HTML and DOM reconciliation. Place SVG shapes inside an `svg` root and keep HTML tags outside SVG; mismatches report `svg-namespace`. `SvgTag` covers shapes, `text` and `tspan`, `title` and `desc`, `defs`, `symbol` and `use`, gradients and `stop`, `clipPath`, `mask`, `pattern`, and `marker`.
- **Line icons**: `Icon.stroked(size, shapes...)` builds a Lucide-style icon on a 24-unit grid, stroked in `currentColor` and hidden from assistive technology, from `Icon.path`, `circle`, `ellipse`, `line`, `rect`, `polyline`, and `polygon`. Pass a Lucide icon's shapes to define your own set. Name an icon that carries meaning with `.ariaHidden(false).role(Role.IMG).ariaLabel(...)`.
- **Script safety**: Portable builders have no `<script>` factory, reject inline event handlers (`on*`) and `srcdoc` attributes in any letter case, and report `javascript:` URLs as `script-url`.

## Choose a component contract

Content in Tessera belongs to three compile-time placement families: portable, document, and live. Portable builders accept only `IntoElement`, which guarantees that portable components remain valid in both backends.

| Contract | Package | Context | Expansion result | Purpose |
| --- | --- | --- | --- | --- |
| `Component` | `core` | `ViewContext` | Any `IntoElement` | Portable composition in the caller's identity scope |
| `Scope` | `core` | `ViewContext` | One `TagElement` host | Portable component with its own CSS scope and identity namespace |
| `DocumentComponent` | `document` | `DocumentContext` | `DocumentContent` | Static document assembly; see [Documents](documents.md) |
| `DocumentScope` | `document` | `DocumentContext` | One `DocumentElement` host | Scoped component holding document content |
| `LiveComponent` | `wasm` | `LiveContext` | `LiveContent` | Browser view with live handlers, bindings, or effects; see [Browser rendering](browser-rendering.md) |
| `LiveScope` | `wasm` | `LiveContext` | One `LiveElement` host | Scoped browser component holding live content |

A `Component` can be a concise lambda:

```java
Component notice = context -> Elements.p().text("Build completed");
```

A `Scope` can be a Java record. This example also appears in [GuideSite.java](../tessera-showcase/src/main/java/dev/nthings/tessera/showcase/guide/GuideSite.java):

```java
public record MetricCard(String id, String title, String value) implements Scope {
  @Override
  public String scopeId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope.root(
        s ->
            s.p(Tokens.SPACE_5)
                .rounded(Tokens.RADIUS_XL)
                .bg(Tokens.SURFACE_CARD)
                .border(Border.hairline(Tokens.BORDER_SUBTLE)));
  }

  @Override
  public TagElement expand(ViewContext context) {
    return Elements.div().children(Elements.h2().text(title), Elements.p().text(value));
  }
}
```

### Scope hosts, names, and state

`Scope.expand(ViewContext)` returns the scope's single `TagElement` host. Expansion stamps `data-scope` and `data-scope-id` directly onto that host without inserting an extra `<div>`. Author the exact HTML tag the parent content model expects, such as `<ul>` for a list scope or `<tr>` for a table row. Place nested scopes inside the host; each keeps its own markers, and outer scope styles stop at nested scopes.

- `scopeName()` defaults to the kebab-cased class name (such as `metric-card`) and drives `data-scope` and CSS scoping. Override it on anonymous implementations to avoid `unstable-scope-name`.
- `scopeId()` names an instance inside its enclosing scope. The default (`null`) generates `<scopeName>-<n>` in encounter order. Supply an explicit ID when links must stay stable or when the scope contains [keyed items](#key-list-items-that-move).
- `ViewContext.state(ViewKey)` stores state for one expansion (one compiled page or one attempted browser render; a rejected render discards it). Keep expansion free of shared mutable application state because site compilation expands routes concurrently, and never retain the context for asynchronous work.
- `ViewContext.report(rule, message, fix)` records a located diagnostic that fails the expansion.

## Use built-in components

Built-in portable components are final classes with static factories for required arguments and fluent configuration methods. Place them with `child(...)` or `children(...)`.

| Category | Components |
| --- | --- |
| Prose and code | `Prose`, `CodeBlock`, `Formatting`, `Heading`, `Section` |
| Status and notices | `Badge`, `Callout`, `ScrollProgressBar` |
| Navigation | `Breadcrumbs`, `NavTabs`, `DocPager`, `StickyHeader` |
| Dialogs and popovers | `Modal`, `Drawer`, `Dropdown` |
| Forms and search | `Forms`, `Field`, `Select`, `SearchPalette` |
| Actions | `Button`, `IconButton`, `ColorSchemeToggle` |
| Data and cards | `DataTable`, `TableGrid`, `Card`, `CardGrid` |
| Layout and disclosure | `Grid`, `Image`, `BulletList`, `NumberedList`, `DescriptionList`, `StepList`, `Accordion`, `ContentTabs`, `Carousel` |

Document-only components (`DocLayout`, `DocumentSection`, `Footnote`, `Footnotes`, `HeroImage`, `SocialMeta`, `TableOfContents`) live in `dev.nthings.tessera.document.components` and are covered in [Documents](documents.md).

`CodeBlock.language(name)` labels the block and colors its syntax for Java, JavaScript, TypeScript, JSON, CSS, HTML and XML, shell, YAML, Python, and SQL. The lexer runs at compile time, so the page ships colored `<span>` elements and no script; other languages stay plain.

Factories encode required structure, such as `Grid.autoFit(minWidth, ...)`, `Grid.columns(count, ...)`, `CodeBlock.of(source)`, and `CodeBlock.ofLines(...)`. Apply optional configuration conditionally with `onlyIf(condition, x -> ...)` or `ifPresent(value, (x, v) -> ...)`.

Across built-in components, shared conventions apply:

- `Tone` selects semantic colors (`NEUTRAL`, `ACCENT`, `INFO`, `SUCCESS`, `WARNING`, `DANGER`).
- `Size` (`SM`, `MD`, `LG`) scales buttons and badges.
- `Link(href, label)` represents navigation entries.
- `label` sets an accessible name, `title` sets a visible heading, `description` supplies secondary text, and `actions(...)` adds footer actions.
- Bundled icons are structured SVG; pass portable icons such as `Icon.stroked(...)` to slots like `Badge.icon(...)`.
- Components that own a tag, such as `Image`, expose it through `element(...)` for custom styles, ARIA, or references.

## Set form defaults

Declare initial form state directly on portable controls. Static pages emit matching HTML defaults, browser roots initialize native defaults on first mount, and native form reset returns each control to those defaults.

```java
var name = Forms.text("name").initialValue("Ada");
var notes = Forms.textarea("notes", 3).initialValue("Draft notes");
var updates = Forms.switchInput("updates").initialChecked(true);
var region =
    Select.of("region").initialValue("br").option("br", "Brazil").option("ca", "Canada");
var languages =
    Elements.select()
        .boolAttr("multiple")
        .initialSelectedValues(List.of("en", "pt"))
        .children(Elements.option("en", "English"), Elements.option("pt", "Portuguese"));
```

Select defaults match option `value` attributes, or collapsed option text when no `value` attribute is present:

- A single select picks the first matching option in document order; a multiple select selects every match.
- Unmatched values leave the browser's native fallback active.
- Supply each default in one place. Do not combine `initialValue` with textarea child text, or `initialSelectedValues` with explicit `selected` attributes on options; conflicts report `initial-control-conflict`.

In a browser root, retained controls keep user edits across re-renders. Change a control's key to mount a fresh control with new defaults, or use [live form bindings](browser-rendering.md#bind-form-values) when application state owns the current value.

## Add buttons, commands, and disclosure controls

`Button` and `IconButton` emit native `<button>` elements with `type="button"` by default. Call `submit()` for form submission, and use anchors (`Elements.a(...)`) for navigation.

```java
var confirm = Modal.of("Confirm");
var open = Button.of("Review").command(confirm.dialogRef(), BuiltinCommand.SHOW_MODAL);
var save = Button.of("Save").submit().variant(Button.Variant.OUTLINE);
var close =
    IconButton.of("Close", Elements.span().text("x"))
        .command(confirm.dialogRef(), BuiltinCommand.CLOSE);
```

`Modal.dialogRef()`, `Drawer.dialogRef()`, and `Dropdown.panelRef()` expose target references for independent triggers. Built-in dialog commands require a `<dialog>` target, and popover commands require a `popover` attribute. For application scripts, `Command.custom("cart", "add", Map.of("data-sku", sku))` emits `command="--cart-add"` and its payload attributes against an existing target.

| Option | Effect |
| --- | --- |
| `IconButton.of(label, icon)` | Sets the button's accessible name and hides the decorative icon from assistive technology |
| `Button.of(label, content...)` | Preserves an accessible name when visible button content differs |
| `disabled()`, `busy()` | Set independent disabled and busy states |
| `interestTooltip(...)` | Attaches a hover and focus tooltip; see [Browser support](browser-support.md#component-scripts) |
| `Modal.opened()` | Emits a non-modal initially open dialog |
| `closedBy(ClosedBy.ANY)` | Enables light dismissal; `CLOSE_REQUEST` and `NONE` restrict dismissal |

`ContentTabs.of(label).tab(key, label, content)` incorporates each tab key into tab and panel IDs. Blank keys throw `IllegalArgumentException`, and duplicate keys report `duplicate-tab`. On static pages, its bundled module manages selection and keyboard focus; in browser roots, wrap it with [`LiveTabs`](browser-rendering.md#adapt-portable-widgets).

Buttons are portable and carry no Java event handlers. Because `Button`, `IconButton`, and `Image` implement `TagHost`, you can wrap them directly in `LiveElement.of(button)` or `DocumentElement.of(button)`.

## Link elements with identity references

Create an `ElementRef`, bind it to a target element with `id(ref)`, and pass the same object to any invoker, label, ARIA relationship, or fragment link:

```java
var target = ElementRef.scoped("help");
var trigger = Elements.button().command(target, BuiltinCommand.SHOW_MODAL).text("Help");
var dialog = Elements.dialog().id(target).ariaLabel("Help").text("Contact support");
var body = Elements.main().children(trigger, dialog);
```

| Factory | Resulting DOM ID |
| --- | --- |
| `ElementRef.scoped("save")` | `save` at the root; `toolbar--save` inside a scope with ID `toolbar` |
| `ElementRef.page("installation")` | `installation` regardless of enclosing scopes; intended for public fragments |
| `ElementRef.anonymous()` | Compiler-assigned `<tag>-<n>` within the current scope |
| `element.ref()` | The element's existing reference, or a newly bound anonymous reference |

References use object identity: two separate `ElementRef.scoped("help")` calls do not target the same element. Unbound references report `unbound-reference`. Multiple references may alias one element, but named references requesting different IDs report `conflicting-element-id`.

Use `id(ElementRef)` on elements and scope-free components such as `Button`. Scoped built-in components take `id(String)` for their instance namespace: `Modal.of("Help").id("help")` binds its dialog to `help--dialog` at the root.

## Key list items that move

Assign stable keys with `key(String)` to list items that can be inserted, removed, or reordered when they bind IDs, hold references, or preserve browser DOM state:

```java
var list = Elements.ul().id(ElementRef.scoped("posts"));
for (var post : posts) {
  var title = ElementRef.scoped("title");
  list.child(
      Elements.li()
          .ariaLabelledBy(title)
          .children(Elements.h2().id(title).text(post.title()), Elements.a(post.url()).text("Read"))
          .key(post.slug()));
}
```

Keying rules apply consistently in static and live output:

- `key(...)` works on elements, components, and scopes without emitting wrapper markup.
- Keys compare exactly among siblings. Duplicate sibling keys report `duplicate-key`, and blank keys throw `IllegalArgumentException`.
- Every enclosing scope around keyed content must declare an explicit `scopeId()`. Encounter-order scope prefixes can shift when siblings change, so keyed content inside one reports `generated-scope-id`.
- Two keyed lists in the same scope that reuse keys and scoped names need distinct named owner elements (such as `posts` above); otherwise their IDs collide and report `duplicate-id`.

## Declare roles and accessibility states

Use `role(Role)` and typed `aria*` methods to express semantics. Generic `attr`, `boolAttr`, and `removeAttr` reject `id`, `role`, and `aria-*` so all accessibility attributes stay checked.

```java
Elements.a("/guide/").ariaCurrent(AriaCurrent.PAGE).text("Guide");
```

Give repeated landmarks distinct names with methods like `Breadcrumbs.label(...)` and `DocPager.label(...)`, and align `Card.headingLevel(...)` with the surrounding document outline. See [Diagnostics](diagnostics.md#accessibility-rules) for checked rules and [Browser verification](development.md#browser-verification) for runtime accessibility audits.

Continue with [Tables](tables.md), [Documents](documents.md), or [Styling](styling.md).
