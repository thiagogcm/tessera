package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.wasm.BrowserRoot;

/// Scheduling evidence: coalescing, budgeted paint yields, runaway diagnostics and stale results.
public final class SchedulingDemo {
  private static final int CHAIN = 5;

  private final MountedRoot schedule = new MountedRoot("schedule view");
  private int renders;
  private int remaining;
  private boolean runaway;
  private boolean reject;
  private boolean fail;
  private String completion = "idle";

  public BrowserRoot mount(String host) {
    var mounted = BrowserRoot.mount(host, new BrowserRoot.Scheduling(2, 8), this::view);
    schedule.set(mounted);
    return mounted;
  }

  public BrowserRoot mountControl(String host) {
    return BrowserRoot.mount(host, this::control);
  }

  private IntoElement view(BrowserRoot root, DocumentContext context) {
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
      return Elements.section().child(new FailingPanel());
    }
    if (reject) {
      reject = false;
      return Elements.section()
          .children(Elements.div().key("duplicate"), Elements.div().key("duplicate"));
    }
    return Elements.section()
        .attr("data-schedule-renders", Integer.toString(renders))
        .attr("data-schedule-remaining", Integer.toString(remaining))
        .children(
            Elements.h2().text("Scheduling"),
            Button.of("Burst invalidations")
                .onClick(
                    root.update(() -> {
                      for (var index = 0; index < 5; index++) {
                        root.invalidate();
                      }
                    })),
            Button.of("Start render chain").onClick(root.update(() -> remaining = CHAIN)),
            Button.of("Start runaway").onClick(root.update(() -> runaway = true)),
            Button.of("Stop runaway").onClick(root.update(() -> runaway = false)),
            Button.of("Reject next render").onClick(root.update(() -> reject = true)),
            Button.of("Fail next render").onClick(root.update(() -> fail = true)),
            Button.of("Refresh schedule").onClick(root.update(() -> {})),
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

  private IntoElement control(BrowserRoot root, DocumentContext context) {
    return Elements.section()
        .attr("data-control-completion", completion)
        .children(
            Elements.h2().text("Scheduling control"),
            Button.of("Complete current update").onClick(root.update(() -> complete("current"))),
            Button.of("Close schedule root").onClick(root.update(() -> schedule.get().close())),
            Button.of("Complete stale update").onClick(root.update(() -> complete("stale"))),
            Button.of("Throw from handler")
                .onClick(() -> {
                  throw new IllegalStateException("Simulated handler failure");
                }),
            Elements.p().attr("data-control-status", "").text(completion));
  }

  private record FailingPanel() implements Component {
    @Override
    public IntoElement expand(DocumentContext context) {
      throw new IllegalStateException("Simulated render failure");
    }
  }

  /// Simulates an asynchronous completion that only applies while the schedule root is open.
  private void complete(String label) {
    completion = label + " ignored";
    schedule.get().update(() -> completion = label + " applied").run();
  }
}
