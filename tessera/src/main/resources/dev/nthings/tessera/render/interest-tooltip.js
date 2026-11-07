// Firefox has not shipped interest invokers. Mirror the browser behavior for `interestfor`
// triggers so hint popovers open on hover and focus and close when interest leaves.
if (!("interestForElement" in HTMLElement.prototype)) {
  const controller = new AbortController();
  const { signal } = controller;
  let active = null;

  const targetFor = (trigger) => {
    const id = trigger.getAttribute("interestfor");
    return id ? document.getElementById(id) : null;
  };

  for (const trigger of document.querySelectorAll("[interestfor]")) {
    const target = targetFor(trigger);
    if (target && !trigger.hasAttribute("aria-describedby")) {
      trigger.setAttribute("aria-describedby", target.id);
    }
  }

  const show = (event) => {
    const trigger = event.target.closest?.("[interestfor]");
    const target = trigger && targetFor(trigger);
    if (!target) return;
    if (active && active !== target) active.hidePopover?.();
    active = target;
    if (!target.matches(":popover-open")) target.showPopover();
  };

  const hide = (event) => {
    const trigger = event.target.closest?.("[interestfor]");
    const target = trigger && targetFor(trigger);
    if (!target || trigger.contains(event.relatedTarget)) return;
    if (target.matches(":popover-open")) target.hidePopover();
    if (active === target) active = null;
  };

  document.addEventListener("pointerover", show, { signal });
  document.addEventListener("pointerout", hide, { signal });
  document.addEventListener("focusin", show, { signal });
  document.addEventListener("focusout", hide, { signal });
}
