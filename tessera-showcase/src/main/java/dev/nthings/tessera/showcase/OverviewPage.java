package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.DescriptionList;
import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.SimpleTable;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Landing page: pitch, area cards, compiler pipeline and build-time guarantees.
final class OverviewPage {

  static final TransitionKey DOCS_HERO = TransitionKey.of("docs-hero", "hero-morph");

  private OverviewPage() {}

  static Page page() {
    return Page.custom(
        "/",
        "Overview",
        "Declarative HTML & CSS components for JDK 25",
        "Tessera compiles Java component trees into static HTML and one layered stylesheet. "
            + "Interactivity comes from the platform — dialogs, popovers, invoker commands, anchor "
            + "positioning, scroll-driven CSS — with automatically bundled search behavior.",
        () -> content(ShowcaseSite.pages()));
  }

  private static Element content(List<Page> pages) {
    var cards = new ArrayList<IntoElement>();
    for (var target : pages.subList(1, pages.size())) {
      var card =
          Card.of(target.navLabel())
              .id("area-" + target.navLabel().toLowerCase(java.util.Locale.ROOT))
              .headingLevel(2)
              .description(target.demos().size() + " demos")
              .bodyText(target.summary())
              .stretchedLink(target.path());
      if (target.path().equals(DocsPage.PATH)) {
        card.media(
            Image.of(Art.DOCS_BANNER, "Documentation page banner")
                .dimensions(600, 240)
                .element(img -> img.transitionKey(DOCS_HERO)));
      }
      cards.add(card);
    }

    var pipeline =
        ContentTabs.of("Compiler phases")
            .id("pipeline")
            .tab(
                "expand",
                "1. Expand",
                p().textWrap(TextWrap.PRETTY)
                    .text(
                        "Expands Component and Scope trees with an explicit DocumentContext,"
                            + " binds ElementRefs to scoped DOM ids, and interns every style"
                            + " into content-hashed utility classes."))
            .tab(
                "link",
                "2. Link",
                p().textWrap(TextWrap.PRETTY)
                    .text(
                        "Resolves late elements (tables of contents, endnotes), fragments and"
                            + " id references such as for, commandfor and aria-*."))
            .tab(
                "validate",
                "3. Validate",
                p().textWrap(TextWrap.PRETTY)
                    .text(
                        "Rejects inaccessible markup, dangling invokers, duplicate ids and"
                            + " broken #fragment links before any HTML is written."))
            .tab(
                "emit",
                "4. Emit",
                p().textWrap(TextWrap.PRETTY)
                    .text(
                        "Writes OWASP-encoded HTML and one deduplicated stylesheet ordered by"
                            + " cascade layer, shared by every page of the site."));

    var guarantees =
        DescriptionList.of()
            .term("Accessibility", "Labelled buttons and links, alt text, CLS-safe images")
            .term("Id integrity", "Every commandfor, for and aria-* reference resolves")
            .term("Links", "Same-page and cross-route #fragments exist")
            .term("Reproducible", "Deterministic ids and class names; byte-identical builds");

    var features =
        SimpleTable.of()
            .caption("Platform features behind the components")
            .headers("Feature", "Used by")
            .row("@scope, @layer, @container", "Every component")
            .row("<dialog closedby>, invoker commands", "Modal, Drawer, Search palette")
            .row("popover, interestfor, anchor positioning", "Dropdown, tooltips, form hints")
            .row("::scroll-marker, ::scroll-button()", "Carousel")
            .row("scroll-target-group, :target-current", "Table of contents")
            .row("appearance: base-select, field-sizing", "Forms")
            .row("Cross-document view transitions, speculation rules", "Site navigation");

    return div()
        .flex()
        .flexCol()
        .gap(CssLength.rem(2.5))
        .children(
            Grid.autoFit(CssLength.rem(18)).gap(CssLength.rem(1.25)).children(cards),
            block(
                "How it compiles",
                CodeBlock.ofLines(
                        "var page = new PageDocument(\"Hello\",",
                        "    main().p(rem(2)).children(",
                        "        h1().textWrap(TextWrap.BALANCE).text(\"Hello\"),",
                        "        Callout.of(Callout.Kind.TIP, \"Rendered without JavaScript\")));",
                        "",
                        "CompiledPage out = Compiler.compile(page); // html + css")
                    .filename("Hello.java")
                    .language("java"),
                pipeline),
            block("Checked at build time", guarantees),
            block("Platform features", features));
  }

  private static Element block(String title, IntoElement... content) {
    return section()
        .flex()
        .flexCol()
        .gap(CssLength.rem(1))
        .child(h2().fontSize(CssLength.rem(1.375)).textColor(Tokens.TEXT_PRIMARY).text(title))
        .children(content);
  }
}
