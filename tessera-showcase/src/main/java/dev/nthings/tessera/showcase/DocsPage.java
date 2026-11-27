package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.StepList;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.components.Footnote;
import dev.nthings.tessera.document.components.Footnotes;
import dev.nthings.tessera.document.components.HeroImage;
import java.util.ArrayList;
import java.util.List;

/// Documentation primitives: hero image, scroll-spy table of contents, footnotes, and steps.
final class DocsPage {

  static final String PATH = "/docs";

  private DocsPage() {}

  static Page page() {
    var demos = List.of(heroImage(), tableOfContents(), footnotes(), steps());
    return new Page(
        PATH,
        "Docs",
        "Documentation primitives",
        "Everything a documentation page needs: an LCP-optimized hero, a table of contents that "
            + "tracks the visible section, numbered footnotes with hover previews, and steps.",
        demos,
        () -> content(demos));
  }

  private static DocumentContent content(List<Demo> demos) {
    var hero =
        HeroImage.of(
            Image.of(Art.DOCS_BANNER, "Tessera documentation banner")
                .dimensions(1200, 480)
                .element(img ->
                    img.transitionKey(OverviewPage.DOCS_HERO)
                        .inlineSize(CssLength.pct(100))
                        .rounded(CssLength.rem(0.75))
                        .border(Border.hairline(Tokens.BORDER_SUBTLE))));
    var sections = new ArrayList<DocumentContent>(demos);
    sections.add(Footnotes.of().title("Notes"));
    return DocumentElement.of(div().flex().flexCol().gap(CssLength.rem(2)))
        .children(hero, Page.docLayout(sections));
  }

  private static Demo heroImage() {
    return Demo.of("hero-image", "Hero image")
        .summary(
            "The banner above is the page's Largest Contentful Paint candidate: it gets "
                + "fetchpriority=\"high\", a <link rel=\"preload\"> in <head>, and it morphs from "
                + "the Docs card on the overview page through a cross-document view transition.")
        .features("fetchpriority", "<link rel=preload>", "view-transition-name", "@view-transition")
        .example(
            p().textWrap(TextWrap.PRETTY)
                .text(
                    "Images must declare dimensions or an aspect-ratio — the compiler rejects "
                        + "images that would cause layout shift. One hero per page is enforced."))
        .code(
            "HeroImage.of(Image.of(src, \"Docs banner\").dimensions(1200, 480)",
            "    .element(img -> img.transitionKey(TransitionKey.of(\"docs-hero\", \"hero-morph\"))));");
  }

  private static Demo tableOfContents() {
    return Demo.of("table-of-contents", "Scroll-spy table of contents")
        .summary(
            "The 'On this page' rail lists every heading collected into the page outline. It is resolved "
                + "after layout, so it may appear anywhere, and it highlights the section in view "
                + "with :target-current.")
        .features("scroll-target-group: auto", ":target-current", "position: sticky", "<details>")
        .example(
            p().textWrap(TextWrap.PRETTY)
                .text(
                    "Scroll this page and watch the highlighted entry follow along. The rail "
                        + "collapses like any <details> element."))
        .code(
            "DocLayout.of(",
            "    Section.of(\"intro\", \"Introduction\").child(p().text(\"…\")), …);");
  }

  private static Demo footnotes() {
    return Demo.of("footnotes", "Footnotes")
        .summary(
            "Notes are numbered by their first reference and previewed on hover. Repeated references "
                + "share one endnote with separate backlinks. The list is placed where the page asks for it — "
                + "or appended automatically.")
        .features("interestfor", "role=\"doc-noteref\"", "role=\"doc-endnotes\"", ":target")
        .example(footnoteExample())
        .code(
            "var engine = Footnote.of(\"latest Chromium and Firefox.\").id(\"engine\");",
            "DocumentElement.of(p().text(\"Tessera targets the latest Chromium and Firefox\"))",
            "    .child(engine.reference()).text(\". Examples follow it too\")",
            "    .child(engine.reference());",
            "Footnotes.of().title(\"Notes\"); // optional: placement of the list");
  }

  private static DocumentContent footnoteExample() {
    var engine = Footnote.of("latest Chromium and Firefox; no polyfills are shipped.").id("engine");
    var zeroJs =
        Footnote.of(
                "Search includes its JavaScript module; speculation rules are declarative JSON.")
            .id("zero-js");
    return DocumentElement.of(p().textWrap(TextWrap.PRETTY))
        .text("Tessera targets the latest Chromium and Firefox")
        .child(engine.reference())
        .text(" and provides script-free native controls")
        .child(zeroJs.reference())
        .text(". The same browser requirements apply to the embedded examples")
        .child(engine.reference())
        .text(".");
  }

  private static Demo steps() {
    return Demo.of("steps", "Steps")
        .summary("Numbered procedures whose markers deepen in tone as they progress.")
        .features("sibling-index()", "sibling-count()", "counter()")
        .example(
            StepList.of("Run just verify", "Run just site", "Upload target/site to any static host")
                .scopeId("publish-steps"))
        .code("StepList.of(\"Run just verify\", \"Run just site\", \"Upload\");");
  }
}
