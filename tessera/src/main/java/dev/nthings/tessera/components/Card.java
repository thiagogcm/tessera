package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Semantic 4-region card component (`media`, `header`, `content`, `footer`) implementing modern
/// container-aware and subgrid-compatible layout patterns.
///
/// - Uses `<article>` (or `<label>` for choice cards) as root with direct child regions in source
///   order.
/// - Adapts to wide containers via `@container` + `:has(> img, > picture, > .media)` so only
///   cards with media switch to a side-by-side column layout.
/// - Supports [Interaction.Content], [Interaction.StretchedLink] (`a.card-link::after` with
///   `z-index: 1` on secondary controls), and [Interaction.Choice] (`:has(> input:checked)`
///   without layout-shifting border widths).
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

  private Card(String id) {
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

  /// Image media rendered as the card's direct `<img>` child.
  public Card media(Image image) {
    this.media = image.intoElement();
    return this;
  }

  /// Arbitrary media (video, figure, illustration) wrapped in a `div.media` region.
  public Card media(IntoElement mediaContent) {
    this.media = Elements.div().attr("class", "media").child(mediaContent);
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

  private static final String MEDIA_CARD = ":scope:has(> img, > picture, > .media)";

  /// Query container name for the card's own width; [CardGrid] switches it off so its cards can
  /// be subgrids (layout containment would make `grid-template-rows: subgrid` compute to `none`).
  static final String CONTAINER = "tessera-card";

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.containerInlineSize(CONTAINER)
                    .flex()
                    .flexCol()
                    .gap(Tokens.SPACE_3.length())
                    .p(Tokens.SPACE_5.length())
                    .rounded(Tokens.RADIUS_XL.length())
                    .bg(Tokens.SURFACE_CARD)
                    .border("2px solid " + Tokens.BORDER_SUBTLE.varReference())
                    .relative())
        .select(":scope a.card-link", s -> s.textDecorationNone())
        .select(
            ":scope a.card-link:hover",
            s ->
                s.refine(
                    r ->
                        r.put(CssProperty.TEXT_DECORATION_LINE, "underline")
                            .put(CssProperty.TEXT_UNDERLINE_OFFSET, CssLength.em(0.2))))
        .select(
            ":scope:has(a.card-link) a.card-link::after",
            s -> s.content("\"\"").absolute().inset("0"))
        .select(":scope:has(a.card-link) a.card-link:focus-visible", s -> s.outlineNone())
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
        // Fixed region rows, so sibling cards line up when a CardGrid makes them subgrids
        // (ignored while the card lays out as a flex column).
        .select(":scope > :is(img, picture, .media, input)", s -> s.gridRow("1"))
        .select(":scope > :is(h3, hgroup)", s -> s.gridRow("2"))
        .select(
            ":scope > :not(img, picture, .media, input, h3, hgroup, footer)", s -> s.gridRow("3"))
        .select(":scope > footer", s -> s.gridRow("4"))
        // Media cards: a two-column template whose children span both columns until the card
        // itself is wide enough (the card is the query container for its children, not for
        // itself, so the template cannot be conditional).
        .select(
            MEDIA_CARD,
            s ->
                s.grid()
                    .gridColumns("minmax(10rem, 35%) 1fr")
                    .colGap(Tokens.SPACE_5.length())
                    .rowGap(Tokens.SPACE_3.length())
                    .itemsStart())
        .select(MEDIA_CARD + " > *", s -> s.gridColumn("1 / -1"))
        .container(
            CONTAINER + " (inline-size >= 28rem)",
            c ->
                c.select(
                        MEDIA_CARD + " > :is(img, picture, .media)",
                        s -> s.gridColumn("1").gridRow("1 / span 3"))
                    .select(MEDIA_CARD + " > :not(img, picture, .media)", s -> s.gridColumn("2"))
                    .select(MEDIA_CARD + " > :is(h3, hgroup)", s -> s.gridRow("1"))
                    .select(
                        MEDIA_CARD + " > :not(img, picture, .media, h3, hgroup, footer)",
                        s -> s.gridRow("2"))
                    .select(MEDIA_CARD + " > footer", s -> s.gridRow("3")));
  }

  @Override
  public Element render() {
    var root =
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
      var headingInner =
          switch (interaction) {
            case Interaction.StretchedLink(var href) ->
                Elements.a(href)
                    .attr("class", "card-link")
                    .textColor(Tokens.TEXT_PRIMARY)
                    .text(headingText);
            case Interaction.Content _, Interaction.Choice _ -> Elements.text(headingText);
          };
      var h3 = Elements.h3().textBalance().child(headingInner);
      if (subtitleText != null) {
        root.child(
            Elements.hgroup()
                .children(
                    h3,
                    Elements.p()
                        .textColor(Tokens.TEXT_MUTED)
                        .fontSize(Tokens.FONT_SIZE_SM.length())
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
      root.child(
          Elements.div().flex().flexCol().gap(Tokens.SPACE_2.length()).children(contentItems));
    }

    // Region 4: Footer (holds independent actions as native links/buttons)
    if (!footerActions.isEmpty()) {
      root.child(
          Elements.footer()
              .flex()
              .itemsCenter()
              .gap(Tokens.SPACE_3.length())
              .children(footerActions));
    }

    return root;
  }
}
