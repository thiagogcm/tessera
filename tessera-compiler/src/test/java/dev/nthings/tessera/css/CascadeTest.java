package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalStateException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.support.CssBlocks;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Cascade")
class CascadeTest {

  private static final String MOTION_OK = "@media (prefers-reduced-motion: no-preference){";

  static Stream<Arguments> shorthandPairs() {
    return Stream.of(
        argumentSet(
            "padding",
            (StyleBlock) s -> s.p(CssLength.rem(1)),
            (StyleBlock) s -> s.pt(CssLength.rem(2)),
            "padding-block-start"),
        argumentSet(
            "margin",
            (StyleBlock) s -> s.my(CssLength.zero()),
            (StyleBlock) s -> s.mt(CssLength.rem(2)),
            "margin-block-start"),
        argumentSet(
            "inset",
            (StyleBlock) s -> s.inset(CssLength.zero()),
            (StyleBlock) s -> s.insetBlockStart(CssLength.rem(1)),
            "inset-block-start"),
        argumentSet(
            "border",
            (StyleBlock) s -> s.border(Border.hairline(Tokens.BORDER_SUBTLE)),
            (StyleBlock) s -> s.borderInlineEnd(Border.none()),
            "border-inline-end"),
        argumentSet(
            "overflow",
            (StyleBlock) s -> s.overflow(Overflow.HIDDEN),
            (StyleBlock) s -> s.overflowX(Overflow.AUTO),
            "overflow-x"),
        argumentSet(
            "gap",
            (StyleBlock) s -> s.gap(CssLength.rem(0.5)),
            (StyleBlock) s -> s.rowGap(CssLength.rem(1)),
            "row-gap"));
  }

  @ParameterizedTest
  @MethodSource("shorthandPairs")
  @DisplayName("the later of a shorthand and its longhand wins")
  void laterOfShorthandAndLonghandWins(StyleBlock shorthand, StyleBlock longhand, String name) {
    var page =
        renderMain(
            div().styles(shorthand).styles(longhand).text("longhand last"),
            p().styles(longhand).styles(shorthand).text("shorthand last"));

    var longhandLast = properties(page.ruleOf(page.element("main > div"), "u-"));
    assertThat(longhandLast).endsWith(name);
    assertThat(longhandLast.size()).isGreaterThan(1);
    assertThat(properties(page.ruleOf(page.element("main > p"), "u-"))).doesNotContain(name);
  }

  @Test
  @DisplayName("writing a property again replaces its value")
  void lastWriteReplaces() {
    var page = renderMain(div().bg(Tokens.SURFACE_CARD).bg(Tokens.SURFACE_SUBTLE));

    assertThat(page.ruleOf(page.element("main > div"), "u-"))
        .contains("var(--surface-subtle)")
        .doesNotContain("--surface-card");
  }

  @Test
  @DisplayName("merging replays the other refinement's writes, so its shorthands win")
  void mergeReplaysWrites() {
    var base = new Declarations().mt(CssLength.rem(2)).textColor(Tokens.TEXT_PRIMARY);

    base.mergeFrom(new Declarations().my(CssLength.zero()));

    assertThat(properties(base.toCssBlockBody()))
        .contains("margin-block", "color")
        .doesNotContain("margin-block-start");
  }

  @Test
  @DisplayName("different custom properties never share a generated class")
  void customPropertiesHashDistinctly() {
    var page =
        renderMain(
            div().styles(s -> s.customProperty(CustomProperty.named("col-1"), CssNumber.of(23))),
            p().styles(s -> s.customProperty(CustomProperty.named("col-12"), CssNumber.of(3))));

    assertThat(page.ruleOf(page.element("main > div"), "u-")).isEqualTo("--col-1:23");
    assertThat(page.ruleOf(page.element("main > p"), "u-")).isEqualTo("--col-12:3");
  }

  @Test
  @DisplayName("states follow State order whatever the call order, with media rules after them")
  void statesFollowStateOrder() {
    var page =
        renderMain(
            button()
                .opacity(1)
                .on(Media.MOTION_SAFE, m -> m.on(State.FOCUS_VISIBLE, s -> s.opacity(0.5)))
                .on(State.DISABLED, s -> s.opacity(0.4))
                .on(State.ACTIVE, s -> s.opacity(0.8))
                .on(State.HOVER, s -> s.opacity(0.9))
                .on(State.HOVER, s -> s.cursor(Cursor.POINTER))
                .text("Save"));

    var rule = page.ruleOf(page.element("button"), "i-");
    assertThat(rule).containsSubsequence("&:hover", "&:active", "&:disabled", MOTION_OK);
    assertThat(rule).containsOnlyOnce("&:hover");
    assertThat(CssBlocks.block(rule, "&:hover:not(:where(:disabled)){"))
        .contains("opacity:0.9", "cursor:pointer");
    assertThat(CssBlocks.block(rule, MOTION_OK)).contains("&:focus-visible{opacity:0.5}");
  }

  @Test
  @DisplayName("plain, state and container rules sit in their layers, with states over containers")
  void elementRulesSplitByLayer() {
    var cards = Container.named("cards", Container.Type.INLINE_SIZE);
    var page =
        renderMain(
            div()
                .container(cards)
                .child(
                    button()
                        .bg(Tokens.SURFACE_CARD)
                        .on(cards.atLeast(CssLength.rem(30)), s -> s.bg(Tokens.SURFACE_SUBTLE))
                        .on(State.DISABLED, s -> s.bg(Tokens.BORDER_SUBTLE))
                        .text("Save")));

    var classes = page.element("button").className();
    var css = page.css();
    assertThat(CssBlocks.block(css, "@layer utilities{"))
        .contains("." + CssBlocks.generatedClass(classes, "u-") + "{");
    assertThat(CssBlocks.block(css, "@layer interactive{"))
        .contains("." + CssBlocks.generatedClass(classes, "i-") + "{");
    assertThat(CssBlocks.block(css, "@layer container{"))
        .contains("@container cards (inline-size >= 30rem){")
        .contains("." + CssBlocks.generatedClass(classes, "cq-") + "{");
    assertThat(layerOrder(css)).containsSubsequence("utilities", "container", "interactive");
  }

  @Test
  @DisplayName("container queries render in the container layer, and SIZE excludes INLINE_SIZE")
  void containerQueries() {
    var bar = Container.named("bar", Container.Type.SCROLL_STATE, Container.Type.INLINE_SIZE);
    var page =
        renderMain(
            div()
                .container(bar)
                .child(
                    p().on(bar.stuck(Container.Edge.TOP), s -> s.opacity(0.9))
                        .on(bar.below(CssLength.rem(36)), s -> s.flexRow())
                        .text("Bar")));

    assertThat(page.ruleOf(page.element("main > div"), "u-"))
        .contains("container-type:inline-size scroll-state");
    assertThat(CssBlocks.block(page.css(), "@layer container{"))
        .contains(
            "@container bar scroll-state(stuck: top){", "@container bar (inline-size < 36rem){");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Container.unnamed(Container.Type.SIZE, Container.Type.INLINE_SIZE));
  }

  @Test
  @DisplayName("scope rules nest states and starting styles, and query groups follow plain rules")
  void scopeRules() {
    var page = renderMain(new Linked());

    var body = page.scopeBody(page.element("nav"));
    assertThat(CssBlocks.block(body, ":scope a{"))
        .contains("&:hover:not(:where(:disabled)){", "@starting-style{");
    assertThat(body).containsSubsequence(":scope a{", "@container", MOTION_OK);
  }

  static Stream<Arguments> reveals() {
    return Stream.of(
        argumentSet(
            "view timeline",
            Animation.enterViewport(Keyframes.FADE_UP),
            List.of("animation-timeline", "animation-range")),
        argumentSet(
            "one-shot trigger",
            Animation.revealOnce(Keyframes.FADE_UP).staggerBySiblingIndex(60),
            List.of("animation-delay", "timeline-trigger", "animation-trigger")));
  }

  @ParameterizedTest
  @MethodSource("reveals")
  @DisplayName("reveals run only without reduced motion, with the shorthand before its longhands")
  void revealsRespectReducedMotion(Animation reveal, List<String> longhands) {
    var page =
        renderMain(
            section().animation(reveal).child(p().text("One")),
            section().animation(reveal).child(p().text("Two")));

    var rule = page.ruleOf(page.element("section"), "i-");
    assertThat(rule).startsWith(MOTION_OK).endsWith("}");
    var animated = properties(CssBlocks.block(rule, MOTION_OK));
    assertThat(animated.getFirst()).isEqualTo("animation");
    assertThat(animated).containsAll(longhands);
    assertThat(page.css()).containsOnlyOnce("@keyframes tessera-fade-up{");
  }

  @Test
  @DisplayName("keyframes used only in an id scope are registered once")
  void scopeAnimationsRegisterKeyframes() {
    var pulse =
        Keyframes.named("pulse")
            .start(s -> s.opacity(1))
            .at(50, s -> s.opacity(0.5))
            .end(s -> s.opacity(1));
    var page = renderMain(new Pulsing(pulse), new Pulsing(pulse));

    assertThat(page.css()).containsOnlyOnce("@keyframes pulse{");
    assertThat(CssBlocks.block(page.scopeBody(page.element("div")), MOTION_OK))
        .contains("animation:pulse ");
  }

  @Test
  @DisplayName("animations and keyframes validate their input")
  void animationValidation() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Animation.enterViewport(Keyframes.named("empty")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Keyframes.named("x").at(120, s -> s.opacity(1)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Animation.enterViewport(Keyframes.FADE_IN).staggerBySiblingIndex(-1));
    assertThatIllegalStateException()
        .isThrownBy(() ->
            Animation.of(Keyframes.FADE_IN, CssDuration.ms(200), Easing.ease())
                .along(TimelineAxis.INLINE));
  }

  /// Property names of a declaration block, in emission order; nested rules are skipped.
  private static List<String> properties(String body) {
    var depth = 0;
    var flat = new StringBuilder();
    for (var c : body.toCharArray()) {
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        flat.append(';');
      } else if (depth == 0) {
        flat.append(c);
      }
    }
    return Arrays.stream(flat.toString().split(";"))
        .filter(declaration -> declaration.contains(":"))
        .map(declaration -> declaration.substring(0, declaration.indexOf(':')).strip())
        .toList();
  }

  /// The layer names of the stylesheet's `@layer` order statement.
  private static List<String> layerOrder(String css) {
    var start = css.indexOf("@layer ");
    var statement = css.substring(start + "@layer ".length(), css.indexOf(';', start));
    return Arrays.stream(statement.split(",")).map(String::strip).toList();
  }

  private record Linked() implements Scope {
    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .select(
              ":scope a",
              s ->
                  s.opacity(1)
                      .on(State.HOVER, h -> h.opacity(0.8))
                      .startingStyle(start -> start.opacity(0)))
          .on(
              Container.unnamed(Container.Type.INLINE_SIZE).atLeast(CssLength.rem(48)),
              c -> c.root(s -> s.flexRow()))
          .on(Media.MOTION_SAFE, m -> m.root(s -> s.opacity(1)));
    }

    @Override
    public TagElement expand(ViewContext context) {
      return nav().child(a("/home").text("Home"));
    }
  }

  private record Pulsing(Keyframes pulse) implements Scope {
    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(s -> s.animation(Animation.of(pulse, CssDuration.ms(800), Easing.ease())));
    }

    @Override
    public TagElement expand(ViewContext context) {
      return div().text("pulsing");
    }
  }
}
