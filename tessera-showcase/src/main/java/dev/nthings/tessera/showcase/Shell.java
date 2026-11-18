package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.components.TableGrid.body;
import static dev.nthings.tessera.components.TableGrid.columnHeader;
import static dev.nthings.tessera.components.TableGrid.data;
import static dev.nthings.tessera.components.TableGrid.head;
import static dev.nthings.tessera.components.TableGrid.row;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;

import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DescriptionList;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Link;
import dev.nthings.tessera.components.NavTabs;
import dev.nthings.tessera.components.ScrollProgressBar;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.StickyHeader;
import dev.nthings.tessera.components.TableGrid;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.WhiteSpace;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;

/// Chrome shared by every route: header, search, quick actions, inspector, heading, pager and
/// footer.
record Shell(Page page) implements DocumentComponent {

  static final CssLength MAX_WIDTH = CssLength.rem(76);
  static final CssLength PAD_X =
      CssLength.fluid(CssLength.rem(1), CssLength.cqi(3), CssLength.rem(2));

  @Override
  public DocumentContent expand(DocumentContext context) {
    var pages = ShowcaseSite.pages();
    var inspector =
        Drawer.of("Compiler & Cascade Inspector")
            .id("inspector")
            .description("What every page compiles to")
            .body(
                DescriptionList.of()
                    .term(
                        "Cascade",
                        "@layer reset, base, theme, components, utilities, interactive, container")
                    .term("Isolation", "@scope ([data-scope]) to (:scope [data-scope] > *)")
                    .term("Top layer", "<dialog>, [popover], @starting-style")
                    .term(
                        "JavaScript",
                        "Search includes its module; native controls use HTML and CSS"),
                DataTable.of(
                    "Compiler phases",
                    TableGrid.of(
                        head(row(columnHeader("Phase"), columnHeader("Responsibility"))),
                        body(
                            row(
                                data("1. Expand"),
                                data("Expand components, bind ElementRefs, intern styles")),
                            row(
                                data("2. Link"),
                                data("Resolve late content, fragments and id references")),
                            row(
                                data("3. Validate"),
                                data("Check accessibility, links, duplicates and invokers")),
                            row(
                                data("4. Emit"),
                                data("Write encoded HTML and one shared stylesheet"))))));

    var search =
        SearchPalette.of()
            .id("search")
            .placeholder("Search components and pages...")
            .triggerLabel("Search");
    var quickActions = Dropdown.of("Pages").id("quick-actions");
    for (var p : pages) {
      search.item("Pages", SearchPalette.Item.of(p.navLabel(), p.path()).description(p.summary()));
      quickActions.item(Dropdown.Item.link(p.path(), p.navLabel()));
      for (var demo : p.demos()) {
        search.item(p.navLabel(), demo.title(), p.sectionHref(demo));
      }
    }
    quickActions
        .separator()
        .item(
            Dropdown.Item.command(
                "open-inspector",
                inspector.dialogRef(),
                BuiltinCommand.SHOW_MODAL,
                "Open inspector"));

    var nav =
        NavTabs.of(
                "Primary navigation",
                pages.stream().map(p -> new Link(p.path(), p.navLabel())).toList())
            .id("primary-nav")
            .active(page.path());

    var header =
        StickyHeader.of(
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
                                        search.triggerButton(),
                                        quickActions,
                                        inspector.triggerButton("Inspector"))),
                        div().overflowX(Overflow.AUTO).child(nav)))
            .id("site-header");

    var index = pages.indexOf(page);
    var pager = DocPager.of();
    if (index > 0) {
      pager.previous(link(pages.get(index - 1)));
    }
    if (index < pages.size() - 1) {
      pager.next(link(pages.get(index + 1)));
    }

    var heading =
        section()
            .flex()
            .flexCol()
            .gap(CssLength.rem(0.5))
            .onlyIf(
                index > 0,
                s ->
                    s.child(
                        Breadcrumbs.of(
                            new Link("/", "Tessera"), new Link(page.path(), page.navLabel()))))
            .children(
                h1().fontSize(
                        CssLength.fluid(CssLength.rem(1.75), CssLength.cqi(4), CssLength.rem(2.6)))
                    .textWrap(TextWrap.BALANCE)
                    .text(page.title()),
                p().textColor(Tokens.TEXT_MUTED)
                    .fontSize(CssLength.rem(1.125))
                    .maxInlineSize(CssLength.ch(75))
                    .textWrap(TextWrap.PRETTY)
                    .text(page.summary()));

    var main =
        DocumentElement.of(
                Elements.main()
                    .maxInlineSize(MAX_WIDTH)
                    .mx(CssLength.auto())
                    .px(PAD_X)
                    .py(CssLength.rem(2))
                    .flex()
                    .flexCol()
                    .gap(CssLength.rem(2.5))
                    .child(heading))
            .child(page.content())
            .child(pager);
    return DocumentElement.of(div().overflowAnchorNone().children(ScrollProgressBar.of(), header))
        .child(main)
        .child(
            footer()
                .maxInlineSize(MAX_WIDTH)
                .mx(CssLength.auto())
                .px(PAD_X)
                .py(CssLength.rem(2))
                .borderBlockStart(Border.hairline(Tokens.BORDER_SUBTLE))
                .textColor(Tokens.TEXT_MUTED)
                .fontSize(CssLength.rem(0.875))
                .text(
                    "Compiled by Tessera on JDK 25 into static HTML and CSS for the latest Chromium"
                        + " and Firefox. Behaviors ship automatically; controls use native HTML and"
                        + " CSS."))
        // The dialogs sit outside the header's content model; their triggers stay in it.
        .child(Documents.deferred(inspector, 20))
        .child(Documents.deferred(search, 30));
  }

  private static Link link(Page target) {
    return new Link(target.path(), target.navLabel());
  }
}
