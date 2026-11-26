const normalize = (text) => text.normalize("NFKD").replace(/\p{M}/gu, "").toLowerCase();

class TesseraSearch extends HTMLDialogElement {
  #controller;
  #input;
  #entries;
  #empty;

  connectedCallback() {
    this.#controller = new AbortController();
    const { signal } = this.#controller;
    this.#input = this.querySelector("input[type=search]");
    this.#empty = this.querySelector(".palette-empty");
    this.#entries = Array.from(this.querySelectorAll(".palette-item"), (link) => ({
      link,
      text: normalize(`${link.closest(".palette-group").querySelector(".palette-group-title").textContent} ${link.textContent}`),
    }));

    this.#input.addEventListener("input", () => this.#filter(), { signal });
    this.addEventListener("keydown", (event) => this.#navigate(event), { signal });
    this.addEventListener("click", (event) => {
      if (
        !event.defaultPrevented && event.button === 0 &&
        !event.altKey && !event.ctrlKey && !event.metaKey && !event.shiftKey &&
        event.target.closest(".palette-item")
      ) this.requestClose();
    }, { signal });
    this.addEventListener("toggle", (event) => {
      if (event.newState === "open") this.#filter();
    }, { signal });
    this.#filter();
  }

  disconnectedCallback() {
    this.#controller.abort();
  }

  #visibleEntries() {
    return this.#entries.filter(({ link }) => !link.hidden);
  }

  #filter() {
    const terms = normalize(this.#input.value).trim().split(/\s+/).filter(Boolean);
    for (const { link, text } of this.#entries) {
      link.hidden = !terms.every((term) => text.includes(term));
    }
    this.#empty.hidden = this.#visibleEntries().length !== 0;
  }

  #navigate(event) {
    if (event.isComposing || event.altKey || event.ctrlKey || event.metaKey) return;
    if (event.target !== this.#input && !event.target.matches(".palette-item")) return;

    if (event.key === "Escape") {
      event.preventDefault();
      this.requestClose();
      return;
    }

    const visible = this.#visibleEntries();
    if (event.key === "Enter" && event.target === this.#input) {
      event.preventDefault();
      visible.at(0)?.link.click();
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      if (visible.length === 0) return;
      event.preventDefault();
      const forward = event.key === "ArrowDown";
      const current = visible.findIndex(({ link }) => link === document.activeElement);
      const next = current < 0
        ? (forward ? 0 : -1)
        : (current + (forward ? 1 : -1) + visible.length) % visible.length;
      visible.at(next).link.focus();
    }
  }
}

customElements.define("tessera-search", TesseraSearch, { extends: "dialog" });

const APPLE = /Mac|iPhone|iPad/.test(navigator.userAgentData?.platform ?? navigator.platform);
if (APPLE) {
  for (const key of document.querySelectorAll("kbd[data-mod-key]")) key.textContent = "⌘";
}

const EDITABLE = "input, textarea, select, [contenteditable]:not([contenteditable=false])";

// Whether `event` is the chord `shortcut`, written as in aria-keyshortcuts ("Control+K", "/").
// Letters match by physical key, so Alt+K still matches where Option turns K into another
// character. Other keys match by the character typed, ignoring the Shift some layouts need for it.
const matches = (event, shortcut) => {
  const parts = shortcut.split("+");
  const key = parts.pop();
  const letter = /^[A-Z]$/.test(key);
  if (letter ? event.code !== `Key${key}` : event.key !== key) return false;
  if (event.ctrlKey !== parts.includes("Control") || event.metaKey !== parts.includes("Meta") ||
      event.altKey !== parts.includes("Alt")) return false;
  if (letter && event.shiftKey !== parts.includes("Shift")) return false;
  // A bare character is typing while focus is in a field.
  return parts.length > 0 || !event.target.closest?.(EDITABLE);
};

// Each trigger lists its chords in aria-keyshortcuts. The first trigger that advertises the chord
// pressed opens its palette, in every browser, whatever the browser does with accesskey.
document.addEventListener("keydown", (event) => {
  if (event.defaultPrevented || event.isComposing || event.repeat) return;
  for (const trigger of document.querySelectorAll("button[aria-keyshortcuts][commandfor]")) {
    const dialog = document.getElementById(trigger.getAttribute("commandfor"));
    if (!(dialog instanceof TesseraSearch)) continue;
    const shortcuts = trigger.getAttribute("aria-keyshortcuts").split(/\s+/).filter(Boolean);
    if (!shortcuts.some((shortcut) => matches(event, shortcut))) continue;
    event.preventDefault();
    // Like the button itself, the chord does nothing while a modal dialog covers the page.
    if (!document.querySelector("dialog:modal")) trigger.click();
    return;
  }
});
