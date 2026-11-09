package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.details;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ol;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.summary;

import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Transition;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Collapsible `<nav>` of links to the page's headings, with the link for the section in view
/// highlighted. Create one with [#of(String)]: it lists every heading registered on the page,
/// such as [Section] headings, in registration order, and is resolved after layout, so it may
/// appear before the sections it lists. [#headings(List)] lists a fixed set instead. [DocLayout]
/// places one beside its sections.
public final class TableOfContents extends BuiltinComponent {

  private static final String CHEVRON_SVG =
      "<svg class=\"toc-chevron\" width=\"14\" height=\"14\" viewBox=\"0 0 24 24\" fill=\"none\""
          + " stroke=\"currentColor\" stroke-width=\"2.2\" stroke-linecap=\"round\""
          + " stroke-linejoin=\"round\" aria-hidden=\"true\"><polyline points=\"9 18 15 12 9"
          + " 6\"/></svg>";

  private final String title;
  private @Nullable String id;
  private @Nullable List<DocumentHeading> headings;

  private TableOfContents(String title) {
    this.title = title;
  }

  /// Creates a table of contents titled `title`, which is its visible heading and the `<nav>`
  /// name.
  public static TableOfContents of(String title) {
    return new TableOfContents(title);
  }

  /// Names this instance in the enclosing view scope. Without it, the compiler assigns
  /// `table-of-contents-<n>`.
  public TableOfContents id(String id) {
    this.id = id;
    return this;
  }

  /// Lists `headings`, in order, instead of the headings registered on the page.
  public TableOfContents headings(List<DocumentHeading> headings) {
    this.headings = List.copyOf(headings);
    return this;
  }

  @Override
  IntoElement render(RenderContext context) {
    var fixed = headings;
    if (fixed != null) {
      return new Links(title, id, fixed);
    }
    var instance = id;
    return Elements.late(late -> new Links(title, instance, late.headings()));
  }

  /// The rendered list, a view for its scoped styles.
  private static final class Links extends BuiltinView {

    private final String title;
    private final List<DocumentHeading> items;

    Links(String title, @Nullable String id, List<DocumentHeading> items) {
      this.title = title;
      this.id = id;
      this.items = List.copyOf(items);
    }

    @Override
    String viewType() {
      return "table-of-contents";
    }

    @Override
    void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.flex()
                      .flexCol()
                      .gap(Tokens.SPACE_2)
                      .p(CssLength.px(0))
                      .bg(CssColor.transparent())
                      .border(Border.none()))
          .select(
              ":scope .toc-summary",
              s ->
                  s.flex()
                      .itemsCenter()
                      .justifyBetween()
                      .py(CssLength.rem(0.35))
                      .px(Tokens.SPACE_1)
                      .listStyleNone()
                      .cursor(Cursor.POINTER)
                      .textColor(Tokens.TEXT_MUTED)
                      .fontSize(Tokens.FONT_SIZE_XS)
                      .fontWeight(700)
                      .on(State.HOVER, h -> h.textColor(Tokens.TEXT_PRIMARY)))
          .select(
              ":scope .toc-chevron",
              s ->
                  s.transition(
                      Transition.of(CssDuration.ms(200), Easing.ease(), Animatable.TRANSFORM)))
          .select(":scope details:open .toc-chevron", s -> s.transform(Transform.rotate(90)))
          .select(
              ":scope details::details-content",
              s ->
                  s.transition(
                      Transition.of(
                              CssDuration.ms(300),
                              Easing.ease(),
                              Animatable.BLOCK_SIZE,
                              Animatable.CONTENT_VISIBILITY)
                          .allowingDiscrete()))
          .select(
              ":scope .toc-collapsible-wrapper",
              s ->
                  s.scrollTargetGroupAuto()
                      .borderInlineStart(Border.hairline(Tokens.BORDER_SUBTLE))
                      .ps(Tokens.SPACE_1))
          .select(
              ":scope ol",
              s -> s.listStyleNone().flex().flexCol().gap(Tokens.SPACE_0_5).p(CssLength.px(0)))
          .select(
              ":scope a",
              s ->
                  s.block()
                      .py(CssLength.rem(0.35))
                      .px(Tokens.SPACE_3)
                      .textColor(Tokens.TEXT_MUTED)
                      .fontSize(Tokens.FONT_SIZE_SM)
                      .rounded(Tokens.RADIUS_SM)
                      .textDecorationNone()
                      .transition(
                          Transition.controlFeedback()
                              .properties(Animatable.COLOR, Animatable.BACKGROUND)))
          .select(
              ":scope a[data-level=\"3\"]", s -> s.ps(Tokens.SPACE_6).fontSize(Tokens.FONT_SIZE_XS))
          .select(":scope a:hover", s -> s.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
          .select(
              ":scope a:target-current",
              s ->
                  s.textColor(Tokens.ACCENT_TEXT)
                      .fontWeight(600)
                      .borderInlineStart(Border.solid(CssLength.px(3), Tokens.ACCENT_PRIMARY))
                      .bg(Tone.ACCENT.color().mixTransparent(24)));
    }

    @Override
    IntoElement render(RenderContext context) {
      var list = ol();
      for (var item : items) {
        list.child(
            li().child(
                    a(item.target())
                        .attr("data-level", Integer.toString(item.level()))
                        .text(item.title())));
      }

      var summaryEl =
          summary()
              .attr("class", "toc-summary")
              .child(span().attr("class", "toc-title").text(title))
              .child(rawHtml(CHEVRON_SVG));

      var detailsEl =
          details()
              .attr("open", "")
              .attr("class", "toc-collapsible-wrapper")
              .child(summaryEl)
              .child(list);

      return nav().attr("class", "toc-container").ariaLabel(title).child(detailsEl);
    }
  }
}
