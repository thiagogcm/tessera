package dev.nthings.tessera.wasm;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Web Image interop stays here rather than in component contracts.
final class Dom {

  private Dom() {}

  @JS.Coerce
  @JS(
      args = {"id"},
      value =
          """
          const host = document.getElementById(id);
          if (!host) throw new Error('Missing Tessera mount host: ' + id);
          return host;
          """)
  static native JSObject host(String id);

  @JS.Coerce
  @JS(
      args = {"tag"},
      value = "return document.createElement(tag);")
  static native JSObject element(String tag);

  @JS.Coerce
  @JS(
      args = {"text"},
      value = "return document.createTextNode(text);")
  static native JSObject text(String text);

  @JS("return document.createComment('');")
  static native JSObject marker();

  @JS.Coerce
  @JS(
      args = {"node", "name", "value"},
      value = "node.setAttribute(name, value);")
  static native void attribute(JSObject node, String name, String value);

  @JS.Coerce
  @JS(
      args = {"node", "name"},
      value = "node.removeAttribute(name);")
  static native void removeAttribute(JSObject node, String name);

  @JS.Coerce
  @JS(
      args = {"node", "name", "value"},
      value = "node.style.setProperty(name, value);")
  static native void setStyleProperty(JSObject node, String name, String value);

  @JS.Coerce
  @JS(
      args = {"node", "name"},
      value = "node.style.removeProperty(name);")
  static native void removeStyleProperty(JSObject node, String name);

  @JS.Coerce
  @JS(
      args = {"node", "text"},
      value = "node.nodeValue = text;")
  static native void textValue(JSObject node, String text);

  @JS(
      args = {"parent", "node", "before"},
      value = "parent.insertBefore(node, before);")
  static native void insert(JSObject parent, JSObject node, @Nullable JSObject before);

  @JS(
      args = {"parent", "first", "last", "before"},
      value =
          """
      if (first.parentNode === parent && last.nextSibling === before) return;
      const stop = last.nextSibling;
      for (let node = first; node !== stop;) {
        const next = node.nextSibling;
        if (parent.moveBefore && node.isConnected && parent.isConnected) {
          parent.moveBefore(node, before);
        } else {
          parent.insertBefore(node, before);
        }
        node = next;
      }
      """)
  static native void moveRange(
      JSObject parent, JSObject first, JSObject last, @Nullable JSObject before);

  @JS(
      args = {"first", "last"},
      value =
          """
      const stop = last.nextSibling;
      for (let node = first; node !== stop;) {
        const next = node.nextSibling;
        node.remove();
        node = next;
      }
      """)
  static native void removeRange(JSObject first, JSObject last);

  @JS(
      args = {"host"},
      value = "host.replaceChildren();")
  static native void clear(JSObject host);

  /// Adopts a new constructed stylesheet into the document. Constructed sheets need no inline
  /// `<style>` element, so they work under a `style-src` policy without `'unsafe-inline'`.
  @JS(
      """
      const sheet = new CSSStyleSheet();
      document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet];
      return sheet;
      """)
  static native JSObject adoptStyleSheet();

  /// Appends `rule` and returns the inserted `CSSRule`. A rule the browser cannot parse is
  /// dropped, as in a parsed stylesheet, and yields `null`.
  @JS.Coerce
  @JS(
      args = {"sheet", "rule"},
      value =
          """
          try {
            return sheet.cssRules[sheet.insertRule(rule, sheet.cssRules.length)];
          } catch (error) {
            if (error instanceof DOMException && error.name === 'SyntaxError') return null;
            throw error;
          }
          """)
  static native @Nullable JSObject insertRule(JSObject sheet, String rule);

  /// An empty set of `CSSRule`s for [#deleteRules].
  @JS("return new Set();")
  static native JSObject ruleSet();

  /// Adds a rule returned by [#insertRule] to `rules`.
  @JS(
      args = {"rules", "rule"},
      value = "rules.add(rule);")
  static native void addRule(JSObject rules, JSObject rule);

  /// Deletes every rule in `rules` from `sheet` in one pass; indices shift as rules come and go,
  /// so it walks from the end.
  @JS(
      args = {"sheet", "rules"},
      value =
          """
          const list = sheet.cssRules;
          for (let i = list.length - 1; i >= 0 && rules.size > 0; i--) {
            if (rules.delete(list[i])) sheet.deleteRule(i);
          }
          """)
  static native void deleteRules(JSObject sheet, JSObject rules);

  @JS.Coerce
  @JS(
      args = {"node", "type", "capture", "passive", "once", "handler"},
      value =
          """
          const listener = event => handler(event);
          node.addEventListener(type, listener, {capture, passive, once});
          return () => node.removeEventListener(type, listener, {capture});
          """)
  static native JSObject listen(
      JSObject node,
      String type,
      boolean capture,
      boolean passive,
      boolean once,
      Consumer<JSObject> handler);

  /// Bubble-phase listener without options, for the reconciler's own bookkeeping.
  static JSObject listen(JSObject node, String type, Consumer<JSObject> handler) {
    return listen(node, type, false, false, false, handler);
  }

  /// Runs a cleanup returned by [#listen], [#timeout], [#interval], [#frame], [#paint] or
  /// [#fetchText].
  @JS(
      args = {"cleanup"},
      value = "cleanup();")
  static native void release(JSObject cleanup);

  /// Runs `work` in a later task; returns a cleanup for [#release].
  static JSObject defer(Runnable work) {
    return timeout(0, work);
  }

  @JS.Coerce
  @JS(
      args = {"work"},
      value =
          """
          let timer = -1;
          const frame = requestAnimationFrame(() => {
            timer = setTimeout(work, 0);
          });
          return () => {
            cancelAnimationFrame(frame);
            if (timer !== -1) clearTimeout(timer);
          };
          """)
  static native JSObject paint(Runnable work);

  @JS.Coerce
  @JS(
      args = {"message"},
      value = "globalThis.reportError(new Error(message));")
  static native void reportError(String message);

  @JS(
      args = {"event"},
      value = "event.preventDefault();")
  static native void preventDefault(JSObject event);

  @JS(
      args = {"event"},
      value = "event.stopPropagation();")
  static native void stopPropagation(JSObject event);

  @JS(
      args = {"event"},
      value = "event.stopImmediatePropagation();")
  static native void stopImmediatePropagation(JSObject event);

  @JS.Coerce
  @JS(
      args = {"event", "name"},
      value = "return String(event[name] ?? '');")
  static native String eventString(JSObject event, String name);

  @JS.Coerce
  @JS(
      args = {"object", "name"},
      value = "return object[name] === true;")
  static native boolean flag(JSObject object, String name);

  @JS.Coerce
  @JS(
      args = {"object", "name"},
      value = "return Number(object[name] ?? 0);")
  static native double number(JSObject object, String name);

  @JS.Coerce
  @JS(
      args = {"event"},
      value = "return event instanceof KeyboardEvent;")
  static native boolean isKeyboardEvent(JSObject event);

  @JS.Coerce
  @JS(
      args = {"event"},
      value = "return event instanceof MouseEvent;")
  static native boolean isMouseEvent(JSObject event);

  /// Closes open dialogs and hides open popovers beneath `host`, before it is emptied.
  @JS(
      args = {"host"},
      value =
          """
          for (const dialog of host.querySelectorAll('dialog[open]')) dialog.close();
          for (const popover of host.querySelectorAll(':popover-open')) popover.hidePopover();
          """)
  static native void closeTransient(JSObject host);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.value;")
  static native String value(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node", "value"},
      value = "node.value = value;")
  static native void value(JSObject node, String value);

  @JS.Coerce
  @JS(
      args = {"node", "checked"},
      value = "node.checked = checked;")
  static native void checked(JSObject node, boolean checked);

  /// The form state of `node` while `event` is dispatched, read in one call and decoded by
  /// [ControlCodec#state]: the checked and composing flags as `0` or `1`, then the value and each
  /// selected option's value (a select's options only), each as `<length>:<text>`.
  @JS.Coerce
  @JS(
      args = {"node", "event"},
      value =
          """
          const text = value => value.length + ':' + value;
          let state = (node.checked === true ? '1' : '0') + (event.isComposing === true ? '1' : '0');
          state += text(String(node.value));
          if (node.options) {
            for (const option of node.options) if (option.selected) state += text(option.value);
          }
          return state;
          """)
  static native String controlState(JSObject node, JSObject event);

  /// Selects exactly the options of a multiple select whose values are in `values`, encoded by
  /// [ControlCodec#values], changing only options whose state differs.
  @JS.Coerce
  @JS(
      args = {"node", "values"},
      value =
          """
          const wanted = new Set();
          for (let at = 0; at < values.length;) {
            const colon = values.indexOf(':', at);
            const end = colon + 1 + Number(values.slice(at, colon));
            wanted.add(values.slice(colon + 1, end));
            at = end;
          }
          for (const option of node.options) {
            const selected = wanted.has(option.value);
            if (option.selected !== selected) option.selected = selected;
          }
          """)
  static native void selectValues(JSObject node, String values);

  // --- Host services ------------------------------------------------------------------------

  /// The element with `id` beneath `host`, or `null`; never an element of another root. Ids are
  /// unique across roots, so the document lookup normally finds it; the scoped query covers an
  /// element shadowed by the host or by host-page content with the same id.
  @JS.Coerce
  @JS(
      args = {"host", "id"},
      value =
          """
          const node = document.getElementById(id);
          if (node !== null && node !== host && host.contains(node)) return node;
          return host.querySelector('#' + CSS.escape(id));
          """)
  static native @Nullable JSObject find(JSObject host, String id);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.tagName;")
  static native String tagName(JSObject node);

  @JS(
      args = {"node"},
      value = "node.focus();")
  static native void focus(JSObject node);

  @JS("return document.activeElement;")
  static native @Nullable JSObject activeElement();

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return document.activeElement === node;")
  static native boolean isActiveElement(JSObject node);

  /// Whether `node` supports `setSelectionRange`, which throws for inputs such as checkboxes.
  @JS.Coerce
  @JS(
      args = {"node"},
      value =
          """
          if (node instanceof HTMLTextAreaElement) return true;
          if (!(node instanceof HTMLInputElement)) return false;
          return node.selectionStart !== null;
          """)
  static native boolean hasSelection(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.selectionStart;")
  static native int selectionStart(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.selectionEnd;")
  static native int selectionEnd(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.selectionDirection ?? 'none';")
  static native String selectionDirection(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node", "start", "end"},
      value = "node.setSelectionRange(start, end);")
  static native void setSelection(JSObject node, int start, int end);

  @JS(
      args = {"node"},
      value = "node.select();")
  static native void selectAll(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.matches(':modal');")
  static native boolean isModal(JSObject node);

  @JS(
      args = {"node"},
      value = "node.showModal();")
  static native void showModal(JSObject node);

  @JS(
      args = {"node"},
      value = "node.show();")
  static native void show(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node", "returnValue"},
      value = "node.close(returnValue);")
  static native void closeDialog(JSObject node, String returnValue);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.hasAttribute('popover');")
  static native boolean hasPopover(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.matches(':popover-open');")
  static native boolean popoverOpen(JSObject node);

  @JS(
      args = {"node"},
      value = "node.showPopover();")
  static native void showPopover(JSObject node);

  @JS(
      args = {"node"},
      value = "node.hidePopover();")
  static native void hidePopover(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node.togglePopover();")
  static native boolean togglePopover(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node", "playing", "failed"},
      value =
          "node.play().then(() => playing(), error => failed(error.name + ': ' + error.message));")
  static native void play(JSObject node, Runnable playing, Consumer<Object> failed);

  @JS(
      args = {"node"},
      value = "node.pause();")
  static native void pause(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node", "seconds"},
      value = "node.currentTime = seconds;")
  static native void seek(JSObject node, double seconds);

  @JS.Coerce
  @JS(
      args = {"node", "muted"},
      value = "node.muted = muted;")
  static native void mute(JSObject node, boolean muted);

  @JS.Coerce
  @JS(
      args = {"millis", "task"},
      value =
          """
          const id = setTimeout(task, millis);
          return () => clearTimeout(id);
          """)
  static native JSObject timeout(int millis, Runnable task);

  @JS.Coerce
  @JS(
      args = {"millis", "task"},
      value =
          """
          const id = setInterval(task, millis);
          return () => clearInterval(id);
          """)
  static native JSObject interval(int millis, Runnable task);

  @JS.Coerce
  @JS(
      args = {"task"},
      value =
          """
          const id = requestAnimationFrame(() => task());
          return () => cancelAnimationFrame(id);
          """)
  static native JSObject frame(Runnable task);

  /// Fetches text with a per-request controller; returns the abort cleanup owned by its lifetime.
  /// An abort reports nothing, since only cancellation or disposal aborts.
  @JS.Coerce
  @JS(
      args = {"url", "completed", "failed"},
      value =
          """
          const controller = new AbortController();
          fetch(url, {signal: controller.signal})
            .then(async response => {
              const body = await response.text();
              if (response.ok) completed(body); else failed(response.status, body);
            })
            .catch(error => {
              if (error.name !== 'AbortError') failed(0, String(error.message));
            });
          return () => controller.abort();
          """)
  static native JSObject fetchText(
      String url, Consumer<Object> completed, BiConsumer<Object, Object> failed);

  // --- Cross-root identity ------------------------------------------------------------------

  /// Why `host` cannot take a new root, or `null`: it already holds one, lies inside one, or
  /// contains one. Reads the document, so it also sees roots of other compiled entries.
  @JS.Coerce
  @JS(
      args = {"host", "attribute"},
      value =
          """
          const selector = '[' + attribute + ']';
          if (host.hasAttribute(attribute)) return "already holds a browser root";
          const outer = host.parentElement && host.parentElement.closest(selector);
          if (outer) return "is inside browser root '" + outer.id + "'";
          const inner = host.querySelector(selector);
          if (inner) return "contains browser root '" + inner.id + "'";
          return null;
          """)
  static native @Nullable String mountConflict(JSObject host, String attribute);

  /// The first of the newline-separated `ids` that an element outside `host` already uses,
  /// followed by a newline and that element's owner, or `null`.
  @JS.Coerce
  @JS(
      args = {"host", "ids", "attribute"},
      value =
          """
          for (const id of ids.split('\\n')) {
            const element = id && document.getElementById(id);
            if (element && !host.contains(element)) {
              const root = element.closest('[' + attribute + ']');
              return id + '\\n' + (root ? "browser root '" + root.id + "'" : 'the host page');
            }
          }
          return null;
          """)
  static native @Nullable String foreignId(JSObject host, String ids, String attribute);
}
