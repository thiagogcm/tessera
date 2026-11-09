package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.Badge;
import dev.nthings.tessera.components.BulletList;
import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.CardGrid;
import dev.nthings.tessera.components.DescriptionList;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.NumberedList;
import dev.nthings.tessera.components.Prose;
import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.components.StepList;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.UnderlineStyle;

/// Grids, cards, prose, lists, callouts, and inline formatting.
final class LayoutPage {

  private LayoutPage() {}

  static Page page() {
    return Page.ofDemos(
        "/layout",
        "Layout",
        "Layout & content",
        "Responsive grids and cards that adapt to their container, editorial prose, semantic lists, "
            + "callouts, badges, and inline formatting.",
        grid(),
        cards(),
        prose(),
        lists(),
        callouts(),
        badges(),
        formatting());
  }

  private static Demo grid() {
    return Demo.of("grid", "Grid")
        .summary(
            "Auto-fit grids reflow without media queries; fixed grids place cells with column and "
                + "row spans.")
        .features(
            "grid-template-columns: repeat(auto-fit, minmax(min(100%, …), 1fr))",
            "grid-column: span")
        .example(
            Grid.autoFit(
                    CssLength.rem(10),
                    Demo.tile("One"),
                    Demo.tile("Two"),
                    Demo.tile("Three"),
                    Demo.tile("Four"))
                .gap(CssLength.rem(0.75)),
            Grid.columns(
                    3,
                    Grid.cell(2, 1, Demo.tile("Span 2 columns")),
                    Grid.cell(1, 2, Demo.tile("Span 2 rows")),
                    Demo.tile("Cell"),
                    Demo.tile("Cell"))
                .gap(CssLength.rem(0.75)))
        .code(
            "Grid.autoFit(rem(10), a, b, c, d).gap(rem(0.75));",
            "Grid.columns(3, Grid.cell(2, 1, wide), Grid.cell(1, 2, tall), c, d).gap(rem(0.75));");
  }

  private static Demo cards() {
    return Demo.of("cards", "Cards")
        .summary(
            "Four-region cards. A stretched link makes the whole card clickable while secondary "
                + "controls stay usable; choice cards wrap a radio or checkbox and restyle on "
                + ":has(:checked). Cards with media switch to a side-by-side layout in wide "
                + "containers.")
        .features("@container", ":has()", "subgrid", "stretched link ::after")
        .example(
            CardGrid.of(
                    CssLength.rem(15),
                    Card.of("Stretched link")
                        .description("Whole card is a link")
                        .bodyText("Click anywhere on this card to open the Data page.")
                        .stretchedLink("/data"),
                    Card.of("Team plan")
                        .description("Choice card (radio)")
                        .bodyText("Selection is styled with :has(> input:checked).")
                        .radio("plan", "team")
                        .checked(),
                    Card.of("Organization plan")
                        .description("Choice card (radio)")
                        .bodyText("Only one plan can be selected.")
                        .radio("plan", "org"))
                .id("card-variants")
                .gap(CssLength.rem(1)),
            Card.of("Media card")
                .media(Image.of(Art.MEDIA_BANNER, "Media card banner").dimensions(600, 240))
                .description("Container-adaptive")
                .bodyText(
                    "This card switches to media-beside-content once its container is wide enough."))
        .code(
            "Card.of(\"Stretched link\").stretchedLink(\"/data\");",
            "Card.of(\"Team plan\").radio(\"plan\", \"team\").checked();",
            "Card.of(\"Media card\").media(Image.of(src, alt).dimensions(600, 240));");
  }

  private static Demo prose() {
    return Demo.of("prose", "Prose")
        .summary(
            "A measured reading column with balanced headings and pretty-wrapped paragraphs that "
                + "avoid orphans.")
        .features("text-wrap: balance", "text-wrap: pretty", "ch-based measure")
        .example(
            Prose.of(
                h3().text("Static output, dynamic platform"),
                p().text(
                        "Every interaction on this site — opening dialogs, toggling popovers, "
                            + "tracking the visible section, sliding carousels — is declared in "
                            + "HTML attributes and CSS. The browser does the rest."),
                p().text(
                        "Because nothing hydrates, pages are interactive as soon as they "
                            + "render, and the markup is exactly what the compiler validated.")))
        .code("Prose.of(h3().text(\"…\"), p().text(\"…\"), p().text(\"…\"));");
  }

  private static Demo lists() {
    return Demo.of("lists", "Lists")
        .summary(
            "Bullet, numbered and description lists, plus step lists whose markers are tinted by "
                + "their position using sibling-index() and sibling-count().")
        .features("sibling-index()", "sibling-count()", "counter()", "color-mix()")
        .example(
            Grid.autoFit(
                    CssLength.rem(14),
                    BulletList.of("Zero JavaScript", "Scoped styles", "Shared stylesheet"),
                    NumberedList.of("Describe", "Compile", "Deploy"),
                    DescriptionList.of()
                        .term("Runtime", "Latest Chromium + Firefox")
                        .term("Language", "Java 25"))
                .gap(CssLength.rem(1.5)),
            StepList.of("Write components", "Compile the site", "Serve static files")
                .id("release-steps"))
        .code(
            "BulletList.of(\"Zero JavaScript\", \"Scoped styles\");",
            "StepList.of(\"Write\", \"Compile\", \"Serve\");");
  }

  private static Demo callouts() {
    return Demo.of("callouts", "Callouts")
        .summary(
            "Five admonition tones, each an <aside role=\"note\"> tinted from its status token.")
        .features("color-mix(in oklab, …)", "light-dark()", "<aside role=note>")
        .surface(Section.Surface.FLAT)
        .example(
            Callout.of(Callout.Kind.NOTE, "Notes add context the reader may want."),
            Callout.of(Callout.Kind.TIP, "Tips suggest a better way to do something."),
            Callout.of(
                Callout.Kind.IMPORTANT, "Important callouts flag information needed to succeed."),
            Callout.of(Callout.Kind.WARNING, "Warnings point out risks."),
            Callout.of(Callout.Kind.CAUTION, "Cautions mark actions with negative consequences."))
        .code("Callout.of(Callout.Kind.TIP, \"Tips suggest a better way to do something.\");");
  }

  private static Demo badges() {
    return Demo.of("badges", "Badges & pills")
        .summary(
            "Compact semantic labels and pills with adaptive OKLCH tints, status dots, and "
                + "outline/solid variants.")
        .features("OKLCH color-mix", "border-radius: 9999px", "status dots")
        .surface(Section.Surface.FLAT)
        .example(
            badgeRow(
                Badge.of("Neutral"),
                Badge.of("Accent").tone(Tone.ACCENT),
                Badge.of("Success").tone(Tone.SUCCESS),
                Badge.of("Warning").tone(Tone.WARNING),
                Badge.of("Danger").tone(Tone.DANGER),
                Badge.of("Info").tone(Tone.INFO)),
            badgeRow(
                Badge.of("Pill").tone(Tone.ACCENT).variant(Badge.Variant.OUTLINE).pill(),
                Badge.of("Live").tone(Tone.SUCCESS).pill().dot(),
                Badge.of("Beta").tone(Tone.WARNING).pill(),
                Badge.of("Critical").tone(Tone.DANGER).variant(Badge.Variant.SOLID).pill(),
                Badge.of("v0.1.0").tone(Tone.INFO).pill(),
                Badge.of("Interactive link →").tone(Tone.ACCENT).pill().href("/docs")))
        .code(
            "Badge.of(\"Accent\").tone(Tone.ACCENT);",
            "Badge.of(\"Live\").tone(Tone.SUCCESS).pill().dot();",
            "Badge.of(\"Beta\").tone(Tone.WARNING).variant(Badge.Variant.OUTLINE).pill();",
            "Badge.of(\"Link\").tone(Tone.ACCENT).pill().href(\"/docs\");");
  }

  private static TagElement badgeRow(IntoElement... badges) {
    return div().flex().flexWrap().itemsCenter().gap(CssLength.rem(0.5)).children(badges);
  }

  private static Demo formatting() {
    return Demo.of("formatting", "Inline formatting")
        .summary("Semantic inline elements styled with modern text decoration.")
        .features(
            "text-decoration-style: wavy", "text-underline-offset", "<mark> <kbd> <ins> <del>")
        .example(
            p().textWrap(TextWrap.PRETTY)
                .children(
                    text("Text can be "),
                    Formatting.highlight("highlighted", Tone.ACCENT),
                    text(", "),
                    Formatting.underline(
                        "underlined with a wavy line", UnderlineStyle.WAVY, Tokens.STATUS_WARNING),
                    text(", "),
                    Formatting.strikethrough("struck through", Tokens.TEXT_MUTED),
                    text(", shown as a diff ("),
                    Formatting.deleted("old()"),
                    text(" → "),
                    Formatting.inserted("new()"),
                    text("), set as "),
                    Formatting.inlineCode("inline code"),
                    text(", or a shortcut like "),
                    Formatting.keybinding("Ctrl+K"),
                    text(".")))
        .code(
            "Formatting.highlight(\"highlighted\", Tone.ACCENT);",
            "Formatting.underline(\"wavy\", UnderlineStyle.WAVY, Tokens.STATUS_WARNING);",
            "Formatting.keybinding(\"Ctrl+K\");");
  }
}
