package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.Footnote;
import dev.nthings.tessera.components.Footnotes;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.StepList;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
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

  private static Element content(List<Demo> demos) {
    var hero =
        Image.of(Art.DOCS_BANNER, "Tessera documentation banner")
            .hero()
            .dimensions(1200, 480)
            .element(
                img ->
                    img.transitionKey(OverviewPage.DOCS_HERO)
                        .inlineSize(CssLength.pct(100))
                        .rounded(CssLength.rem(0.75))
                        .border(Border.hairline(Tokens.BORDER_SUBTLE)));
    var sections = new ArrayList<IntoElement>(demos);
    sections.add(Footnotes.of().title("Notes"));
    return div().flex().flexCol().gap(CssLength.rem(2)).children(hero, Page.docLayout(sections));
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
            "Image.of(src, \"Docs banner\").hero().dimensions(1200, 480)",
            "    .element(img -> img.transitionKey(TransitionKey.of(\"docs-hero\", \"hero-morph\")));");
  }

  private static Demo tableOfContents() {
    return Demo.of("table-of-contents", "Scroll-spy table of contents")
        .summary(
            "The 'On this page' rail lists every section registered on the page. It is resolved "
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
            "References are numbered in reading order and preview their note on hover. The notes "
                + "list links back to each reference and is placed where the page asks for it — "
                + "or appended automatically.")
        .features("interestfor", "role=\"doc-noteref\"", "role=\"doc-endnotes\"", ":target")
        .example(
            p().textWrap(TextWrap.PRETTY)
                .children(
                    text("Tessera targets the latest Chromium and Firefox"),
                    Footnote.of("latest Chromium and Firefox; no polyfills are shipped.")
                        .id("engine"),
                    text(" and provides script-free native controls"),
                    Footnote.of(
                            "Search includes its JavaScript module; speculation rules are"
                                + " declarative JSON.")
                        .id("zero-js"),
                    text(".")))
        .code(
            "p().children(text(\"Tessera targets the latest Chromium and Firefox\"),",
            "    Footnote.of(\"latest Chromium and Firefox.\"));",
            "Footnotes.of().title(\"Notes\"); // optional: placement of the list");
  }

  private static Demo steps() {
    return Demo.of("steps", "Steps")
        .summary("Numbered procedures whose markers deepen in tone as they progress.")
        .features("sibling-index()", "sibling-count()", "counter()")
        .example(
            StepList.of("Run just verify", "Run just site", "Upload target/site to any static host")
                .id("publish-steps"))
        .code("StepList.of(\"Run just verify\", \"Run just site\", \"Upload\");");
  }
}
