const NAVIGATION_KEYS = new Set(["ArrowLeft", "ArrowRight", "Home", "End"]);

class TesseraTabs extends HTMLElement {
  #controller;
  #tabs = [];
  #panels = [];

  connectedCallback() {
    this.#controller = new AbortController();
    const { signal } = this.#controller;
    this.#tabs = Array.from(this.querySelectorAll(':scope > [role="tablist"] > [role="tab"]'));
    this.#panels = this.#tabs.map((tab) =>
      document.getElementById(tab.getAttribute("aria-controls")),
    );
    const initial = this.#tabs.findIndex((tab) => tab.getAttribute("aria-selected") === "true");
    this.#select(initial);

    this.addEventListener(
      "click",
      (event) => {
        const tab = event.target.closest('[role="tab"]');
        const index = this.#tabs.indexOf(tab);
        if (index >= 0 && !tab.disabled) this.#select(index);
      },
      { signal },
    );
    this.addEventListener("keydown", (event) => this.#navigate(event), { signal });
    this.addEventListener("focusout", (event) => {
      if (!this.#tabs.includes(event.relatedTarget)) {
        this.#tabStop(this.#tabs.findIndex((tab) => tab.getAttribute("aria-selected") === "true"));
      }
    }, { signal });
  }

  disconnectedCallback() {
    this.#controller.abort();
  }

  #navigate(event) {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
    if (!NAVIGATION_KEYS.has(event.key)) return;
    const tab = event.target.closest('[role="tab"]');
    const enabled = this.#tabs.filter((candidate) => !candidate.disabled);
    const current = enabled.indexOf(tab);
    if (current < 0) return;

    // Home/End jump to the ends; arrows wrap and follow visual (RTL-aware) order.
    const rtl = getComputedStyle(this).direction === "rtl";
    const forward = rtl ? "ArrowLeft" : "ArrowRight";
    const backward = rtl ? "ArrowRight" : "ArrowLeft";
    const next = {
      [forward]: (current + 1) % enabled.length,
      [backward]: (current - 1 + enabled.length) % enabled.length,
      Home: 0,
      End: enabled.length - 1,
    }[event.key];
    if (next === undefined) return;

    event.preventDefault();
    const target = enabled[next];
    const index = this.#tabs.indexOf(target);
    if (this.dataset.activation === "manual") this.#tabStop(index);
    else this.#select(index);
    target.focus();
  }

  #tabStop(index) {
    this.#tabs.forEach((tab, i) => { tab.tabIndex = i === index ? 0 : -1; });
  }

  #select(index) {
    this.#tabStop(index);
    this.#tabs.forEach((tab, i) => {
      const selected = i === index;
      tab.setAttribute("aria-selected", String(selected));
      const panel = this.#panels[i];
      if (panel) panel.hidden = !selected;
    });
  }
}

customElements.define("tessera-tabs", TesseraTabs);
