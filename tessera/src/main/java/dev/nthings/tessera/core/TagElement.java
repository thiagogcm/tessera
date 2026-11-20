package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.PositionArea;
import dev.nthings.tessera.css.PositionTry;
import dev.nthings.tessera.css.StyleBlock;
import dev.nthings.tessera.css.Styled;
import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.IdReference;
import dev.nthings.tessera.internal.authoring.PortableNode;
import dev.nthings.tessera.internal.authoring.SourceProvenance;
import dev.nthings.tessera.internal.authoring.TagData;
import dev.nthings.tessera.internal.foundation.Decimals;
import java.util.List;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// HTML element node backed by a [TagName] and a [Declarations]. Authors usually create one through
/// the [Elements] factories.
///
/// Besides the [Styled] methods and the child methods of [ParentElement], an element takes:
///
/// - Attributes: [#attr], [#boolAttr], [#removeAttr], and typed shortcuts such as [#title],
///   [#tabIndex], [#popover] and [#dimensions].
/// - References to other elements, resolved to their DOM ids: [#idRef], [#labelFor], [#list],
///   [#href(ElementRef)] and [#command].
/// - Accessibility: [#role(Role)] and one typed method per `aria-*` attribute, such as [#ariaLabel]
///   or [#ariaExpanded]. The generic attribute methods refuse `id`, `role` and `aria-*`, so these
///   go through the typed methods, and the compiler checks the result against the [Role]
///   requirements.
///
/// Give the element an id with [#id(ElementRef)]. Features that need the element's own id
/// ([#ref()], [#interestTooltip], [#anchorName()]) bind an anonymous ref when none is bound; a
/// named ref bound before or after them gives the element its id, and every ref names the same
/// element. Two named refs asking for different ids report `conflicting-element-id`.
///
/// An element is portable: it holds nothing that only one backend executes. Continuously changing
/// styles ([#live(StyleBlock)]) compile to ordinary declarations in a document and to one stable
/// rule in a browser root. A built-in widget's bundled behavior (such as tabs) is loaded by
/// document output only. Event handlers and controlled form properties belong to the browser
/// module's live elements, which wrap a portable element without changing it.
public final class TagElement
    implements Element,
        TagHost,
        Styled<TagElement>,
        ParentElement<TagElement>,
        FluentBuilder<TagElement> {

  static {
    AuthoringAccess.registerTag(element -> element.data);
  }

  private final TagData data;

  TagElement(TagName tag) {
    this(tag, SourceProvenance.capture());
  }

  private TagElement(TagName tag, @Nullable SourceLocation source) {
    data = new TagData(tag, source);
  }

  /// Copies this builder and every descendant element, so expansion can wire the copy without
  /// writing the builder the caller still holds. [ElementRef] handles are the same objects: a ref
  /// is an identity, not configuration. Component and scope instances nested in the tree are
  /// shared. Live styles are a new declaration set.
  public TagElement copy() {
    var copy = new TagElement(data.tag, data.source);
    copy.data.style.mergeFrom(data.style);
    copy.data.attributes.putAll(data.attributes);
    copy.data.idReferences.putAll(data.idReferences);
    for (var child : data.children) {
      copy.data.children.add(detach(child));
    }
    copy.data.refs.addAll(data.refs);
    var liveStyle = data.live;
    if (liveStyle != null) {
      copy.data.live = new Declarations().mergeFrom(liveStyle);
    }
    copy.data.interestTooltip = data.interestTooltip == null ? null : data.interestTooltip.copy();
    copy.data.anchorSelf = data.anchorSelf;
    copy.data.anchorTarget = data.anchorTarget;
    copy.data.transitionKey = data.transitionKey;
    copy.data.navigationHeading = data.navigationHeading;
    copy.data.behavior = data.behavior;
    copy.data.initial = data.initial;
    return copy;
  }

  /// A copy of `element` when it owns descendant elements, otherwise `element` itself.
  static Element detach(Element element) {
    return switch (element) {
      case TagElement tag -> tag.copy();
      case FragmentElement fragment -> fragment.copy();
      case PortablePlacement(PortableNode.Keyed keyed) ->
          new PortablePlacement(new PortableNode.Keyed(keyed.key(), detach(keyed.content())));
      case PortablePlacement _ -> element;
    };
  }

  @Override
  public TagElement self() {
    return this;
  }

  @Override
  public TagElement toTagElement() {
    return this;
  }

  // --- Attributes -------------------------------------------------------------------------------

  /// Sets an arbitrary attribute. An HTML element stores the name in lowercase, since HTML names
  /// are case-insensitive; SVG names keep their case. The name must start with an ASCII letter, `_`
  /// or `:` and contain only ASCII letters, digits, `-`, `_`, `.` and `:`, or
  /// IllegalArgumentException is thrown. `id` ([#id(ElementRef)]), `role` and `aria-*` have typed
  /// methods and are refused here with IllegalArgumentException. So are the attributes that embed
  /// script or markup, inline event handlers (`on*`) and `srcdoc`: a portable element runs no
  /// script of its own, and expansion reports a `javascript:` URL as `script-url`.
  public TagElement attr(String name, String value) {
    name = Attributes.generic(data.tag, name);
    data.attributes.put(name, value);
    data.idReferences.remove(name);
    return this;
  }

  /// Sets the initial value and native form-reset default of an input other than file, a textarea,
  /// or a single select. A select uses the first option with the matching value, or the browser's
  /// default selection if none matches. Use [#initialSelectedValues(List)] for a multiple select.
  /// Leave the corresponding generic attributes and textarea children unset. A retained control
  /// keeps its original default across browser renders; to initialize it again, change its key.
  public TagElement initialValue(String value) {
    data.initial = data.initial.withValue(value);
    return this;
  }

  /// Sets the initial checked state and native form-reset default of a checkbox or radio input.
  /// Leave the generic `checked` attribute unset.
  public TagElement initialChecked(boolean checked) {
    data.initial = data.initial.withChecked(checked);
    return this;
  }

  /// Sets the initial selection and native form-reset default of a multiple select. Each matching
  /// option starts selected. Leave the options' generic `selected` attributes unset.
  public TagElement initialSelectedValues(List<String> values) {
    data.initial = data.initial.withSelectedValues(values);
    return this;
  }

  /// Sets a boolean attribute such as `disabled` or `open`: present with an empty value. Use
  /// `onlyIf(condition, el -> el.boolAttr(name))` to set it conditionally. Refuses `id`, `role` and
  /// `aria-*` as [#attr(String, String)] does.
  public TagElement boolAttr(String name) {
    name = Attributes.generic(data.tag, name);
    data.attributes.put(name, "");
    data.idReferences.remove(name);
    return this;
  }

  /// Removes the attribute `name`, including one set through [#idRef], so it is not emitted. Does
  /// nothing if it is not set. Refuses `id`, `role` and `aria-*` as [#attr(String, String)] does.
  public TagElement removeAttr(String name) {
    name = Attributes.generic(data.tag, name);
    data.attributes.remove(name);
    data.idReferences.remove(name);
    return this;
  }

  /// Sets `attribute` to the DOM ids of `targets` once they are bound on the page. Throws
  /// IllegalArgumentException if no target is given, for a name [#attr(String, String)] refuses, or
  /// for a URL-valued attribute such as `href` or `src`; link to an element with
  /// [#href(ElementRef)].
  public TagElement idRef(String attribute, ElementRef... targets) {
    return reference(Attributes.idList(data.tag, attribute), new IdReference("", List.of(targets)));
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
    return attr("popover", WireToken.of(mode));
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
    return aria("aria-current", WireToken.of(current));
  }

  public TagElement ariaExpanded(boolean expanded) {
    return aria("aria-expanded", Boolean.toString(expanded));
  }

  public TagElement ariaChecked(AriaChecked checked) {
    return aria("aria-checked", WireToken.of(checked));
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
    return aria("aria-live", WireToken.of(live));
  }

  public TagElement ariaHasPopup(AriaPopup popup) {
    return aria("aria-haspopup", WireToken.of(popup));
  }

  /// Sets `aria-level`, the level of a `role="heading"`. Throws IllegalArgumentException outside 1
  /// through 6.
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
    data.attributes.put(attribute, value);
    return this;
  }

  private TagElement reference(String attribute, IdReference reference) {
    // Reserve the slot so resolved ids keep declaration order.
    data.attributes.putIfAbsent(attribute, "");
    data.idReferences.put(attribute, reference);
    return this;
  }

  /// Appends `child`. Throws IllegalStateException if this is a void element, which cannot have
  /// children.
  @Override
  public TagElement child(IntoElement child) {
    var node = child.intoElement();
    data.requireChildren();
    data.children.add(node);
    return this;
  }

  /// Binds `elementRef` to this element, so its DOM id is resolved on the page and other elements
  /// can reference it. An element may carry several refs: they all resolve to one DOM id, taken
  /// from the named ref ([ElementRef#scoped] or [ElementRef#page]) if there is one, otherwise
  /// generated as `<tag>-<n>`. Two named refs asking for different ids report
  /// `conflicting-element-id`. Binding the same ref twice on one element has no further effect.
  public TagElement id(ElementRef elementRef) {
    for (var bound : data.refs) {
      if (bound == elementRef) {
        return this;
      }
    }
    data.refs.add(elementRef);
    return this;
  }

  /// Returns a ref to this element: the first one bound, or a new anonymous ref bound now. Pass it
  /// to `command`, `labelFor`, `aria*` or `href` on other elements.
  public ElementRef ref() {
    if (data.refs.isEmpty()) {
      data.refs.add(ElementRef.anonymous());
    }
    return data.refs.getFirst();
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
    data.interestTooltip = tooltipBuilder.apply(base);
    return this;
  }

  /// Declares an anchor named after this element's DOM id, so [#anchoredTo] elements can position
  /// against it. Binds an anonymous ref when this element has none.
  public TagElement anchorName() {
    ref();
    data.anchorSelf = true;
    return this;
  }

  /// Positions this element against `anchor`, with `area` and the given fallbacks. The anchor must
  /// call [#anchorName()] and be laid out earlier on the page. Otherwise the position names
  /// nothing, and no error is reported. An unbound `anchor` is reported as `unbound-reference`.
  public TagElement anchoredTo(
      ElementRef anchor, PositionArea area, PositionTry firstFallback, PositionTry... fallbacks) {
    data.anchorTarget = anchor;
    return positionArea(area).positionTryFallbacks(firstFallback, fallbacks);
  }

  /// Emits `view-transition-name` (and class) for a cross-document transition. Keys are checked per
  /// page (`duplicate-transition-key`) and across the site (`unshared-transition-key`). See
  /// [TransitionKey].
  public TagElement transitionKey(TransitionKey key) {
    data.transitionKey = key;
    return this;
  }

  /// Writes styles that change continuously, such as an animated size or a progress width, through
  /// `styles`. A live backend compiles them into one rule that reads each value from a custom
  /// property on this element, so new values add no style rules; static compilation emits them as
  /// ordinary declarations. Calling again extends the same live styles. Only plain declarations are
  /// live: write state and media rules with `on(...)`.
  public TagElement live(StyleBlock styles) {
    var target = data.live;
    if (target == null) {
      target = new Declarations();
      data.live = target;
    }
    styles.configure(target);
    return this;
  }
}
