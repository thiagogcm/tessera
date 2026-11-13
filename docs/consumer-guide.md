# Component guide

Build HTML with element factories, then extract reusable components as your page grows. This guide covers composition, identity, document contributions, and interactions. Start with [Getting started](getting-started.md) for installation and a complete site.

## Compose elements

`Elements` provides factories such as `div()`, `h1()`, and `a(href)`. They return mutable `TagElement` builders. Add content with `child(...)`, `children(...)`, or `text(...)`; text is HTML-escaped.

```java
var content = Elements.main().children(
    Elements.h1().text("Build status"),
    Elements.p().text("All checks passed."));
```

Factories, components, and scopes implement `IntoElement`, so they can be placed directly in a parent. `Elements.fragment()` groups siblings without emitting a wrapper:

```java
Elements.ul().child(Elements.fragment().children(
    Elements.li().text("One"),
    Elements.li().text("Two")));
```

Use `Elements.el(HtmlTag.TIME)` for a built-in tag without a dedicated factory. `Elements.custom("my-widget")` creates an autonomous custom element; its name must be lowercase and contain a hyphen. Check any additional naming requirements against the HTML standard. Register a [page script](#contribute-a-page-script) to define its browser behavior.

`Elements.rawHtml(trustedHtml)` emits content verbatim. Its IDs, links, and accessibility semantics are outside compiler validation. Pass only trusted, pre-sanitized content; use `text(...)` for user-controlled text.

## Choose a component contract

| Contract | Use it for | Expansion result |
| --- | --- | --- |
| `Component` | Reusable composition within the caller's identity scope | Any `IntoElement` |
| `Scope` | A component with its own identity namespace and scoped CSS | One element host. A fragment, text, raw HTML, another scope, or nothing reports `scope-root` |

A component can be a lambda:

```java
Component notice = context -> Elements.p().text("Build completed");
```

A scope can be a record. This example is also in [GuideSite.java](../tessera-showcase/src/main/java/dev/nthings/tessera/showcase/guide/GuideSite.java):

```java
public record MetricCard(String id, String title, String value) implements Scope {
  @Override
  public String scopeId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope.root(s -> s.p(Tokens.SPACE_5).rounded(Tokens.RADIUS_XL)
        .bg(Tokens.SURFACE_CARD).border(Border.hairline(Tokens.BORDER_SUBTLE)));
  }

  @Override
  public IntoElement expand(DocumentContext context) {
    return Elements.div().children(Elements.h2().text(title), Elements.p().text(value));
  }
}
```

Place it with `parent.child(new MetricCard("builds", "Successful builds", "128"))`. Return one element host. A list scope returns `ul` or `ol`, and a row scope returns `tr`, so that host can sit in its parent's content model:

```java
public record ItemList(String id) implements Scope {
  @Override
  public String scopeId() {
    return id;
  }

  @Override
  public IntoElement expand(DocumentContext context) {
    return Elements.ul().children(
        Elements.li().text("One"),
        Elements.li().text("Two"));
  }
}
```

`Elements.main().child(new ItemList("items"))` emits the `ul` with `data-scope` and `data-scope-id`. A component that resolves to one element is a valid host. Returning another scope reports `scope-root` and leaves that scope's host alone. A fragment, text, raw HTML, late or deferred content, or nothing also reports `scope-root`. Expansion does not insert a wrapper.

`scopeName()` defaults to the kebab-cased class name, such as `metric-card`. It identifies the component's `data-scope` attribute and CSS scope. Override it for anonymous implementations, which otherwise report `unstable-scope-name`. The root also carries `data-scope-id`; scope styles stop at nested scopes.

`scopeId()` identifies an instance within its enclosing scope. Give repeated instances distinct IDs. The default, `null`, assigns `<scopeName>-<n>` in encounter order. Use explicit IDs when links must stay stable or the scope contains [keyed items](#key-list-items-that-move).

Keep expansion free of shared mutable application state: site compilation expands pages concurrently. The supplied `DocumentContext` belongs to the callback; do not retain it for asynchronous work.

## Use the built-in components

Built-in components are final classes with factories for required arguments and fluent options. Place them with `child(...)` or `children(...)`.

```java
var modal = Modal.of("Publish release?").id("publish").bodyText("Publish this version.");
Elements.main().children(
    Badge.of("Ready").tone(Tone.SUCCESS).pill().dot(),
    modal.triggerButton("Publish"),
    Elements.deferred(modal, 10));
```

| Purpose | Components |
| --- | --- |
| Prose and code | `Prose`, `CodeBlock`, `Formatting`, `Footnote`, `Footnotes` |
| Status and notices | `Badge`, `Callout` |
| Navigation | `Breadcrumbs`, `NavTabs`, `DocPager`, `StickyHeader`, `TableOfContents`, `DocLayout` |
| Dialogs and popovers | `Modal`, `Drawer`, `Dropdown` |
| Forms and search | `Forms`, `Field`, `Select`, `SearchPalette` |
| Actions | `Button`, `IconButton` |
| Data | `DataTable`, `TableGrid`, `Card`, `CardGrid` |
| Layout and disclosure | `Section`, `Heading`, `Grid`, `Image`, `BulletList`, `NumberedList`, `DescriptionList`, `StepList`, `Accordion`, `ContentTabs`, `Carousel` |

Factories describe the required shape, such as `Grid.autoFit(minWidth, ...)`, `Grid.columns(count, ...)`, `CodeBlock.of(source)`, and `CodeBlock.ofLines(...)`. Optional content uses methods such as `description(...)`, `previous(link)`, or `next(link)`.

Initial state uses named methods: `Card.radio(name, value).checked()`, `Accordion.openItem(title, content)`, `Select.selectedOption(value, label)`, and `Forms.checkedSwitchField(label, name)`. Apply an option conditionally with `onlyIf(condition, x -> x.checked())`, or `ifPresent(value, (x, v) -> ...)` for a nullable value.

Components share `Tone` for semantic color, `Size` (`SM`, `MD`, `LG`) for buttons and badges, and `Link(href, label)` for navigation items. A `label` is an accessible name; a `title` is a visible heading; a `description` is secondary text; `actions(...)` supplies footer actions. [Styling](styling.md) covers their appearance.

## Build tables with a logical grid

`DataTable.of(caption, grid)` renders a `TableGrid` in a labelled, keyboard-scrollable region. Build the grid from rows inside `head(...)`, `body(...)`, and `foot(...)` groups. Use a separate body group for each section of the table. Head and foot groups occur at most once.

```java
import static dev.nthings.tessera.components.TableGrid.Merge.LEFT;
import static dev.nthings.tessera.components.TableGrid.Merge.UP;
import static dev.nthings.tessera.components.TableGrid.body;
import static dev.nthings.tessera.components.TableGrid.columnHeader;
import static dev.nthings.tessera.components.TableGrid.data;
import static dev.nthings.tessera.components.TableGrid.head;
import static dev.nthings.tessera.components.TableGrid.row;
import static dev.nthings.tessera.components.TableGrid.rowHeader;

var grid = TableGrid.of(
    head(
        row(columnHeader("Region"), columnHeader("2026"), LEFT),
        row(UP, columnHeader("Q1"), columnHeader("Q2"))),
    body(
        row(rowHeader("East"), data("12"), data("15")),
        row(rowHeader("West"), data("9"), data("11"))));

var table = DataTable.of("Quarterly sales", grid)
    .id("sales")
    .stickyHead()
    .stickyFirstColumn();
```

Every row has one slot per logical column. OTSL continuation slots extend the owning cell: `LEFT` extends its first row, `UP` extends its first column, and `BOTH` fills the interior where both spans meet. For example, `row(data("A"), LEFT)` followed by `row(UP, BOTH)` creates one cell spanning two rows and two columns. `empty()` creates an independent empty cell. Construction rejects ragged rows, missing merge neighbors, nonrectangular cells, and merges across row-group boundaries.

Use `columnHeader(...)` for headers above columns, `rowHeader(...)` for headers to the left of rows, and `sectionHeader(...)` to name a body group. `cornerHeader(...)` marks the intersection of row and column headings without associating it with data cells. The renderer binds headers through table-local references and emits the corresponding HTML `headers` attributes. Column headers in the head group apply to subsequent groups; column headers inside another group apply only within that group. Section headers label data and row-header cells within their own body group.

Cells accept text or any `IntoElement`, including links, code, and nested tables. For a rich column heading, use `columnHeader(plainTextLabel, content)` to provide its card-mode label. `cell.align(TableGrid.Align.NUMERIC)` aligns that cell to the end with tabular numbers; `START`, `CENTER`, and `END` control the other alignments. Grid topology is immutable, while cell content follows the usual mutable element-builder contract.

Tables scroll horizontally by default. `stickyHead()` sticks the whole head group, including multiple header rows. `stickyFirstColumn()` sticks row headers starting in logical column zero; other columns continue to scroll. These options are independent.

For a flat grid, `.stackBelow(CssLength.rem(36))` turns body rows into labelled cards below the specified container width. Card mode requires exactly one head row of column headers, unspanned cells, and body groups containing only data or row headers. Other shapes throw `IllegalArgumentException` when card mode is requested. The labels are HTML content, hidden from assistive technology because the header relationships already name the cells. Stacking resets numeric alignment and sticky row headers so each card reads from start to end.

## Link elements with identity references

Create an `ElementRef`, bind it with `id(ref)`, and pass the same object to the invoker, label, or link that targets it:

```java
var target = ElementRef.scoped("help");
var trigger = Elements.button().command(target, BuiltinCommand.SHOW_MODAL).text("Help");
var dialog = Elements.dialog().id(target).ariaLabel("Help").text("Contact support");
var body = Elements.main().children(trigger, dialog);
```

| Factory | Resulting DOM ID |
| --- | --- |
| `ElementRef.scoped("save")` | `save` in the root scope; `toolbar--save` inside a scope with ID `toolbar` |
| `ElementRef.page("installation")` | `installation` regardless of the enclosing scope; suitable for public fragments |
| `ElementRef.anonymous()` | A compiler-assigned `<tag>-<n>` within the current scope |
| `element.ref()` | The element's existing reference, or a newly bound anonymous reference |

References are identity tokens. Creating another `ElementRef.scoped("help")` does not target the first one. Unbound references report `unbound-reference`. Several references can alias one element, but named references that request different DOM IDs report `conflicting-element-id`.

Use `id(ref)` for elements. Scoped built-in components instead accept `id(String)` for their instance namespace: `Modal.of("Help").id("help")` binds its dialog as `help--dialog` at the root. `Button` opens no scope, so its `id(ElementRef)` binds in the caller's namespace. A `Section` uses a page-level ID for links from other pages.

## Key list items that move

Give changing list items stable keys when they bind IDs or contain references. Keys keep those IDs stable when siblings are inserted, removed, or reordered:

```java
var list = Elements.ul().id(ElementRef.scoped("posts"));
for (var post : posts) {
  var title = ElementRef.scoped("title");
  list.child(Elements.li().ariaLabelledBy(title).children(
      Elements.h2().id(title).text(post.title()),
      Elements.a(post.url()).text("Read"))
      .key(post.slug()));
}
```

`key(...)` works on elements, components, and scopes and emits no wrapper. Keys compare exactly among siblings. A repeated sibling key reports `duplicate-key`; a blank key throws `IllegalArgumentException`.

Every enclosing scope must have an explicit `scopeId()`. A generated scope prefix can change with sibling order, so keyed content within it reports `generated-scope-id`, including content returned by components or deferred callbacks.

Two keyed lists in the same scope also need distinct named owner elements when they reuse keys and scoped names. Bind a named reference to each list's parent, as `posts` above. Otherwise their IDs share a namespace and can report `duplicate-id`. Lists whose items need no stable identity can omit keys.

## Add buttons and dialog interactions

`Button` and `IconButton` emit native buttons. An ordinary button uses `type="button"`; `submit()` makes it submit its form. Use anchors for navigation.

```java
var confirm = Modal.of("Confirm");
var open = Button.of("Review").command(confirm.dialogRef(), BuiltinCommand.SHOW_MODAL);
var save = Button.of("Save").submit().variant(Button.Variant.OUTLINE);
var close = IconButton.of("Close", Elements.span().text("x"))
    .command(confirm.dialogRef(), BuiltinCommand.CLOSE);
```

`Modal.dialogRef()`, `Drawer.dialogRef()`, and `Dropdown.panelRef()` expose targets for independent triggers. Native dialog commands require a `<dialog>` target; popover commands require a `popover` attribute. `Command.custom("cart", "add", Map.of("data-sku", sku))` emits `command="--cart-add"` and payload attributes for your own script. Its target must exist.

| Option | Effect |
| --- | --- |
| `IconButton.of(label, icon)` | Names the button and marks its icon decorative |
| `Button.of(label, content...)` | Keeps an accessible name when visible content differs |
| `disabled()`, `busy()` | Set independent button states |
| `interestTooltip(...)` | Adds a hover/focus hint; see [browser support](browser-support.md#component-scripts) |
| `Modal.opened()` | Emits the dialog in its non-modal `open` state |
| `closedBy(ClosedBy.ANY)` | Allows light dismissal; `CLOSE_REQUEST` and `NONE` restrict dismissal |

`ContentTabs.of(label).tab(key, label, content)` uses the author key in its tab and panel IDs. Blank keys throw `IllegalArgumentException`; repeated keys report `duplicate-tab`. Its module updates panel selection and keyboard focus.

## Declare roles and states

Use `role(Role)` and typed `aria*` methods to describe semantics. Generic `attr`, `boolAttr`, and `removeAttr` reject `id`, `role`, and `aria-*`; use the corresponding typed methods.

```java
Elements.a("/guide/").ariaCurrent(AriaCurrent.PAGE).text("Guide");
```

Name repeated landmarks with methods such as `Breadcrumbs.label(...)` and `DocPager.label(...)`. Set `Card.headingLevel(...)` to match the page outline. The compiler checks [accessibility rules](diagnostics.md#accessibility-rules); keyboard and browser evaluation remain part of [verification](development.md#browser-verification).

## Contribute document content

Components receive `DocumentContext` during expansion. It supports these document-level tasks:

| Task | API and behavior |
| --- | --- |
| Keep page-local component state | `document(DocumentKey<T>)` creates state on first use |
| Add head content | `contributeHead(element)` preserves registration order; use page-local state if it must be deduplicated |
| Add an end-of-body section | `atDocumentEnd(owner, callback)` keeps the first registration for each owner |
| Outline a heading | `Heading.outline(target)` or `Section.of(id, title)` collects the heading; bind the target on the page |
| Read collected headings | `headings()` returns a snapshot; a late callback sees headings collected from the body |
| Report an authoring error | `report(rule, message, fix)` adds a located diagnostic that fails compilation |

`Elements.deferred(content, priority)` moves content to the end of the body while retaining its lexical identity scope. Lower priorities come first; equal priorities retain placement order. `Elements.late(context -> ...)` resolves after body expansion, before document-end sections and head contributions. Use it for content such as a table of contents that depends on headings collected later in the body.

## Outline a heading

`Heading` is a native `h1` through `h6`. It does not join the document outline until `outline(target)`. The table of contents and the search index use the plain label. Rich phrasing stays in the HTML.

```java
var install = ElementRef.page("install");
Heading.of(2, "Install", Elements.code().text("mvn"), Elements.text(" verify"))
    .id(install)
    .outline(install);
```

`Heading.of(level, text)` uses `text` for both the visible heading and the label. Levels outside 1 through 6 throw `IllegalArgumentException`. This primitive does not represent heading levels above 6.

`Section.of(id, title)` keeps `id` on the `<section>` and outlines that same id through a `Heading`. Section levels are 2 through 4. Use `Heading` directly for levels 1 through 6 or a rich title. Dialog titles and the footnotes list heading are ordinary HTML headings and are not outlined.

`TableOfContents` and the search index read the collected outline. A late table of contents sees headings from the body. The first heading for a target is kept.

## Place footnotes

A `Footnote` is one definition. `reference()` places one occurrence. The first reference on the page numbers the note. Every reference gets its own marker and backlink, and the body is expanded once, in the `Footnotes` list.

```java
var engine = Footnote.of("latest Chromium and Firefox.").id("engine");
Elements.p().children(
    Elements.text("Tessera targets current browsers"),
    engine.reference());
Footnotes.of().title("Notes");
```

A plain-text note previews that text: the preview is new text, not a second copy of an element. A markup body has no preview until `preview(String)` or `preview(IntoElement)`. A preview element is placed at every reference, so keep ids out of one that is reused, and use a string preview when the note is referenced more than once.

The list may precede the references. Without `Footnotes.of()`, the list is appended at the end of the document. Its heading is not a document heading. A second list repeats every note and reports `duplicate-id`. Numbering follows the first reference. Set `id` before the note is referenced; it names the endnote `fn-<id>` in the footnotes scope. This registry is a document contribution, not a portable component.

## Contribute a page script

A `PageScript` is an ES module with a stable ID:

```java
context.contributeScript(new PageScript("my-widget", moduleSource));
```

The first registration of an ID wins; each page emits that module once. Keep the source consistent for every use of an ID. [Output modes](sites.md#choose-an-output-mode) determine whether it is embedded or published as a file. [Browser support](browser-support.md#component-scripts) lists the modules supplied by built-in components.

For complete application concerns, continue with [Styling](styling.md), [Sites](sites.md), or [Kotlin](kotlin.md).
