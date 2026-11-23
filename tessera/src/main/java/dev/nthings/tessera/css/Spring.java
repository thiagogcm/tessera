package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.internal.foundation.Decimals;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/// A spring-driven motion curve, tuned by how it feels rather than by physical constants: a
/// perceptual `duration` and a `bounce`. Tessera solves the spring in closed form and compiles it
/// to a native CSS `linear()` easing ([#easing()]) that runs over the time the spring takes to
/// settle ([#settlingDuration()]), so the browser plays it without script. Apply it with
/// [Transition#spring(Spring, Animatable...)], or pass both values to [Animation#of(Keyframes,
/// CssDuration, Easing)].
///
/// The spring has unit mass, an angular frequency `ω = 2π / duration`, and a damping ratio `ζ = 1 -
/// bounce` for a bounce of zero or more, or `ζ = 1 / (1 + bounce)` below zero:
///
/// | Bounce         | Damping            | Motion                                         |
/// | -------------- | ------------------ | ---------------------------------------------- |
/// | `0`            | critically damped  | Fastest approach without overshoot             |
/// | `0` to `1`     | underdamped        | Overshoots the end value and oscillates around it |
/// | `-1` to `0`    | overdamped         | Approaches the end value more slowly, never passing it |
///
/// The compiled curve stays within 0.002 of the exact spring position and ends once the spring
/// stays within 0.001 of its end value. Values are computed with [StrictMath] and rounded
/// half-even, so every JVM and browser runtime emits the same CSS. Each spring compiles its curve
/// once, when it is created: keep springs in constants rather than creating them on every render.
@CheckReturnValue
public final class Spring {

  /// Distance from the end value, as a fraction of the whole move, within which the spring counts
  /// as settled.
  private static final double SETTLED = 0.001;

  /// The largest gap between the compiled curve and the exact spring position.
  private static final double TOLERANCE = 0.002;

  /// Samples taken over the settling time: at least this many, and at least [#SAMPLES_PER_PERIOD]
  /// per oscillation.
  private static final int MIN_SAMPLES = 2000;

  private static final int SAMPLES_PER_PERIOD = 128;

  /// Steps of the scan that finds the settling time below its analytic upper bound.
  private static final int SETTLE_SCAN = 4096;

  private static final double TAU = 2 * Math.PI;

  /// 500ms without bounce: a calm, critically damped arrival.
  public static final Spring SMOOTH = of(Duration.ofMillis(500), 0);

  /// 500ms with a 0.15 bounce: a quick arrival with a small overshoot.
  public static final Spring SNAPPY = of(Duration.ofMillis(500), 0.15);

  /// 500ms with a 0.3 bounce: a visible overshoot that settles in a few oscillations.
  public static final Spring BOUNCY = of(Duration.ofMillis(500), 0.3);

  private final Duration perceptualDuration;
  private final double bounce;
  private final CssDuration settlingDuration;
  private final Easing easing;

  private Spring(Duration perceptualDuration, double bounce) {
    this.perceptualDuration = perceptualDuration;
    // Folds -0.0 into 0.0, so equal springs compare equal.
    this.bounce = bounce == 0 ? 0 : bounce;
    var omega = TAU / (perceptualDuration.toNanos() / 1e9);
    var zeta = bounce >= 0 ? 1 - bounce : 1 / (1 + bounce);
    var motion = new Motion(omega, zeta);
    var settlingMillis = Math.max(1, Math.ceil(motion.settlingTime() * 1000));
    this.settlingDuration = CssDuration.ms(settlingMillis);
    this.easing = motion.compile(settlingMillis / 1000);
  }

  /// A spring that takes about `perceptualDuration` to reach its end value, with `bounce` between
  /// `-1` and `1` (exclusive): `0` arrives without overshoot, a positive bounce overshoots and
  /// oscillates (`0.15` is subtle, `0.3` playful), and a negative bounce approaches more slowly.
  /// The higher the bounce, the longer the spring oscillates and the longer its compiled curve:
  /// above about `0.5` it keeps moving for several seconds. Throws IllegalArgumentException if
  /// `perceptualDuration` is not positive or `bounce` is out of range.
  public static Spring of(Duration perceptualDuration, double bounce) {
    if (perceptualDuration.isNegative() || perceptualDuration.isZero()) {
      throw new IllegalArgumentException("Spring duration must be positive: " + perceptualDuration);
    }
    if (!(bounce > -1 && bounce < 1)) {
      throw new IllegalArgumentException("Spring bounce must be within (-1, 1): " + bounce);
    }
    return new Spring(perceptualDuration, bounce);
  }

  /// The perceptual duration this spring was created with: one undamped oscillation period.
  public Duration perceptualDuration() {
    return perceptualDuration;
  }

  /// The bounce this spring was created with.
  public double bounce() {
    return bounce;
  }

  /// How long the spring takes to settle, in whole milliseconds: the duration its [#easing()] runs
  /// over. Longer than [#perceptualDuration()], since the spring keeps easing in once it looks
  /// arrived.
  public CssDuration settlingDuration() {
    return settlingDuration;
  }

  /// The spring's position over [#settlingDuration()] as a CSS `linear()` easing, such as
  /// `linear(0, 0.031 3.05%, ..., 1)`. A bouncy spring passes `1` on its way to the end value.
  public Easing easing() {
    return easing;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Spring spring
        && spring.perceptualDuration.equals(perceptualDuration)
        && Double.compare(spring.bounce, bounce) == 0;
  }

  @Override
  public int hashCode() {
    return perceptualDuration.hashCode() * 31 + Double.hashCode(bounce);
  }

  @Override
  public String toString() {
    return "Spring[" + perceptualDuration.toMillis() + "ms, bounce " + bounce + "]";
  }

  /// The unit step response of a spring with unit mass, angular frequency `omega` (rad/s) and
  /// damping ratio `zeta`, starting at rest at 0 and moving to 1.
  private record Motion(double omega, double zeta) {

    /// The position `seconds` after the move starts, in closed form for each damping regime.
    double position(double seconds) {
      if (zeta < 1) {
        var damped = omega * StrictMath.sqrt(1 - zeta * zeta);
        var decay = StrictMath.exp(-zeta * omega * seconds);
        return 1
            - decay
                * (StrictMath.cos(damped * seconds)
                    + zeta * omega / damped * StrictMath.sin(damped * seconds));
      }
      if (zeta == 1) {
        return 1 - StrictMath.exp(-omega * seconds) * (1 + omega * seconds);
      }
      var spread = StrictMath.sqrt(zeta * zeta - 1);
      var slow = -omega * (zeta - spread);
      var fast = -omega * (zeta + spread);
      return 1
          - (fast * StrictMath.exp(slow * seconds) - slow * StrictMath.exp(fast * seconds))
              / (fast - slow);
    }

    /// The time after which the position stays within [#SETTLED] of the end value.
    double settlingTime() {
      var bound = settlingBound();
      // The last scanned time still outside the band, then the band crossing just after it.
      var step = bound / SETTLE_SCAN;
      var last = 0;
      for (var i = 1; i <= SETTLE_SCAN; i++) {
        if (!settledAt(step * i)) {
          last = i;
        }
      }
      var outside = step * last;
      var inside = step * (last + 1);
      for (var i = 0; i < 48; i++) {
        var middle = (outside + inside) / 2;
        if (settledAt(middle)) {
          inside = middle;
        } else {
          outside = middle;
        }
      }
      return inside;
    }

    /// An upper bound of the settling time: the decay envelope of an underdamped spring, or a
    /// doubling search over the monotonic approach of the others.
    private double settlingBound() {
      if (zeta < 1) {
        var amplitude = 1 / StrictMath.sqrt(1 - zeta * zeta);
        return StrictMath.log(amplitude / SETTLED) / (zeta * omega);
      }
      var bound = 1 / omega;
      while (!settledAt(bound)) {
        bound *= 2;
      }
      return bound;
    }

    private boolean settledAt(double seconds) {
      return Math.abs(1 - position(seconds)) < SETTLED;
    }

    /// The positions over `seconds`, as a `linear()` easing with as few stops as keep every sample
    /// within [#TOLERANCE] of the exact position. Stops are rounded before they are checked, so the
    /// rounding stays inside the tolerance too.
    Easing compile(double seconds) {
      var periods = zeta < 1 ? seconds * omega * StrictMath.sqrt(1 - zeta * zeta) / TAU : 0;
      var samples = (int) Math.max(MIN_SAMPLES, Math.ceil(periods * SAMPLES_PER_PERIOD));
      var exact = new double[samples + 1];
      var rounded = new double[samples + 1];
      for (var i = 0; i <= samples; i++) {
        exact[i] = position(seconds * i / samples);
        // The nearest thousandth, which Decimals.format then writes exactly.
        rounded[i] = Math.rint(exact[i] * 1000) / 1000;
      }
      rounded[0] = 0;
      rounded[samples] = 1;

      var css = new StringBuilder("linear(0");
      var from = 0;
      while (from < samples) {
        var to = from + 1;
        while (to < samples && fits(exact, rounded, from, to + 1)) {
          to++;
        }
        if (to < samples) {
          css.append(", ")
              .append(Decimals.format(rounded[to]))
              .append(' ')
              .append(Decimals.format(100.0 * to / samples))
              .append('%');
        }
        from = to;
      }
      return new Easings.LinearStops(css.append(", 1)").toString());
    }

    /// Whether the straight line between the rounded stops `from` and `to` stays within
    /// [#TOLERANCE] of every exact sample between them.
    private static boolean fits(double[] exact, double[] rounded, int from, int to) {
      var start = rounded[from];
      var slope = (rounded[to] - start) / (to - from);
      for (var i = from + 1; i < to; i++) {
        if (Math.abs(start + slope * (i - from) - exact[i]) > TOLERANCE) {
          return false;
        }
      }
      return true;
    }
  }
}
