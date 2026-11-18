package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.concurrent.atomic.AtomicInteger;

/// Scheduling evidence: coalescing, budgeted paint yields, runaway diagnostics and stale results.
public final class SchedulingDemo {
  private static final int CHAIN = 5;

  /// Counts tallies within one expansion; a rejected render's count must not carry over.
  private static final ViewKey<AtomicInteger> TALLY =
      ViewKey.of("schedule-tally", AtomicInteger.class, AtomicInteger::new);

  private final MountedRoot schedule = new MountedRoot("schedule view");
  private int renders;
  private int remaining;
  private boolean runaway;
  private boolean reject;
  private boolean fail;
  private String completion = "idle";
  private int rejectedStarts;

  public BrowserRoot mount(String host) {
    var mounted = BrowserRoot.mount(host, new BrowserRoot.Scheduling(2, 8), this::view);
    schedule.set(mounted);
    return mounted;
  }

  public BrowserRoot mountControl(String host) {
    return BrowserRoot.mount(host, this::control);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    renders++;
    if (remaining > 0) {
      remaining--;
      if (remaining > 0) {
        root.invalidate();
      }
    }
    if (runaway) {
      root.invalidate();
    }
    if (fail) {
      fail = false;
      return LiveContent.of(Elements.section().child(new FailingPanel()));
    }
    if (reject) {
      reject = false;
      // Rejected for its duplicate keys: its tallies and effect must leave no trace.
      return LiveElement.of(Elements.section().children(new Tally(), new Tally()))
          .child(Live.effect("rejected", _ -> rejectedStarts++))
          .children(
              LiveContent.of(Elements.div().key("duplicate")),
              LiveContent.of(Elements.div().key("duplicate")));
    }
    return LiveElement.of(
            Elements.section()
                .attr("data-schedule-renders", Integer.toString(renders))
                .attr("data-schedule-remaining", Integer.toString(remaining))
                .attr("data-schedule-rejected-effects", Integer.toString(rejectedStarts))
                .children(Elements.h2().text("Scheduling"), new Tally()))
        .children(
            Ui.button(
                "Burst invalidations",
                root.update(() -> {
                  for (var index = 0; index < 5; index++) {
                    root.invalidate();
                  }
                })),
            Ui.button("Start render chain", root.update(() -> remaining = CHAIN)),
            Ui.button("Start runaway", root.update(() -> runaway = true)),
            Ui.button("Stop runaway", root.update(() -> runaway = false)),
            Ui.button("Reject next render", root.update(() -> reject = true)),
            Ui.button("Fail next render", root.update(() -> fail = true)),
            Ui.button("Refresh schedule", root.update(() -> {})))
        .child(
            Elements.p()
                .attr("data-schedule-status", "")
                .text(
                    "renders="
                        + renders
                        + "; remaining="
                        + remaining
                        + "; completion="
                        + completion));
  }

  private LiveContent control(LiveContext context) {
    var root = context.root();
    return LiveElement.of(
            Elements.section()
                .attr("data-control-completion", completion)
                .child(Elements.h2().text("Scheduling control")))
        .children(
            Ui.button("Complete current update", root.update(() -> complete("current"))),
            Ui.button("Close schedule root", root.update(() -> schedule.get().close())),
            Ui.button("Complete stale update", root.update(() -> complete("stale"))),
            Ui.button(
                "Throw from handler",
                () -> {
                  throw new IllegalStateException("Simulated handler failure");
                }))
        .child(Elements.p().attr("data-control-status", "").text(completion));
  }

  /// Shows how many tallies this expansion has counted so far.
  private record Tally() implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      var count = context.state(TALLY).incrementAndGet();
      return Elements.span().attr("data-schedule-tally", Integer.toString(count));
    }
  }

  private record FailingPanel() implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      throw new IllegalStateException("Simulated render failure");
    }
  }

  /// Simulates an asynchronous completion that only applies while the schedule root is open.
  private void complete(String label) {
    completion = label + " ignored";
    schedule.get().update(() -> completion = label + " applied").run();
  }
}
