package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.within;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.support.CssBlocks;
import dev.nthings.tessera.support.RenderedPage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Springs")
class SpringTest {

  private static final String MOTION_OK = Media.MOTION_SAFE.prelude() + "{";

  static Stream<Arguments> springs() {
    return Stream.of(
        argumentSet("smooth", Spring.SMOOTH),
        argumentSet("snappy", Spring.SNAPPY),
        argumentSet("bouncy", Spring.BOUNCY),
        argumentSet("short and lively", Spring.of(Duration.ofMillis(250), 0.5)),
        argumentSet("overdamped", Spring.of(Duration.ofMillis(400), -0.5)),
        argumentSet("barely underdamped", Spring.of(Duration.ofMillis(500), 1e-9)),
        argumentSet("barely overdamped", Spring.of(Duration.ofMillis(500), -1e-9)));
  }

  @ParameterizedTest
  @MethodSource("springs")
  @DisplayName("the compiled curve follows the exact spring position and ends once it settles")
  void curveFollowsTheSpring(Spring spring) {
    var curve = Curve.parse(spring.easing().toCss());
    var seconds = millis(spring.settlingDuration()) / 1000;

    for (var i = 0; i <= 4000; i++) {
      var progress = i / 4000.0;
      assertThat(curve.at(progress))
          .as("progress %s", progress)
          .isCloseTo(exact(spring, progress * seconds), within(0.0025));
    }
    for (var i = 0; i <= 400; i++) {
      var after = seconds * (1 + i / 100.0);
      assertThat(Math.abs(1 - exact(spring, after))).as("at %ss", after).isLessThan(0.001);
    }
  }

  @Test
  @DisplayName("the settling duration is the first whole millisecond after the spring settles")
  void settlingDurationIsTight() {
    for (var spring : List.of(Spring.SMOOTH, Spring.of(Duration.ofMillis(400), -0.5))) {
      var seconds = millis(spring.settlingDuration()) / 1000;
      assertThat(Math.abs(1 - exact(spring, seconds - 0.001))).isGreaterThanOrEqualTo(0.001);
    }
    assertThat(millis(Spring.SMOOTH.settlingDuration())).isEqualTo(735);
  }

  @Test
  @DisplayName("bounce controls overshoot: none without bounce, a measured peak with it")
  void bounceControlsOvershoot() {
    assertThat(Curve.parse(Spring.SMOOTH.easing().toCss()).max()).isLessThanOrEqualTo(1);
    assertThat(Curve.parse(Spring.of(Duration.ofMillis(400), -0.5).easing().toCss()).max())
        .isLessThanOrEqualTo(1);
    // An underdamped step response first peaks at 1 + exp(-ζπ / √(1 - ζ²)).
    var zeta = 0.7;
    var peak = 1 + Math.exp(-zeta * Math.PI / Math.sqrt(1 - zeta * zeta));
    assertThat(Curve.parse(Spring.BOUNCY.easing().toCss()).max()).isCloseTo(peak, within(0.002));
  }

  @Test
  @DisplayName("equal springs compile byte-identical, compact CSS")
  void compilationIsDeterministicAndCompact() {
    var spring = Spring.of(Duration.ofMillis(500), 0.15);

    assertThat(spring).isEqualTo(Spring.SNAPPY).hasSameHashCodeAs(Spring.SNAPPY);
    assertThat(spring.easing()).isEqualTo(Spring.SNAPPY.easing());
    assertThat(spring.easing().toCss())
        .startsWith("linear(0, ")
        .endsWith(", 1)")
        .hasSizeLessThan(260);
    assertThat(Curve.parse(spring.easing().toCss()).stops()).hasSizeLessThanOrEqualTo(20);
    assertThat(Spring.of(Duration.ofMillis(500), -0.0)).isEqualTo(Spring.SMOOTH);
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.5, 0.7})
  @DisplayName("the extreme bounces settle within about four perceptual durations in a short curve")
  void extremeBouncesStayBounded(double bounce) {
    var perceptual = Duration.ofSeconds(2);
    var spring = Spring.of(perceptual, bounce);

    assertThat(millis(spring.settlingDuration())).isLessThan(4.5 * perceptual.toMillis());
    assertThat(spring.easing().toCss()).hasSizeLessThan(600);
  }

  @Test
  @DisplayName("a spring needs a positive duration and a bounce within [-0.5, 0.7]")
  void springValidation() {
    assertThatIllegalArgumentException().isThrownBy(() -> Spring.of(Duration.ZERO, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> Spring.of(Duration.ofMillis(-1), 0));
    for (var bounce : List.of(-0.51, 0.71, -1.0, 1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Spring.of(Duration.ofMillis(500), bounce));
    }
  }

  @Test
  @DisplayName("a spring transition runs only without reduced motion, over a plain fallback")
  void springTransitionIsMotionSafe() {
    var page =
        renderMain(
            div()
                .transition(Transition.of(CssDuration.ms(150), Easing.ease(), Animatable.OPACITY))
                .transition(
                    Transition.spring(Spring.SNAPPY, Animatable.TRANSLATE, Animatable.OPACITY))
                .text("Panel"));

    var element = page.element("main > div");
    assertThat(page.ruleOf(element, "u-"))
        .contains("transition-property:opacity", "transition-duration:150ms")
        .doesNotContain("linear(");
    var interactive = page.ruleOf(element, "i-");
    assertThat(interactive).startsWith(MOTION_OK);
    assertThat(CssBlocks.block(interactive, MOTION_OK))
        .isEqualTo(
            "transition-property:translate, opacity;transition-duration:697ms;"
                + "transition-timing-function:"
                + Spring.SNAPPY.easing().toCss());
  }

  @Test
  @DisplayName("motion-safe styles inside a reduced-motion guard join it instead of nesting again")
  void motionSafeStylesJoinAnEnclosingGuard() {
    var spring = Transition.spring(Spring.SNAPPY, Animatable.TRANSLATE);
    var shared = new Declarations().transition(spring);
    var page =
        renderMain(
            div()
                .on(Media.MOTION_SAFE, m -> m.opacity(0.5).transition(spring))
                .animation(Animation.enterViewport(Keyframes.FADE_IN))
                .text("Guarded"),
            p().on(Media.MOTION_SAFE, m -> m.mergeFrom(shared)).text("Merged"));

    var guarded = page.ruleOf(page.element("main > div"), "i-");
    assertThat(guarded).containsOnlyOnce("@media");
    assertThat(CssBlocks.block(guarded, MOTION_OK))
        .contains("opacity:0.5", "transition-property:translate", "animation:tessera-fade-in");
    assertThat(page.ruleOf(page.element("main > p"), "i-")).containsOnlyOnce("@media");
  }

  @Test
  @DisplayName("easing and duration tokens emit a spring's curve once, in the theme")
  void tokensEmitTheCurveOnce() {
    var settle = Spring.of(Duration.ofMillis(450), 0.2);
    var easing = new DesignToken.Easing("ease-settle", settle.easing());
    var duration = new DesignToken.Duration("settle-duration", settle.settlingDuration());
    var theme =
        Theme.defaults()
            .with(easing, easing.defaultValue())
            .with(duration, duration.defaultValue());
    var slide = Transition.of(duration, easing, Animatable.TRANSLATE).whenMotionSafe();
    var content =
        main()
            .children(
                div().transition(slide).text("One"),
                p().transition(slide.properties(Animatable.TRANSFORM)).text("Two"),
                button().transition(slide.allowingDiscrete()).text("Three"));

    var css = Compiler.forPage().withTheme(theme).compile(PageDocument.of("Tokens", content)).css();
    assertThat(css)
        .containsOnlyOnce("linear(")
        .contains("--ease-settle:" + settle.easing().toCss())
        .contains("--settle-duration:" + settle.settlingDuration().toCss())
        .contains(
            "transition-duration:var(--settle-duration);"
                + "transition-timing-function:var(--ease-settle)");
    assertThat(
            RenderedPage.of(
                    Compiler.forPage()
                        .withTheme(theme.with(easing, Easing.easeOut()))
                        .compile(PageDocument.of("Override", content)))
                .css())
        .contains("--ease-settle:ease-out")
        .doesNotContain("linear(");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            theme.with(new DesignToken.Easing("settle-duration", Easing.ease()), Easing.ease()));
  }

  /// The exact position of `spring`'s unit step response at `seconds`, solved independently of the
  /// implementation.
  private static double exact(Spring spring, double seconds) {
    var omega = 2 * Math.PI / (spring.perceptualDuration().toNanos() / 1e9);
    var bounce = spring.bounce();
    var zeta = bounce >= 0 ? 1 - bounce : 1 / (1 + bounce);
    if (zeta == 1) {
      return 1 - Math.exp(-omega * seconds) * (1 + omega * seconds);
    }
    if (zeta < 1) {
      var damped = omega * Math.sqrt(1 - zeta * zeta);
      return 1
          - Math.exp(-zeta * omega * seconds)
              * (Math.cos(damped * seconds) + zeta * omega / damped * Math.sin(damped * seconds));
    }
    var spread = Math.sqrt(zeta * zeta - 1);
    var slow = -omega * (zeta - spread);
    var fast = -omega * (zeta + spread);
    return 1 - (fast * Math.exp(slow * seconds) - slow * Math.exp(fast * seconds)) / (fast - slow);
  }

  private static double millis(CssDuration duration) {
    if (duration instanceof CssDurations.Millis(var millis)) {
      return millis;
    }
    throw new AssertionError("Not a literal duration: " + duration);
  }

  /// A parsed `linear()` function: its stops, as (progress, value) pairs with explicit positions.
  private record Curve(List<double[]> stops) {

    static Curve parse(String css) {
      var body = css.substring("linear(".length(), css.length() - 1);
      var parts = body.split(", ");
      var stops = new ArrayList<double[]>();
      for (var i = 0; i < parts.length; i++) {
        var tokens = parts[i].split(" ");
        var value = Double.parseDouble(tokens[0]);
        double position;
        if (tokens.length == 2) {
          position = Double.parseDouble(tokens[1].replace("%", "")) / 100;
        } else if (i == 0) {
          position = 0;
        } else if (i == parts.length - 1) {
          position = 1;
        } else {
          throw new AssertionError("Stop without a position: " + parts[i]);
        }
        stops.add(new double[] {position, value});
      }
      return new Curve(stops);
    }

    double at(double progress) {
      for (var i = 1; i < stops.size(); i++) {
        var from = stops.get(i - 1);
        var to = stops.get(i);
        if (progress <= to[0]) {
          return from[1] + (to[1] - from[1]) * (progress - from[0]) / (to[0] - from[0]);
        }
      }
      return stops.getLast()[1];
    }

    double max() {
      return stops.stream().mapToDouble(stop -> stop[1]).max().orElseThrow();
    }
  }
}
