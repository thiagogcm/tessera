package dev.nthings.tessera.wasm.example;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.Badge;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.wasm.BrowserEntry;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Host;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.Map;

/// The showcase's main entry point: every view the published pages mount, plus the home page's
/// counter with shared component styling and a live-animated meter.
public final class WebImageDemo {

  private int count;
  private boolean badge;
  private int liveMeter;
  private final TagElement name = Forms.text("name");

  private WebImageDemo() {}

  /// Mounts the hosts the page declares for this entry; see [BrowserEntry].
  public static void main(String[] args) {
    var home = new WebImageDemo();
    var activity = new ActivityLog();
    var lifetime = new LifetimeDemo(activity);
    var closure = new ClosureDemo(activity);
    var scheduling = new SchedulingDemo();
    BrowserEntry.start(
        args,
        Map.ofEntries(
            Map.entry(
                "broken",
                host ->
                    BrowserRoot.mount(
                        host, _ -> LiveContent.of(Elements.section().child(new BrokenPanel())))),
            Map.entry("app", host -> BrowserRoot.mount(host, home::view)),
            Map.entry(
                "notes",
                host ->
                    BrowserRoot.mount(
                        host,
                        _ ->
                            LiveContent.of(
                                Field.of("Notes", Forms.text("notes"))
                                    .hint("An independent mounted root")))),
            Map.entry("forms", ControlledFormDemo::mount),
            Map.entry("defaults", DefaultsDemo::mount),
            Map.entry("shipping", ShippingDemo::mount),
            Map.entry("lists", KeyedListDemo::mount),
            Map.entry("tabs", TabsDemo::mount),
            Map.entry("dialogs", DialogDemo::mount),
            Map.entry("search", SearchDemo::mount),
            Map.entry("events", EventsDemo::mount),
            Map.entry("services", ServicesDemo::mount),
            Map.entry("activity", activity::mount),
            Map.entry("lifetime", lifetime::mount),
            Map.entry("toolbar", lifetime::mountToolbar),
            Map.entry("closure", closure::mount),
            Map.entry("schedule", scheduling::mount),
            Map.entry("collisions", CollisionsDemo::mount)));
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    return LiveElement.of(
            Elements.main()
                .flex()
                .flexCol()
                .gap(Tokens.SPACE_4)
                .p(Tokens.SPACE_6)
                .maxInlineSize(CssLength.rem(36))
                .children(
                    h1().text("Java in the browser"),
                    count % 2 == 0
                        ? Field.of("Name", name).hint("Rendered by shared Java components")
                        : Field.of("Name", name).error("Enter a name"),
                    new Counter(count),
                    badge ? Badge.of("Saved").tone(Tone.SUCCESS) : Elements.empty()))
        .child(Ui.button("Increment", root.update(() -> count++)))
        .child(Ui.button("Show badge", root.update(() -> badge = true)))
        // A value that changes every frame, declared live so it never produces a new rule.
        .child(
            Elements.div()
                .role(Role.PROGRESSBAR)
                .ariaLabel("Upload progress")
                .live(style -> style.inlineSize(CssLength.px(liveMeter)))
                .blockSize(CssLength.px(4))
                .bg(Tokens.ACCENT_PRIMARY))
        .child(Ui.button("Animate progress", () -> animate(root.host(), 120)))
        .child(Ui.button(Button.of("Unmount").variant(Button.Variant.OUTLINE), root::close));
  }

  /// Advances the live meter once per frame for `frames` frames.
  private void animate(Host host, int frames) {
    if (frames > 0) {
      host.frame(() -> {
        liveMeter++;
        animate(host, frames - 1);
      });
    }
  }

  private record BrokenPanel() implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      throw new IllegalStateException("Simulated initial render failure");
    }
  }

  private record Counter(int value) implements Scope {
    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(style -> style.fontWeight(value % 2 == 0 ? 600 : 400));
    }

    @Override
    public TagElement expand(ViewContext context) {
      return p().id(ElementRef.scoped("value")).role(Role.STATUS).text("Count: " + value);
    }
  }
}
