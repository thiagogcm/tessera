// Firefox lacks interest invokers: mirror `interestfor` so hint popovers open on hover and
// focus and close when interest leaves.
if (!("interestForElement" in HTMLElement.prototype)) {
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
    if (active && active !== target) active.hidePopover();
    active = target;
    // The source makes the trigger the implicit anchor that positions the hint next to it.
    if (!target.matches(":popover-open")) target.showPopover({ source: trigger });
  };

  const hide = (event) => {
    const trigger = event.target.closest?.("[interestfor]");
    const target = trigger && targetFor(trigger);
    if (!target || trigger.contains(event.relatedTarget)) return;
    if (target.matches(":popover-open")) target.hidePopover();
    if (active === target) active = null;
  };

  document.addEventListener("pointerover", show);
  document.addEventListener("pointerout", hide);
  document.addEventListener("focusin", show);
  document.addEventListener("focusout", hide);
}
