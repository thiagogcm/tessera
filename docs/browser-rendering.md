# Browser rendering (experimental)

The opt-in `tessera-wasm` module mounts the same components in a page as Java compiled to WebAssembly with GraalVM Web Image. A mounted view re-renders from application state, and the backend reconciles the native DOM so focus, selection and form values survive updates. The separate `tessera-wasm-showcase` module builds the runnable images and runs browser acceptance. The [roadmap](roadmap.md) describes planned capability and module boundaries.

A browser root renders `LiveContent`: portable content plus the live-only features below. Portable components expand exactly as the compiler expands them and receive the same portable `ViewContext`. Live content cannot be placed in portable or document content, and document content (raw HTML, deferred and late content, document components) cannot be placed in a root; both mistakes are compile errors. Expansion state from `context.state(key)` lasts one attempted render. A root loads no page scripts. Use the [adaptive widget adapters](#adapt-portable-widgets) for the built-in browser interactions.

Import live contracts, `BrowserEvent`, `EventOptions`, `Effect`, and `Lifetime` from `dev.nthings.tessera.wasm`. A named Java application can use `requires dev.nthings.tessera.wasm;` in its module descriptor. This also makes the shared authoring packages available. The showcase's Web Image build uses the classpath.

## Build and verify

Use Oracle GraalVM **25.5.5.0.3+1.1**, matching the `webimage-preview` SDK pinned in [tessera-wasm/pom.xml](../tessera-wasm/pom.xml). Install Native Image prerequisites and Binaryen **119 or later** on `PATH`. Set `JAVA_HOME` to that GraalVM distribution. The default JVM build does not invoke these tools.

```sh
# Build both entry points and assemble the browser distribution
just wasm

# Serve the generated host over HTTP
jwebserver -b 127.0.0.1 -p 8000 -d tessera-wasm-showcase/target/web

# Build and exercise the real images in Chromium and Firefox
just wasm-verify
```

Install the Playwright browsers with `just browsers` before verification. The acceptance suite serves the compiled distribution and exercises root lifetimes, events, controls, identity, styling, scheduling, and adaptive widgets. See [runtime internals](browser-internals.md) for the implementation contracts.

## Publish a browser bundle

Build the GraalVM Web Image entry with the pinned toolchain and `-H:-AutoRunVM`. On the JVM, import the generated launcher and image with `GraalWebImage.read`. Add a document-only mount with its host ID, view name, and static fallback. The fallback can contain ordinary links and form controls.

```java
var bundle = GraalWebImage.read(Path.of("editor.js"), Path.of("editor.js.wasm"));
var page = Compiler.compile(
    new PageDocument("Editor", Documents.wasmMount(
        "editor", "editor", bundle, Elements.a("/edit/").text("Open the static editor"))),
    Theme.defaults(), OutputMode.external("/assets/editor"));
```

Publish every returned asset at its path and media type, and apply `page.responseHeaders()` to the HTML response. For sites, `SiteWriter` writes the complete output and `CompiledSite.pageHeaders()` carries headers by route. `SitePreviewServer` applies those headers. Configure the same headers on a file-based host. Use an external output mode for the full Content Security Policy. Bundle assets always remain separate files. Inline output puts them under `/assets/wasm/`.

The compiler validates the resource graph and publishes content-derived filenames under the chosen asset base. The bootstrap applies SHA-384 integrity to scripts and to binary fetches before starting the entry. It keeps the fallback in place when loading fails. The pinned GraalVM adapter accepts the classic launcher and primary image. A prepared `WasmBundle` can also declare supplementary binary resources. Compatible launchers receive verified bytes by logical name through `config.tesseraResources`.

`just wasm` builds the published sample in `tessera-wasm-showcase/target/published-web`. The existing `target/web` sample supports local development of individual runtime fixtures. `just wasm-verify` tests compiled publication at root and nested asset bases in Chromium and Firefox.

## Mount a view

A [document mount](#publish-a-browser-bundle) declares the host ID and the view to instantiate, and supplies fallback content.

The entry's `main` mounts every host the page declares for it. The view is a `LiveComponent`: it receives a `LiveContext`, whose `root()` is the mounted root, and returns live content:

```java
public final class Editor {
  private String name = "";
  private boolean saved;

  public static void main(String[] args) {
    var editor = new Editor();
    BrowserEntry.start(args, Map.of("editor", host -> BrowserRoot.mount(host, editor::view)));
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var input = Forms.text("name");
    return LiveElement.of(Elements.main().child(Elements.label().labelFor(input.ref()).text("Name")))
        .child(LiveElement.of(input).bindValue(name).on("input", event -> {
          name = event.control().value();
          root.invalidate();
        }))
        .child(LiveElement.of(Button.of("Save").element()).onClick(root.update(() -> saved = true)))
        .child(Elements.p().text(saved ? "Saved " + name : "Not saved"));
  }
}
```

`LiveElement.of(tag)` wraps a portable element without changing it, so the same `TagElement` or component markup stays valid in a static page. Portable content is accepted directly as a child of live content; `LiveContent.of(content)` converts it where a `LiveContent` value is required. `LiveComponent` and `LiveScope` are the live counterparts of `Component` and `Scope`. Buttons expose `element()` so a `LiveElement` can bind their handlers.

Several entries can share a page; each launcher loads once however many hosts it serves. The first committed render replaces fallback content. A rejected candidate keeps the fallback and a later invalidation retries. A native failure during initial installation restores the fallback nodes. `root.close()` empties the host, which can then be mounted again.

Generated and `ElementRef.scoped` ids use a namespace derived from the host id; an introduced id used outside the host rejects the render with `cross-root-id`. Mounting on a host that already holds a root fails separately.

The check covers ids a render introduces: once a root has committed an id, it owns it, and an element the host page adds later with that id does not reject the root's renders. The host's own ID is reserved: a render claiming it for a child is rejected with `cross-root-id`, and the committed view or fallback stays. Remaining limits are listed under [current limits](architecture.md#current-limits).

## Schedule updates

Call `root.invalidate()` after application state changes. Repeated invalidations before the scheduled task coalesce into one render, and a render that calls `invalidate()` schedules a later turn instead of recursing. `root.update(change)` returns a `Runnable` that applies `change` and invalidates the root, the usual shape for click handlers. Wrap an asynchronous completion when its work starts: after `root.close()` the wrapper does nothing, so a late result leaves the disposed root untouched.

`BrowserRoot.Scheduling(workBudget, runawayLimit)` configures one root. The default applies eight render turns per browser task, then waits for an animation frame so the browser can paint, and reports a runaway after 32 consecutive turns that each request another render. A runaway root reports one browser error, keeps the last committed tree and accepts a later external invalidation.

```java
var root = BrowserRoot.mount("app", new BrowserRoot.Scheduling(8, 32), state::view);
```

## Handle events

`LiveElement.on(type, handler)` binds a Java handler on the wrapped element; `on(type, options, handler)` adds native listener options, and `onClick(runnable)` covers the common case:

```java
LiveElement.of(Elements.div())
    .on("click", EventOptions.DEFAULT.withCapture(), event -> audit())
    .on("wheel", EventOptions.DEFAULT.withPassive(), event -> zoom(event.pointer()))
    .child(LiveElement.of(Elements.a("/help").text("Help")).on("click", event -> {
      event.preventDefault();
      openHelp();
    }));
```

Handlers run synchronously within the browser's dispatch and user activation, so default prevention and propagation calls take effect immediately, and a click handler may open a modal dialog. `event.control()`, `event.keyboard()` and `event.pointer()` copy typed data; asking for the wrong kind fails, and the failure is reported like any handler failure while the root stays interactive. An element keeps one handler per type and phase. Each render rebinds the current handler, so it may capture that render's state. A `once` handler stays spent while later renders keep binding it, and is re-armed after a render omits it.

## Bind form values

Set native defaults on the portable host with `initialValue`, `initialChecked`, or `initialSelectedValues`. Use `bindValue`, `bindChecked`, or `bindSelectedValues` on the live wrapper to apply application state. The binding sets the current property from the first mount onward. The portable default stays the baseline for native form reset.

```java
var enabled = LiveElement.of(Elements.input("checkbox").initialChecked(true))
    .bindChecked(state.enabled)
    .onCheckedChange(checked -> root.update(() -> state.enabled = checked).run());
var picks = LiveElement.of(Elements.select().boolAttr("multiple")
        .initialSelectedValues(List.of("en"))
        .children(Elements.option("en", "English"), Elements.option("pt", "Portuguese")))
    .bindSelectedValues(state.picks)
    .onSelectedValuesChange(values -> root.update(() -> state.picks = values).run());
```

| Binding | Accepted hosts | Change callback |
| --- | --- | --- |
| `bindValue(String)` | Input with an editable value, textarea, single select | `onValueChange` reports the value on `input` |
| `bindChecked(boolean)` | Checkbox or radio input | `onCheckedChange` reports checked state on `change` |
| `bindSelectedValues(List<String>)` | Multiple select | `onSelectedValuesChange` reports the selected values in option order on `input` |

Each callback observes the user's edit. Update application state and trigger a render to accept it. A render reapplies an unchanged binding if the user edited the control. Removing a binding hands the current state back to the browser. While an input method composes text, value writes wait for composition to finish.

Native form reset restores the insertion defaults. A later render reapplies any live bindings, so an application that wants reset to update both needs to update its state in the form's `reset` handler as well. Change the control's key when the application needs new native defaults.

## Adapt portable widgets

The live adapters use the same markup and styles as their portable components. They run their interactions inside the browser root and release native listeners when removed.

| Adapter | State and actions |
| --- | --- |
| `LiveTabs.of(tabs)` | The browser keeps the selected key while it remains enabled. If it disappears, selection moves to the next enabled tab, then the previous one. With no enabled tabs, none is selected. Use `bindSelection(Optional<String>)` and `onSelectionRequest` to own selection in the application. Give the portable tabs an explicit `id` so keys stay stable. |
| `LiveField.of(field, bindControl)` | Use the callback to bind the existing input, select, or other control. Set `validation(invalid, message)` to set or clear application-driven validity and the linked error message without resetting the control. |
| `LiveDialog.of(modalOrDrawer)` | The native dialog keeps its own open state by default. Use `bindOpen`, `onOpenRequest`, and `onDismiss` to own it in the application. Use `mode(Mode.NON_MODAL)` to open without making the page inert. Portable `opened()` starts a browser-owned dialog non-modally. Later invocations use the selected mode. `focusFallback(ref)` supplies a focus target if the opener is removed. |
| `LiveSearchPalette.of(palette)` | Place its `triggerButton()` and the adapter in the root. The browser retains the query and keeps focus on the current result when the list changes. It filters results, moves focus with the arrow keys, and dismisses the palette on Escape or result activation. |
| `LiveTooltip.of(trigger)` | Adapt a tag configured with `interestTooltip`. Hover and focus reveal the linked tooltip, and Escape dismisses it. |

Tabs use automatic keyboard activation by default. With `ContentTabs.Activation.MANUAL`, the arrow keys, Home and End move focus, and Enter or Space requests selection. A bound key must name an enabled tab. Use `Optional.empty()` when no enabled tabs remain, and update the binding in the same render that removes its selected tab.

A bound dialog reconciles after native dismissal. Set the bound state to false in `onDismiss` to accept a dismissal, or leave it true to keep the dialog open. The adapter requests a render after each callback.

```java
var dialog = LiveDialog.of(Modal.of("Discard changes?").body(Elements.p().text("Unsaved changes will be lost.")))
    .bindOpen(state.confirming)
    .onOpenRequest(() -> state.confirming = true)
    .onDismiss(() -> state.confirming = false);
return LiveElement.of(Elements.div())
    .child(dialog.triggerButton("Discard"))
    .child(dialog);
```

## Give children stable identities

Call `.key(...)` on an element, component or fragment whose position can change. Keys are exact strings, compared among the children of one parent.

```java
var entries = LiveElement.of(Elements.div());
for (var item : items) {
  entries.child(Live.fragment(
          LiveContent.of(Field.of(item.label(), Forms.text(item.id()))),
          LiveElement.of(Button.of("Choose " + item.label()).element()).onClick(() -> choose(item)))
      .key(item.id()));
}
```

A keyed child keeps its DOM, focus and form state while siblings are inserted, reordered or removed; changing its tag or component type replaces it. An unkeyed child matches the earliest unmatched previous sibling of its kind (same tag, text, or component type), so a field keeps its control while its hint or error appears beside it; key the children of lists whose same-kind siblings come and go. Ids bound inside keyed content are scoped by the key and the nearest named ref owner, in static output too, so they stay stable when it moves. Two lists in one view that reuse a key need a named ref on each parent, as in [Key list items that move](consumer-guide.md#key-list-items-that-move). Duplicate sibling keys report `duplicate-key`. Static compilation emits keyed content without any wrapper.

## Bind work to a lifetime

Place an effect where work should live, and register its cleanup on its `Lifetime`:

```java
var ticker = Live.effect("ticker-" + intervalMillis, lifetime -> {
  var timer = startTimer(intervalMillis, lifetime.guard(root.update(() -> ticks++)));
  lifetime.onDispose(timer::cancel);
});
```

The effect starts after its tree is committed and keeps running while renders place the same key there; the key names its dependencies. It is a sibling key like `key(...)`, so it must differ from the keys of the effect's keyed siblings. Removing it, changing its key or closing the root disposes its lifetime, and a key change then starts the new effect. Use `root.lifetime()` for root-wide cleanup. A cleanup that closes its own root settles at the commit boundary: the committed tree is released once, later effects in the same batch do not start, and nothing detached runs application handlers afterwards.

## Use browser services

`root.host()` reaches the browser on behalf of a root without handing out native objects. Element operations take an `ElementRef`:

```java
final class Discard {
  final ElementRef confirm = ElementRef.scoped("confirm");
  final ElementRef draft = ElementRef.scoped("draft");
  FocusReturn returnFocus;
  String saved = "";
  RequestFailure error;

  LiveContent view(LiveContext context) {
    var host = context.root().host();
    return LiveElement.of(Elements.main().children(
            Elements.input("text").id(draft).ariaLabel("Draft"),
            Modal.of("Discard changes?").dialogRef(confirm)))
        .children(
            LiveElement.of(Button.of("Discard").element()).onClick(() -> {
              returnFocus = host.rememberFocus();
              host.showModal(confirm);
            }),
            LiveElement.of(Button.of("Select draft").element()).onClick(() -> host.selectAll(draft)),
            LiveElement.of(Button.of("Autosave").element()).onClick(() ->
                host.fetchText("/save", body -> saved = body, failure -> error = failure)));
  }
}
```

Timers (`timeout`, `interval`, `frame`), `afterRender` tasks and `fetchText` completions run only while the root is open; timers and request completions then re-render the root. Closing the root cancels them and aborts requests. Inside an effect, `host.within(lifetime)` cancels them with the effect instead.

References resolve against the latest committed render. A handler may use references created in the same render. Anything that runs later, such as a timer, request callback, effect or `afterRender` task, needs a reference kept in application state and passed to the component on every render, as `confirm` is above. `Modal.dialogRef(ref)`, `Drawer.dialogRef(ref)` and `Dropdown.panelRef(ref)` accept such references.

## Animate values without new rules

Every distinct style value becomes its own rule, so a value that changes on every render would keep adding rules. Write such values with `live(...)`:

```java
Elements.div()
    .blockSize(CssLength.px(4))
    .live(style -> style.inlineSize(CssLength.px(progress)));
```

In a browser mount the element keeps one style rule however the value changes; the value travels as a custom property set on the element. Static pages compile it like any other declaration. Keep values that rarely change as ordinary declarations.
