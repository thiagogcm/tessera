package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.view.ViewExpansion.Reporter;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Browser-owned payloads carried through the linked tree to reconciliation.
sealed interface LiveBehavior {

  record Bindings(Map<EventListener.Key, EventListener> events, ControlProperties controls)
      implements LiveBehavior {
    static final Bindings NONE = new Bindings(Map.of(), ControlProperties.UNCONTROLLED);

    public Bindings {
      events =
          events.isEmpty() ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(events));
    }
  }

  record Reaction(Effect effect) implements LiveBehavior {}

  static Bindings bindings(ViewSnapshot.Tag<LiveBehavior> tag) {
    return tag.payload() instanceof Bindings binding ? binding : Bindings.NONE;
  }

  static void validate(
      LiveBehavior payload,
      TagName tag,
      Map<String, String> attributes,
      Location location,
      Reporter reporter) {
    if (payload instanceof Bindings binding) {
      binding.controls().validate(tag, attributes, location, reporter, binding.events().values());
    }
  }
}
