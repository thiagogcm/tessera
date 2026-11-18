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
import org.graalvm.webimage.api.JS;

/// First browser fixture: shared component styling, linked labels and a captured Java callback.
public final class WebImageDemo {

  private int count;
  private boolean badge;
  private int meter;
  private int liveMeter;
  private final TagElement name = Forms.text("name");

  private WebImageDemo() {}

  /// Mounts the hosts the page declares for this entry; see [BrowserEntry].
  public static void main(String[] args) {
    var demo = new WebImageDemo();
    var scheduling = new SchedulingDemo();
    var lifetime = new LifetimeDemo();
    var closure = new ClosureDemo();
    BrowserEntry.start(
        args,
        Map.ofEntries(
            // Declared first: its failed initial render must not prevent the roots after it.
            Map.entry(
                "broken",
                host ->
                    BrowserRoot.mount(
                        host, _ -> LiveContent.of(Elements.section().child(new BrokenPanel())))),
            Map.entry("app", host -> BrowserRoot.mount(host, demo::view)),
            Map.entry(
                "secondary",
                host ->
                    BrowserRoot.mount(
                        host,
                        _ ->
                            LiveContent.of(
                                Field.of("Notes", Forms.text("notes"))
                                    .hint("An independent mounted root")))),
            Map.entry("list", KeyedListDemo::mount),
            Map.entry("controls", ControlledFormDemo::mount),
            Map.entry("schedule", scheduling::mount),
            Map.entry("schedule-control", scheduling::mountControl),
            Map.entry("lifetime", lifetime::mount),
            Map.entry("lifetime-control", lifetime::mountControl),
            Map.entry("events", EventsDemo::mount),
            Map.entry("services", ServicesDemo::mount),
            Map.entry("collisions", CollisionsDemo::mount),
            Map.entry("closure", closure::mount),
            Map.entry("closure-control", closure::mountControl)));
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
        // A value that changes on every render, producing a new atomic rule each time.
        .child(
            Elements.div()
                .attr("data-meter", "")
                .inlineSize(CssLength.px(meter))
                .blockSize(CssLength.px(4)))
        .child(Ui.button("Advance meter", root.update(() -> meter++)))
        // The same kind of value, declared live: one rule however it changes.
        .child(
            Elements.div()
                .attr("data-live-meter", "")
                .live(style -> style.inlineSize(CssLength.px(liveMeter)))
                .blockSize(CssLength.px(4)))
        .child(Ui.button("Animate live meter", () -> animate(root.host(), 120)))
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

  @JS.Coerce
  @JS("return new URLSearchParams(location.search).has('fail-mount');")
  private static native boolean failMount();

  private record BrokenPanel() implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      if (failMount()) {
        throw new IllegalStateException("Simulated initial render failure");
      }
      return Elements.p().text("Rendered without a simulated failure");
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
