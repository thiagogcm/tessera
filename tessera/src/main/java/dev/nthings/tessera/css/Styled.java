package dev.nthings.tessera.css;

import dev.nthings.tessera.core.TagElement;

/// Fluent, typed styling shared by element builders and [Declarations]. Each method writes one
/// declaration (or a small fixed group) and returns the receiver; it takes the value category its
/// property accepts ([CssLength], [CssColor], [Border], ...), so a color cannot be passed where a
/// length belongs. The last call wins, and a shorthand removes the longhands it covers.
///
/// The methods, by concern:
///
/// - Layout: `flex`, `inlineFlex`, `grid`, `block`, `inlineBlock`, `hidden`, [#layout(FlexLayout)],
///   `flexRow`/`flexCol`/`flexWrap`, `items*`, `justify*`, `flexGrow`/`flexShrink`,
///   `gridColumns`/`gridRows`/`gridColumn`/`gridRow`/`colSpan`/`rowSpan`/`gridAutoFit`/
///   `subgridRows`, `gap`/`rowGap`/`colGap`.
/// - Position and size: `relative`, `absolute`, `fixed`, `sticky`, `inset*`, `zIndex`,
///   `inlineSize`/`blockSize` and their `min`/`max` forms, `aspectRatio`, [#objectFit(ObjectFit)],
///   [#overflow(Overflow)] (and `overflowX`/`overflowY`), [#container(Container)] and
///   `containerNone`, [#contentVisibilityAuto(CssLength)].
/// - Box: padding `p`/`px`/`py`/`pt`/`pb`/`ps`/`pe`, margin `m`/`mx`/`my`/`mt`/`mb`/`ms`/`me`,
///   `bg`/`background`, [#border(Border)] and its sides (`borderBlockStart`, `borderBlockEnd`,
///   `borderInlineStart`, `borderInlineEnd`), `borderColor`, `rounded`, `shadow`, `outline`,
///   `focusRing`, `opacity`, `accentColor`, [#appearance(Appearance)], [#resize(Resize)].
/// - Text: `textColor`, `fontSize`, `fontFamily`, `fontWeight`, `lineHeight`, `typography`,
///   `textAlign`, [#textWrap(TextWrap)], `whiteSpace`, `underline`, `strikethrough`, `highlight`.
/// - Effects: [#transition(Transition)] (including [Spring] transitions), [#animation(Animation)],
///   `transform`, `translate`, `backdropFilter`, [#cursor(Cursor)].
/// - Conditions: [#on(Condition, StyleBlock)] for states, group states, media features and
///   container queries.
/// - Presets: [#styles(StyleBlock)] runs a shared [StyleBlock] on this target.
/// - Scrolling and anchoring: snapping, scroll margins and markers, `anchorName`, `positionAnchor`,
///   `positionArea`, `positionTryFallbacks`.
///
/// A keyword property with several useful values takes an enum (`cursor(Cursor)`,
/// `overflow(Overflow)`); one with a single useful non-default value has a named toggle
/// (`listStyleNone()`, `tabularNums()`).
///
/// @param <SELF> concrete element or style builder type
public sealed interface Styled<SELF extends Styled<SELF>>
    extends LayoutStyled<SELF>,
        BoxStyled<SELF>,
        TextStyled<SELF>,
        EffectStyled<SELF>,
        ScrollStyled<SELF>,
        AnchorStyled<SELF>
    permits Declarations, TagElement {

  /// Runs `preset` on this target's declarations, so one [StyleBlock] can be shared by elements,
  /// [ScopeStyle#select(String, StyleBlock)] rules, and state callbacks.
  default SELF styles(StyleBlock preset) {
    preset.configure(StyleTarget.declarations(this));
    return self();
  }

  /// Runs `styles` on declarations that apply only while `condition` holds: a [State] such as
  /// [State#HOVER] (a nested `&:hover:not(:where(:disabled))` rule), a [GroupState] of an enclosing
  /// [Group] such as `row.state(State.HOVER)`, a [Media] feature such as [Media#MOTION_SAFE], or a
  /// [ContainerQuery] from a [Container]. Calling again with an equal condition extends the same
  /// rules, and `styles` may call `on` again to nest conditions.
  ///
  /// On an element, container queries sit in the `container` cascade layer and state and media
  /// rules in the later `interactive` layer, so both override the element's base style and states
  /// override container queries. Inside a [ScopeStyle] rule they nest in that rule. States are
  /// emitted in [State] order whatever the call order, so `ACTIVE` beats `HOVER`, and group states
  /// follow them in the same order.
  default SELF on(Condition condition, StyleBlock styles) {
    styles.configure(StyleTarget.declarations(this).nested(condition));
    return self();
  }

  /// Keeps content in the accessibility tree while removing it from the visual layout.
  default SELF visuallyHidden() {
    return absolute()
        .inlineSize(CssLength.px(1))
        .blockSize(CssLength.px(1))
        .p(CssLength.zero())
        .m(CssLength.px(-1))
        .overflow(Overflow.HIDDEN)
        .whiteSpace(WhiteSpace.NOWRAP)
        .border(Border.none());
  }
}
