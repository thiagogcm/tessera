package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssDeclaration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Semantic 4-region card component ({@code media}, {@code header}, {@code content}, {@code footer})
 * implementing modern container-aware and subgrid-compatible layout patterns.
 *
 * <ul>
 *   <li>Uses {@code <article>} (or {@code <label>} for choice cards) as root with direct child
 *       regions in source order.
 *   <li>Adapts to wide containers via {@code @container} + {@code :has(> img, > picture, > .media)}
 *       so only cards with media switch to a side-by-side column layout.
 *   <li>Supports {@link Interaction.Content}, {@link Interaction.StretchedLink} ({@code
 *       a.card-link::after} with {@code z-index: 1} on secondary controls), and {@link
 *       Interaction.Choice} ({@code :has(> input:checked)} without layout-shifting border widths).
 * </ul>
 */
public final class Card implements RenderView, FluentBuilder<Card> {

  public sealed interface Interaction
      permits Interaction.Content, Interaction.StretchedLink, Interaction.Choice {

    enum Content implements Interaction {
      INSTANCE
    }

    record StretchedLink(String href) implements Interaction {}

    record Choice(String name, String value, boolean multiSelect, boolean checked)
        implements Interaction {}
  }

  private final String id;
  private @Nullable Element media;
  private String headingText = "";
  private @Nullable String subtitleText;
  private final List<Element> contentItems = new ArrayList<>();
  private final List<Element> footerActions = new ArrayList<>();
  private Interaction interaction = Interaction.Content.INSTANCE;

  public Card(String id) {
    this.id = id;
  }

  public static Card create(String id) {
    return new Card(id);
  }

  @Override
  public Card self() {
    return this;
  }

  public Card mediaImg(String src, String alt, String aspectRatio) {
    this.media = Elements.img(src, alt).aspectRatio(aspectRatio).objectCover().fullWidth();
    return this;
  }

  public Card media(IntoElement mediaElement) {
    Element el = mediaElement.intoElement();
    if (el instanceof TagElement tag && tag.tag() != HtmlTag.IMG && tag.tag() != HtmlTag.PICTURE) {
      tag.attr("class", "media");
    }
    this.media = el;
    return this;
  }

  public Card title(String heading) {
    this.headingText = heading;
    return this;
  }

  public Card subtitle(String subtitle) {
    this.subtitleText = subtitle;
    return this;
  }

  public Card bodyText(String paragraph) {
    this.contentItems.add(Elements.p().textPretty().text(paragraph));
    return this;
  }

  public Card content(IntoElement... items) {
    for (var item : items) {
      this.contentItems.add(item.intoElement());
    }
    return this;
  }

  public Card action(IntoElement... actions) {
    for (var action : actions) {
      this.footerActions.add(action.intoElement());
    }
    return this;
  }

  public Card stretchedLink(String href) {
    this.interaction = new Interaction.StretchedLink(href);
    return this;
  }

  public Card choiceRadio(String groupName, String value, boolean checked) {
    this.interaction = new Interaction.Choice(groupName, value, false, checked);
    return this;
  }

  public Card choiceCheckbox(String name, String value, boolean checked) {
    this.interaction = new Interaction.Choice(name, value, true, checked);
    return this;
  }

  @Override
  public ElementId entityId() {
    return ElementId.of("card-" + id);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.containerInlineSize()
                    .flex()
                    .flexCol()
                    .gap(CssLength.rem(0.75))
                    .p(CssLength.rem(1.25))
                    .rounded(CssLength.rem(0.75))
                    .bg(Tokens.SURFACE_CARD)
                    .border("2px solid " + Tokens.BORDER_SUBTLE.varReference())
                    .relative())
        .select(":scope a.card-link", s -> s.textDecorationNone())
        .select(
            ":scope a.card-link:hover",
            s ->
                s.refine(
                    r ->
                        r.put(new CssDeclaration.TextDecorationLine("underline"))
                            .put(new CssDeclaration.TextUnderlineOffset(CssLength.em(0.2)))))
        .select(
            ":scope:has(a.card-link) a.card-link::after",
            s -> s.cssContent("\"\"").absolute().inset("0"))
        .select(":scope:has(a.card-link) a.card-link:focus-visible", s -> s.focusOutlineNone())
        .select(":scope:has(a.card-link:focus-visible)", s -> s.focusRing(Tokens.ACCENT_PRIMARY))
        .select(":scope:has(a.card-link:hover)", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope:has(a.card-link) :is(a:not(.card-link),button,input,select,textarea)",
            s -> s.relative().zIndex(1))
        .select(
            ":scope:has(> input:checked)",
            s ->
                s.border("2px solid " + Tokens.ACCENT_PRIMARY.varReference())
                    .bg(Tokens.SURFACE_SUBTLE))
        .containerMinInlineSize(
            CssLength.rem(28),
            c ->
                c.select(
                        ":scope:has(> img,> picture,> .media)",
                        s ->
                            s.grid()
                                .gridColumns("minmax(10rem, 35%) 1fr")
                                .colGap(CssLength.rem(1.25))
                                .itemsStart())
                    .select(
                        ":scope:has(> img,> picture,> .media) > :is(img,picture,.media)",
                        s -> s.gridColumn("1").gridRow("1 / span 3"))
                    .select(
                        ":scope:has(> img,> picture,> .media) > :not(img,picture,.media)",
                        s -> s.gridColumn("2")));
  }

  @Override
  public Element render() {
    TagElement root =
        switch (interaction) {
          case Interaction.Choice choice -> {
            var input =
                Elements.input(choice.multiSelect() ? "checkbox" : "radio")
                    .attr("name", choice.name())
                    .attr("value", choice.value())
                    .boolAttr("checked", choice.checked());
            yield Elements.label().cursorPointer().child(input);
          }
          case Interaction.Content _, Interaction.StretchedLink _ -> Elements.article();
        };

    // Region 1: Media (direct child, never wrapped if <img> or <picture>)
    if (media != null) {
      root.child(media);
    }

    // Region 2: Header (direct heading or <hgroup> when subtitle is present)
    if (!headingText.isEmpty()) {
      Element headingInner =
          switch (interaction) {
            case Interaction.StretchedLink(var href) ->
                Elements.a(href)
                    .attr("class", "card-link")
                    .textColor(Tokens.TEXT_PRIMARY)
                    .text(headingText);
            case Interaction.Content _, Interaction.Choice _ -> Elements.text(headingText);
          };
      TagElement h3 = Elements.h3().textBalance().child(headingInner);
      if (subtitleText != null && !subtitleText.isBlank()) {
        root.child(
            Elements.hgroup()
                .children(
                    h3,
                    Elements.p()
                        .textColor(Tokens.TEXT_MUTED)
                        .textSize(CssLength.rem(0.875))
                        .text(subtitleText)));
      } else {
        root.child(h3);
      }
    }

    // Region 3: Content (single element stands on its own; multiple elements grouped in one <div>
    // for subgrid track alignment)
    if (contentItems.size() == 1) {
      root.child(contentItems.getFirst());
    } else if (contentItems.size() > 1) {
      root.child(Elements.div().flex().flexCol().gap(CssLength.rem(0.5)).children(contentItems));
    }

    // Region 4: Footer (holds independent actions as native links/buttons)
    if (!footerActions.isEmpty()) {
      root.child(
          Elements.footer().flex().itemsCenter().gap(CssLength.rem(0.75)).children(footerActions));
    }

    return root;
  }
}
