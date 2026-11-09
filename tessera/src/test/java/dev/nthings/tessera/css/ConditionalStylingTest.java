package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalStateException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.support.CssBlocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("conditional styling")
class ConditionalStylingTest {

  private static final String MOTION_OK = "@media (prefers-reduced-motion: no-preference){";

  @Test
  @DisplayName("states nest as & rules in State order, whatever the call order")
  void statesFollowEnumOrder() {
    var style =
        new StyleRefinement()
            .opacity(1)
            .on(State.ACTIVE, s -> s.opacity(0.8))
            .on(State.HOVER, s -> s.opacity(0.9))
            .on(State.HOVER, s -> s.cursor(Cursor.POINTER));

    assertThat(style.toCssBody())
        .isEqualTo(
            "opacity:1;&:hover:not(:where(:disabled)){opacity:0.9;cursor:pointer}"
                + "&:active:not(:where(:disabled)){opacity:0.8}");
  }

  @Test
  @DisplayName("conditions nest, and media rules follow states")
  void conditionsNest() {
    var style =
        new StyleRefinement()
            .on(Media.MOTION_SAFE, m -> m.on(State.FOCUS_VISIBLE, s -> s.opacity(0.5)))
            .on(State.DISABLED, s -> s.opacity(0.4));

    assertThat(style.toCssBody())
        .isEqualTo(
            "&:disabled{opacity:0.4}"
                + "@media (prefers-reduced-motion: no-preference){&:focus-visible{opacity:0.5}}");
  }

  @Test
  @DisplayName(
      "an element's states go to the interactive layer and container queries to the"
          + " container layer")
  void elementRulesSplitByLayer() {
    var cards = Container.named("cards", Container.Type.INLINE_SIZE);
    var page =
        renderMain(
            div()
                .container(cards)
                .child(
                    section()
                        .p(CssLength.rem(1))
                        .on(State.HOVER, s -> s.opacity(0.9))
                        .on(cards.atLeast(CssLength.rem(30)), s -> s.flexRow())
                        .text("card")));

    var section = page.element("section");
    assertThat(page.ruleOf(section, "u-")).isEqualTo("padding-inline:1rem;padding-block:1rem");
    assertThat(page.ruleOf(section, "i-")).isEqualTo("&:hover:not(:where(:disabled)){opacity:0.9}");
    var cqClass = CssBlocks.generatedClass(section.className(), "cq-");
    assertThat(page.css())
        .contains("@layer container{@container cards (inline-size >= 30rem){." + cqClass + "{")
        .contains("container-name:cards;container-type:inline-size");
  }

  @Test
  @DisplayName("containers combine types and validate them")
  void containerTypes() {
    var style =
        new StyleRefinement()
            .container(Container.unnamed(Container.Type.SCROLL_STATE, Container.Type.INLINE_SIZE));

    assertThat(style.toCssBlockBody()).isEqualTo("container-type:inline-size scroll-state");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Container.unnamed(Container.Type.SIZE, Container.Type.INLINE_SIZE));
    assertThat(
            StyleRefinement.prelude(
                Container.named("bar", Container.Type.SCROLL_STATE).stuck(Container.Edge.TOP)))
        .isEqualTo("@container bar scroll-state(stuck: top)");
    assertThat(
            StyleRefinement.prelude(
                Container.unnamed(Container.Type.INLINE_SIZE).below(CssLength.rem(36))))
        .isEqualTo("@container (inline-size < 36rem)");
  }

  @Test
  @DisplayName("scope rules nest conditions and starting styles, and queries group rules")
  void scopeRules() {
    var wide = Container.unnamed(Container.Type.INLINE_SIZE).atLeast(CssLength.rem(48));
    var scope =
        new ScopeStyle()
            .select(
                ":scope a",
                s ->
                    s.opacity(1)
                        .on(State.HOVER, h -> h.opacity(0.8))
                        .startingStyle(start -> start.opacity(0)))
            .on(wide, c -> c.root(s -> s.flexRow()))
            .on(Media.MOTION_SAFE, m -> m.root(s -> s.opacity(1)));

    assertThat(scope.toCssBody())
        .isEqualTo(
            ":scope a{opacity:1;&:hover:not(:where(:disabled)){opacity:0.8}@starting-style{opacity:0}}"
                + "@container (inline-size >= 48rem){:scope{flex-direction:row}}"
                + "@media (prefers-reduced-motion: no-preference){:scope{opacity:1}}");
  }

  @Test
  @DisplayName("reveal drives the animation from a view timeline, gated by reduced motion")
  void revealUsesViewTimelineBehindReducedMotionQuery() {
    var page =
        renderMain(
            section()
                .animation(Animation.reveal(Keyframes.FADE_UP))
                .child(p().text("Revealed on scroll")));

    var rule = page.ruleOf(page.element("section"), "i-");

    assertThat(CssBlocks.block(rule, MOTION_OK))
        .isEqualTo(
            "animation:tessera-fade-up auto linear backwards;animation-timeline:view(block);"
                + "animation-range:entry 0% entry 100%");
    assertThat(page.css())
        .contains(
            "@keyframes tessera-fade-up{from{opacity:0;transform:translateY(1.25rem)}"
                + "to{opacity:1;transform:translateY(0)}}");
  }

  @Test
  @DisplayName("revealOnce triggers a one-shot animation, and stagger delays it by sibling index")
  void revealOnceTriggersOneShotAnimation() {
    var page =
        renderMain(
            p().animation(Animation.revealOnce(Keyframes.FADE_UP).staggerBySiblingIndex(60))
                .text("hello"));

    var rule = page.ruleOf(page.element("main p"), "i-");

    assertThat(CssBlocks.block(rule, MOTION_OK))
        .contains("animation:tessera-fade-up 0.5s ease-out both")
        .contains("animation-delay:calc((sibling-index() - 1) * 60ms)")
        .contains("timeline-trigger:--tessera-reveal view() cover 0% cover 100%")
        .contains("animation-trigger:--tessera-reveal play-once");
  }

  @Test
  @DisplayName("keyframes used only in a view scope are registered")
  void scopeAnimationsRegisterKeyframes() {
    var pulse =
        Keyframes.named("pulse")
            .start(s -> s.opacity(1))
            .at(50, s -> s.opacity(0.5))
            .end(s -> s.opacity(1));
    var page = renderMain(new PulseView(pulse));

    assertThat(page.css())
        .contains("@keyframes pulse{from{opacity:1}50%{opacity:0.5}to{opacity:1}}")
        .contains(":scope{" + MOTION_OK + "animation:pulse 800ms ease}");
  }

  @Test
  @DisplayName("animations and keyframes validate their input")
  void animationValidation() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Animation.reveal(Keyframes.named("empty")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Keyframes.named("x").at(120, s -> s.opacity(1)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Animation.reveal(Keyframes.FADE_IN).staggerBySiblingIndex(-1));
    assertThatIllegalStateException()
        .isThrownBy(
            () ->
                Animation.of(Keyframes.FADE_IN, CssDuration.ms(200), Easing.ease())
                    .along(TimelineAxis.INLINE));
  }

  private record PulseView(Keyframes pulse) implements View {
    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(s -> s.animation(Animation.of(pulse, CssDuration.ms(800), Easing.ease())));
    }

    @Override
    public Element render(RenderContext context) {
      return div().text("pulsing");
    }
  }
}
