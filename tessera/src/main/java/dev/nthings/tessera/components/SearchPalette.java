package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.input;
import static dev.nthings.tessera.core.Elements.kbd;
import static dev.nthings.tessera.core.Elements.span;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ClosedBy;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssFilter;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.BundledBehavior;
import dev.nthings.tessera.internal.components.Icons;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.components.WidgetBridge.SearchLink;
import dev.nthings.tessera.internal.components.WidgetBridge.SearchParts;
import dev.nthings.tessera.internal.components.WidgetBridge.SearchResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Quick-open search dialog over the links you add with [#item]. Create one with [#of()], place its
/// [#triggerButton()] where users open it, and place the palette itself, usually with
/// `Documents.deferred`. A static document loads the palette's script once, shared by every palette
/// on the page; without it, the dialog opens and closes and lists every link unfiltered.
///
/// The dialog, input, no-results status and result ids are prefixed with its [#id(String)], as in
/// `<id>--dialog`, `<id>--input`, `<id>--empty` and `<id>--result-<n>`, numbering results from 1 in
/// order. Without an id, the compiler numbers instances `search-palette-<n>`.
public final class SearchPalette extends BuiltinScope<SearchPalette> {

  static {
    WidgetBridge.registerSearch((palette, _) -> palette.render(false));
  }

  /// One link of a [SearchPalette], added with [SearchPalette#item(String, Item)]. The value is
  /// immutable: [#description(String)] and [#badge(String)] return a new one.
  @CheckReturnValue
  public static final class Item {
    private final String title;
    private final String href;
    private final @Nullable String description;
    private final @Nullable String badge;

    private Item(String title, String href, @Nullable String description, @Nullable String badge) {
      this.title = title;
      this.href = href;
      this.description = description;
      this.badge = badge;
    }

    /// A link titled `title`, pointing to `href`.
    public static Item of(String title, String href) {
      return new Item(title, href, null, null);
    }

    /// The same link with a secondary `description` line under its title.
    public Item description(String description) {
      return new Item(title, href, description, badge);
    }

    /// The same link with `badge`, such as `New`, shown at its end instead of the arrow.
    public Item badge(String badge) {
      return new Item(title, href, description, badge);
    }
  }

  private final ElementRef dialog = ElementRef.scoped("dialog");
  private String placeholder = "Search documentation, routes, and sections...";
  private String triggerLabel = "Search docs...";
  private final Map<String, List<Item>> groupedItems = new LinkedHashMap<>();

  private SearchPalette() {}

  /// Creates an empty palette.
  public static SearchPalette of() {
    return new SearchPalette();
  }

  /// Sets the search field's placeholder text.
  public SearchPalette placeholder(String placeholder) {
    this.placeholder = placeholder;
    return this;
  }

  /// Sets the visible text of the [#triggerButton()]. The default is `Search docs...`.
  public SearchPalette triggerLabel(String label) {
    this.triggerLabel = label;
    return this;
  }

  /// Adds a link titled `title`, pointing to `href`, under `group`. Groups appear in the order they
  /// are first used, and links keep their insertion order.
  public SearchPalette item(String group, String title, String href) {
    return item(group, Item.of(title, href));
  }

  /// Adds `item` under `group`, for a link with a description or a badge: `item("Guides",
  /// SearchPalette.Item.of("Install", "/install").description("Set up"))`.
  public SearchPalette item(String group, Item item) {
    groupedItems.computeIfAbsent(group, _ -> new ArrayList<>()).add(item);
    return this;
  }

  /// Creates a button that opens the palette with `command="show-modal"`, showing the trigger label
  /// and an Alt K hint. In a static document the palette's script opens the first such palette on
  /// Alt+K in every browser; the button's `accesskey` of `k` remains for pages without the script,
  /// where the browser picks the chord. Returns a plain [TagElement].
  public TagElement triggerButton() {
    return button()
        .command(dialog, BuiltinCommand.SHOW_MODAL)
        .ariaKeyShortcuts("Alt+K")
        .attr("accesskey", "k")
        .flex()
        .itemsCenter()
        .justifyBetween()
        .gap(Tokens.SPACE_3)
        .px(Tokens.SPACE_3)
        .py(CssLength.rem(0.4))
        .rounded(Tokens.RADIUS_LG)
        .border(Border.hairline(Tokens.BORDER_SUBTLE))
        .bg(Tokens.SURFACE_CARD)
        .textColor(Tokens.TEXT_MUTED)
        .fontSize(Tokens.FONT_SIZE_SM)
        .cursor(Cursor.POINTER)
        .child(
            div()
                .flex()
                .itemsCenter()
                .gap(Tokens.SPACE_2)
                .child(Icons.search())
                .child(span().text(triggerLabel)))
        .child(
            div()
                .flex()
                .itemsCenter()
                .gap(CssLength.rem(0.2))
                .child(
                    kbd()
                        .px(CssLength.rem(0.35))
                        .py(CssLength.rem(0.1))
                        .rounded(Tokens.RADIUS_SM)
                        .bg(Tokens.SURFACE_SUBTLE)
                        .border(Border.hairline(Tokens.BORDER_SUBTLE))
                        .fontSize(Tokens.FONT_SIZE_XS)
                        .fontWeight(600)
                        .textColor(Tokens.TEXT_PRIMARY)
                        .text("Alt"))
                .child(
                    kbd()
                        .px(CssLength.rem(0.35))
                        .py(CssLength.rem(0.1))
                        .rounded(Tokens.RADIUS_SM)
                        .bg(Tokens.SURFACE_SUBTLE)
                        .border(Border.hairline(Tokens.BORDER_SUBTLE))
                        .fontSize(Tokens.FONT_SIZE_XS)
                        .fontWeight(600)
                        .textColor(Tokens.TEXT_PRIMARY)
                        .text("K")));
  }

  @Override
  public SearchPalette self() {
    return this;
  }

  @Override
  String scopeName() {
    return "search-palette";
  }

  @Override
  TagElement expand(ViewContext context) {
    return render(true).host();
  }

  private SearchParts render(boolean bundled) {
    var input = ElementRef.scoped("input");
    var empty = ElementRef.scoped("empty");
    var results = new ArrayList<SearchResult>();
    var headerEl =
        Elements.search()
            .attr("class", "palette-header")
            .child(Icons.search())
            .child(
                input("search")
                    .id(input)
                    .attr("class", "palette-input")
                    .attr("placeholder", placeholder)
                    .attr("autocomplete", "off")
                    .attr("autofocus", "")
                    .ariaLabel("Search query"))
            .child(
                button()
                    .attr("class", "palette-close")
                    .command(dialog, BuiltinCommand.CLOSE)
                    .ariaLabel("Esc, close search palette")
                    .child(kbd().attr("class", "palette-esc-kbd").text("Esc")));

    var resultsContainer =
        div()
            .attr("class", "palette-results")
            .role(Role.REGION)
            .ariaLabel("Search navigation links");

    for (var entry : groupedItems.entrySet()) {
      var groupBlock = div().attr("class", "palette-group");
      groupBlock.child(div().attr("class", "palette-group-title").text(entry.getKey()));

      for (var item : entry.getValue()) {
        var ref = ElementRef.scoped("result-" + (results.size() + 1));
        results.add(
            new SearchResult(
                ref,
                new SearchLink(
                    entry.getKey(), item.title, item.href, item.description, item.badge)));
        var link =
            a(item.href)
                .id(ref)
                .attr("class", "palette-item")
                .tabIndex(0)
                .child(
                    div()
                        .attr("class", "palette-item-main")
                        .child(span().attr("class", "palette-item-title").text(item.title))
                        .ifPresent(
                            item.description,
                            (main, description) ->
                                main.child(
                                    span().attr("class", "palette-item-desc").text(description))));

        var badge = item.badge;
        if (badge != null) {
          link.child(span().attr("class", "palette-item-badge").text(badge));
        } else {
          link.child(span().attr("class", "palette-item-arrow").ariaHidden(true).text("→"));
        }
        groupBlock.child(link);
      }
      resultsContainer.child(groupBlock);
    }

    resultsContainer.child(
        Elements.p()
            .id(empty)
            .attr("class", "palette-empty")
            .attr("hidden", "")
            .role(Role.STATUS)
            .text("No results found. Try another search."));

    var footerEl =
        footer()
            .attr("class", "palette-footer")
            .child(
                div()
                    .attr("class", "palette-hint")
                    .child(kbd().attr("class", "palette-hint-kbd").text("↑ ↓"))
                    .child(span().text("navigate")))
            .child(
                div()
                    .attr("class", "palette-hint")
                    .child(kbd().attr("class", "palette-hint-kbd").text("↵"))
                    .child(span().text("select")))
            .child(
                div()
                    .attr("class", "palette-hint")
                    .child(kbd().attr("class", "palette-hint-kbd").text("esc"))
                    .child(span().text("close")));

    var root =
        Elements.dialog()
            .id(dialog)
            .closedBy(ClosedBy.ANY)
            .ariaLabel("Quick open command palette")
            .child(headerEl)
            .child(resultsContainer)
            .child(footerEl);
    if (bundled) {
      // Portable expansion marks the host `is="tessera-search"` so static pages load its script.
      AuthoringAccess.tag(root).behavior = BundledBehavior.SEARCH;
    }
    return new SearchParts(root, input, empty, results);
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s ->
            s.m(CssLength.auto())
                .insetBlockStart(CssLength.rem(5))
                .maxInlineSize(CssLength.rem(42))
                .inlineSize(CssLength.pct(100).minus(CssLength.rem(2)))
                .p(CssLength.px(0))
                .rounded(Tokens.RADIUS_XL)
                .bg(Tokens.SURFACE_CARD)
                .textColor(Tokens.TEXT_PRIMARY)
                .border(Border.hairline(Tokens.BORDER_SUBTLE))
                .shadow(
                    Shadow.drop(
                        CssLength.px(25),
                        CssLength.px(50),
                        CssLength.px(-12),
                        CssColor.oklch(0, 0, 0, 0.35)))
                .overflow(Overflow.HIDDEN))
        .select(
            ":scope::backdrop",
            s -> s.bg(CssColor.oklch(0, 0, 0, 0.5)).backdropFilter(CssFilter.blur(CssLength.px(4))))
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(
                        ":scope",
                        s ->
                            s.opacity(0)
                                .transform(
                                    Transform.translateY(CssLength.px(-12))
                                        .then(Transform.scale(0.98)))
                                .transition(
                                    Transition.of(
                                            CssDuration.ms(200),
                                            Easing.cubicBezier(0.16, 1, 0.3, 1),
                                            Animatable.OPACITY,
                                            Animatable.TRANSFORM,
                                            Animatable.OVERLAY,
                                            Animatable.DISPLAY)
                                        .allowingDiscrete()))
                    .select(
                        ":scope:open",
                        s ->
                            s.opacity(1)
                                .transform(
                                    Transform.translateY(CssLength.zero()).then(Transform.scale(1)))
                                .startingStyle(start ->
                                    start
                                        .opacity(0)
                                        .transform(
                                            Transform.translateY(CssLength.px(-12))
                                                .then(Transform.scale(0.98))))))
        .select(
            ":scope .palette-header",
            s ->
                s.flex()
                    .itemsCenter()
                    .gap(Tokens.SPACE_3)
                    .px(Tokens.SPACE_4)
                    .py(Tokens.SPACE_3)
                    .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE))
                    .textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope .palette-input",
            s ->
                s.flexGrow(1)
                    .fontSize(Tokens.FONT_SIZE_MD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .bg(CssColor.transparent())
                    .border(Border.none())
                    .outline(Border.none()))
        .select(
            ":scope .palette-close",
            s ->
                s.bg(Tokens.SURFACE_SUBTLE)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .rounded(Tokens.RADIUS_MD)
                    .px(CssLength.rem(0.4))
                    .py(CssLength.rem(0.2))
                    .cursor(Cursor.POINTER))
        .select(
            ":scope .palette-esc-kbd",
            s -> s.fontSize(Tokens.FONT_SIZE_XS).fontWeight(600).textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope .palette-results",
            s -> s.maxBlockSize(CssLength.rem(24)).overflowY(Overflow.AUTO).p(Tokens.SPACE_3))
        .select(":scope .palette-group", s -> s.m(CssLength.px(0)).p(CssLength.px(0)))
        .select(":scope .palette-group:not(:has(.palette-item:not([hidden])))", s -> s.hidden())
        .select(
            ":scope .palette-group-title",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XS)
                    .fontWeight(600)
                    .textColor(Tokens.TEXT_MUTED)
                    .px(Tokens.SPACE_3)
                    .py(Tokens.SPACE_1_5))
        .select(
            ":scope .palette-item",
            s ->
                s.flex()
                    .itemsCenter()
                    .justifyBetween()
                    .px(Tokens.SPACE_3)
                    .py(Tokens.SPACE_2_5)
                    .rounded(Tokens.RADIUS_LG)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .textDecorationNone()
                    .cursor(Cursor.POINTER))
        .select(":scope .palette-item:hover", s -> s.bg(Tokens.SURFACE_SUBTLE))
        // The transparent outline is invisible until forced colors paint it, where the
        // background tint that marks focus is dropped.
        .select(
            ":scope .palette-item:focus-visible",
            s ->
                s.bg(Tokens.SURFACE_SUBTLE)
                    .outline(Border.solid(CssLength.px(2), CssColor.transparent()))
                    .outlineOffset(CssLength.px(-2)))
        .select(":scope .palette-item-main", s -> s.flex().flexCol().gap(CssLength.rem(0.15)))
        .select(":scope .palette-item-title", s -> s.fontSize(Tokens.FONT_SIZE_SM).fontWeight(500))
        .select(
            ":scope .palette-item-desc",
            s -> s.fontSize(Tokens.FONT_SIZE_XS).textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope .palette-item-badge",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XS)
                    .textColor(Tokens.TEXT_MUTED)
                    .bg(Tokens.SURFACE_SUBTLE)
                    .px(CssLength.rem(0.4))
                    .py(CssLength.rem(0.15))
                    .rounded(Tokens.RADIUS_SM))
        .select(
            ":scope .palette-item-arrow",
            s -> s.textColor(Tokens.TEXT_MUTED).fontSize(Tokens.FONT_SIZE_SM))
        .select(
            ":scope .palette-footer",
            s ->
                s.flex()
                    .itemsCenter()
                    .gap(Tokens.SPACE_4)
                    .px(Tokens.SPACE_4)
                    .py(Tokens.SPACE_2)
                    .borderBlockStart(Border.hairline(Tokens.BORDER_SUBTLE))
                    .bg(Tokens.SURFACE_SUBTLE)
                    .textColor(Tokens.TEXT_MUTED)
                    .fontSize(Tokens.FONT_SIZE_XS))
        .select(":scope .palette-hint", s -> s.flex().itemsCenter().gap(Tokens.SPACE_1))
        .select(
            ":scope .palette-hint-kbd",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XS)
                    .px(CssLength.rem(0.3))
                    .py(CssLength.rem(0.1))
                    .rounded(Tokens.RADIUS_SM)
                    .bg(Tokens.SURFACE_CARD)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE))
                    .textColor(Tokens.TEXT_PRIMARY))
        .select(":scope [hidden]", s -> s.hidden());
  }
}
