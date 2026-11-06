const normalize = (text) => text.normalize("NFKD").replace(/\p{M}/gu, "").toLowerCase();

for (const dialog of document.querySelectorAll("[data-search-palette]")) {
  const input = dialog.querySelector("input[type=search]");
  const groups = [...dialog.querySelectorAll(".palette-group")];
  const links = [...dialog.querySelectorAll(".palette-item")];
  const empty = dialog.querySelector(".palette-empty");
  const searchable = new Map(links.map((link) => [link, normalize(
    `${link.closest(".palette-group").querySelector(".palette-group-title").textContent} ${link.textContent}`
  )]));
  const visibleLinks = () => links.filter((link) => !link.hidden);

  const filter = () => {
    const terms = normalize(input.value).trim().split(/\s+/).filter(Boolean);
    for (const link of links) {
      link.hidden = !terms.every((term) => searchable.get(link).includes(term));
    }
    for (const group of groups) {
      group.hidden = ![...group.querySelectorAll(".palette-item")].some((link) => !link.hidden);
    }
    empty.hidden = visibleLinks().length !== 0;
  };
  input.addEventListener("input", filter);
  filter();

  dialog.addEventListener("keydown", (event) => {
    if (event.isComposing) return;
    const visible = visibleLinks();
    if (event.key === "Enter" && event.target === input) {
      event.preventDefault();
      visible[0]?.click();
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      if (visible.length === 0) return;
      event.preventDefault();
      const current = visible.indexOf(document.activeElement);
      const next = current < 0
        ? (event.key === "ArrowDown" ? 0 : visible.length - 1)
        : (current + (event.key === "ArrowDown" ? 1 : -1) + visible.length) % visible.length;
      visible[next].focus();
    }
  });
}
