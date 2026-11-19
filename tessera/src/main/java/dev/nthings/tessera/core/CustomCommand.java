package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.Map;

/// Custom invoker command `--<namespace>-<name>`, created by [Command#custom].
record CustomCommand(String namespace, String name, Map<String, String> payload)
    implements Command {

  CustomCommand {
    namespace = Fnv1a64.sanitizeIdentifier(namespace);
    name = Fnv1a64.sanitizeIdentifier(name);
    payload.keySet().forEach(Attributes::requireGeneric);
    payload = Map.copyOf(payload);
  }

  @Override
  public String wireValue() {
    return "--" + namespace + "-" + name;
  }
}
