package dev.nthings.tessera.css;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StyleRefinement")
class StyleRefinementTest {

  @Test
  @DisplayName("writing a property again replaces its value")
  void lastWriteReplaces() {
    var style = new StyleRefinement().bg(Tokens.SURFACE_CARD).bg(Tokens.SURFACE_SUBTLE);

    assertThat(style.toCssBlockBody()).isEqualTo("background:var(--surface-subtle)");
  }

  @Test
  @DisplayName("a shorthand written after its longhands removes them")
  void shorthandClearsEarlierLonghands() {
    var style =
        new StyleRefinement()
            .pt(CssLength.rem(2))
            .pe(CssLength.rem(3))
            .rowGap(CssLength.rem(1))
            .overflowX(Overflow.AUTO)
            .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE))
            .p(CssLength.rem(1))
            .gap(CssLength.rem(0.5))
            .overflow(Overflow.HIDDEN)
            .border(Border.none());

    assertThat(style.toCssBlockBody())
        .isEqualTo("overflow:hidden;gap:0.5rem;padding-inline:1rem;padding-block:1rem;border:none");
  }

  @Test
  @DisplayName("a longhand written after its shorthand is kept and emitted after it")
  void longhandAfterShorthandWins() {
    var style =
        new StyleRefinement()
            .px(CssLength.rem(1))
            .py(CssLength.rem(1))
            .pb(CssLength.zero())
            .ps(CssLength.rem(2))
            .border(Border.hairline(Tokens.BORDER_SUBTLE))
            .borderInlineEnd(Border.none());

    assertThat(style.toCssBlockBody())
        .isEqualTo(
            "padding-inline:1rem;padding-block:1rem;padding-inline-start:2rem;"
                + "padding-block-end:0;border:1px solid var(--border-subtle);"
                + "border-inline-end:none");
  }

  @Test
  @DisplayName("merging replays the other refinement's writes, so its shorthands win")
  void mergeReplaysWrites() {
    var base = new StyleRefinement().mt(CssLength.rem(2)).textColor(Tokens.TEXT_PRIMARY);
    var override = new StyleRefinement().my(CssLength.zero());

    base.mergeFrom(override);

    assertThat(base.toCssBlockBody()).isEqualTo("margin-block:0;color:var(--text-primary)");
  }

  @Test
  @DisplayName("a preset applies through styles and keeps the chain")
  void presetsApply() {
    StyleBlock card = s -> s.p(Tokens.SPACE_4).rounded(Tokens.RADIUS_LG).textWrap(TextWrap.PRETTY);

    var style = new StyleRefinement().styles(card).objectFit(ObjectFit.COVER);

    assertThat(style.toCssBlockBody())
        .isEqualTo(
            "object-fit:cover;padding-inline:var(--space-4);padding-block:var(--space-4);"
                + "text-wrap:pretty;border-radius:var(--radius-lg)");
  }

  @Test
  @DisplayName("keyword properties take their enum")
  void keywordEnums() {
    var style =
        new StyleRefinement()
            .cursor(Cursor.POINTER)
            .appearance(Appearance.NONE)
            .resize(Resize.VERTICAL)
            .overflowY(Overflow.CLIP);

    assertThat(style.toCssBlockBody())
        .isEqualTo("overflow-y:clip;cursor:pointer;appearance:none;resize:vertical");
  }
}
