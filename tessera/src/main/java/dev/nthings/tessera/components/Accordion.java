package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.details;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.summary;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;
import java.util.ArrayList;
import java.util.List;

/// Accordion on native `<details>`/`<summary>`, so "Find in page" works. Exclusive mode shares a
/// `name` group. Height animates via `::details-content` (`interpolate-size: allow-keywords`).
public final class Accordion implements RenderView {

  public record Item(String summaryTitle, Element content, boolean initiallyOpen) {
    public static Item ofText(String summaryTitle, String bodyText, boolean initiallyOpen) {
      return new Item(
          summaryTitle,
          p().textColor(Tokens.TEXT_MUTED).textPretty().text(bodyText),
          initiallyOpen);
    }

    public static Item of(String summaryTitle, IntoElement content, boolean initiallyOpen) {
      return new Item(summaryTitle, content.intoElement(), initiallyOpen);
    }
  }

  private final ElementId id;
  private final String groupName;
  private final boolean exclusive;
  private final List<Item> items = new ArrayList<>();

  private Accordion(String id, boolean exclusive) {
    this.id = ElementId.of(id);
    this.groupName = this.id.slug();
    this.exclusive = exclusive;
  }

  public static Accordion exclusive(String id) {
    return new Accordion(id, true);
  }

  public static Accordion multiOpen(String id) {
    return new Accordion(id, false);
  }

  public Accordion item(String summaryTitle, String bodyText, boolean initiallyOpen) {
    items.add(Item.ofText(summaryTitle, bodyText, initiallyOpen));
    return this;
  }

  public Accordion item(String summaryTitle, IntoElement content, boolean initiallyOpen) {
    items.add(Item.of(summaryTitle, content, initiallyOpen));
    return this;
  }

  @Override
  public ElementId entityId() {
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
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .overflowClip())
        .select(":scope > details", s -> s.borderBlockEndSubtle(Tokens.BORDER_SUBTLE))
        .select(":scope > details:last-child", s -> s.borderNone())
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
                    .cursorPointer()
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
                            s.blockSize(CssLength.px(0)).overflowClip().motion(Motion.disclosure()))
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
