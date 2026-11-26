const KEY = "tessera-color-scheme";
const ORDER = ["system", "light", "dark"];
const root = document.documentElement;

const current = () => {
  const scheme = root.dataset.colorScheme;
  return scheme === "light" || scheme === "dark" ? scheme : "system";
};

const show = (scheme) => {
  if (scheme === "system") delete root.dataset.colorScheme;
  else root.dataset.colorScheme = scheme;
  for (const toggle of document.querySelectorAll("tessera-color-scheme")) toggle.sync();
};

class TesseraColorScheme extends HTMLElement {
  #controller;

  connectedCallback() {
    this.#controller = new AbortController();
    this.#button().addEventListener("click", () => {
      const next = ORDER[(ORDER.indexOf(current()) + 1) % ORDER.length];
      try {
        if (next === "system") localStorage.removeItem(KEY);
        else localStorage.setItem(KEY, next);
      } catch {
        // Storage can be unavailable; the choice then lasts for this page only.
      }
      show(next);
    }, { signal: this.#controller.signal });
    this.sync();
  }

  disconnectedCallback() {
    this.#controller.abort();
  }

  sync() {
    this.#button().setAttribute("aria-label", `Color scheme: ${current()}`);
  }

  #button() {
    return this.querySelector("button[data-color-scheme-toggle]");
  }
}

customElements.define("tessera-color-scheme", TesseraColorScheme);

// Another tab of the site changed the scheme.
addEventListener("storage", (event) => {
  if (event.key === KEY || event.key === null) {
    show(event.newValue === "light" || event.newValue === "dark" ? event.newValue : "system");
  }
});
