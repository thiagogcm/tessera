# Browser rendering (experimental)

The opt-in `tessera-wasm` module mounts the same components in a page as Java compiled to WebAssembly with GraalVM Web Image. A mounted view re-renders from application state, and the backend reconciles the native DOM so focus, selection and form values survive updates. The separate `tessera-wasm-showcase` module builds the runnable images and runs browser acceptance. The [roadmap](roadmap.md) describes planned capability and module boundaries.

A browser root renders `LiveContent`: portable content plus the live-only features below. Portable components expand exactly as the compiler expands them and receive the same portable `ViewContext`. Live content cannot be placed in portable or document content, and document content (raw HTML, deferred and late content, document components) cannot be placed in a root; both mistakes are compile errors. Expansion state from `context.state(key)` lasts one attempted render. A root loads no page scripts, so portable widgets with a bundled script, such as `ContentTabs` or the interest-tooltip fallback, render their structure without it.

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

Install the Playwright browsers with `just browsers` before verification. The acceptance suite serves the compiled distribution and exercises root lifetimes, events, controls, identity, styling, and scheduling. See [runtime internals](browser-internals.md) for the implementation contracts.

## Mount a view

Declare each mount on its host, with fallback content for browsers that cannot start it:

```html
<div id="editor" data-tessera-entry="editor.js" data-tessera-view="editor">
  <p>The editor needs a current browser.</p>
</div>
<script defer src="bootstrap.js"></script>
```

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
        .child(LiveElement.of(input).value(name).on("input", event -> {
          name = event.control().value();
          root.invalidate();
        }))
        .child(LiveElement.of(Button.of("Save").element()).onClick(root.update(() -> saved = true)))
        .child(Elements.p().text(saved ? "Saved " + name : "Not saved"));
  }
}
```

`LiveElement.of(tag)` wraps a portable element without changing it, so the same `TagElement` or component markup stays valid in a static page. Portable content is accepted directly as a child of live content; `LiveContent.of(content)` converts it where a `LiveContent` value is required. `LiveComponent` and `LiveScope` are the live counterparts of `Component` and `Scope`. Buttons expose `element()` so a `LiveElement` can bind their handlers.

Several entries can share a page; each launcher loads once however many hosts it serves. The first committed render replaces fallback content. A rejected candidate keeps it and a later invalidation retries; a native commit failure can clear it. `root.close()` empties the host, which can then be mounted again.

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

Initial HTML and live state are separate. `LiveElement.value(String)`, `checked(boolean)` and `selectedValues(List<String>)` control the current value, checked state or multiple-select selection; attributes such as `value`, `checked` and `selected` on the portable element still describe the initial markup.

```java
var enabled = LiveElement.of(Elements.input("checkbox")).checked(state.enabled)
    .on("change", event -> {
      state.enabled = event.control().checked();
      root.invalidate();
    });
var picks = LiveElement.of(Elements.select().boolAttr("multiple")).selectedValues(state.picks)
    .on("change", event -> {
      state.picks = event.control().selectedValues();
      root.invalidate();
    });
```

| Binding | Accepted hosts | Event data |
| --- | --- | --- |
| `value(String)` | Input other than file, textarea, single select | `ControlState.value()` |
| `checked(boolean)` | Checkbox or radio input | `ControlState.checked()` |
| `selectedValues(List<String>)` | Multiple select | `ControlState.selectedValues()` |

Another host reports `control-value`, `control-checked` or `control-selection`. Each render compares the desired value with the control's actual state: equal values are not written, so caret, selection and focus survive unrelated updates, and different values are written even when Java state did not change, which restores an edit the application rejected. While an input method composes text (`ControlState.composing()`), value writes wait for the composition to end. A render that leaves a binding off hands the control back to the browser without changing its initial attributes.

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
