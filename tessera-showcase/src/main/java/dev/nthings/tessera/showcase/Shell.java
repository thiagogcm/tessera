package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;

import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.Breadcrumbs.Crumb;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Lists.DescriptionList;
import dev.nthings.tessera.components.Lists.Term;
import dev.nthings.tessera.components.Overlays;
import dev.nthings.tessera.components.ScrollProgressBar;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.SimpleTable;
import dev.nthings.tessera.components.StickyHeader;
import dev.nthings.tessera.components.Tabs;
import dev.nthings.tessera.components.Tabs.NavItem;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.WhiteSpace;

/// Site chrome shared by every route: sticky header with navigation, search palette, quick
/// actions and inspector drawer; page heading; previous/next pager; footer.
record Shell(Page page) implements RenderOnce {

  static final CssLength MAX_WIDTH = CssLength.rem(76);
  static final CssLength PAD_X =
      CssLength.fluid(CssLength.rem(1), CssLength.cqi(3), CssLength.rem(2));

  @Override
  public Element render(RenderContext context) {
    var pages = ShowcaseSite.pages();
    var inspector =
        Overlays.drawer("inspector", "Compiler & Cascade Inspector")
            .subtitle("What every page compiles to")
            .body(
                DescriptionList.of(
                    Term.of(
                        "Cascade",
                        "@layer reset, base, theme, components, utilities, interactive, container"),
                    Term.of("Isolation", "@scope ([data-view]) to (:scope [data-view] > *)"),
                    Term.of("Top layer", "<dialog>, [popover], @starting-style"),
                    Term.of(
                        "JavaScript",
                        "Search includes its module; native controls use HTML and CSS")),
                SimpleTable.create()
                    .caption("Compiler phases")
                    .headers("Phase", "Responsibility")
                    .row("1. requestLayout", "Expand views, bind ElementRefs, intern styles")
                    .row("2. prepaint", "Link id references, verify accessibility and links")
                    .row("3. paint", "Stream encoded HTML and one shared stylesheet"));

    var search = SearchPalette.create("search", "Search components and pages...");
    var quickActions = Dropdown.create("quick-actions", "Pages");
    for (var p : pages) {
      search.item("Pages", p.navLabel(), p.path(), p.summary());
      quickActions.linkItem(p.path(), p.navLabel(), null);
      for (var demo : p.demos()) {
        search.item(p.navLabel(), demo.title(), p.sectionHref(demo));
      }
    }
    quickActions
        .separator()
        .commandItem(
            "open-inspector", inspector.dialogRef(), BuiltinCommand.SHOW_MODAL, "Open inspector");

    var nav =
        Tabs.navTabs(
            "primary-nav",
            "Primary navigation",
            page.path(),
            pages.stream().map(p -> new NavItem(p.path(), p.navLabel())).toList());

    var header =
        new StickyHeader(
            "site-header",
            div()
                .maxInlineSize(MAX_WIDTH)
                .mx(CssLength.auto())
                .px(PAD_X)
                .py(CssLength.rem(0.5))
                .flex()
                .flexCol()
                .gap(CssLength.rem(0.25))
                .children(
                    div()
                        .flex()
                        .flexWrap()
                        .itemsCenter()
                        .justifyBetween()
                        .gap(CssLength.rem(0.75))
                        .children(
                            Elements.a("/")
                                .textColor(Tokens.TEXT_PRIMARY)
                                .textDecorationNone()
                                .fontWeight(700)
                                .fontSize(CssLength.rem(1.0625))
                                .whiteSpace(WhiteSpace.NOWRAP)
                                .transitionKey(TransitionKey.of("site-brand", "brand-morph"))
                                .text("Tessera"),
                            div()
                                .flex()
                                .itemsCenter()
                                .gap(CssLength.rem(0.5))
                                .children(
                                    search.triggerButton("search-btn", "Search"),
                                    quickActions,
                                    inspector.triggerButton("inspector-btn", "Inspector"),
                                    inspector.asDeferred(20),
                                    search.asDeferred(30))),
                    div().overflowXAuto().child(nav)));

    var index = pages.indexOf(page);
    var pager =
        DocPager.of(
            index > 0 ? pageRef(pages.get(index - 1)) : null,
            index < pages.size() - 1 ? pageRef(pages.get(index + 1)) : null);

    var heading =
        section()
            .flex()
            .flexCol()
            .gap(CssLength.rem(0.5))
            .when(
                index > 0,
                s ->
                    s.child(
                        Breadcrumbs.of(
                            "crumbs",
                            Crumb.of("/", "Tessera"),
                            Crumb.of(page.path(), page.navLabel()))))
            .children(
                h1().fontSize(
                        CssLength.fluid(CssLength.rem(1.75), CssLength.cqi(4), CssLength.rem(2.6)))
                    .textBalance()
                    .text(page.title()),
                p().textColor(Tokens.TEXT_MUTED)
                    .fontSize(CssLength.rem(1.125))
                    .maxInlineSize(CssLength.ch(75))
                    .textPretty()
                    .text(page.summary()));

    return div()
        .overflowAnchorNone()
        .children(
            new ScrollProgressBar("reading-progress"),
            header,
            Elements.main()
                .maxInlineSize(MAX_WIDTH)
                .mx(CssLength.auto())
                .px(PAD_X)
                .py(CssLength.rem(2))
                .flex()
                .flexCol()
                .gap(CssLength.rem(2.5))
                .children(heading, page.content(), pager),
            footer()
                .maxInlineSize(MAX_WIDTH)
                .mx(CssLength.auto())
                .px(PAD_X)
                .py(CssLength.rem(2))
                .borderBlockStartSubtle(Tokens.BORDER_SUBTLE)
                .textColor(Tokens.TEXT_MUTED)
                .fontSize(CssLength.rem(0.875))
                .text(
                    "Compiled by Tessera on JDK 25 into static HTML and CSS for the latest Chromium"
                        + " and Firefox. Behaviors ship automatically; controls use native HTML and"
                        + " CSS."));
  }

  private static DocPager.PageRef pageRef(Page target) {
    return new DocPager.PageRef(target.path(), target.navLabel());
  }
}
