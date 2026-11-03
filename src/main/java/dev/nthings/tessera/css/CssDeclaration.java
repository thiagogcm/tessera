package dev.nthings.tessera.css;

/**
 * Sealed algebraic representation of individual CSS declarations mapped 1-to-1 to {@link
 * CssProperty}.
 */
public sealed interface CssDeclaration
    permits CssDeclaration.Display,
        CssDeclaration.Position,
        CssDeclaration.InsetBlockStart,
        CssDeclaration.InsetInlineStart,
        CssDeclaration.ZIndex,
        CssDeclaration.Overflow,
        CssDeclaration.FlexDirection,
        CssDeclaration.FlexWrap,
        CssDeclaration.AlignItems,
        CssDeclaration.JustifyContent,
        CssDeclaration.FlexGrow,
        CssDeclaration.FlexShrink,
        CssDeclaration.GridTemplateColumns,
        CssDeclaration.GridTemplateRows,
        CssDeclaration.GridColumn,
        CssDeclaration.GridRow,
        CssDeclaration.Gap,
        CssDeclaration.RowGap,
        CssDeclaration.ColumnGap,
        CssDeclaration.InlineSize,
        CssDeclaration.BlockSize,
        CssDeclaration.MinInlineSize,
        CssDeclaration.MaxInlineSize,
        CssDeclaration.MinBlockSize,
        CssDeclaration.MaxBlockSize,
        CssDeclaration.AspectRatio,
        CssDeclaration.ObjectFit,
        CssDeclaration.PaddingInline,
        CssDeclaration.PaddingBlock,
        CssDeclaration.MarginInline,
        CssDeclaration.MarginBlock,
        CssDeclaration.Background,
        CssDeclaration.Color,
        CssDeclaration.FontSize,
        CssDeclaration.FontWeight,
        CssDeclaration.FontVariantNumeric,
        CssDeclaration.LineHeight,
        CssDeclaration.TextAlign,
        CssDeclaration.TextWrap,
        CssDeclaration.TextDecorationLine,
        CssDeclaration.TextDecorationStyle,
        CssDeclaration.TextDecorationColor,
        CssDeclaration.TextDecorationThickness,
        CssDeclaration.TextUnderlineOffset,
        CssDeclaration.TextDecorationSkipInk,
        CssDeclaration.Border,
        CssDeclaration.BorderRadius,
        CssDeclaration.BoxShadow,
        CssDeclaration.Outline,
        CssDeclaration.OutlineOffset,
        CssDeclaration.Opacity,
        CssDeclaration.Cursor,
        CssDeclaration.Transition,
        CssDeclaration.ViewTimeline,
        CssDeclaration.ScrollTimeline,
        CssDeclaration.TimelineScope,
        CssDeclaration.Animation,
        CssDeclaration.AnimationDelay,
        CssDeclaration.AnimationTimeline,
        CssDeclaration.AnimationRange,
        CssDeclaration.ContainerName,
        CssDeclaration.ContainerType,
        CssDeclaration.AnchorName,
        CssDeclaration.PositionAnchor,
        CssDeclaration.PositionArea,
        CssDeclaration.PositionTryFallbacks,
        CssDeclaration.ViewTransitionName,
        CssDeclaration.ViewTransitionClass,
        CssDeclaration.ContentVisibility,
        CssDeclaration.ContainIntrinsicSize {

  CssProperty property();

  String cssValue();

  default String toCssRule() {
    return property().cssName() + ":" + cssValue();
  }

  enum DisplayMode {
    FLEX("flex"),
    INLINE_FLEX("inline-flex"),
    GRID("grid"),
    BLOCK("block"),
    INLINE_BLOCK("inline-block"),
    CONTENTS("contents"),
    NONE("none");

    private final String css;

    DisplayMode(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  enum PositionMode {
    RELATIVE("relative"),
    ABSOLUTE("absolute"),
    FIXED("fixed"),
    STICKY("sticky");

    private final String css;

    PositionMode(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  enum Direction {
    ROW("row"),
    COLUMN("column"),
    ROW_REVERSE("row-reverse"),
    COLUMN_REVERSE("column-reverse");

    private final String css;

    Direction(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  enum Wrap {
    NOWRAP("nowrap"),
    WRAP("wrap"),
    WRAP_REVERSE("wrap-reverse");

    private final String css;

    Wrap(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  enum Alignment {
    START("flex-start"),
    CENTER("center"),
    END("flex-end"),
    STRETCH("stretch"),
    BASELINE("baseline");

    private final String css;

    Alignment(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  enum Justify {
    START("flex-start"),
    CENTER("center"),
    END("flex-end"),
    BETWEEN("space-between"),
    AROUND("space-around"),
    EVENLY("space-evenly");

    private final String css;

    Justify(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  enum TextWrapMode {
    BALANCE("balance"),
    PRETTY("pretty"),
    NOWRAP("nowrap");

    private final String css;

    TextWrapMode(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  enum ContainerKind {
    INLINE_SIZE("inline-size"),
    SIZE("size"),
    NORMAL("normal");

    private final String css;

    ContainerKind(String css) {
      this.css = css;
    }

    public String css() {
      return css;
    }
  }

  record Display(DisplayMode mode) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.DISPLAY;
    }

    @Override
    public String cssValue() {
      return mode.css();
    }
  }

  record Position(PositionMode mode) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.POSITION;
    }

    @Override
    public String cssValue() {
      return mode.css();
    }
  }

  record InsetBlockStart(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.INSET_BLOCK_START;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record InsetInlineStart(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.INSET_INLINE_START;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record ZIndex(int level) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.Z_INDEX;
    }

    @Override
    public String cssValue() {
      return Integer.toString(level);
    }
  }

  record Overflow(String mode) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.OVERFLOW;
    }

    @Override
    public String cssValue() {
      return mode;
    }
  }

  record FlexDirection(Direction direction) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.FLEX_DIRECTION;
    }

    @Override
    public String cssValue() {
      return direction.css();
    }
  }

  record FlexWrap(Wrap wrap) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.FLEX_WRAP;
    }

    @Override
    public String cssValue() {
      return wrap.css();
    }
  }

  record AlignItems(Alignment alignment) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ALIGN_ITEMS;
    }

    @Override
    public String cssValue() {
      return alignment.css();
    }
  }

  record JustifyContent(Justify justify) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.JUSTIFY_CONTENT;
    }

    @Override
    public String cssValue() {
      return justify.css();
    }
  }

  record FlexGrow(int factor) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.FLEX_GROW;
    }

    @Override
    public String cssValue() {
      return Integer.toString(factor);
    }
  }

  record FlexShrink(int factor) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.FLEX_SHRINK;
    }

    @Override
    public String cssValue() {
      return Integer.toString(factor);
    }
  }

  record GridTemplateColumns(String template) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.GRID_TEMPLATE_COLUMNS;
    }

    @Override
    public String cssValue() {
      return template;
    }
  }

  record GridTemplateRows(String template) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.GRID_TEMPLATE_ROWS;
    }

    @Override
    public String cssValue() {
      return template;
    }
  }

  record GridColumn(String spanSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.GRID_COLUMN;
    }

    @Override
    public String cssValue() {
      return spanSpec;
    }
  }

  record GridRow(String spanSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.GRID_ROW;
    }

    @Override
    public String cssValue() {
      return spanSpec;
    }
  }

  record Gap(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.GAP;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record RowGap(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ROW_GAP;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record ColumnGap(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.COLUMN_GAP;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record InlineSize(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.INLINE_SIZE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record BlockSize(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.BLOCK_SIZE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record MinInlineSize(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.MIN_INLINE_SIZE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record MaxInlineSize(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.MAX_INLINE_SIZE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record MinBlockSize(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.MIN_BLOCK_SIZE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record MaxBlockSize(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.MAX_BLOCK_SIZE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record AspectRatio(String ratio) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ASPECT_RATIO;
    }

    @Override
    public String cssValue() {
      return ratio;
    }
  }

  record ObjectFit(String fit) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.OBJECT_FIT;
    }

    @Override
    public String cssValue() {
      return fit;
    }
  }

  record PaddingInline(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.PADDING_INLINE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record PaddingBlock(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.PADDING_BLOCK;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record MarginInline(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.MARGIN_INLINE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record MarginBlock(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.MARGIN_BLOCK;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record Background(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.BACKGROUND;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record Color(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.COLOR;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record FontSize(CssLength length) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.FONT_SIZE;
    }

    @Override
    public String cssValue() {
      return length.toCss();
    }
  }

  record FontWeight(int weight) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.FONT_WEIGHT;
    }

    @Override
    public String cssValue() {
      return Integer.toString(weight);
    }
  }

  record FontVariantNumeric(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.FONT_VARIANT_NUMERIC;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record LineHeight(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.LINE_HEIGHT;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record TextAlign(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_ALIGN;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record TextWrap(TextWrapMode mode) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_WRAP;
    }

    @Override
    public String cssValue() {
      return mode.css();
    }
  }

  record TextDecorationLine(String lineSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_DECORATION_LINE;
    }

    @Override
    public String cssValue() {
      return lineSpec;
    }
  }

  record TextDecorationStyle(String styleSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_DECORATION_STYLE;
    }

    @Override
    public String cssValue() {
      return styleSpec;
    }
  }

  record TextDecorationColor(String colorSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_DECORATION_COLOR;
    }

    @Override
    public String cssValue() {
      return colorSpec;
    }
  }

  record TextDecorationThickness(String thicknessSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_DECORATION_THICKNESS;
    }

    @Override
    public String cssValue() {
      return thicknessSpec;
    }
  }

  record TextUnderlineOffset(CssLength offset) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_UNDERLINE_OFFSET;
    }

    @Override
    public String cssValue() {
      return offset.toCss();
    }
  }

  record TextDecorationSkipInk(String mode) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TEXT_DECORATION_SKIP_INK;
    }

    @Override
    public String cssValue() {
      return mode;
    }
  }

  record Border(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.BORDER;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record BorderRadius(CssLength radius) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.BORDER_RADIUS;
    }

    @Override
    public String cssValue() {
      return radius.toCss();
    }
  }

  record BoxShadow(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.BOX_SHADOW;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record Outline(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.OUTLINE;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record OutlineOffset(CssLength offset) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.OUTLINE_OFFSET;
    }

    @Override
    public String cssValue() {
      return offset.toCss();
    }
  }

  record Opacity(double value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.OPACITY;
    }

    @Override
    public String cssValue() {
      return CssLength.formatNumber(value);
    }
  }

  record Cursor(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.CURSOR;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record Transition(String value) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TRANSITION;
    }

    @Override
    public String cssValue() {
      return value;
    }
  }

  record ViewTimeline(String spec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.VIEW_TIMELINE;
    }

    @Override
    public String cssValue() {
      return spec;
    }
  }

  record ScrollTimeline(String spec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.SCROLL_TIMELINE;
    }

    @Override
    public String cssValue() {
      return spec;
    }
  }

  record TimelineScope(String scopeIdent) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.TIMELINE_SCOPE;
    }

    @Override
    public String cssValue() {
      return scopeIdent;
    }
  }

  record Animation(String spec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ANIMATION;
    }

    @Override
    public String cssValue() {
      return spec;
    }
  }

  record AnimationDelay(String delayExpr) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ANIMATION_DELAY;
    }

    @Override
    public String cssValue() {
      return delayExpr;
    }
  }

  record AnimationTimeline(String timelineSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ANIMATION_TIMELINE;
    }

    @Override
    public String cssValue() {
      return timelineSpec;
    }
  }

  record AnimationRange(String rangeSpec) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ANIMATION_RANGE;
    }

    @Override
    public String cssValue() {
      return rangeSpec;
    }
  }

  record ContainerName(String name) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.CONTAINER_NAME;
    }

    @Override
    public String cssValue() {
      return name;
    }
  }

  record ContainerType(ContainerKind kind) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.CONTAINER_TYPE;
    }

    @Override
    public String cssValue() {
      return kind.css();
    }
  }

  record AnchorName(String customIdent) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.ANCHOR_NAME;
    }

    @Override
    public String cssValue() {
      return customIdent;
    }
  }

  record PositionAnchor(String customIdent) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.POSITION_ANCHOR;
    }

    @Override
    public String cssValue() {
      return customIdent;
    }
  }

  record PositionArea(String area) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.POSITION_AREA;
    }

    @Override
    public String cssValue() {
      return area;
    }
  }

  record PositionTryFallbacks(String fallbacks) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.POSITION_TRY_FALLBACKS;
    }

    @Override
    public String cssValue() {
      return fallbacks;
    }
  }

  record ViewTransitionName(String ident) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.VIEW_TRANSITION_NAME;
    }

    @Override
    public String cssValue() {
      return ident;
    }
  }

  record ViewTransitionClass(String cls) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.VIEW_TRANSITION_CLASS;
    }

    @Override
    public String cssValue() {
      return cls;
    }
  }

  record ContentVisibility(String mode) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.CONTENT_VISIBILITY;
    }

    @Override
    public String cssValue() {
      return mode;
    }
  }

  record ContainIntrinsicSize(String size) implements CssDeclaration {
    @Override
    public CssProperty property() {
      return CssProperty.CONTAIN_INTRINSIC_SIZE;
    }

    @Override
    public String cssValue() {
      return size;
    }
  }
}
