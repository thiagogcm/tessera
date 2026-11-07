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
    const initial = Math.max(
      0,
      this.#tabs.findIndex((tab) => tab.getAttribute("aria-selected") === "true"),
    );
    this.#select(initial, false);

    this.addEventListener(
      "click",
      (event) => {
        const tab = event.target.closest('[role="tab"]');
        const index = this.#tabs.indexOf(tab);
        if (index >= 0) this.#select(index, false);
      },
      { signal },
    );
    this.addEventListener("keydown", (event) => this.#navigate(event), { signal });
  }

  disconnectedCallback() {
    this.#controller.abort();
  }

  #navigate(event) {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
    const tab = event.target.closest('[role="tab"]');
    const current = this.#tabs.indexOf(tab);
    if (current < 0) return;

    // Home and End move to the ends; the horizontal arrows wrap and follow visual order.
    const rtl = getComputedStyle(this).direction === "rtl";
    const forward = rtl ? "ArrowLeft" : "ArrowRight";
    const backward = rtl ? "ArrowRight" : "ArrowLeft";
    const next = {
      [forward]: (current + 1) % this.#tabs.length,
      [backward]: (current - 1 + this.#tabs.length) % this.#tabs.length,
      Home: 0,
      End: this.#tabs.length - 1,
    }[event.key];
    if (next === undefined) return;

    event.preventDefault();
    this.#select(next, true);
  }

  #select(index, focus) {
    if (index < 0 || index >= this.#tabs.length) return;
    this.#tabs.forEach((tab, i) => {
      const selected = i === index;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      const panel = this.#panels[i];
      if (panel) panel.hidden = !selected;
    });
    if (focus) this.#tabs[index].focus();
  }
}

customElements.define("tessera-tabs", TesseraTabs);
