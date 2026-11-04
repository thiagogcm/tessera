package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.Badge;
import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.CardGrid;
import dev.nthings.tessera.components.ContentSection.SurfaceKind;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.Lists.BulletList;
import dev.nthings.tessera.components.Lists.DescriptionList;
import dev.nthings.tessera.components.Lists.NumberedList;
import dev.nthings.tessera.components.Lists.StepList;
import dev.nthings.tessera.components.Lists.Term;
import dev.nthings.tessera.components.Pill;
import dev.nthings.tessera.components.Prose;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.Tokens;
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
                CssLength.rem(0.75),
                Demo.tile("One"),
                Demo.tile("Two"),
                Demo.tile("Three"),
                Demo.tile("Four")),
            Grid.columns(
                3,
                CssLength.rem(0.75),
                Grid.cell(2, 1, Demo.tile("Span 2 columns")),
                Grid.cell(1, 2, Demo.tile("Span 2 rows")),
                Demo.tile("Cell"),
                Demo.tile("Cell")))
        .code(
            "Grid.autoFit(rem(10), rem(0.75), a, b, c, d);",
            "Grid.columns(3, rem(0.75), Grid.cell(2, 1, wide), Grid.cell(1, 2, tall), c, d);");
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
                "card-variants",
                CssLength.rem(15),
                CssLength.rem(1),
                Card.create("linked")
                    .title("Stretched link")
                    .subtitle("Whole card is a link")
                    .bodyText("Click anywhere on this card to open the Data page.")
                    .stretchedLink("/data"),
                Card.create("plan-team")
                    .title("Team plan")
                    .subtitle("Choice card (radio)")
                    .bodyText("Selection is styled with :has(> input:checked).")
                    .choiceRadio("plan", "team", true),
                Card.create("plan-org")
                    .title("Organization plan")
                    .subtitle("Choice card (radio)")
                    .bodyText("Only one plan can be selected.")
                    .choiceRadio("plan", "org", false)),
            Card.create("media-card")
                .media(Image.of(Art.MEDIA_BANNER, "Media card banner").dimensions(600, 240))
                .title("Media card")
                .subtitle("Container-adaptive")
                .bodyText(
                    "This card switches to media-beside-content once its container is wide enough."))
        .code(
            "Card.create(\"linked\").title(\"Stretched link\").stretchedLink(\"/data\");",
            "Card.create(\"plan-team\").title(\"Team plan\").choiceRadio(\"plan\", \"team\", true);",
            "Card.create(\"media-card\").media(Image.of(src, alt).dimensions(600, 240));");
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
                CssLength.rem(1.5),
                BulletList.ofStrings("Zero JavaScript", "Scoped styles", "Shared stylesheet"),
                NumberedList.ofStrings("Describe", "Compile", "Deploy"),
                DescriptionList.of(
                    Term.of("Runtime", "Chrome 155+"), Term.of("Language", "Java 25"))),
            StepList.ofStrings(
                "release-steps", "Write components", "Compile the site", "Serve static files"))
        .code(
            "BulletList.ofStrings(\"Zero JavaScript\", \"Scoped styles\");",
            "StepList.ofStrings(\"release-steps\", \"Write\", \"Compile\", \"Serve\");");
  }

  private static Demo callouts() {
    return Demo.of("callouts", "Callouts")
        .summary(
            "Five admonition tones, each an <aside role=\"note\"> tinted from its status token.")
        .features("color-mix(in oklab, …)", "light-dark()", "<aside role=note>")
        .surface(SurfaceKind.FLAT)
        .example(
            Callout.note("Notes add context the reader may want."),
            Callout.tip("Tips suggest a better way to do something."),
            Callout.important("Important callouts flag information needed to succeed."),
            Callout.warning("Warnings point out risks."),
            Callout.caution("Cautions mark actions with negative consequences."))
        .code("Callout.tip(\"Tips suggest a better way to do something.\");");
  }

  private static Demo badges() {
    return Demo.of("badges", "Badges & pills")
        .summary(
            "Compact semantic labels and pills with adaptive OKLCH tints, status dots, and "
                + "outline/solid variants.")
        .features("OKLCH color-mix", "border-radius: 9999px", "status dots")
        .surface(SurfaceKind.FLAT)
        .example(
            div()
                .flex()
                .flexWrap()
                .itemsCenter()
                .gap(CssLength.rem(0.5))
                .children(
                    Badge.neutral("Neutral"),
                    Badge.accent("Accent"),
                    Badge.success("Success"),
                    Badge.warning("Warning"),
                    Badge.danger("Danger"),
                    Badge.info("Info")),
            div()
                .flex()
                .flexWrap()
                .itemsCenter()
                .gap(CssLength.rem(0.5))
                .children(
                    Pill.accent("Pill").outline(),
                    Pill.success("Live").dot(),
                    Pill.warning("Beta"),
                    Pill.danger("Critical").solid(),
                    Pill.info("v0.1.0"),
                    Badge.accent("Interactive link →").pill().href("/docs")))
        .code(
            "Badge.accent(\"Accent\");",
            "Pill.success(\"Live\").dot();",
            "Pill.warning(\"Beta\").outline();",
            "Badge.accent(\"Link\").pill().href(\"/docs\");");
  }

  private static Demo formatting() {
    return Demo.of("formatting", "Inline formatting")
        .summary("Semantic inline elements styled with modern text decoration.")
        .features(
            "text-decoration-style: wavy", "text-underline-offset", "<mark> <kbd> <ins> <del>")
        .example(
            p().textPretty()
                .children(
                    text("Text can be "),
                    Formatting.highlight("highlighted", HighlightTone.ACCENT),
                    text(", "),
                    Formatting.underline(
                        "underlined with a wavy line", UnderlineStyle.WAVY, Tokens.STATUS_WARNING),
                    text(", "),
                    Formatting.strikethrough("struck through"),
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
            "Formatting.highlight(\"highlighted\", HighlightTone.ACCENT);",
            "Formatting.underline(\"wavy\", UnderlineStyle.WAVY, Tokens.STATUS_WARNING);",
            "Formatting.keybinding(\"Ctrl+K\");");
  }
}
