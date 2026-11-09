package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.details;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.summary;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.List;

/// Stack of collapsible sections on native `<details>` and `<summary>` elements, so "Find in
/// page" can reach closed items. Create one with [#exclusive(String)], where opening an item
/// closes the others, or [#multiOpen(String)]. The open and close animation is skipped under
/// reduced motion.
public final class Accordion implements View {

  /// One section: the `summaryTitle` shown in its summary, its `content`, and whether it starts
  /// open.
  public record Item(String summaryTitle, Element content, boolean initiallyOpen) {
    /// Creates an item whose body is `bodyText` in a muted paragraph.
    public static Item ofText(String summaryTitle, String bodyText, boolean initiallyOpen) {
      return new Item(
          summaryTitle,
          p().textColor(Tokens.TEXT_MUTED).textWrap(TextWrap.PRETTY).text(bodyText),
          initiallyOpen);
    }

    /// Creates an item whose body is `content`.
    public static Item of(String summaryTitle, IntoElement content, boolean initiallyOpen) {
      return new Item(summaryTitle, content.intoElement(), initiallyOpen);
    }
  }

  private final String id;
  private final String groupName;
  private final boolean exclusive;
  private final List<Item> items = new ArrayList<>();

  private Accordion(String id, boolean exclusive) {
    this.id = Fnv1a64.sanitizeLocalId(id);
    this.groupName = this.id;
    this.exclusive = exclusive;
  }

  /// Creates an accordion where opening one item closes the others. Its `name` group is the slug of
  /// `id`, so give each accordion on a page a distinct id. Throws IllegalArgumentException if `id`
  /// is empty.
  public static Accordion exclusive(String id) {
    return new Accordion(id, true);
  }

  /// Creates an accordion where any number of items can be open at once. Throws
  /// IllegalArgumentException if `id` is empty.
  public static Accordion multiOpen(String id) {
    return new Accordion(id, false);
  }

  /// Adds an item whose body is `bodyText` in a muted paragraph.
  public Accordion item(String summaryTitle, String bodyText, boolean initiallyOpen) {
    items.add(Item.ofText(summaryTitle, bodyText, initiallyOpen));
    return this;
  }

  /// Adds an item whose body is `content`.
  public Accordion item(String summaryTitle, IntoElement content, boolean initiallyOpen) {
    items.add(Item.of(summaryTitle, content, initiallyOpen));
    return this;
  }

  @Override
  public String viewId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.flex()
                    .flexCol()
                    .rounded(Tokens.RADIUS_XL)
                    .bg(Tokens.SURFACE_CARD)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .overflow(Overflow.CLIP))
        .select(":scope > details", s -> s.borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE)))
        .select(":scope > details:last-child", s -> s.border(Border.none()))
        .select(
            ":scope > details > summary",
            s ->
                s.flex()
                    .itemsCenter()
                    .justifyBetween()
                    .gap(Tokens.SPACE_4)
                    .px(Tokens.SPACE_5)
                    .py(Tokens.SPACE_4)
                    .fontWeight(600)
                    .cursor(Cursor.POINTER)
                    .listStyleNone())
        .select(":scope > details > summary:hover", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope > details > summary .accordion-chevron", s -> s.textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope > details:open > summary .accordion-chevron",
            s -> s.transform(Transform.rotate(180)).textColor(Tokens.ACCENT_PRIMARY))
        .select(
            ":scope > details > .accordion-body",
            s -> s.px(Tokens.SPACE_5).py(Tokens.SPACE_4).pt(Tokens.SPACE_1))
        .motionSafe(
            m ->
                m.select(
                        ":scope > details > summary .accordion-chevron",
                        s -> s.motion(Motion.controlFeedback().properties(Animatable.TRANSFORM)))
                    .select(
                        ":scope > details::details-content",
                        s ->
                            s.blockSize(CssLength.px(0))
                                .overflow(Overflow.CLIP)
                                .motion(Motion.disclosure()))
                    .select(
                        ":scope > details:open::details-content",
                        s -> s.blockSize(CssLength.auto())));
  }

  @Override
  public Element render(RenderContext context) {
    var root = div();
    for (var item : items) {
      var detailsEl =
          details()
              .when(exclusive, d -> d.attr("name", groupName))
              .boolAttr("open", item.initiallyOpen())
              .children(
                  summary()
                      .children(
                          span().text(item.summaryTitle()),
                          span().attr("class", "accordion-chevron").ariaHidden(true).text("▾")),
                  div().attr("class", "accordion-body").child(item.content()));
      root.child(detailsEl);
    }
    return root;
  }
}
