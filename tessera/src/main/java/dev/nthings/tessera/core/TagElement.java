package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.StyleBlock;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.foundation.Decimals;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// HTML element node backed by a [TagName] and a [Declarations]. Authors usually create one
/// through the [Elements] factories.
///
/// Besides the [Styled] methods and the child methods of [ParentElement], an element takes:
///
/// - Attributes: [#attr], [#boolAttr], [#removeAttr], and typed shortcuts such as [#title],
///   [#tabIndex], [#popover] and [#dimensions].
/// - References to other elements, resolved to their DOM ids: [#idRef], [#labelFor], [#list],
///   [#href(ElementRef)] and [#command].
/// - Accessibility: [#role(Role)] and one typed method per `aria-*` attribute, such as
///   [#ariaLabel] or [#ariaExpanded]. The generic attribute methods refuse `id`, `role` and
///   `aria-*`, so these go through the typed methods, and the compiler checks the result against
///   the [Role] requirements.
///
/// Give the element an id with [#id(ElementRef)]. Features that need the element's own id
/// ([#ref()], [#interestTooltip], [#anchorName()]) bind an anonymous ref when none is bound; a
/// named ref bound before or after them gives the element its id, and every ref names the same
/// element. Two named refs asking for different ids report `conflicting-element-id`.
///
/// Live backends add Java event handlers ([#onEvent(String, Consumer)]), controlled form properties
/// ([#value(String)], [#checked(boolean)], [#selectedValues(List)]) and continuously changing
/// styles ([#live(StyleBlock)]). Static compilation rejects handlers and controlled properties with
/// `native-event-handlers` and `native-control-properties`, and compiles live styles as ordinary
/// declarations.
public final class TagElement
    implements Element, Styled<TagElement>, ParentElement<TagElement>, FluentBuilder<TagElement> {

  private final TagName tag;
  private final Declarations style = new Declarations();
  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final Map<String, IdReference> idReferences = new LinkedHashMap<>();
  private final List<Element> children = new ArrayList<>();
  private final List<ElementRef> refs = new ArrayList<>(1);
  /// Allocated on the first handler: most elements bind none.
  private Map<EventListener.Key, EventListener> events = Map.of();
  private ControlProperties controlProperties = ControlProperties.UNCONTROLLED;
  private @Nullable Declarations live;
  private @Nullable TagElement interestTooltip;
  private boolean anchorSelf;
  private @Nullable ElementRef anchorTarget;
  private @Nullable TransitionKey transitionKey;
  private @Nullable DocumentHeading navigationHeading;
  private final @Nullable SourceLocation source;

  /// Creates an element for `tag`. Authors normally use the [Elements] factories.
  public TagElement(TagName tag) {
    this(tag, SourceProvenance.capture());
  }

  private TagElement(TagName tag, @Nullable SourceLocation source) {
    this.tag = tag;
    this.source = source;
  }

  /// Copies this builder and every descendant element, so expansion can wire the copy without
  /// writing the builder the caller still holds. [ElementRef] handles are the same objects: a ref
  /// is an identity, not configuration. Component and scope instances nested in the tree are
  /// shared. Event handlers and controlled properties are copied; live styles are a new
  /// declaration set.
  public TagElement copy() {
    var copy = new TagElement(tag, source);
    copy.style.mergeFrom(style);
    copy.attributes.putAll(attributes);
    copy.idReferences.putAll(idReferences);
    for (var child : children) {
      copy.children.add(detach(child));
    }
    copy.refs.addAll(refs);
    if (!events.isEmpty()) {
      copy.events = new LinkedHashMap<>(events);
    }
    copy.controlProperties = controlProperties;
    var liveStyle = live;
    if (liveStyle != null) {
      copy.live = new Declarations().mergeFrom(liveStyle);
    }
    copy.interestTooltip = interestTooltip == null ? null : interestTooltip.copy();
    copy.anchorSelf = anchorSelf;
    copy.anchorTarget = anchorTarget;
    copy.transitionKey = transitionKey;
    copy.navigationHeading = navigationHeading;
    return copy;
  }

  /// A copy of `element` when it owns descendant elements, otherwise `element` itself.
  static Element detach(Element element) {
    return switch (element) {
      case TagElement tag -> tag.copy();
      case FragmentElement fragment -> fragment.copy();
      case KeyedElement keyed -> new KeyedElement(keyed.key(), detach(keyed.content()));
      case DeferredElement deferred ->
          new DeferredElement(detach(deferred.child()), deferred.priority());
      default -> element;
    };
  }

  @Override
  public TagElement self() {
    return this;
  }

  /// Compiler-facing: the authoring call site, present only while source tracking is active.
  public @Nullable SourceLocation source() {
    return source;
  }

  /// Compiler-facing: the emitted tag name.
  public TagName tag() {
    return tag;
  }

  @Override
  public Declarations style() {
    return style;
  }

  /// Compiler-facing: a read-only view of the attributes, in the order they were first set. The
  /// id comes from bound refs and generated style classes are appended to `class` at compile time,
  /// so neither is in this map; slots of [#idReferences()] hold an empty placeholder.
  public Map<String, String> attributes() {
    return Collections.unmodifiableMap(attributes);
  }

  /// Compiler-facing: a read-only view of the attributes whose values are DOM ids of [ElementRef]
  /// targets, resolved by the compiler.
  public Map<String, IdReference> idReferences() {
    return Collections.unmodifiableMap(idReferences);
  }

  /// Compiler-facing: a read-only view of the child elements in the order they were added.
  public List<Element> childrenList() {
    return Collections.unmodifiableList(children);
  }

  // --- Attributes -------------------------------------------------------------------------------

  /// Sets an arbitrary attribute. `id` ([#id(ElementRef)]), `role` and `aria-*` have typed methods
  /// and are refused here with IllegalArgumentException.
  public TagElement attr(String name, String value) {
    Attributes.requireGeneric(name);
    attributes.put(name, value);
    idReferences.remove(name);
    return this;
  }

  /// Sets a boolean attribute such as `disabled` or `open`: present with an empty value. Use
  /// `onlyIf(condition, el -> el.boolAttr(name))` to set it conditionally. Refuses `id`, `role` and
  /// `aria-*` as [#attr(String, String)] does.
  public TagElement boolAttr(String name) {
    Attributes.requireGeneric(name);
    attributes.put(name, "");
    idReferences.remove(name);
    return this;
  }

  /// Removes the attribute `name`, including one set through [#idRef], so it is not emitted. Does
  /// nothing if it is not set. Refuses `id`, `role` and `aria-*` as [#attr(String, String)] does.
  public TagElement removeAttr(String name) {
    Attributes.requireGeneric(name);
    attributes.remove(name);
    idReferences.remove(name);
    return this;
  }

  /// Sets `attribute` to the DOM ids of `targets` once they are bound on the page. Throws
  /// IllegalArgumentException if no target is given, or for `id`, `role` and `aria-*`.
  public TagElement idRef(String attribute, ElementRef... targets) {
    Attributes.requireGeneric(attribute);
    return reference(attribute, new IdReference("", List.of(targets)));
  }

  /// Sets `for` to the DOM id of `control`, on a `<label>`.
  public TagElement labelFor(ElementRef control) {
    return idRef("for", control);
  }

  /// Sets `list` to the DOM id of a `<datalist>` that offers suggestions for this input.
  public TagElement list(ElementRef datalist) {
    return idRef("list", datalist);
  }

  /// Same-page fragment link: `href="#<target dom id>"`.
  public TagElement href(ElementRef target) {
    return reference("href", new IdReference("#", List.of(target)));
  }

  /// Sets `commandfor` and `command`, so activating this element runs `command` on the element
  /// bound to `target`, plus any payload attributes of a custom command. `show-modal`, `close` and
  /// `request-close` need a `<dialog>` target, and `toggle-popover`, `show-popover` and
  /// `hide-popover` need a target with a `popover` attribute; otherwise the compiler reports
  /// `incompatible-command-target`. Custom commands accept any target. Use `element.ref()` to
  /// target an element you hold.
  public TagElement command(ElementRef target, Command command) {
    idRef("commandfor", target).attr("command", command.wireValue());
    command.payload().forEach(this::attr);
    return this;
  }

  /// Sets the `popover` attribute to `mode`.
  public TagElement popover(PopoverMode mode) {
    return attr("popover", mode.wireValue());
  }

  /// Sets `closedby` on a `<dialog>`, choosing which user actions close it. See [ClosedBy].
  public TagElement closedBy(ClosedBy closedBy) {
    return attr("closedby", closedBy.wireValue());
  }

  /// Sets the `width` and `height` attributes, in CSS pixels.
  public TagElement dimensions(int width, int height) {
    return attr("width", Integer.toString(width)).attr("height", Integer.toString(height));
  }

  public TagElement title(String title) {
    return attr("title", title);
  }

  /// Sets `tabindex`. A positive value is reported as `tabindex-positive`; use `0` or `-1`.
  public TagElement tabIndex(int index) {
    return attr("tabindex", Integer.toString(index));
  }

  // --- Accessibility ----------------------------------------------------------------------------

  /// Sets `role`. The compiler checks the required name, children and attributes of the role.
  public TagElement role(Role role) {
    return aria("role", role.wireValue());
  }

  /// Sets `aria-label`, the accessible name as text.
  public TagElement ariaLabel(String label) {
    return aria("aria-label", label);
  }

  /// Sets `aria-labelledby` to the bound `labels`, which name the element. Throws
  /// IllegalArgumentException with no targets.
  public TagElement ariaLabelledBy(ElementRef... labels) {
    return reference("aria-labelledby", new IdReference("", List.of(labels)));
  }

  /// Sets `aria-describedby` to the bound `descriptions`. Throws IllegalArgumentException with no
  /// targets.
  public TagElement ariaDescribedBy(ElementRef... descriptions) {
    return reference("aria-describedby", new IdReference("", List.of(descriptions)));
  }

  public TagElement ariaErrorMessage(ElementRef message) {
    return reference("aria-errormessage", new IdReference("", List.of(message)));
  }

  /// Sets `aria-controls` to the bound `controlled` elements, e.g. a tab's panel. Throws
  /// IllegalArgumentException with no targets.
  public TagElement ariaControls(ElementRef... controlled) {
    return reference("aria-controls", new IdReference("", List.of(controlled)));
  }

  public TagElement ariaHidden(boolean hidden) {
    return aria("aria-hidden", Boolean.toString(hidden));
  }

  public TagElement ariaCurrent(AriaCurrent current) {
    return aria("aria-current", current.wireValue());
  }

  public TagElement ariaExpanded(boolean expanded) {
    return aria("aria-expanded", Boolean.toString(expanded));
  }

  public TagElement ariaChecked(AriaChecked checked) {
    return aria("aria-checked", checked.wireValue());
  }

  public TagElement ariaSelected(boolean selected) {
    return aria("aria-selected", Boolean.toString(selected));
  }

  public TagElement ariaPressed(boolean pressed) {
    return aria("aria-pressed", Boolean.toString(pressed));
  }

  public TagElement ariaBusy(boolean busy) {
    return aria("aria-busy", Boolean.toString(busy));
  }

  public TagElement ariaDisabled(boolean disabled) {
    return aria("aria-disabled", Boolean.toString(disabled));
  }

  public TagElement ariaInvalid(boolean invalid) {
    return aria("aria-invalid", Boolean.toString(invalid));
  }

  public TagElement ariaRequired(boolean required) {
    return aria("aria-required", Boolean.toString(required));
  }

  public TagElement ariaModal(boolean modal) {
    return aria("aria-modal", Boolean.toString(modal));
  }

  public TagElement ariaLive(AriaLive live) {
    return aria("aria-live", live.wireValue());
  }

  public TagElement ariaHasPopup(AriaPopup popup) {
    return aria("aria-haspopup", popup.wireValue());
  }

  /// Sets `aria-level`, the level of a `role="heading"`. Throws IllegalArgumentException outside
  /// 1 through 6.
  public TagElement ariaLevel(int level) {
    if (level < 1 || level > 6) {
      throw new IllegalArgumentException("aria-level must be 1 to 6: " + level);
    }
    return aria("aria-level", Integer.toString(level));
  }

  /// Sets `aria-keyshortcuts`, the keys that activate the element, e.g. `Alt+K`.
  public TagElement ariaKeyShortcuts(String shortcuts) {
    return aria("aria-keyshortcuts", shortcuts);
  }

  public TagElement ariaValueNow(double value) {
    return aria("aria-valuenow", Decimals.format(value));
  }

  public TagElement ariaValueMin(double value) {
    return aria("aria-valuemin", Decimals.format(value));
  }

  public TagElement ariaValueMax(double value) {
    return aria("aria-valuemax", Decimals.format(value));
  }

  private TagElement aria(String attribute, String value) {
    attributes.put(attribute, value);
    return this;
  }

  private TagElement reference(String attribute, IdReference reference) {
    // Reserve the slot so resolved ids keep declaration order.
    attributes.putIfAbsent(attribute, "");
    idReferences.put(attribute, reference);
    return this;
  }

  /// Appends `child`. Throws IllegalStateException if this is a void element, which cannot have
  /// children.
  @Override
  public void appendChild(Element child) {
    if (tag.isVoid()) {
      throw new IllegalStateException(
          "HTML5 void element <" + tag.tagName() + "> cannot have children");
    }
    children.add(child);
  }

  /// Binds `elementRef` to this element, so its DOM id is resolved on the page and other elements
  /// can reference it. An element may carry several refs: they all resolve to one DOM id, taken
  /// from the named ref ([ElementRef#scoped] or [ElementRef#page]) if there is one, otherwise
  /// generated as `<tag>-<n>`. Two named refs asking for different ids report
  /// `conflicting-element-id`. Binding the same ref twice on one element has no further effect.
  public TagElement id(ElementRef elementRef) {
    for (var bound : refs) {
      if (bound == elementRef) {
        return this;
      }
    }
    refs.add(elementRef);
    return this;
  }

  /// Returns a ref to this element: the first one bound, or a new anonymous ref bound now. Pass it
  /// to `command`, `labelFor`, `aria*` or `href` on other elements.
  public ElementRef ref() {
    if (refs.isEmpty()) {
      refs.add(ElementRef.anonymous());
    }
    return refs.getFirst();
  }

  /// Compiler-facing: the refs bound to this element, in binding order.
  public List<ElementRef> boundRefs() {
    return Collections.unmodifiableList(refs);
  }

  /// Adds a hint tooltip that this element's `interestfor` points at, shown on hover or focus. The
  /// tooltip is emitted right after this element and positioned above it, with flip fallbacks.
  /// `tooltipBuilder` receives the default tooltip, a `<span>` with `popover="hint"` and
  /// `role="tooltip"`, and returns the one to emit. Its DOM id is this element's id followed by
  /// `--tooltip`. Binds an anonymous ref when this element has none.
  public TagElement interestTooltip(UnaryOperator<TagElement> tooltipBuilder) {
    ref();
    var base =
        Elements.span()
            .attr("popover", "hint")
            .role(Role.TOOLTIP)
            .positionArea(PositionArea.of(PositionArea.Part.BLOCK_START))
            .positionTryFallbacks(
                PositionTry.flip(PositionTry.Flip.BLOCK), PositionTry.flip(PositionTry.Flip.INLINE))
            .m(CssLength.rem(0.25));
    this.interestTooltip = tooltipBuilder.apply(base);
    return this;
  }

  /// Compiler-facing: the tooltip element, or `null` if none was added.
  public @Nullable TagElement interestTooltip() {
    return interestTooltip;
  }

  /// Marks this element as a document-outline heading. Expansion collects `heading` when the
  /// element is expanded. The first heading for a target is kept. Ordinary headings leave this
  /// unset, so dialog titles, footnote chrome, and other interface headings stay out of the
  /// outline. Heading components set it when the author opts in.
  public TagElement navigationHeading(DocumentHeading heading) {
    this.navigationHeading = heading;
    return this;
  }

  /// The outline heading carried by this element, or `null`.
  public @Nullable DocumentHeading navigationHeading() {
    return navigationHeading;
  }

  /// Declares an anchor named after this element's DOM id, so [#anchoredTo] elements can position
  /// against it. Binds an anonymous ref when this element has none.
  public TagElement anchorName() {
    ref();
    this.anchorSelf = true;
    return this;
  }

  /// Compiler-facing: whether [#anchorName()] was called.
  public boolean isAnchor() {
    return anchorSelf;
  }

  /// Positions this element against `anchor`, with `area` and the given fallbacks. The anchor must
  /// call [#anchorName()] and be laid out earlier on the page. Otherwise the position names
  /// nothing, and no error is reported. An unbound `anchor` is reported as `unbound-reference`.
  public TagElement anchoredTo(
      ElementRef anchor, PositionArea area, PositionTry firstFallback, PositionTry... fallbacks) {
    this.anchorTarget = anchor;
    return positionArea(area).positionTryFallbacks(firstFallback, fallbacks);
  }

  /// Compiler-facing: the element this one is positioned against, or `null`.
  public @Nullable ElementRef anchorTarget() {
    return anchorTarget;
  }

  /// Emits `view-transition-name` (and class) for a cross-document transition. Keys are checked
  /// per page (`duplicate-transition-key`) and across the site (`unshared-transition-key`). See
  /// [TransitionKey].
  public TagElement transitionKey(TransitionKey key) {
    this.transitionKey = key;
    return this;
  }

  /// Compiler-facing: the transition key, or `null` if none was set.
  public @Nullable TransitionKey transitionKey() {
    return transitionKey;
  }

  /// Runs `handler` synchronously when the browser dispatches `event` (such as `click`, `input`
  /// or `keydown`) to this element in a live backend, bubble phase. A handler for the same type
  /// and phase replaces the previous one. Re-rendering rebinds the current handler, so it may
  /// capture the state of the render that created it. Static compilation reports
  /// `native-event-handlers`. Throws IllegalArgumentException if `event` is blank.
  public TagElement onEvent(String event, Consumer<BrowserEvent> handler) {
    return onEvent(event, EventOptions.DEFAULT, handler);
  }

  /// Runs `handler` for `event` with native listener `options`: capture phase, passive, or once.
  /// A handler for the same type and phase replaces the previous one. Throws
  /// IllegalArgumentException if `event` is blank.
  public TagElement onEvent(String event, EventOptions options, Consumer<BrowserEvent> handler) {
    var listener = new EventListener(event, options, handler);
    if (events.isEmpty()) {
      events = new LinkedHashMap<>();
    }
    events.put(listener.key(), listener);
    return this;
  }

  /// Backend-facing: the bound handlers, keyed by [EventListener#key()].
  public Map<EventListener.Key, EventListener> events() {
    return events.isEmpty() ? events : Collections.unmodifiableMap(events);
  }

  /// Writes styles that change continuously, such as an animated size or a progress width, through
  /// `styles`. A live backend compiles them into one rule that reads each value from a custom
  /// property on this element, so new values add no style rules; static compilation emits them as
  /// ordinary declarations. Calling again extends the same live styles. Only plain declarations
  /// are live: write state and media rules with `on(...)`.
  public TagElement live(StyleBlock styles) {
    var target = live;
    if (target == null) {
      target = new Declarations();
      live = target;
    }
    styles.configure(target);
    return this;
  }

  /// Backend-facing: the live styles, or `null` when [#live(StyleBlock)] was never called.
  public @Nullable Declarations liveStyle() {
    return live;
  }

  /// Controls the live `value` of an input, textarea or single select in a live backend: each
  /// render writes it to the control unless the user is composing text with an input method. The
  /// initial `value` attribute is unaffected. A host other than those (a file input, a multiple
  /// select, any other element) reports `control-value`; static compilation reports
  /// `native-control-properties`.
  public TagElement value(String value) {
    controlProperties = controlProperties.withValue(value);
    return this;
  }

  /// Controls the live checked state of a checkbox or radio input in a live backend. Another host
  /// reports `control-checked`; static compilation reports `native-control-properties`.
  public TagElement checked(boolean checked) {
    controlProperties = controlProperties.withChecked(checked);
    return this;
  }

  /// Controls the selected options of a multiple select in a live backend; an empty list
  /// deselects every option. Another host reports `control-selection`; static compilation reports
  /// `native-control-properties`.
  public TagElement selectedValues(List<String> values) {
    controlProperties = controlProperties.withSelectedValues(values);
    return this;
  }

  /// Stops controlling the live form properties, leaving the browser's current values and the
  /// initial attributes as they are.
  public TagElement uncontrolled() {
    controlProperties = ControlProperties.UNCONTROLLED;
    return this;
  }

  /// Backend-facing: the controlled form properties.
  public ControlProperties controlProperties() {
    return controlProperties;
  }
}
