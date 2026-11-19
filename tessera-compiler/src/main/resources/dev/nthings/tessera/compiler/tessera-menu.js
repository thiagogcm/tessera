// A modal dialog gives focus back to whatever opened it. When a menu item opened it, the menu is
// hidden by then, so focus would fall to the page body; send it to the menu's button instead.
class TesseraMenu extends HTMLElement {
  #controller;

  connectedCallback() {
    this.#controller = new AbortController();
    const { signal } = this.#controller;
    this.addEventListener("click", (event) => {
      const item = event.target.closest?.("button[commandfor]");
      const panel = item?.closest("[popover]");
      if (!panel || !this.contains(panel)) return;
      const dialog = document.getElementById(item.getAttribute("commandfor"));
      const trigger = this.querySelector(`:scope > [commandfor="${CSS.escape(panel.id)}"]`);
      if (!(dialog instanceof HTMLDialogElement) || dialog.open || !trigger) return;
      // Firefox still has focus inside the closed dialog when "close" fires; Chromium has already
      // dropped it to the body.
      dialog.addEventListener("close", () => {
        const focused = document.activeElement;
        if (
          focused === null || focused === document.body ||
          panel.contains(focused) || dialog.contains(focused)
        ) trigger.focus();
      }, { once: true, signal });
    }, { signal });
  }

  disconnectedCallback() {
    this.#controller.abort();
  }
}

customElements.define("tessera-menu", TesseraMenu);
