package dev.nthings.tessera.document.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.details;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ol;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.summary;

import dev.nthings.tessera.core.OutlineHeading;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.components.Icons;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Collapsible `<nav>` of links to the page's headings, with the link for the section in view
/// highlighted. Create one with [#of(String)]: it lists every heading collected into the document
/// outline, such as `Section` and `Heading` entries, in collection order, and is resolved after
/// layout, so it may appear before the sections it lists. Headings that do not opt into the
/// outline, including dialog titles and the footnotes heading, are omitted. [#headings(List)] lists
/// a fixed set instead. [DocLayout] places one beside its sections.
public final class TableOfContents extends BuiltinDocumentComponent
    implements FluentBuilder<TableOfContents> {

  private final String title;
  private @Nullable String id;
  private @Nullable List<OutlineHeading> headings;
  private int minLevel = 1;
  private int maxLevel = 6;

  private TableOfContents(String title) {
    this.title = title;
  }

  /// Creates a table of contents titled `title`, which is its visible heading and the `<nav>` name.
  public static TableOfContents of(String title) {
    return new TableOfContents(title);
  }

  @Override
  public TableOfContents self() {
    return this;
  }

  /// Names this instance in the enclosing id scope. Without it, the compiler assigns
  /// `table-of-contents-<n>`.
  public TableOfContents id(String id) {
    this.id = id;
    return this;
  }

  /// Lists `headings`, in order, instead of the headings registered on the page.
  public TableOfContents headings(List<OutlineHeading> headings) {
    this.headings = List.copyOf(headings);
    return this;
  }

  /// Limits rendered entries to headings with levels between `min` and `max`, inclusive. Throws
  /// IllegalArgumentException unless `1 <= min <= max <= 6`. Applies to both collected outline
  /// headings and explicit [#headings(List)] entries. Defaults to `1` through `6`.
  public TableOfContents levels(int min, int max) {
    if (min < 1 || max > 6 || min > max) {
      throw new IllegalArgumentException(
          "Heading level range must satisfy 1 <= min <= max <= 6: [" + min + ", " + max + "]");
    }
    this.minLevel = min;
    this.maxLevel = max;
    return this;
  }

  @Override
  DocumentContent expand(DocumentContext context) {
    var min = minLevel;
    var max = maxLevel;
    var fixed = headings;
    if (fixed != null) {
      return DocumentContent.of(new Links(title, id, filterLevels(fixed, min, max)));
    }
    var instance = id;
    return Documents.late(late ->
        DocumentContent.of(new Links(title, instance, filterLevels(late.headings(), min, max))));
  }

  private static List<OutlineHeading> filterLevels(
      List<OutlineHeading> headings, int min, int max) {
    return headings.stream()
        .filter(heading -> heading.level() >= min && heading.level() <= max)
        .toList();
  }

  /// The rendered list, a scope for its scoped styles.
  private static final class Links implements Scope {

    private final String title;
    private final @Nullable String id;
    private final List<OutlineHeading> items;

    Links(String title, @Nullable String id, List<OutlineHeading> items) {
      this.title = title;
      this.id = id;
      this.items = items;
    }

    @Override
    public @Nullable String scopeId() {
      return id;
    }

    @Override
    public String scopeName() {
      return "table-of-contents";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s ->
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
          .select(":scope details:open .toc-chevron", s -> s.transform(Transform.rotate(90)))
          .on(
              Media.MOTION_SAFE,
              m ->
                  m.select(
                          ":scope .toc-chevron",
                          s ->
                              s.transition(
                                  Transition.of(
                                      CssDuration.ms(200), Easing.ease(), Animatable.TRANSFORM)))
                      .select(
                          ":scope details::details-content",
                          s ->
                              s.transition(
                                  Transition.of(
                                          CssDuration.ms(300),
                                          Easing.ease(),
                                          Animatable.BLOCK_SIZE,
                                          Animatable.CONTENT_VISIBILITY)
                                      .allowingDiscrete())))
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
                              .properties(Animatable.COLOR, Animatable.BACKGROUND))
                      .on(
                          State.HOVER,
                          h -> h.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE)))
          .select(
              ":scope a[data-level=\"3\"]", s -> s.ps(Tokens.SPACE_6).fontSize(Tokens.FONT_SIZE_XS))
          .select(
              ":scope a[data-level=\"4\"]", s -> s.ps(Tokens.SPACE_8).fontSize(Tokens.FONT_SIZE_XS))
          .select(
              ":scope a[data-level=\"5\"]",
              s -> s.ps(Tokens.SPACE_10).fontSize(Tokens.FONT_SIZE_XS))
          .select(
              ":scope a[data-level=\"6\"]",
              s -> s.ps(Tokens.SPACE_12).fontSize(Tokens.FONT_SIZE_XS))
          .select(
              ":scope a:target-current",
              s ->
                  s.textColor(Tokens.ACCENT_TEXT)
                      .fontWeight(600)
                      .borderInlineStart(Border.solid(CssLength.px(3), Tokens.ACCENT_PRIMARY))
                      .bg(Tone.ACCENT.color().mixTransparent(24)));
    }

    @Override
    public TagElement expand(ViewContext context) {
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
              .child(Icons.chevronRight(14).attr("class", "toc-chevron"));

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
