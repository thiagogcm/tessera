package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Select;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.time.Duration;

/// A checkout step whose options load a minute after mounting: a bound delivery choice gains an
/// express option the server marks as the default, and a country select declared with an initial
/// value receives its options.
final class ShippingDemo {
  private String delivery = "standard";
  private boolean loaded;

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new ShippingDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var country = Select.of("country").initialValue("br");
    if (loaded) {
      country.option("ca", "Canada").option("br", "Brazil").option("pt", "Portugal");
    }
    var group = LiveElement.of(Elements.fieldset().child(Elements.legend().text("Delivery")));
    if (loaded) {
      group.child(radio("express", "Express", true, root).key("express"));
    }
    group
        .child(radio("standard", "Standard", false, root).key("standard"))
        .child(radio("pickup", "Pickup", false, root).key("pickup"));
    return LiveElement.of(Elements.section().ariaLabel("Shipping"))
        .child(
            Live.effect(
                "options",
                lifetime ->
                    root.host()
                        .within(lifetime)
                        .timeout(Duration.ofMinutes(1), () -> loaded = true)))
        .child(Elements.p().role(Role.STATUS).text("Delivery: " + delivery))
        .child(group)
        .child(Field.of("Country", country));
  }

  private LiveElement radio(String value, String label, boolean byDefault, BrowserRoot root) {
    return LiveElement.of(Elements.label())
        .child(
            LiveElement.of(
                    Elements.input(InputType.RADIO)
                        .attr("name", "delivery")
                        .attr("value", value)
                        .initialChecked(byDefault))
                .bindChecked(delivery.equals(value))
                .onCheckedChange(checked -> {
                  if (checked) {
                    root.update(() -> delivery = value).run();
                  }
                }))
        .text(label);
  }
}
