package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.GridPlacement;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Card with four regions, in source order: media, header (title and description), body, and footer
/// (actions). It renders as an `<article>`, or as a `<label>` after [#radio] or [#checkbox]. Create
/// one with [#of(String)].
///
/// - A card whose media is an image or a `media` region places the media beside the other regions
///   once the card is 28rem or wider.
/// - [#stretchedLink(String)] makes the title a link that covers the whole card. Other links,
///   buttons, and form controls in the card stay clickable.
/// - Only one of `stretchedLink`, `radio`, and `checkbox` applies; the last call wins. [#checked()]
///   starts a radio or checkbox card checked, in any call order.
///
/// Without an [#scopeId(String)], the compiler numbers instances `card-<n>`.
public final class Card extends BuiltinScope<Card> {

  /// How a card responds to interaction; a card without one has only its own content.
  private sealed interface Interaction {}

  private record StretchedLink(String href) implements Interaction {}

  private record Choice(String name, String value, boolean multiSelect) implements Interaction {}

  private final String title;
  private @Nullable Element media;
  private @Nullable String description;
  private int headingLevel = 3;
  private final List<Element> body = new ArrayList<>();
  private final List<Element> actions = new ArrayList<>();
  private @Nullable Interaction interaction;
  private boolean checked;

  private Card(String title) {
    this.title = title;
  }

  /// Creates a card whose heading shows `title`. An empty title renders no heading, so the
  /// description and any stretched link are not rendered either.
  public static Card of(String title) {
    return new Card(title);
  }

  @Override
  public Card self() {
    return this;
  }

  /// Sets the media to `image`, placed as a direct `<img>` child.
  public Card media(Image image) {
    this.media = image.intoElement();
    return this;
  }

  /// Sets the media to other content, wrapped in a `div.media` region.
  public Card media(IntoElement content) {
    this.media = Elements.div().attr("class", "media").child(content);
    return this;
  }

  /// Sets the title's heading level, 2 to 6 (default 3), to match the enclosing section and keep
  /// the outline free of gaps. Throws IllegalArgumentException outside that range.
  public Card headingLevel(int level) {
    if (level < 2 || level > 6) {
      throw new IllegalArgumentException("Card heading level must be 2 to 6: " + level);
    }
    this.headingLevel = level;
    return this;
  }

  /// Sets muted text under the title. It is rendered only when the card has a title.
  public Card description(String description) {
    this.description = description;
    return this;
  }

  /// Adds a paragraph to the body region.
  public Card bodyText(String paragraph) {
    this.body.add(Elements.p().textWrap(TextWrap.PRETTY).text(paragraph));
    return this;
  }

  /// Adds content to the body region, in order.
  public Card body(IntoElement... content) {
    for (var item : content) {
      this.body.add(item.intoElement());
    }
    return this;
  }

  /// Adds links or buttons to the footer region, which is omitted when there are none.
  public Card actions(IntoElement... actions) {
    for (var action : actions) {
      this.actions.add(action.intoElement());
    }
    return this;
  }

  /// Makes the title a link to `href` that covers the whole card. Without a title, no link is
  /// rendered.
  public Card stretchedLink(String href) {
    this.interaction = new StretchedLink(href);
    return this;
  }

  /// Makes the card a radio choice: a `<label>` around a radio input named `name` with `value`.
  /// Cards sharing a `name` form one group; mark the one selected on load with [#checked()].
  public Card radio(String name, String value) {
    this.interaction = new Choice(name, value, false);
    return this;
  }

  /// Makes the card a checkbox choice: a `<label>` around a checkbox input named `name` with
  /// `value`. Add [#checked()] to check it on load.
  public Card checkbox(String name, String value) {
    this.interaction = new Choice(name, value, true);
    return this;
  }

  /// Checks the card's radio or checkbox input on load. It has no effect on a card that is not a
  /// [#radio(String, String)] or [#checkbox(String, String)] choice.
  public Card checked() {
    this.checked = true;
    return this;
  }

  @Override
  String scopeName() {
    return "card";
  }

  private static final String MEDIA_CARD = ":scope:has(> img, > picture, > .media)";

  /// Container name for the card's width. [CardGrid] resets it, since layout containment makes
  /// `subgrid` compute to `none`.
  private static final Container CONTAINER =
      Container.named("tessera-card", Container.Type.INLINE_SIZE);

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s ->
            s.container(CONTAINER)
                .flex()
                .flexCol()
                .gap(Tokens.SPACE_3)
                .p(Tokens.SPACE_5)
                .rounded(Tokens.RADIUS_XL)
                .bg(Tokens.SURFACE_CARD)
                .border(Border.solid(CssLength.px(2), Tokens.BORDER_SUBTLE))
                .relative())
        .select(
            ":scope a.card-link",
            s ->
                s.textDecorationNone()
                    .on(State.HOVER, h -> h.textDecorationUnderline(CssLength.em(0.2))))
        .select(
            ":scope:has(a.card-link) a.card-link::after",
            s -> s.content(CssContent.empty()).absolute().inset(CssLength.zero()))
        .select(":scope:has(a.card-link) a.card-link:focus-visible", s -> s.outline(Border.none()))
        .select(":scope:has(a.card-link:focus-visible)", s -> s.focusRing(Tokens.ACCENT_PRIMARY))
        .select(":scope:has(a.card-link:hover)", s -> s.bg(Tokens.SURFACE_SUBTLE))
        .select(
            ":scope:has(a.card-link) :is(a:not(.card-link),button,input,select,textarea)",
            s -> s.relative().zIndex(1))
        .select(
            ":scope:has(> input:checked)",
            s ->
                s.border(Border.solid(CssLength.px(2), Tokens.ACCENT_PRIMARY))
                    .bg(Tokens.SURFACE_SUBTLE))
        // Fixed rows so siblings align under a CardGrid subgrid; ignored in flex-column layout.
        .select(":scope > :is(img, picture, .media, input)", s -> s.gridRow(GridPlacement.at(1)))
        .select(":scope > :is(h2, h3, h4, h5, h6, hgroup)", s -> s.gridRow(GridPlacement.at(2)))
        .select(
            ":scope > :not(img, picture, .media, input, h2, h3, h4, h5, h6, hgroup, footer)",
            s -> s.gridRow(GridPlacement.at(3)))
        .select(":scope > footer", s -> s.gridRow(GridPlacement.at(4)))
        // Media cards: children span both columns until the 28rem container query places them.
        // The card cannot query its own size, so the base template is unconditional.
        .select(
            MEDIA_CARD,
            s ->
                s.grid()
                    .gridColumns(
                        TrackList.of(
                            GridTrack.minmax(
                                GridTrack.length(CssLength.rem(10)),
                                GridTrack.length(CssLength.pct(35))),
                            GridTrack.fr(1)))
                    .colGap(Tokens.SPACE_5)
                    .rowGap(Tokens.SPACE_3)
                    .itemsStart())
        .select(MEDIA_CARD + " > *", s -> s.gridColumn(GridPlacement.between(1, -1)))
        .on(
            CONTAINER.atLeast(CssLength.rem(28)),
            c ->
                c.select(
                        MEDIA_CARD + " > :is(img, picture, .media)",
                        s -> s.gridColumn(GridPlacement.at(1)).gridRow(GridPlacement.from(1, 3)))
                    .select(
                        MEDIA_CARD + " > :not(img, picture, .media)",
                        s -> s.gridColumn(GridPlacement.at(2)))
                    .select(
                        MEDIA_CARD + " > :is(h2, h3, h4, h5, h6, hgroup)",
                        s -> s.gridRow(GridPlacement.at(1)))
                    .select(
                        MEDIA_CARD
                            + " > :not(img, picture, .media, h2, h3, h4, h5, h6, hgroup, footer)",
                        s -> s.gridRow(GridPlacement.at(2)))
                    .select(MEDIA_CARD + " > footer", s -> s.gridRow(GridPlacement.at(3))));
  }

  @Override
  TagElement expand(ViewContext context) {
    var current = interaction;
    var root =
        current instanceof Choice choice
            ? Elements.label()
                .cursor(Cursor.POINTER)
                .child(
                    Elements.input(choice.multiSelect() ? InputType.CHECKBOX : InputType.RADIO)
                        .attr("name", choice.name())
                        .attr("value", choice.value())
                        .initialChecked(checked))
            : Elements.article();

    // Region 1: media, never wrapped for img/picture.
    if (media != null) {
      root.child(media);
    }

    // Region 2: header; <hgroup> when there is a description.
    if (!title.isEmpty()) {
      var heading =
          Elements.el(HtmlTag.heading(headingLevel))
              .textWrap(TextWrap.BALANCE)
              .child(
                  current instanceof StretchedLink(var href)
                      ? Elements.a(href)
                          .attr("class", "card-link")
                          .textColor(Tokens.TEXT_PRIMARY)
                          .text(title)
                      : Elements.text(title));
      if (description != null) {
        root.child(
            Elements.hgroup()
                .children(
                    heading,
                    Elements.p()
                        .textColor(Tokens.TEXT_MUTED)
                        .fontSize(Tokens.FONT_SIZE_SM)
                        .text(description)));
      } else {
        root.child(heading);
      }
    }

    // Region 3: body. Several items share one wrapper div so they occupy one grid row.
    if (body.size() == 1) {
      root.child(body.getFirst());
    } else if (body.size() > 1) {
      root.child(Elements.div().flex().flexCol().gap(Tokens.SPACE_2).children(body));
    }

    // Region 4: footer for actions (links/buttons).
    if (!actions.isEmpty()) {
      root.child(Elements.footer().flex().itemsCenter().gap(Tokens.SPACE_3).children(actions));
    }

    return root;
  }
}
