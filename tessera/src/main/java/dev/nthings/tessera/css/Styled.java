package dev.nthings.tessera.css;

import java.util.function.Consumer;

/// Fluent, typed styling shared by element builders and [StyleRefinement]. Each method writes one
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
///   `containerNone`.
/// - Box: padding `p`/`px`/`py`/`pt`/`pb`/`ps`/`pe`, margin `m`/`mx`/`my`/`mt`/`mb`/`ms`/`me`,
///   `bg`/`background`, [#border(Border)] and its sides (`borderBlockStart`, `borderBlockEnd`,
///   `borderInlineStart`, `borderInlineEnd`), `borderColor`, `rounded`, `shadow`, `outline`,
///   `focusRing`, `opacity`, `accentColor`, [#appearance(Appearance)], [#resize(Resize)].
/// - Text: `textColor`, `fontSize`, `fontFamily`, `fontWeight`, `lineHeight`, `typography`,
///   `textAlign`, [#textWrap(TextWrap)], `whiteSpace`, `underline`, `strikethrough`, `highlight`.
/// - Effects: [#transition(Transition)], [#animation(Animation)], `transform`, `translate`,
///   `backdropFilter`, [#cursor(Cursor)].
/// - Conditions: [#on(Condition, Consumer)] for states, media features and container queries.
/// - Scrolling and anchoring: snapping, scroll margins and markers, `anchorName`,
///   `positionAnchor`, `positionArea`, `positionTryFallbacks`.
///
/// A keyword property with several useful values takes an enum (`cursor(Cursor)`,
/// `overflow(Overflow)`); one with a single useful non-default value has a named toggle
/// (`listStyleNone()`, `tabularNums()`).
///
/// @param <SELF> concrete element or style builder type
public interface Styled<SELF extends Styled<SELF>>
    extends LayoutStyled<SELF>,
        BoxStyled<SELF>,
        TextStyled<SELF>,
        EffectStyled<SELF>,
        ScrollStyled<SELF>,
        AnchorStyled<SELF> {

  /// Runs `preset` on this target's declarations, so one style function can be shared by
  /// elements, [ScopeStyle#select(String, Consumer)] rules, and state callbacks.
  default SELF apply(Consumer<StyleRefinement> preset) {
    preset.accept(style());
    return self();
  }

  /// Runs `styles` on declarations that apply only while `condition` holds: a [State] such as
  /// [State#HOVER] (a nested `&:hover:not(:disabled)` rule), a [Media] feature such as
  /// [Media#MOTION_SAFE], or a [ContainerQuery] from a [Container]. Calling again with an equal
  /// condition extends the same rules, and `styles` may call `on` again to nest conditions.
  ///
  /// On an element, state and media rules sit in the `interactive` cascade layer and container
  /// queries in the later `container` layer, so both override the element's base style. Inside a
  /// [ScopeStyle] rule they nest in that rule. States are emitted in [State] order whatever the
  /// call order, so `ACTIVE` beats `HOVER`.
  default SELF on(Condition condition, Consumer<StyleRefinement> styles) {
    styles.accept(style().nested(condition));
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
