package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.css.Animation;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Keyframes;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.Resize;
import dev.nthings.tessera.css.TextWrap;
import dev.nthings.tessera.css.Tokens;

/// Scroll-driven animation, scroll-state and size container queries, and view transitions.
final class MotionPage {

  private static final Container RESIZABLE =
      Container.named("resizable", Container.Type.INLINE_SIZE);

  private MotionPage() {}

  static Page page() {
    return Page.ofDemos(
        "/motion",
        "Motion",
        "Motion, scroll & responsive",
        "Animations driven by scroll position instead of timers, layouts that respond to their "
            + "container instead of the viewport, and page-to-page transitions. All motion respects "
            + "prefers-reduced-motion.",
        scrollReveal(),
        revealOnce(),
        stagger(),
        containerQueries(),
        scrollState(),
        viewTransitions());
  }

  private static Demo scrollReveal() {
    return Demo.of("scroll-reveal", "Scroll reveal")
        .summary(
            "Each panel animates as it enters the viewport using a view() timeline — the "
                + "animation's progress is the element's scroll position. The reading progress bar "
                + "at the top of the page uses a scroll(root) timeline.")
        .features("animation-timeline: view()", "animation-range: entry", "scroll(root)")
        .example(
            Grid.autoFit(
                    CssLength.rem(12),
                    Demo.tile("Fade in").animation(Animation.reveal(Keyframes.FADE_IN)),
                    Demo.tile("Fade up").animation(Animation.reveal(Keyframes.FADE_UP)),
                    Demo.tile("Scale in").animation(Animation.reveal(Keyframes.SCALE_IN)),
                    Demo.tile("Slide in").animation(Animation.reveal(Keyframes.SLIDE_INLINE)))
                .gap(CssLength.rem(1)))
        .code(
            "div().animation(Animation.reveal(Keyframes.FADE_UP));",
            "ScrollProgressBar.of(); // Animation.scrollProgress(...)");
  }

  private static Demo revealOnce() {
    return Demo.of("reveal-once", "Reveal once")
        .summary(
            "Scroll-triggered animations: each panel plays a short time-based animation the first "
                + "time it becomes visible, and stays put when scrolled back — unlike the "
                + "scroll-scrubbed reveal above.")
        .features("timeline-trigger: view() cover", "animation-trigger: play-once")
        .example(
            Grid.autoFit(
                    CssLength.rem(12),
                    Demo.tile("Fade up").animation(Animation.revealOnce(Keyframes.FADE_UP)),
                    Demo.tile("Scale in").animation(Animation.revealOnce(Keyframes.SCALE_IN)),
                    Demo.tile("Slide in").animation(Animation.revealOnce(Keyframes.SLIDE_INLINE)))
                .gap(CssLength.rem(1)))
        .code("div().animation(Animation.revealOnce(Keyframes.SCALE_IN));");
  }

  private static Demo stagger() {
    var row = div().flex().flexWrap().gap(CssLength.rem(0.5));
    for (var i = 1; i <= 8; i++) {
      row.child(
          Demo.surface(span().px(CssLength.rem(0.75)).py(CssLength.rem(0.375)), CssLength.px(999))
              .animation(Animation.revealOnce(Keyframes.FADE_UP).staggerBySiblingIndex(60))
              .text("Item " + i));
    }
    return Demo.of("stagger", "Staggered entry")
        .summary("Siblings delay their animation by their own index — no per-item classes.")
        .features("sibling-index()", "animation-delay: calc()")
        .example(row)
        .code("chip.animation(Animation.revealOnce(Keyframes.FADE_UP).staggerBySiblingIndex(60));");
  }

  private static Demo containerQueries() {
    var card =
        Demo.surface(
                div().flex().flexCol().gap(CssLength.rem(0.5)).p(CssLength.rem(1)),
                CssLength.rem(0.75))
            .on(
                RESIZABLE.atLeast(CssLength.rem(28)),
                s -> s.flexRow().itemsCenter().justifyBetween())
            .children(
                span().fontWeight(700).text("Responsive to its container"),
                span().textColor(Tokens.TEXT_MUTED).text("Stacked when narrow, a row when wide."));
    return Demo.of("container-queries", "Container queries")
        .summary(
            "Drag the corner of the dashed box. The card switches layout at 28rem of container "
                + "width, regardless of the viewport.")
        .features("container-type: inline-size", "@container (inline-size >= 28rem)", "resize")
        .example(
            div()
                .container(RESIZABLE)
                .resize(Resize.HORIZONTAL)
                .overflow(Overflow.HIDDEN)
                .minInlineSize(CssLength.rem(14))
                .maxInlineSize(CssLength.pct(100))
                .inlineSize(CssLength.rem(22))
                .p(CssLength.rem(0.75))
                .border(Border.dashed(CssLength.px(2), Tokens.BORDER_SUBTLE))
                .rounded(CssLength.rem(0.75))
                .child(card))
        .code(
            "var resizable = Container.named(\"resizable\", Container.Type.INLINE_SIZE);",
            "div().container(resizable).child(",
            "    card.on(resizable.atLeast(rem(28)), s -> s.flexRow()));");
  }

  private static Demo scrollState() {
    return Demo.of("scroll-state", "Scroll-state queries")
        .summary(
            "Scroll down: the site header gains a translucent, blurred surface and a shadow once "
                + "it is stuck to the top. The carousel highlights the snapped slide the same way.")
        .features(
            "container-type: scroll-state", "scroll-state(stuck: top)", "scroll-state(snapped: x)")
        .example(
            p().textWrap(TextWrap.PRETTY)
                .text(
                    "No scroll listeners: the browser evaluates the query as the sticky element "
                        + "becomes stuck."))
        .code("new StickyHeader(\"site-header\", content); // queries scroll-state(stuck: top)");
  }

  private static Demo viewTransitions() {
    return Demo.of("view-transitions", "View transitions")
        .summary(
            "Navigating between pages cross-fades the document while the brand link and the docs "
                + "banner morph into place. Links are prerendered on hover through speculation "
                + "rules, so transitions start instantly.")
        .features(
            "@view-transition { navigation: auto }",
            "view-transition-class",
            "<script type=speculationrules>")
        .example(
            p().textWrap(TextWrap.PRETTY)
                .children(
                    text("Try it: open the "),
                    a("/").text("overview"),
                    text(", then click the Docs card.")))
        .code(
            "heroImage.element(img -> img.transitionKey(TransitionKey.of(\"docs-hero\", \"hero-morph\")));",
            "SiteDefinition.of(routes); // prerenders same-site links by default");
  }
}
