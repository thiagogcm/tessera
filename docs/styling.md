# Styling

Use typed CSS values for individual elements, `ScopeStyle` for component rules, and `Theme` for shared design tokens. The [component guide](consumer-guide.md#choose-a-component-contract) shows how a scope receives its styling callback.

## Set a theme

Built-in components reference the CSS custom properties defined by `Tokens`. Start with `Theme.defaults()` and retain the result of each `with(token, value)` call:

```java
var theme = Theme.defaults()
    .with(Tokens.FONT_BODY, FontStack.of(FontStack.named("Georgia"), FontStack.Generic.SERIF))
    .with(Tokens.DENSITY, CssNumber.of(0.8))
    .with(Tokens.RADIUS_XL, CssLength.rem(1))
    .with(Tokens.CONTROL_FG, Tokens.ACCENT_TEXT);

var site = SiteDefinition.builder().theme(theme).routes(routes).build();
var page = Compiler.compile(document, theme, OutputMode.inline());
```

A token accepts values from its own category: color, length, duration, font family, or number. A token is also a value of that category, so `CONTROL_FG` can refer to `ACCENT_TEXT`. Define an application token with a category record, such as `new DesignToken.Color("brand", CssColor.oklch(60, 0.2, 300))`, then add it with `theme.with(brand, value)`.

Token names accept `[A-Za-z0-9_-]` and cannot start with `-`. Invalid names throw rather than being rewritten.

| Family | Tokens | Default values or behavior |
| --- | --- | --- |
| Fonts | `FONT_BODY`, `FONT_MONO` | System UI and monospace stacks |
| Type scale | `FONT_SIZE_XS` through `FONT_SIZE_2XL` | 0.75, 0.875, 1, 1.125, 1.25, 1.5 rem |
| Line height and tracking | `LINE_HEIGHT_BODY`, `TRACKING_NORMAL` | 1.5 and 0 |
| Spacing | `SPACE_1` through `SPACE_6`, `SPACE_8`, `SPACE_10`, `SPACE_12`; half steps `SPACE_0_5`, `SPACE_1_5`, `SPACE_2_5`, `SPACE_3_5` | Quarter-rem steps multiplied by density |
| Density | `DENSITY` | 1; scales spacing and control padding, leaving type and motion unchanged |
| Controls | `CONTROL_PADDING_INLINE`, `CONTROL_FAMILY`, `CONTROL_FONT_SIZE`, `CONTROL_LINE_HEIGHT`, `CONTROL_TRACKING`, `CONTROL_FG` | Density-aware padding; body family, small type, body line height, normal tracking, primary text |
| Motion | `CONTROL_FEEDBACK_DURATION`, `OVERLAY_DURATION`, `DISCLOSURE_DURATION` | 150 ms, 200 ms, 250 ms |
| Corners | `RADIUS_SM`, `RADIUS_MD`, `RADIUS_LG`, `RADIUS_XL`, `RADIUS_FULL` | 0.25, 0.375, 0.5, 0.75 rem; pill radius |
| Colors | `SURFACE_*`, `TEXT_*`, `ACCENT_PRIMARY`, `BORDER_SUBTLE`, `STATUS_*` | Adaptive light/dark OKLCH colors |

`Typography.LABEL` combines the control typography tokens. Field labels and callout titles use this role and `CONTROL_FG`. Replacing `CONTROL_FONT_SIZE` changes their size directly; replacing `FONT_SIZE_SM` changes it while the control token still refers to that scale value.

`Tone` provides semantic colors (`NEUTRAL`, `ACCENT`, `INFO`, `SUCCESS`, `WARNING`, `DANGER`). Components decide how each tone paints their surfaces. `Callout.Kind` also chooses the notice's icon and title.

## Write typed declarations

Style methods accept the value category their property needs. Elements and `Declarations` implement `Styled`; components that own an element, such as `Image`, expose it through `element(...)`.

```java
Elements.div()
    .p(Tokens.SPACE_4)
    .bg(Tokens.SURFACE_CARD)
    .border(Border.hairline(Tokens.BORDER_SUBTLE))
    .rounded(Tokens.RADIUS_XL)
    .gridColumns(TrackList.autoFit(CssLength.rem(16)))
    .inlineSize(CssLength.pct(100).minus(CssLength.rem(2)));
```

| Need | Values and operations |
| --- | --- |
| Dimensions and calculations | `CssLength`: `plus`, `minus`, `times`, `dividedBy`, `min`, `max`, `anchor` |
| Colors | `CssColor`: `mix`, `mixTransparent`, `sameHue`, `lightDark`, `contrast` |
| Shadows | `Shadow.drop(...)`, `Shadow.inset(...)`; `offsetX(...)` adjusts a layer |
| Keyword properties | Enums such as `Overflow.AUTO`, `TextWrap.BALANCE`, `Cursor.POINTER`, `Appearance.NONE`, `Resize.VERTICAL` |
| Single-purpose settings | Methods such as `listStyleNone()` and `tabularNums()` |
| Named CSS resources | Validated `Container`, `Keyframes`, `Anchor`, `Counter`, and `CustomProperty` handles |
| Grouped typography | `Typography` sets family, size, weight, line height, and tracking |

Selectors passed to `ScopeStyle.select(...)` are emitted as written. Treat selectors and authored CSS resources as trusted input.

Later writes replace the same property. A shorthand removes earlier longhands it covers; a later longhand refines the shorthand:

| Calls                  | Retained declarations             |
| ---------------------- | --------------------------------- |
| `pt(...).p(...)`       | Padding shorthand                 |
| `p(...).pt(...)`       | Padding shorthand and top padding |
| `rowGap(...).gap(...)` | Gap shorthand                     |
| `gap(...).rowGap(...)` | Gap shorthand and row gap         |

The same rule applies to borders and to merged declarations. Immutable values such as `Theme`, `CssLength`, `Transition`, and `Animation` carry `@CheckReturnValue`; retain their derived values.

## Share rules

A `StyleBlock` works as a reusable preset, conditional style callback, scope rule, starting style, or keyframe step:

```java
StyleBlock card = s -> s.p(Tokens.SPACE_4).rounded(Tokens.RADIUS_XL).bg(Tokens.SURFACE_CARD);

Elements.div().styles(card).on(State.HOVER, s -> s.bg(Tokens.SURFACE_SUBTLE));
scope.select(":scope > article", card);
```

## Lay out rows and columns

`FlexLayout` configures the element that should be the flex container, preserving its HTML tag:

```java
var links = FlexLayout.row(Tokens.SPACE_4, FlexLayout.Align.CENTER, FlexLayout.Wrap.WRAP);
var stack = FlexLayout.column(Tokens.SPACE_4, FlexLayout.Align.STRETCH, FlexLayout.Wrap.NOWRAP);

Elements.section().layout(stack).children(
    Elements.nav().layout(links).ariaLabel("Site navigation")
        .children(Elements.a("/").text("Home"), Elements.a("/guide/").text("Guide")),
    Elements.p().text("Introduction"));
```

`inlineRow` and `inlineColumn` use `inline-flex`. A later property call, such as `gap(...)`, replaces that declaration on the element without changing the shared layout value.

For a scrolling panel inside a fixed-height column, give the column a block size. Give the inner panel `flexGrow(1)`, `flexShrink(1)`, `minBlockSize(CssLength.zero())`, and `overflowY(Overflow.AUTO)`.

## Apply conditional styles

`on(condition, styles)` applies styles while a `Condition` holds. Conditions can nest.

| Condition | Behavior |
| --- | --- |
| `State` | Pseudo-classes emitted in order: `HOVER`, `FOCUS_VISIBLE`, `ACTIVE`, `CHECKED`, `OPEN`, `USER_INVALID`, `USER_VALID`, `DISABLED` |
| `Media.MOTION_SAFE` | Matches `prefers-reduced-motion: no-preference` |
| Container query | Matches size, stuck-edge, or snapped-axis conditions on a container handle |

State order is fixed, independent of call order. `ACTIVE` wins over `HOVER`, and `DISABLED` wins over the other states. Hover and active rules exclude disabled elements using a zero-specificity guard.

On elements, container queries use the `container` layer; state and media rules use the later `interactive` layer. Both override base element styles, and states override container queries, so a disabled or hovered element keeps its state styles at every container size. Inside a scope rule they nest in that selector. `ScopeStyle.on(query, rules)` wraps a group of rules in a media or container query. See [stylesheet assembly](architecture.md#stylesheet-assembly) for the layer order.

Use `startingStyle(...)` for the values an entry transition starts from:

```java
scope.select(":scope:open", s -> s.opacity(1).startingStyle(start -> start.opacity(0)));
```

Declare a container once and query it by handle:

```java
var cards = Container.named("cards", Container.Type.INLINE_SIZE);
Elements.div().container(cards).child(
    Elements.article().on(cards.atLeast(CssLength.rem(30)), s -> s.flexRow()));
```

Size queries are `atLeast(length)` and `below(length)`. A `SCROLL_STATE` container supports `stuck(edge)` and `snapped(axis)`. An unnamed handle targets the nearest ancestor container of the required type. `containerNone()` removes a container setting.

## Add transitions and animations

`Transition.controlFeedback()`, `overlayEntry()`, and `disclosure()` supply duration, easing, and property sets. For movement, apply the transition inside `on(Media.MOTION_SAFE, ...)` and keep the visible end state outside that condition.

`Animation` registers its `Keyframes` with the stylesheet and applies the animation only when the user has no reduced-motion preference:

```java
var pulse = Keyframes.named("pulse")
    .start(s -> s.opacity(1))
    .at(50, s -> s.opacity(0.5))
    .end(s -> s.opacity(1));

Elements.div().animation(Animation.of(pulse, CssDuration.ms(800), Easing.ease()));
Elements.div().animation(Animation.enterViewport(Keyframes.FADE_UP));
```

| Effect | API |
| --- | --- |
| Animate through viewport entry or exit | `enterViewport(...)`, `leaveViewport(...)` |
| Reveal once with sibling delay | `revealOnce(...).staggerBySiblingIndex(60)` |
| Follow page scroll progress | `scrollProgress(...)` |
| Use the inline axis | `along(TimelineAxis.INLINE)` |
| Use a supplied keyframe preset | `FADE_IN`, `FADE_UP`, `SCALE_IN`, `SLIDE_INLINE`, `FADE_OUT` |

Check motion and container behavior against the [browser contract](browser-support.md) and [browser verification](development.md#browser-verification).
