package dev.nthings.tessera.css;

import java.util.function.UnaryOperator;

/**
 * Composable fluent layout and typography styling trait.
 *
 * @param <SELF> concrete element or style builder type
 */
public interface Styled<SELF extends Styled<SELF>> {

  SELF self();

  StyleRefinement style();

  default SELF refine(UnaryOperator<StyleRefinement> mutator) {
    mutator.apply(style());
    return self();
  }

  default SELF cssContent(String value) {
    style().put(new CssDeclaration.Content(value));
    return self();
  }

  default SELF flex() {
    style().put(new CssDeclaration.Display(CssDeclaration.DisplayMode.FLEX));
    return self();
  }

  default SELF inlineFlex() {
    style().put(new CssDeclaration.Display(CssDeclaration.DisplayMode.INLINE_FLEX));
    return self();
  }

  default SELF grid() {
    style().put(new CssDeclaration.Display(CssDeclaration.DisplayMode.GRID));
    return self();
  }

  default SELF block() {
    style().put(new CssDeclaration.Display(CssDeclaration.DisplayMode.BLOCK));
    return self();
  }

  default SELF hidden() {
    style().put(new CssDeclaration.Display(CssDeclaration.DisplayMode.NONE));
    return self();
  }

  default SELF relative() {
    style().put(new CssDeclaration.Position(CssDeclaration.PositionMode.RELATIVE));
    return self();
  }

  default SELF absolute() {
    style().put(new CssDeclaration.Position(CssDeclaration.PositionMode.ABSOLUTE));
    return self();
  }

  default SELF fixed() {
    style().put(new CssDeclaration.Position(CssDeclaration.PositionMode.FIXED));
    return self();
  }

  default SELF sticky() {
    style().put(new CssDeclaration.Position(CssDeclaration.PositionMode.STICKY));
    return self();
  }

  default SELF inset(String value) {
    style().put(new CssDeclaration.Inset(value));
    return self();
  }

  default SELF insetBlockStart(CssLength length) {
    style().put(new CssDeclaration.InsetBlockStart(length));
    return self();
  }

  default SELF insetInlineStart(CssLength length) {
    style().put(new CssDeclaration.InsetInlineStart(length));
    return self();
  }

  default SELF zIndex(int level) {
    style().put(new CssDeclaration.ZIndex(level));
    return self();
  }

  default SELF overflowAuto() {
    style().put(new CssDeclaration.Overflow("auto"));
    return self();
  }

  default SELF listStyleNone() {
    style().put(new CssDeclaration.ListStyle("none"));
    return self();
  }

  default SELF counterReset(String counterName) {
    style().put(new CssDeclaration.CounterReset(counterName));
    return self();
  }

  default SELF counterIncrement(String counterName) {
    style().put(new CssDeclaration.CounterIncrement(counterName));
    return self();
  }

  default SELF flexRow() {
    style().put(new CssDeclaration.FlexDirection(CssDeclaration.Direction.ROW));
    return self();
  }

  default SELF flexCol() {
    style().put(new CssDeclaration.FlexDirection(CssDeclaration.Direction.COLUMN));
    return self();
  }

  default SELF flexWrap() {
    style().put(new CssDeclaration.FlexWrap(CssDeclaration.Wrap.WRAP));
    return self();
  }

  default SELF itemsStart() {
    style().put(new CssDeclaration.AlignItems(CssDeclaration.Alignment.START));
    return self();
  }

  default SELF itemsCenter() {
    style().put(new CssDeclaration.AlignItems(CssDeclaration.Alignment.CENTER));
    return self();
  }

  default SELF itemsEnd() {
    style().put(new CssDeclaration.AlignItems(CssDeclaration.Alignment.END));
    return self();
  }

  default SELF itemsStretch() {
    style().put(new CssDeclaration.AlignItems(CssDeclaration.Alignment.STRETCH));
    return self();
  }

  default SELF itemsBaseline() {
    style().put(new CssDeclaration.AlignItems(CssDeclaration.Alignment.BASELINE));
    return self();
  }

  default SELF justifyStart() {
    style().put(new CssDeclaration.JustifyContent(CssDeclaration.Justify.START));
    return self();
  }

  default SELF justifyCenter() {
    style().put(new CssDeclaration.JustifyContent(CssDeclaration.Justify.CENTER));
    return self();
  }

  default SELF justifyEnd() {
    style().put(new CssDeclaration.JustifyContent(CssDeclaration.Justify.END));
    return self();
  }

  default SELF justifyBetween() {
    style().put(new CssDeclaration.JustifyContent(CssDeclaration.Justify.BETWEEN));
    return self();
  }

  default SELF flexGrow(int factor) {
    style().put(new CssDeclaration.FlexGrow(factor));
    return self();
  }

  default SELF flexShrink(int factor) {
    style().put(new CssDeclaration.FlexShrink(factor));
    return self();
  }

  default SELF gridColumns(String template) {
    style().put(new CssDeclaration.GridTemplateColumns(template));
    return self();
  }

  default SELF gridRows(String template) {
    style().put(new CssDeclaration.GridTemplateRows(template));
    return self();
  }

  default SELF subgridRows(int trackSpan) {
    if (trackSpan <= 0) {
      throw new IllegalArgumentException("trackSpan must be > 0");
    }
    return grid().gridRow("span " + trackSpan).gridRows("subgrid");
  }

  default SELF gridColumn(String spanSpec) {
    style().put(new CssDeclaration.GridColumn(spanSpec));
    return self();
  }

  default SELF gridRow(String spanSpec) {
    style().put(new CssDeclaration.GridRow(spanSpec));
    return self();
  }

  default SELF colSpan(int columns) {
    if (columns <= 0) {
      throw new IllegalArgumentException("columns must be > 0");
    }
    return gridColumn("span " + columns);
  }

  default SELF rowSpan(int rows) {
    if (rows <= 0) {
      throw new IllegalArgumentException("rows must be > 0");
    }
    return gridRow("span " + rows);
  }

  default SELF gridAutoFit(CssLength minColumnWidth) {
    return grid()
        .gridColumns("repeat(auto-fit, minmax(min(100%, " + minColumnWidth.toCss() + "), 1fr))");
  }

  default SELF gap(CssLength length) {
    style().put(new CssDeclaration.Gap(length));
    return self();
  }

  default SELF rowGap(CssLength length) {
    style().put(new CssDeclaration.RowGap(length));
    return self();
  }

  default SELF colGap(CssLength length) {
    style().put(new CssDeclaration.ColumnGap(length));
    return self();
  }

  default SELF inlineSize(CssLength length) {
    style().put(new CssDeclaration.InlineSize(length));
    return self();
  }

  default SELF blockSize(CssLength length) {
    style().put(new CssDeclaration.BlockSize(length));
    return self();
  }

  default SELF minInlineSize(CssLength length) {
    style().put(new CssDeclaration.MinInlineSize(length));
    return self();
  }

  default SELF maxInlineSize(CssLength length) {
    style().put(new CssDeclaration.MaxInlineSize(length));
    return self();
  }

  default SELF minBlockSize(CssLength length) {
    style().put(new CssDeclaration.MinBlockSize(length));
    return self();
  }

  default SELF maxBlockSize(CssLength length) {
    style().put(new CssDeclaration.MaxBlockSize(length));
    return self();
  }

  default SELF aspectRatio(String ratio) {
    style().put(new CssDeclaration.AspectRatio(ratio));
    return self();
  }

  default SELF objectCover() {
    style().put(new CssDeclaration.ObjectFit("cover"));
    return self();
  }

  default SELF fullWidth() {
    return inlineSize(CssLength.pct(100));
  }

  default SELF px(CssLength length) {
    style().put(new CssDeclaration.PaddingInline(length));
    return self();
  }

  default SELF py(CssLength length) {
    style().put(new CssDeclaration.PaddingBlock(length));
    return self();
  }

  default SELF p(CssLength length) {
    return px(length).py(length);
  }

  default SELF pt(CssLength length) {
    style().put(new CssDeclaration.PaddingBlockStart(length));
    return self();
  }

  default SELF ps(CssLength length) {
    style().put(new CssDeclaration.PaddingInlineStart(length));
    return self();
  }

  default SELF mx(CssLength length) {
    style().put(new CssDeclaration.MarginInline(length));
    return self();
  }

  default SELF my(CssLength length) {
    style().put(new CssDeclaration.MarginBlock(length));
    return self();
  }

  default SELF m(CssLength length) {
    return mx(length).my(length);
  }

  default SELF mt(CssLength length) {
    style().put(new CssDeclaration.MarginBlockStart(length));
    return self();
  }

  default SELF mb(CssLength length) {
    style().put(new CssDeclaration.MarginBlockEnd(length));
    return self();
  }

  default SELF bg(DesignToken token) {
    style().put(new CssDeclaration.Background(token.varReference()));
    return self();
  }

  default SELF bgRaw(String colorValue) {
    style().put(new CssDeclaration.Background(colorValue));
    return self();
  }

  default SELF textColor(DesignToken token) {
    style().put(new CssDeclaration.Color(token.varReference()));
    return self();
  }

  default SELF textColorRaw(String colorValue) {
    style().put(new CssDeclaration.Color(colorValue));
    return self();
  }

  default SELF textSize(CssLength size) {
    style().put(new CssDeclaration.FontSize(size));
    return self();
  }

  default SELF fontWeight(int weight) {
    style().put(new CssDeclaration.FontWeight(weight));
    return self();
  }

  default SELF tabularNums() {
    style().put(new CssDeclaration.FontVariantNumeric("tabular-nums"));
    return self();
  }

  default SELF lineHeight(String value) {
    style().put(new CssDeclaration.LineHeight(value));
    return self();
  }

  default SELF textAlign(String alignment) {
    style().put(new CssDeclaration.TextAlign(alignment));
    return self();
  }

  default SELF textBalance() {
    style().put(new CssDeclaration.TextWrap(CssDeclaration.TextWrapMode.BALANCE));
    return self();
  }

  default SELF textPretty() {
    style().put(new CssDeclaration.TextWrap(CssDeclaration.TextWrapMode.PRETTY));
    return self();
  }

  default SELF highlight(HighlightTone tone) {
    style().put(new CssDeclaration.Background(tone.toCssBackground()));
    style().put(new CssDeclaration.PaddingInline(CssLength.rem(0.25)));
    style().put(new CssDeclaration.BorderRadius(CssLength.rem(0.25)));
    return self();
  }

  default SELF underline() {
    return underline(UnderlineStyle.SOLID, Tokens.ACCENT_PRIMARY);
  }

  default SELF underline(UnderlineStyle lineStyle, DesignToken colorToken) {
    style().put(new CssDeclaration.TextDecorationLine("underline"));
    style().put(new CssDeclaration.TextDecorationStyle(lineStyle.css()));
    style().put(new CssDeclaration.TextDecorationColor(colorToken.varReference()));
    style().put(new CssDeclaration.TextDecorationThickness("0.1em"));
    style().put(new CssDeclaration.TextUnderlineOffset(CssLength.em(0.18)));
    style().put(new CssDeclaration.TextDecorationSkipInk("auto"));
    return self();
  }

  default SELF strikethrough() {
    return strikethrough(Tokens.TEXT_MUTED);
  }

  default SELF strikethrough(DesignToken colorToken) {
    style().put(new CssDeclaration.TextDecorationLine("line-through"));
    style().put(new CssDeclaration.TextDecorationStyle(UnderlineStyle.SOLID.css()));
    style().put(new CssDeclaration.TextDecorationColor(colorToken.varReference()));
    style().put(new CssDeclaration.TextDecorationThickness("0.09em"));
    return self();
  }

  default SELF border(String borderSpec) {
    style().put(new CssDeclaration.Border(borderSpec));
    return self();
  }

  default SELF borderSubtle(DesignToken borderToken) {
    style().put(new CssDeclaration.Border("1px solid " + borderToken.varReference()));
    return self();
  }

  default SELF borderBlockStartSubtle(DesignToken borderToken) {
    style().put(new CssDeclaration.BorderBlockStart("1px solid " + borderToken.varReference()));
    return self();
  }

  default SELF borderBlockEndSubtle(DesignToken borderToken) {
    style().put(new CssDeclaration.BorderBlockEnd("1px solid " + borderToken.varReference()));
    return self();
  }

  default SELF borderSeparateZero() {
    style().put(new CssDeclaration.BorderCollapse("separate"));
    style().put(new CssDeclaration.BorderSpacing("0"));
    return self();
  }

  default SELF rounded(CssLength radius) {
    style().put(new CssDeclaration.BorderRadius(radius));
    return self();
  }

  default SELF shadow(String shadowSpec) {
    style().put(new CssDeclaration.BoxShadow(shadowSpec));
    return self();
  }

  default SELF focusRing(DesignToken accentToken) {
    style().put(new CssDeclaration.Outline("2px solid " + accentToken.varReference()));
    style().put(new CssDeclaration.OutlineOffset(CssLength.px(2)));
    return self();
  }

  default SELF focusOutlineNone() {
    style().put(new CssDeclaration.Outline("none"));
    return self();
  }

  default SELF opacity(double value) {
    style().put(new CssDeclaration.Opacity(value));
    return self();
  }

  default SELF cursorPointer() {
    style().put(new CssDeclaration.Cursor("pointer"));
    return self();
  }

  default SELF transition(String value) {
    style().put(new CssDeclaration.Transition(value));
    return self();
  }

  default SELF containerInlineSize() {
    style().put(new CssDeclaration.ContainerType(CssDeclaration.ContainerKind.INLINE_SIZE));
    return self();
  }

  default SELF contentVisibilityAuto(String intrinsicBlockSize) {
    style().put(new CssDeclaration.ContentVisibility("auto"));
    style().put(new CssDeclaration.ContainIntrinsicSize("auto " + intrinsicBlockSize));
    return self();
  }

  default SELF contentVisibility(String mode) {
    style().put(new CssDeclaration.ContentVisibility(mode));
    return self();
  }

  default SELF insetBlockEnd(CssLength length) {
    style().put(new CssDeclaration.InsetBlockEnd(length));
    return self();
  }

  default SELF insetInlineEnd(CssLength length) {
    style().put(new CssDeclaration.InsetInlineEnd(length));
    return self();
  }

  default SELF overflowClip() {
    style().put(new CssDeclaration.Overflow("clip"));
    return self();
  }

  default SELF overflowHidden() {
    style().put(new CssDeclaration.Overflow("hidden"));
    return self();
  }

  default SELF overflowVisible() {
    style().put(new CssDeclaration.Overflow("visible"));
    return self();
  }

  default SELF overflowXAuto() {
    style().put(new CssDeclaration.OverflowX("auto"));
    return self();
  }

  default SELF overflowYAuto() {
    style().put(new CssDeclaration.OverflowY("auto"));
    return self();
  }

  default SELF overflowAnchorNone() {
    style().put(new CssDeclaration.OverflowAnchor("none"));
    return self();
  }

  default SELF scrollBehaviorSmooth() {
    style().put(new CssDeclaration.ScrollBehavior("smooth"));
    return self();
  }

  default SELF scrollSnapType(String spec) {
    style().put(new CssDeclaration.ScrollSnapType(spec));
    return self();
  }

  default SELF scrollSnapAlign(String align) {
    style().put(new CssDeclaration.ScrollSnapAlign(align));
    return self();
  }

  default SELF scrollMarginBlockStart(CssLength length) {
    style().put(new CssDeclaration.ScrollMarginBlockStart(length));
    return self();
  }

  default SELF scrollTargetGroupAuto() {
    style().put(new CssDeclaration.ScrollTargetGroup("auto"));
    return self();
  }

  default SELF scrollMarkerGroup(String position) {
    style().put(new CssDeclaration.ScrollMarkerGroup(position));
    return self();
  }

  default SELF overscrollBehaviorX(String mode) {
    style().put(new CssDeclaration.OverscrollBehaviorX(mode));
    return self();
  }

  default SELF scrollbarWidthNone() {
    style().put(new CssDeclaration.ScrollbarWidth("none"));
    return self();
  }

  default SELF ms(CssLength length) {
    style().put(new CssDeclaration.MarginInlineStart(length));
    return self();
  }

  default SELF me(CssLength length) {
    style().put(new CssDeclaration.MarginInlineEnd(length));
    return self();
  }

  default SELF borderInlineStart(String spec) {
    style().put(new CssDeclaration.BorderInlineStart(spec));
    return self();
  }

  default SELF borderInlineStartSubtle(DesignToken borderToken) {
    style().put(new CssDeclaration.BorderInlineStart("1px solid " + borderToken.varReference()));
    return self();
  }

  default SELF borderNone() {
    style().put(new CssDeclaration.Border("none"));
    return self();
  }

  default SELF backdropFilter(String filter) {
    style().put(new CssDeclaration.BackdropFilter(filter));
    return self();
  }

  default SELF transform(String value) {
    style().put(new CssDeclaration.Transform(value));
    return self();
  }

  default SELF translate(String value) {
    style().put(new CssDeclaration.Translate(value));
    return self();
  }

  default SELF transitionProperty(String properties) {
    style().put(new CssDeclaration.TransitionProperty(properties));
    return self();
  }

  default SELF transitionDuration(String duration) {
    style().put(new CssDeclaration.TransitionDuration(duration));
    return self();
  }

  default SELF transitionTimingFunction(String easing) {
    style().put(new CssDeclaration.TransitionTimingFunction(easing));
    return self();
  }

  default SELF transitionAllowDiscrete() {
    style().put(new CssDeclaration.TransitionBehavior("allow-discrete"));
    return self();
  }

  default SELF containerScrollState(String containerName) {
    style().put(new CssDeclaration.ContainerName(containerName));
    style().put(new CssDeclaration.ContainerType(CssDeclaration.ContainerKind.SCROLL_STATE));
    return self();
  }

  default SELF containerScrollStateInlineSize(String containerName) {
    style().put(new CssDeclaration.ContainerName(containerName));
    style()
        .put(
            new CssDeclaration.ContainerType(
                CssDeclaration.ContainerKind.SCROLL_STATE_INLINE_SIZE));
    return self();
  }

  default SELF anchorName(String customIdent) {
    style().put(new CssDeclaration.AnchorName(customIdent));
    return self();
  }

  default SELF positionAnchor(String customIdent) {
    style().put(new CssDeclaration.PositionAnchor(customIdent));
    return self();
  }

  default SELF positionArea(String area) {
    style().put(new CssDeclaration.PositionArea(area));
    return self();
  }

  default SELF positionTryFallbacks(String fallbacks) {
    style().put(new CssDeclaration.PositionTryFallbacks(fallbacks));
    return self();
  }

  default SELF whiteSpace(String mode) {
    style().put(new CssDeclaration.WhiteSpace(mode));
    return self();
  }
}
