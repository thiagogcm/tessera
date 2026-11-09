package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.details;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.summary;

import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/// Stack of collapsible sections on native `<details>` and `<summary>` elements, so "Find in
/// page" can reach closed items. Create one with [#of()]; by default any number of items can be
/// open, and [#exclusive()] makes opening one close the others. The open and close animation is
/// skipped under reduced motion.
///
/// An exclusive accordion also uses its [#id(String)] as its `<details name>` group. Without an id,
/// the compiler numbers instances `accordion-<n>` and the group gets a page-unique name.
public final class Accordion extends BuiltinView<Accordion> {

  private static final DocumentKey<AtomicInteger> GROUPS =
      DocumentKey.of("accordion-groups", AtomicInteger.class, AtomicInteger::new);

  private record Item(String title, Element content, boolean open) {}

  private boolean exclusive;
  private final List<Item> items = new ArrayList<>();

  private Accordion() {}

  /// Creates an empty accordion in which any number of items can be open.
  public static Accordion of() {
    return new Accordion();
  }

  /// Makes opening one item close the others, through a shared `<details name>` group.
  public Accordion exclusive() {
    this.exclusive = true;
    return this;
  }

  /// Adds an item titled `title` whose body is `text` in a muted paragraph, open on load when
  /// `open` is true.
  public Accordion item(String title, String text, boolean open) {
    return item(title, p().textColor(Tokens.TEXT_MUTED).textWrap(TextWrap.PRETTY).text(text), open);
  }

  /// Adds an item titled `title` whose body is `content`, open on load when `open` is true.
  public Accordion item(String title, IntoElement content, boolean open) {
    items.add(new Item(title, content.intoElement(), open));
    return this;
  }

  @Override
  public Accordion self() {
    return this;
  }

  @Override
  String viewType() {
    return "accordion";
  }

  @Override
  void configureScope(ScopeStyle scope) {
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
                    .listStyleNone()
                    .on(State.HOVER, h -> h.bg(Tokens.SURFACE_SUBTLE)))
        .select(
            ":scope > details > summary .accordion-chevron", s -> s.textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope > details:open > summary .accordion-chevron",
            s -> s.transform(Transform.rotate(180)).textColor(Tokens.ACCENT_PRIMARY))
        .select(
            ":scope > details > .accordion-body",
            s -> s.px(Tokens.SPACE_5).py(Tokens.SPACE_4).pt(Tokens.SPACE_1))
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(
                        ":scope > details > summary .accordion-chevron",
                        s ->
                            s.transition(
                                Transition.controlFeedback().properties(Animatable.TRANSFORM)))
                    .select(
                        ":scope > details::details-content",
                        s ->
                            s.blockSize(CssLength.px(0))
                                .overflow(Overflow.CLIP)
                                .transition(Transition.disclosure()))
                    .select(
                        ":scope > details:open::details-content",
                        s -> s.blockSize(CssLength.auto())));
  }

  @Override
  IntoElement render(RenderContext context) {
    var root = div();
    var group = exclusive ? groupName(context) : null;
    for (var item : items) {
      var detailsEl =
          details()
              .whenPresent(group, (d, name) -> d.attr("name", name))
              .boolAttr("open", item.open())
              .children(
                  summary()
                      .children(
                          span().text(item.title()),
                          span().attr("class", "accordion-chevron").ariaHidden(true).text("▾")),
                  div().attr("class", "accordion-body").child(item.content()));
      root.child(detailsEl);
    }
    return root;
  }

  private String groupName(RenderContext context) {
    var explicit = id;
    return explicit != null
        ? Fnv1a64.sanitizeLocalId(explicit)
        : "accordion-" + context.document(GROUPS).incrementAndGet();
  }
}
