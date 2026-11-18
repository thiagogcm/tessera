package dev.nthings.tessera.showcase.e2e;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.ul;

import dev.nthings.tessera.compiler.OutputMode;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.Carousel;
import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssNumber;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Small sites for situations the showcase pages do not cover: two search palettes on one page,
/// right-to-left content, fragment ids with unusual characters, and two densities of one theme.
/// Each is published at `/fixtures/<name>/`.
final class Fixtures {

  /// Names of the fragment targets on the `ids` fixture, as authored.
  static final List<String> UNUSUAL_IDS = List.of("C#", "100%", "café", "release.notes");

  private Fixtures() {}

  /// Every fixture site by name.
  static Map<String, SiteDefinition> all() {
    var sites = new LinkedHashMap<String, SiteDefinition>();
    sites.put("search", site("Search examples", searchPage()));
    sites.put("rtl", site("Right to left", rtlPage()));
    sites.put("ids", site("Fragment links", idsPage()));
    sites.put("density-regular", site(Theme.defaults(), "Regular density", densityPage()));
    sites.put(
        "density-compact",
        site(
            Theme.defaults().with(Tokens.DENSITY, CssNumber.of(0.5)),
            "Compact density",
            densityPage()));
    return sites;
  }

  private static SiteDefinition site(String title, DocumentElement body) {
    return site(Theme.defaults(), title, body);
  }

  private static SiteDefinition site(Theme theme, String title, DocumentElement body) {
    return SiteDefinition.builder()
        .outputMode(OutputMode.inline())
        .theme(theme)
        .route(new SiteRoute("/", new PageDocument(title, body)))
        .build();
  }

  /// Two independent palettes whose links point at headings on the same page.
  private static DocumentElement searchPage() {
    var articles =
        SearchPalette.of()
            .triggerLabel("Find articles")
            .item("Guides", "Café guide", "#cafe")
            .item("Guides", "Tea notes", "#tea");
    var guides =
        SearchPalette.of()
            .triggerLabel("Find guides")
            .item("Guides", "Layout guide", "#layout")
            .item("Guides", "Forms guide", "#forms");
    var content =
        main()
            .children(
                h1().text("Search examples"),
                articles.triggerButton(),
                guides.triggerButton(),
                tallSection(ElementRef.page("cafe"), "Café guide"),
                tallSection(ElementRef.page("tea"), "Tea notes"),
                tallSection(ElementRef.page("layout"), "Layout guide"),
                tallSection(ElementRef.page("forms"), "Forms guide"));
    return DocumentElement.of(content)
        .children(Documents.deferred(articles, 10), Documents.deferred(guides, 20));
  }

  /// Tabs, a carousel and a start drawer inside right-to-left content.
  private static DocumentElement rtlPage() {
    var drawer =
        Drawer.of("Filters")
            .id("filters")
            .side(Drawer.Side.INLINE_START)
            .body(p().text("Status, owner and region."));
    var carousel = Carousel.of("Highlights").id("highlights");
    for (var i = 1; i <= 5; i++) {
      carousel.slide("Slide " + i, "Highlight number " + i + ".");
    }
    var content =
        main()
            .attr("dir", "rtl")
            .children(
                h1().text("Right to left"),
                ContentTabs.of("Stages")
                    .id("stages")
                    .tab("first", "First", p().text("First stage."))
                    .tab("second", "Second", p().text("Second stage."))
                    .tab("third", "Third", p().text("Third stage.")),
                h2().text("Highlights"),
                carousel,
                Button.of("Open filters").command(drawer.dialogRef(), BuiltinCommand.SHOW_MODAL),
                drawer);
    return DocumentElement.of(content);
  }

  /// Links to headings whose ids hold `#`, `%`, non-ASCII letters and dots, a field whose control
  /// has a dotted id and a tooltip, and a link back to the top.
  private static DocumentElement idsPage() {
    var refs = UNUSUAL_IDS.stream().map(ElementRef::page).toList();
    var links = ul();
    for (var i = 0; i < refs.size(); i++) {
      links.child(li().child(a(refs.get(i)).text("Go to " + UNUSUAL_IDS.get(i))));
    }
    var content =
        main()
            .children(
                h1().text("Fragment links"),
                Field.of(
                        "Contact email",
                        Forms.email("contact").id(ElementRef.page("contact.email")))
                    .tooltip("A work address is best"),
                links);
    for (var i = 0; i < refs.size(); i++) {
      content.child(tallSection(refs.get(i), UNUSUAL_IDS.get(i) + " section"));
    }
    content.child(p().child(a("#top").text("Back to top")));
    return DocumentElement.of(content);
  }

  /// The same card and field, compiled once per density.
  private static DocumentElement densityPage() {
    return DocumentElement.of(
        main()
            .children(
                h1().text("Density"),
                Card.of("Release")
                    .headingLevel(2)
                    .bodyText("Ready to publish.")
                    .actions(Button.of("Publish")),
                Field.of("Release name", Forms.text("release"))));
  }

  private static TagElement tallSection(ElementRef ref, String title) {
    return section()
        .minBlockSize(CssLength.dvh(120))
        .children(h2().id(ref).text(title), div().child(p().text("Section " + title + ".")));
  }
}
