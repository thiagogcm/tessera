package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.input;
import static dev.nthings.tessera.core.Elements.kbd;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.DeferredElement;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// Native quick-open dialog with live filtering and keyboard selection.
/// Rendering a palette automatically contributes its ES module once per document.
public final class SearchPalette implements RenderView {

  private static final class ScriptContribution {
    private boolean added;
  }

  private static final DocumentKey<ScriptContribution> SCRIPT =
      DocumentKey.of("search-script", ScriptContribution.class, ScriptContribution::new);

  private static final class ClientScript {
    private static final String SOURCE = readScript();

    private static String readScript() {
      try (var input =
          Objects.requireNonNull(
              SearchPalette.class.getResourceAsStream("search-palette.js"),
              "Bundled search palette module")) {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private static final String SEARCH_ICON_SVG =
      "<svg width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\""
          + " stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\""
          + " aria-hidden=\"true\"><circle cx=\"11\" cy=\"11\" r=\"8\"/><line x1=\"21\" y1=\"21\""
          + " x2=\"16.65\" y2=\"16.65\"/></svg>";

  /// Search entry item representation.
  public record SearchItem(
      String group,
      String title,
      String href,
      @Nullable String description,
      @Nullable String badge) {}

  private final ElementId id;
  private final ElementRef dialog = ElementRef.named("dialog");
  private String placeholder = "Search documentation, routes, and sections...";
  private String triggerLabel = "Search docs...";
  private final Map<String, List<SearchItem>> groupedItems = new LinkedHashMap<>();

  private SearchPalette(ElementId id) {
    this.id = id;
  }

  public static SearchPalette create(String id) {
    return new SearchPalette(ElementId.of(id));
  }

  public static SearchPalette create(String id, String placeholder) {
    return new SearchPalette(ElementId.of(id)).placeholder(placeholder);
  }

  public SearchPalette placeholder(String placeholder) {
    this.placeholder = placeholder;
    return this;
  }

  public SearchPalette triggerLabel(String label) {
    this.triggerLabel = label;
    return this;
  }

  public SearchPalette item(String group, String title, String href) {
    return item(group, title, href, null, null);
  }

  public SearchPalette item(String group, String title, String href, @Nullable String description) {
    return item(group, title, href, description, null);
  }

  public SearchPalette item(
      String group,
      String title,
      String href,
      @Nullable String description,
      @Nullable String badge) {
    groupedItems
        .computeIfAbsent(group, _ -> new ArrayList<>())
        .add(new SearchItem(group, title, href, description, badge));
    return this;
  }

  public SearchPalette group(String groupName, List<SearchItem> items) {
    groupedItems.computeIfAbsent(groupName, _ -> new ArrayList<>()).addAll(items);
    return this;
  }

  /// Creates an accessible header trigger button wired via `command="show-modal"` to this search
  /// palette, complete with keyboard shortcut badge.
  public TagElement triggerButton(String buttonId, String label) {
    return button()
        .id(buttonId)
        .command(dialog, BuiltinCommand.SHOW_MODAL)
        .ariaLabel(label)
        .attr("aria-keyshortcuts", "Alt+K")
        .attr("accesskey", "k")
        .flex()
        .itemsCenter()
        .justifyBetween()
        .gap(Tokens.SPACE_3.length())
        .px(Tokens.SPACE_3.length())
        .py(CssLength.rem(0.4))
        .rounded(Tokens.RADIUS_LG.length())
        .borderSubtle(Tokens.BORDER_SUBTLE)
        .bg(Tokens.SURFACE_CARD)
        .textColor(Tokens.TEXT_MUTED)
        .fontSize(Tokens.FONT_SIZE_SM.length())
        .cursorPointer()
        .child(
            div()
                .flex()
                .itemsCenter()
                .gap(Tokens.SPACE_2.length())
                .child(rawHtml(SEARCH_ICON_SVG))
                .child(span().text(label)))
        .child(
            div()
                .flex()
                .itemsCenter()
                .gap(CssLength.rem(0.2))
                .child(
                    kbd()
                        .px(CssLength.rem(0.35))
                        .py(CssLength.rem(0.1))
                        .rounded(Tokens.RADIUS_SM.length())
                        .bg(Tokens.SURFACE_SUBTLE)
                        .borderSubtle(Tokens.BORDER_SUBTLE)
                        .fontSize(Tokens.FONT_SIZE_XS.length())
                        .fontWeight(600)
                        .textColor(Tokens.TEXT_PRIMARY)
                        .text("Alt"))
                .child(
                    kbd()
                        .px(CssLength.rem(0.35))
                        .py(CssLength.rem(0.1))
                        .rounded(Tokens.RADIUS_SM.length())
                        .bg(Tokens.SURFACE_SUBTLE)
                        .borderSubtle(Tokens.BORDER_SUBTLE)
                        .fontSize(Tokens.FONT_SIZE_XS.length())
                        .fontWeight(600)
                        .textColor(Tokens.TEXT_PRIMARY)
                        .text("K")));
  }

  public TagElement triggerButton() {
    return triggerButton(id.slug() + "-trigger", triggerLabel);
  }

  /// Wraps this search palette in a [DeferredElement] to hoist it to the end of body.
  public DeferredElement asDeferred(int priority) {
    return deferred(this, priority);
  }

  @Override
  public ElementId entityId() {
    return id;
  }

  @Override
  public Element render() {
    var context = RenderContext.current();
    var script = context.document(SCRIPT);
    if (!script.added) {
      script.added = true;
      context.contributeHead(
          Elements.el(HtmlTag.SCRIPT).attr("type", "module").child(rawHtml(ClientScript.SOURCE)));
    }

    var headerEl =
        Elements.search()
            .attr("class", "palette-header")
            .child(rawHtml(SEARCH_ICON_SVG))
            .child(
                input("search")
                    .id("input")
                    .attr("class", "palette-input")
                    .attr("placeholder", placeholder)
                    .attr("autocomplete", "off")
                    .attr("autofocus", "")
                    .ariaLabel("Search query"))
            .child(
                button()
                    .attr("class", "palette-close")
                    .command(dialog, BuiltinCommand.CLOSE)
                    .ariaLabel("Close search palette")
                    .child(kbd().attr("class", "palette-esc-kbd").text("Esc")));

    var resultsContainer =
        div().attr("class", "palette-results").role("region").ariaLabel("Search navigation links");

    for (var entry : groupedItems.entrySet()) {
      var groupBlock = div().attr("class", "palette-group");
      groupBlock.child(div().attr("class", "palette-group-title").text(entry.getKey()));

      for (var item : entry.getValue()) {
        var link =
            a(item.href())
                .attr("class", "palette-item")
                .tabIndex(0)
                .child(
                    div()
                        .attr("class", "palette-item-main")
                        .child(span().attr("class", "palette-item-title").text(item.title()))
                        .child(
                            item.description() != null
                                ? span().attr("class", "palette-item-desc").text(item.description())
                                : div().hidden()));

        if (item.badge() != null) {
          link.child(span().attr("class", "palette-item-badge").text(item.badge()));
        } else {
          link.child(span().attr("class", "palette-item-arrow").ariaHidden(true).text("→"));
        }
        groupBlock.child(link);
      }
      resultsContainer.child(groupBlock);
    }

    resultsContainer.child(
        Elements.p()
            .attr("class", "palette-empty")
            .attr("hidden", "")
            .attr("role", "status")
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

    return Elements.dialog()
        .id(dialog)
        .attr("is", "tessera-search")
        .dialogClosedByAny()
        .ariaLabel("Quick open command palette")
        .child(headerEl)
        .child(resultsContainer)
        .child(footerEl);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.m(CssLength.auto())
                    .insetBlockStart(CssLength.rem(5))
                    .maxInlineSize(CssLength.rem(42))
                    .inlineSize(CssLength.expr("calc(100% - 2rem)"))
                    .p(CssLength.px(0))
                    .rounded(Tokens.RADIUS_XL.length())
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .shadow("0 25px 50px -12px oklch(0% 0 0 / 0.35)")
                    .overflowHidden())
        .select(":scope::backdrop", s -> s.bgRaw("oklch(0% 0 0 / 0.5)").backdropFilter("blur(4px)"))
        .motionSafe(
            m ->
                m.select(
                        ":scope",
                        s ->
                            s.opacity(0)
                                .transform("translateY(-12px) scale(0.98)")
                                .transitionProperty("opacity, transform, overlay, display")
                                .transitionDuration("0.2s")
                                .transitionTimingFunction("cubic-bezier(0.16, 1, 0.3, 1)")
                                .transitionAllowDiscrete())
                    .select(":scope:open", s -> s.opacity(1).transform("translateY(0) scale(1)"))
                    .selectWithStartingStyle(
                        ":scope:open",
                        s -> s.opacity(1).transform("translateY(0) scale(1)"),
                        s -> s.opacity(0).transform("translateY(-12px) scale(0.98)")))
        .select(
            ":scope .palette-header",
            s ->
                s.flex()
                    .itemsCenter()
                    .gap(Tokens.SPACE_3.length())
                    .px(Tokens.SPACE_4.length())
                    .py(Tokens.SPACE_3.length())
                    .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
                    .textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope .palette-input",
            s ->
                s.flexGrow(1)
                    .fontSize(Tokens.FONT_SIZE_MD.length())
                    .textColor(Tokens.TEXT_PRIMARY)
                    .bgRaw("transparent")
                    .borderNone()
                    .outlineNone())
        .select(
            ":scope .palette-close",
            s ->
                s.bg(Tokens.SURFACE_SUBTLE)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .rounded(Tokens.RADIUS_MD.length())
                    .px(CssLength.rem(0.4))
                    .py(CssLength.rem(0.2))
                    .cursorPointer())
        .select(
            ":scope .palette-esc-kbd",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XS.length())
                    .fontWeight(600)
                    .textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope .palette-results",
            s -> s.maxBlockSize(CssLength.rem(24)).overflowYAuto().p(Tokens.SPACE_3.length()))
        .select(":scope .palette-group", s -> s.m(CssLength.px(0)).p(CssLength.px(0)))
        .select(":scope .palette-group:not(:has(.palette-item:not([hidden])))", s -> s.hidden())
        .select(
            ":scope .palette-group-title",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XS.length())
                    .fontWeight(600)
                    .textColor(Tokens.TEXT_MUTED)
                    .px(Tokens.SPACE_3.length())
                    .py(Tokens.SPACE_1_5.length()))
        .select(
            ":scope .palette-item",
            s ->
                s.flex()
                    .itemsCenter()
                    .justifyBetween()
                    .px(Tokens.SPACE_3.length())
                    .py(Tokens.SPACE_2_5.length())
                    .rounded(Tokens.RADIUS_LG.length())
                    .textColor(Tokens.TEXT_PRIMARY)
                    .textDecorationNone()
                    .cursorPointer())
        .select(
            ":scope .palette-item:hover, :scope .palette-item:focus-visible",
            s -> s.bg(Tokens.SURFACE_SUBTLE).outlineNone())
        .select(":scope .palette-item-main", s -> s.flex().flexCol().gap(CssLength.rem(0.15)))
        .select(
            ":scope .palette-item-title",
            s -> s.fontSize(Tokens.FONT_SIZE_SM.length()).fontWeight(500))
        .select(
            ":scope .palette-item-desc",
            s -> s.fontSize(Tokens.FONT_SIZE_XS.length()).textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope .palette-item-badge",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XS.length())
                    .textColor(Tokens.TEXT_MUTED)
                    .bg(Tokens.SURFACE_SUBTLE)
                    .px(CssLength.rem(0.4))
                    .py(CssLength.rem(0.15))
                    .rounded(Tokens.RADIUS_SM.length()))
        .select(
            ":scope .palette-item-arrow",
            s -> s.textColor(Tokens.TEXT_MUTED).fontSize(Tokens.FONT_SIZE_SM.length()))
        .select(
            ":scope .palette-footer",
            s ->
                s.flex()
                    .itemsCenter()
                    .gap(Tokens.SPACE_4.length())
                    .px(Tokens.SPACE_4.length())
                    .py(Tokens.SPACE_2.length())
                    .borderBlockStartSubtle(Tokens.BORDER_SUBTLE)
                    .bg(Tokens.SURFACE_SUBTLE)
                    .textColor(Tokens.TEXT_MUTED)
                    .fontSize(Tokens.FONT_SIZE_XS.length()))
        .select(":scope .palette-hint", s -> s.flex().itemsCenter().gap(Tokens.SPACE_1.length()))
        .select(
            ":scope .palette-hint-kbd",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XS.length())
                    .px(CssLength.rem(0.3))
                    .py(CssLength.rem(0.1))
                    .rounded(Tokens.RADIUS_SM.length())
                    .bg(Tokens.SURFACE_CARD)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .textColor(Tokens.TEXT_PRIMARY))
        .select(":scope [hidden]", s -> s.hidden());
  }
}
