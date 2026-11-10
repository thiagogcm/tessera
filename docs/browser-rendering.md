# Browser rendering (experimental)

The opt-in `tessera-wasm` module mounts the same components in a page as Java compiled to WebAssembly with GraalVM Web Image. A mounted view re-renders from application state, and the backend reconciles the native DOM so focus, selection and form values survive updates. The separate `tessera-wasm-showcase` module builds the runnable images and runs browser acceptance. The [roadmap](roadmap.md) describes planned capability and module boundaries.

A browser root receives the same `DocumentContext` and expands components exactly like the compiler. It has no document, so head contributions, document-end sections, page scripts (including the interest-tooltip fallback), deferred and late elements, and raw HTML are reported as `native-*` diagnostics; the render is rejected and the committed view stays. Per-page state from `context.document(key)` and headings last one render. The other way round, static compilation rejects the live-only features below with `native-event-handlers`, `native-control-properties` and `native-effect`, so a component that uses them belongs in a browser mount.

## Build and verify

Use Oracle GraalVM **25.4.4.1.1**, matching the `webimage-preview` SDK pinned in [tessera-wasm/pom.xml](../tessera-wasm/pom.xml). Install Native Image prerequisites and Binaryen **119 or later** on `PATH`. Set `JAVA_HOME` to that GraalVM distribution. The default JVM build does not invoke these tools.

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

The entry's `main` mounts every host the page declares for it. The view callback receives the root and the render context:

```java
public final class Editor {
  private String name = "";
  private boolean saved;

  public static void main(String[] args) {
    var editor = new Editor();
    BrowserEntry.start(args, Map.of("editor", host -> BrowserRoot.mount(host, editor::view)));
  }

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    return Elements.main().children(
        Field.of("Name", Forms.text("name").value(name)
            .onEvent("input", event -> {
              name = event.control().value();
              root.invalidate();
            })),
        Button.of("Save").onClick(root.update(() -> saved = true)),
        Elements.p().text(saved ? "Saved " + name : "Not saved"));
  }
}
```

Several entries can share a page; each launcher loads once however many hosts it serves. The first render replaces the fallback content; a render that fails keeps it and a later invalidation retries. `root.close()` empties the host, which can then be mounted again. Generated and `ElementRef.scoped` ids are prefixed with the host id, so roots never collide; an id that another root or the host page already uses (such as an `ElementRef.page` id) rejects the render with `cross-root-id`, as does mounting on a host that already holds a root. The check covers ids a render introduces: once a root has committed an id, it owns it, and an element the host page adds later with that id does not reject the root's renders.

## Schedule updates

Call `root.invalidate()` after application state changes. Repeated invalidations before the scheduled task coalesce into one render, and a render that calls `invalidate()` schedules a later turn instead of recursing. `root.update(change)` returns a `Runnable` that applies `change` and invalidates the root, the usual shape for click handlers. Wrap an asynchronous completion when its work starts: after `root.close()` the wrapper does nothing, so a late result leaves the disposed root untouched.

`BrowserRoot.Scheduling(workBudget, runawayLimit)` configures one root. The default applies eight render turns per browser task, then waits for an animation frame so the browser can paint, and reports a runaway after 32 consecutive turns that each request another render. A runaway root reports one browser error, keeps the last committed tree and accepts a later external invalidation.

```java
var root = BrowserRoot.mount("app", new BrowserRoot.Scheduling(8, 32), state::view);
```

## Handle events

`onEvent(type, handler)` binds a Java handler on any element; `onEvent(type, options, handler)` adds native listener options, and `Button.onClick(runnable)` covers the common case:

```java
Elements.div()
    .onEvent("click", EventOptions.DEFAULT.withCapture(), event -> audit())
    .onEvent("wheel", EventOptions.DEFAULT.withPassive(), event -> zoom(event.pointer()))
    .child(Elements.a("/help").text("Help").onEvent("click", event -> {
      event.preventDefault();
      openHelp();
    }));
```

Handlers run synchronously within the browser's dispatch and user activation, so default prevention and propagation calls take effect immediately, and a click handler may open a modal dialog. `event.control()`, `event.keyboard()` and `event.pointer()` copy typed data; asking for the wrong kind fails, and the failure is reported like any handler failure while the root stays interactive. An element keeps one handler per type and phase. Each render rebinds the current handler, so it may capture that render's state. A `once` handler stays spent while later renders keep binding it, and is re-armed after a render omits it.

## Bind form values

Initial HTML and live state are separate. `value(String)`, `checked(boolean)` and `selectedValues(List<String>)` control the current value, checked state or multiple-select selection; attributes such as `value`, `checked` and `selected` still describe the initial markup.

```java
var enabled = Elements.input("checkbox").checked(state.enabled)
    .onEvent("change", event -> {
      state.enabled = event.control().checked();
      root.invalidate();
    });
var picks = Elements.select().boolAttr("multiple").selectedValues(state.picks)
    .onEvent("change", event -> {
      state.picks = event.control().selectedValues();
      root.invalidate();
    });
```

| Binding | Accepted hosts | Event data |
| --- | --- | --- |
| `value(String)` | Input other than file, textarea, single select | `ControlState.value()` |
| `checked(boolean)` | Checkbox or radio input | `ControlState.checked()` |
| `selectedValues(List<String>)` | Multiple select | `ControlState.selectedValues()` |

Another host reports `control-value`, `control-checked` or `control-selection`. Each render compares the desired value with the control's actual state: equal values are not written, so caret, selection and focus survive unrelated updates, and different values are written even when Java state did not change, which restores an edit the application rejected. While an input method composes text (`ControlState.composing()`), value writes wait for the composition to end. `uncontrolled()` hands the control back to the browser without changing its initial attributes.

## Give children stable identities

Call `.key(...)` on an element, component or fragment whose position can change. Keys are exact strings, compared among the children of one parent.

```java
var entries = Elements.div();
for (var item : items) {
  entries.child(Elements.fragment().children(
      Field.of(item.label(), Forms.text(item.id())),
      Button.of("Choose " + item.label()))
      .key(item.id()));
}
```

A keyed child keeps its DOM, focus and form state while siblings are inserted, reordered or removed; changing its tag or component type replaces it. An unkeyed child matches the earliest unmatched previous sibling of its kind (same tag, text, or component type), so a field keeps its control while its hint or error appears beside it; key the children of lists whose same-kind siblings come and go. Ids bound inside keyed content are scoped by the key and the nearest named ref owner, in static output too, so they stay stable when it moves. Two lists in one view that reuse a key need a named ref on each parent, as in [Key list items that move](consumer-guide.md#key-list-items-that-move). Duplicate sibling keys report `duplicate-key`. Static compilation emits keyed content without any wrapper.

## Bind work to a lifetime

Place an effect where work should live, and register its cleanup on its `Lifetime`:

```java
var ticker = Elements.effect("ticker-" + intervalMillis, lifetime -> {
  var timer = startTimer(intervalMillis, lifetime.guard(root.update(() -> ticks++)));
  lifetime.onDispose(timer::cancel);
});
```

The effect starts after its tree is committed and keeps running while renders place the same key there; the key names its dependencies. It is a sibling key like `key(...)`, so it must differ from the keys of the effect's keyed siblings. Removing it, changing its key or closing the root disposes its lifetime, and a key change then starts the new effect. Use `root.lifetime()` for root-wide cleanup.

## Use browser services

`root.host()` reaches the browser on behalf of a root without handing out native objects. Element operations take an `ElementRef`:

```java
final class Discard {
  final ElementRef confirm = ElementRef.scoped("confirm");
  final ElementRef draft = ElementRef.scoped("draft");
  FocusReturn returnFocus;
  String saved = "";
  RequestFailure error;

  IntoElement view(BrowserRoot root, DocumentContext context) {
    var host = root.host();
    return Elements.main().children(
        Elements.input("text").id(draft).ariaLabel("Draft"),
        Modal.of("Discard changes?").dialogRef(confirm),
        Button.of("Discard").onClick(() -> {
          returnFocus = host.rememberFocus();
          host.showModal(confirm);
        }),
        Button.of("Select draft").onClick(() -> host.selectAll(draft)),
        Button.of("Autosave").onClick(() ->
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
